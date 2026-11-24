package com.gamedraft.preview

import kotlin.random.Random

private const val MinimumWorkingDurability = 50

internal enum class OrderStage { Available, Active, Delivered }

internal data class HardwareSpecification(
    val performanceTier: Int = 1,
    val capacityGb: Int = 0,
    val memoryRateMt: Int = 0,
    val powerWatts: Int = 0,
) {
    fun meets(minimum: HardwareSpecification): Boolean =
        performanceTier >= minimum.performanceTier &&
            capacityGb >= minimum.capacityGb &&
            memoryRateMt >= minimum.memoryRateMt &&
            powerWatts >= minimum.powerWatts
}

internal enum class DiskSystem { Blank, Original, Reinstalled }

internal data class CustomerPart(
    val specification: HardwareSpecification,
    val durability: Int = 100,
    val system: DiskSystem = DiskSystem.Blank,
    val product: Product? = null,
) {
    val working: Boolean
        get() = durability >= MinimumWorkingDurability
}

internal object StarterOrder {
    const val title = "重装操作系统"
    const val request = "这台电脑用了好几年，想请你帮我重新装一套干净的系统。"

    private val prototypes = ComputerPart.entries.associateWith { part ->
        val type = if (part == ComputerPart.MemoryRight) ComputerPart.MemoryLeft else part
        Product.entries.firstOrNull { it.slot == type }?.newPart() ?: CustomerPart(HardwareSpecification())
    }

    val originalParts = (prototypes - ComputerPart.SolidStateDrive).mapValues { (slot, part) ->
        if (slot == ComputerPart.HardDrive) part.copy(system = DiskSystem.Original) else part
    }

    fun createParts(savedDurability: Map<String, Int> = emptyMap(), legacy: Boolean = false): Map<ComputerPart, CustomerPart> =
        (if (legacy) prototypes + (ComputerPart.SolidStateDrive to prototypes.getValue(ComputerPart.SolidStateDrive).copy(system = DiskSystem.Original)) else originalParts).mapValues { (part, original) ->
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

internal data class DriveRequirement(val brand: DriveBrand, val capacityGb: Int, val durability: Int) {
    fun accepts(drive: CustomerPart): Boolean =
        drive.working && drive.product?.driveBrand == brand &&
            drive.specification.capacityGb >= capacityGb && drive.durability >= durability

    companion object {
        fun from(drive: CustomerPart) = DriveRequirement(
            requireNotNull(drive.product?.driveBrand), drive.specification.capacityGb, drive.durability,
        )
    }
}

internal data class DeliveryRequirements(
    val partsIntact: Boolean,
    val startsNormally: Boolean,
    val reinstalled: Boolean,
) {
    val ready: Boolean
        get() = partsIntact && startsNormally && reinstalled
}

internal data class RepairSession(
    val stage: OrderStage = OrderStage.Available,
    val assembly: ComputerAssembly = ComputerAssembly(inventory = listOf(ComputerPart.SolidStateDrive)),
    val preferredDisk: ComputerPart = ComputerPart.HardDrive,
    val parts: Map<ComputerPart, CustomerPart> = StarterOrder.originalParts,
    val purchases: List<Purchase> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val driveRequirement: DriveRequirement = DriveRequirement.from(StarterOrder.originalParts.getValue(ComputerPart.HardDrive)),
    private val startupVerifiedFor: HardwareConfiguration? = null,
) {
    private val hardware: HardwareConfiguration
        get() = HardwareConfiguration(assembly, parts, bootDisk)

    private val startupVerified: Boolean
        get() = startupVerifiedFor == hardware

    fun accept(): RepairSession =
        if (stage == OrderStage.Available) {
            val received = StarterOrder.createParts()
            copy(stage = OrderStage.Active, assembly = ComputerAssembly(inventory = listOf(ComputerPart.SolidStateDrive)),
                preferredDisk = ComputerPart.HardDrive, parts = received,
                driveRequirement = DriveRequirement.from(received.getValue(ComputerPart.HardDrive)))
        } else {
            this
        }

    fun buy(product: Product): RepairSession {
        val number = (purchases.lastOrNull()?.number ?: 0) + 1
        return copy(purchases = purchases + Purchase(number, product))
    }

    private fun newItem(type: ComputerPart, part: CustomerPart) =
        InventoryItem((inventory.maxOfOrNull { it.id } ?: 0) + 1, type, part)

    fun receive(number: Int): RepairSession {
        val order = purchases.firstOrNull { it.number == number && !it.received } ?: return this
        return copy(
            purchases = purchases.map { if (it.number == number) it.copy(received = true) else it },
            inventory = listOf(newItem(order.product.slot, order.product.newPart())) + inventory,
        )
    }

    fun remove(part: ComputerPart): RepairSession {
        if (stage != OrderStage.Active || !assembly.canRemove(part)) return this
        val instance = parts[part] ?: return this
        return copy(assembly = assembly.remove(part), parts = parts - part,
            inventory = listOf(newItem(part, instance)) + inventory, startupVerifiedFor = null)
    }

    fun install(itemId: Int, slot: ComputerPart): RepairSession {
        val item = inventory.firstOrNull { it.id == itemId } ?: return this
        if (stage != OrderStage.Active || slot !in item.type.compatibleSlots || !assembly.canInstall(slot)) return this
        return copy(
            assembly = assembly.install(slot),
            parts = parts + (slot to item.part),
            inventory = inventory.filterNot { it.id == itemId },
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
        val requiredPartsIntact = StarterOrder.originalParts.all { (slot, original) ->
            val installed = parts[slot] ?: return@all false
            assembly.isInstalled(slot) && when (slot) {
                ComputerPart.HardDrive -> driveRequirement.accepts(installed)
                else -> installed.specification.meets(original.specification)
            }
        }
        val assemblyIntact = parts.all { (slot, part) ->
            part.working && assembly.screwsRemaining(slot) == slot.screws.size
        }
        return DeliveryRequirements(
            partsIntact = requiredPartsIntact && assemblyIntact,
            startsNormally = startupVerified,
            reinstalled = bootDisk?.let { parts[it]?.system } == DiskSystem.Reinstalled,
        )
    }

    private fun hasWorkingPart(part: ComputerPart): Boolean =
        assembly.isInstalled(part) && parts[part]?.working == true

    fun deliver(): RepairSession =
        if (stage == OrderStage.Active && deliveryRequirements().ready) {
            RepairSession(stage = OrderStage.Delivered, inventory = inventory, purchases = purchases)
        } else {
            this
        }

    fun save(): List<String> = buildList {
        add(stage.name)
        add("SystemOrder")
        add("Preferred="+preferredDisk.name)
        add("ShopInventory")
        add("OriginalDrive|${driveRequirement.brand.name}|${driveRequirement.capacityGb}|${driveRequirement.durability}")
        if (startupVerified) add(StartupVerifiedMarker)
        for ((slot, part) in parts) add("Mounted|${slot.name}|${part.encode()}")
        for (item in inventory) add("Item|${item.id}|${item.type.name}|${item.part.encode()}")
        for (order in purchases) add("Purchase|${order.number}|${order.product.name}|${order.received}")
        addAll(assembly.save())
    }

    companion object {
        fun restore(entries: List<String>): RepairSession {
            val stage = OrderStage.entries.firstOrNull { it.name == entries.firstOrNull() } ?: return RepairSession()
            if ("ShopInventory" in entries) {
                val purchases = mutableListOf<Purchase>()
                val inventory = mutableListOf<InventoryItem>()
                val parts = mutableMapOf<ComputerPart, CustomerPart>()
                var originalDrive: DriveRequirement? = null
                for (entry in entries) {
                    val fields = entry.split('|')
                    when (fields.first()) {
                        "OriginalDrive" -> originalDrive = DriveRequirement(
                            DriveBrand.valueOf(fields[1]), fields[2].toInt(), fields[3].toInt(),
                        )
                        "Purchase" -> purchases += Purchase(fields[1].toInt(), Product.valueOf(fields[2]), fields[3].toBoolean())
                        "Item" -> inventory += InventoryItem(fields[1].toInt(), ComputerPart.valueOf(fields[2]), decodePart(fields, 3))
                        "Mounted" -> parts[ComputerPart.valueOf(fields[1])] = decodePart(fields, 2)
                    }
                }
                val session = RepairSession(stage = stage, parts = parts, purchases = purchases, inventory = inventory,
                    driveRequirement = originalDrive ?: legacyDriveRequirement(parts, inventory),
                    assembly = ComputerAssembly.restore(entries),
                    preferredDisk = ComputerPart.entries.firstOrNull { "Preferred="+it.name in entries } ?: ComputerPart.SolidStateDrive)
                return if (StartupVerifiedMarker in entries) session.recordSuccessfulStartup() else session
            }
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
                parts = StarterOrder.createParts(durability, legacy = true).mapValues { (slot, part) ->
                    val system = DiskSystem.entries.firstOrNull { "System="+slot.name+"="+it.name in entries }
                    if (system == null) part else part.copy(system = system)
                },
            )
            val loose = session.assembly.inventory.mapIndexed { index, type ->
                InventoryItem(index + 1, type, session.parts.getValue(type))
            }
            val mounted = session.parts.filterKeys(session.assembly::isInstalled)
            val migrated = session.copy(parts = mounted, inventory = loose,
                driveRequirement = legacyDriveRequirement(mounted, loose))
            return if (startupVerified) migrated.recordSuccessfulStartup() else migrated
        }
    }
}

// Older saves tracked individual parts but did not identify the intake disk after a swap.
// New stock had 100 durability. Use the lowest plausible original when several disks match.
private fun legacyDriveRequirement(parts: Map<ComputerPart, CustomerPart>, inventory: List<InventoryItem>): DriveRequirement {
    val original = StarterOrder.originalParts.getValue(ComputerPart.HardDrive)
    val candidates = inventory.filter { it.type == ComputerPart.HardDrive }.map { it.part } +
        listOfNotNull(parts[ComputerPart.HardDrive])
    val minimum = candidates.filter { it.product == original.product && it.specification.capacityGb == original.specification.capacityGb }
        .minOfOrNull { it.durability } ?: MinimumWorkingDurability
    return DriveRequirement.from(original).copy(durability = minimum)
}

private fun CustomerPart.encode(): String = with(specification) {
    listOf(product?.name.orEmpty(), durability, system.name, performanceTier, capacityGb, memoryRateMt, powerWatts).joinToString("|")
}

private fun decodePart(fields: List<String>, start: Int): CustomerPart {
    val spec = HardwareSpecification(fields[start+3].toInt(), fields[start+4].toInt(), fields[start+5].toInt(), fields[start+6].toInt())
    return CustomerPart(spec, fields[start+1].toInt(), DiskSystem.valueOf(fields[start+2]), Product.entries.firstOrNull { it.name == fields[start] })
}
