package com.gamedraft.preview

import kotlin.random.Random

private const val MinimumWorkingDurability = 50

internal enum class OrderStage { Available, Active, Delivered }

internal data class HardwareSpecification(
    val performanceTier: Int = 1,
    val capacityGb: Int = 0,
    val frequencyMhz: Int = 0,
    val powerWatts: Int = 0,
) {
    fun meets(minimum: HardwareSpecification): Boolean =
        performanceTier >= minimum.performanceTier &&
            capacityGb >= minimum.capacityGb &&
            frequencyMhz >= minimum.frequencyMhz &&
            powerWatts >= minimum.powerWatts
}

internal enum class DiskSystem { Blank, Original, Reinstalled }

internal data class CustomerPart(
    val specification: HardwareSpecification,
    val durability: Int = 100,
    val system: DiskSystem = DiskSystem.Blank,
) {
    val working: Boolean
        get() = durability >= MinimumWorkingDurability
}

internal object StarterOrder {
    const val title = "重装操作系统"
    const val request = "这台电脑用了好几年，想请你帮我重新装一套干净的系统。"

    val originalParts = ComputerPart.entries.associateWith { part ->
        val specification = when (part) {
            ComputerPart.GraphicsCard -> HardwareSpecification(performanceTier = 2)
            ComputerPart.MemoryLeft, ComputerPart.MemoryRight -> HardwareSpecification(capacityGb = 8, frequencyMhz = 3200)
            ComputerPart.SolidStateDrive -> HardwareSpecification(performanceTier = 2, capacityGb = 512)
            ComputerPart.HardDrive -> HardwareSpecification(capacityGb = 1000)
            ComputerPart.PowerSupply -> HardwareSpecification(powerWatts = 550)
            else -> HardwareSpecification()
        }
        CustomerPart(specification, system = if (part == ComputerPart.SolidStateDrive) DiskSystem.Original else DiskSystem.Blank)
    }

    fun createParts(savedDurability: Map<String, Int> = emptyMap()): Map<ComputerPart, CustomerPart> =
        originalParts.mapValues { (part, original) ->
            original.copy(durability = savedDurability[part.name] ?: Random.nextInt(MinimumWorkingDurability, 101))
        }
}

internal enum class StartupStage(val durationMillis: Long) {
    Power(420),
    SelfTest(640),
    Video(540),
    BootDrive(900),
    SystemLoading(2100),
    Welcome(950),
    Desktop(0),
}

internal data class HardwareConfiguration(
    val assembly: ComputerAssembly,
    val parts: Map<ComputerPart, CustomerPart>,
    val bootDisk: ComputerPart?,
)

private const val StartupVerifiedMarker = "StartupVerified"
private const val DurabilityPrefix = "Durability:"

internal data class DeliveryRequirements(val partsIntact: Boolean, val startsNormally: Boolean, val reinstalled: Boolean) {
    val ready: Boolean
        get() = partsIntact && startsNormally && reinstalled
}

internal data class RepairSession(
    val stage: OrderStage = OrderStage.Available,
    val assembly: ComputerAssembly = ComputerAssembly(),
    val preferredDisk: ComputerPart = ComputerPart.SolidStateDrive,
    val parts: Map<ComputerPart, CustomerPart> = StarterOrder.originalParts,
    private val startupVerifiedFor: HardwareConfiguration? = null,
) {
    private val hardware: HardwareConfiguration
        get() = HardwareConfiguration(assembly, parts, bootDisk)

    private val startupVerified: Boolean
        get() = startupVerifiedFor == hardware

    fun accept(): RepairSession =
        if (stage == OrderStage.Available) {
            RepairSession(stage = OrderStage.Active, parts = StarterOrder.createParts())
        } else {
            this
        }

    fun remove(part: ComputerPart): RepairSession =
        if (stage == OrderStage.Active) withAssembly(assembly.remove(part)) else this

    fun install(part: ComputerPart, slot: ComputerPart = part): RepairSession {
        if (stage != OrderStage.Active || part !in assembly.inventory || part !in parts ||
            slot !in part.compatibleSlots || !assembly.canInstall(slot)) return this
        // Slots also identify inventory entries. Move the other loose stick to the vacated key.
        val installedParts = if (part == slot) parts else {
            (parts - part - slot).toMutableMap().apply {
                put(slot, parts.getValue(part))
                parts[slot]?.let { put(part, it) }
            }
        }
        return copy(
            assembly = assembly.install(slot),
            parts = installedParts,
            startupVerifiedFor = null,
        )
    }

    fun turnScrew(part: ComputerPart, index: Int, action: ScrewAction): RepairSession =
        if (stage == OrderStage.Active) withAssembly(assembly.turnScrew(part, index, action)) else this

    private fun withAssembly(updated: ComputerAssembly): RepairSession =
        if (updated == assembly) this else copy(assembly = updated, startupVerifiedFor = null)

    fun firstStartupFailure(): StartupStage? = when {
        !hasWorkingPart(ComputerPart.PowerSupply) ||
            !hasWorkingPart(ComputerPart.MotherboardCable) -> StartupStage.Power
        !hasWorkingPart(ComputerPart.Motherboard) ||
            !hasWorkingPart(ComputerPart.Processor) ||
            !(hasWorkingPart(ComputerPart.MemoryLeft) || hasWorkingPart(ComputerPart.MemoryRight)) -> StartupStage.SelfTest
        !hasWorkingPart(ComputerPart.GraphicsCard) ||
            !hasWorkingPart(ComputerPart.GraphicsCable) -> StartupStage.Video
        bootDisk == null -> StartupStage.BootDrive
        else -> null
    }

    val availableDisks: List<ComputerPart>
        get() = listOf(ComputerPart.SolidStateDrive, ComputerPart.HardDrive).filter {
            hasWorkingPart(it) && (it != ComputerPart.HardDrive || hasWorkingPart(ComputerPart.DriveCable))
        }

    val bootDisk: ComputerPart?
        get() = (listOf(preferredDisk) + availableDisks).firstOrNull {
            it in availableDisks && parts.getValue(it).system != DiskSystem.Blank
        }

    fun finishInstallation(disk: ComputerPart): RepairSession {
        if (stage != OrderStage.Active || disk !in availableDisks || !coolingAvailable ||
            firstStartupFailure()?.let { it != StartupStage.BootDrive } == true) return this
        return copy(parts = parts + (disk to parts.getValue(disk).copy(system = DiskSystem.Reinstalled)),
            preferredDisk = disk, startupVerifiedFor = null)
    }

    val coolingAvailable: Boolean get() = hasWorkingPart(ComputerPart.Cooler)

    fun recordSuccessfulStartup(): RepairSession =
        if (stage == OrderStage.Active && coolingAvailable && firstStartupFailure() == null) {
            copy(startupVerifiedFor = hardware)
        } else {
            this
        }

    fun deliveryRequirements(): DeliveryRequirements {
        val partsIntact = StarterOrder.originalParts.all { (part, original) ->
            val current = parts[part] ?: return@all false
            assembly.isInstalled(part) && current.working &&
                current.specification.meets(original.specification) &&
                assembly.screwsRemaining(part) == part.screws.size
        }
        return DeliveryRequirements(partsIntact, startupVerified, bootDisk?.let { parts[it]?.system } == DiskSystem.Reinstalled)
    }

    private fun hasWorkingPart(part: ComputerPart): Boolean =
        assembly.isInstalled(part) && parts[part]?.working == true

    fun deliver(): RepairSession =
        if (stage == OrderStage.Active && deliveryRequirements().ready) {
            RepairSession(stage = OrderStage.Delivered)
        } else {
            this
        }

    fun save(): List<String> = buildList {
        add(stage.name)
        add("SystemOrder")
        add("Preferred="+preferredDisk.name)
        for (disk in listOf(ComputerPart.SolidStateDrive, ComputerPart.HardDrive)) {
            parts[disk]?.let { add("System="+disk.name+"="+it.system.name) }
        }
        val originalSpecifications = parts.keys == StarterOrder.originalParts.keys && parts.all { (slot, part) ->
            part.specification == StarterOrder.originalParts.getValue(slot).specification
        }
        if (startupVerified && originalSpecifications) add(StartupVerifiedMarker)
        for ((slot, part) in parts) add("$DurabilityPrefix${slot.name}:${part.durability}")
        addAll(assembly.save())
    }

    companion object {
        fun restore(entries: List<String>): RepairSession {
            val stage = OrderStage.entries.firstOrNull { it.name == entries.firstOrNull() } ?: return RepairSession()
            if (stage != OrderStage.Active) return if (entries.getOrNull(1) == "SystemOrder") RepairSession(stage = stage) else RepairSession()
            val currentOrder = entries.getOrNull(1) == "SystemOrder"
            val startupVerified = currentOrder && StartupVerifiedMarker in entries
            val (durabilityEntries, assemblyEntries) = entries.drop(2)
                .partition { it.startsWith(DurabilityPrefix) }
            val durability = durabilityEntries.mapNotNull { entry ->
                val value = entry.substringAfterLast(':').toIntOrNull()?.takeIf { it in 0..100 }
                    ?: return@mapNotNull null
                entry.removePrefix(DurabilityPrefix).substringBefore(':') to value
            }.toMap()
            val session = RepairSession(
                stage = stage,
                assembly = ComputerAssembly.restore(assemblyEntries),
                preferredDisk = ComputerPart.entries.firstOrNull { "Preferred="+it.name in entries } ?: ComputerPart.SolidStateDrive,
                parts = StarterOrder.createParts(durability).mapValues { (slot, part) ->
                    val system = DiskSystem.entries.firstOrNull { "System="+slot.name+"="+it.name in entries }
                    if (system == null) part else part.copy(system = system)
                },
            )
            return if (startupVerified) session.recordSuccessfulStartup() else session
        }
    }
}
