package com.gamedraft.preview

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

internal data class CustomerPart(
    val specification: HardwareSpecification,
    val working: Boolean = true,
)

internal object StarterOrder {
    const val title = "开机后显示器无信号"
    const val request = "按下电源键后，主机风扇会转，显示器一直提示无信号。"

    val originalParts = ComputerPart.entries.associateWith { part ->
        val specification = when (part) {
            ComputerPart.GraphicsCard -> HardwareSpecification(performanceTier = 2)
            ComputerPart.MemoryLeft, ComputerPart.MemoryRight -> HardwareSpecification(capacityGb = 8, frequencyMhz = 3200)
            ComputerPart.SolidStateDrive -> HardwareSpecification(performanceTier = 2, capacityGb = 512)
            ComputerPart.HardDrive -> HardwareSpecification(capacityGb = 1000)
            ComputerPart.PowerSupply -> HardwareSpecification(powerWatts = 550)
            else -> HardwareSpecification()
        }
        CustomerPart(specification)
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
    val graphicsCableReseated: Boolean,
)

private const val StartupVerifiedMarker = "StartupVerified"

internal data class DeliveryRequirements(val partsIntact: Boolean, val startsNormally: Boolean) {
    val ready: Boolean
        get() = partsIntact && startsNormally
}

internal data class RepairSession(
    val stage: OrderStage = OrderStage.Available,
    val assembly: ComputerAssembly = ComputerAssembly(),
    val graphicsCableReseated: Boolean = false,
    val parts: Map<ComputerPart, CustomerPart> = StarterOrder.originalParts,
    private val startupVerifiedFor: HardwareConfiguration? = null,
) {
    private val hardware: HardwareConfiguration
        get() = HardwareConfiguration(assembly, parts, graphicsCableReseated)

    private val startupVerified: Boolean
        get() = startupVerifiedFor == hardware

    fun accept(): RepairSession =
        if (stage == OrderStage.Available) RepairSession(stage = OrderStage.Active) else this

    fun remove(part: ComputerPart): RepairSession =
        if (stage == OrderStage.Active) withAssembly(assembly.remove(part)) else this

    fun install(part: ComputerPart): RepairSession {
        if (stage != OrderStage.Active || !assembly.canInstall(part) || part !in parts) return this
        return copy(
            assembly = assembly.install(part),
            graphicsCableReseated = graphicsCableReseated || part == ComputerPart.GraphicsCable,
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
            !hasWorkingPart(ComputerPart.Cooler) ||
            !(hasWorkingPart(ComputerPart.MemoryLeft) || hasWorkingPart(ComputerPart.MemoryRight)) -> StartupStage.SelfTest
        !graphicsCableReseated ||
            !hasWorkingPart(ComputerPart.GraphicsCard) ||
            !hasWorkingPart(ComputerPart.GraphicsCable) -> StartupStage.Video
        !hasWorkingPart(ComputerPart.SolidStateDrive) -> StartupStage.BootDrive
        else -> null
    }

    fun recordSuccessfulStartup(): RepairSession =
        if (stage == OrderStage.Active && firstStartupFailure() == null) {
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
        return DeliveryRequirements(partsIntact, startupVerified)
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
        add(graphicsCableReseated.toString())
        if (startupVerified && parts == StarterOrder.originalParts) add(StartupVerifiedMarker)
        addAll(assembly.save())
    }

    companion object {
        fun restore(entries: List<String>): RepairSession {
            val stage = OrderStage.entries.firstOrNull { it.name == entries.firstOrNull() } ?: return RepairSession()
            if (stage != OrderStage.Active) return RepairSession(stage = stage)
            val startupVerified = entries.getOrNull(2) == StartupVerifiedMarker
            val session = RepairSession(
                stage = stage,
                assembly = ComputerAssembly.restore(entries.drop(if (startupVerified) 3 else 2)),
                graphicsCableReseated = entries.getOrNull(1) == "true",
            )
            return if (startupVerified) session.recordSuccessfulStartup() else session
        }
    }
}
