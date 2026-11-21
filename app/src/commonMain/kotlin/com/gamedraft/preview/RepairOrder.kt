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

private val requiredStartupParts = listOf(
    ComputerPart.PowerSupply,
    ComputerPart.Motherboard,
    ComputerPart.MotherboardCable,
    ComputerPart.Processor,
    ComputerPart.Cooler,
    ComputerPart.SolidStateDrive,
    ComputerPart.GraphicsCard,
    ComputerPart.GraphicsCable,
)

internal data class DeliveryRequirements(val partsIntact: Boolean, val startsNormally: Boolean) {
    val ready: Boolean
        get() = partsIntact && startsNormally
}

internal data class RepairSession(
    val stage: OrderStage = OrderStage.Available,
    val assembly: ComputerAssembly = ComputerAssembly(),
    val graphicsCableReseated: Boolean = false,
    val parts: Map<ComputerPart, CustomerPart> = StarterOrder.originalParts,
) {
    fun accept(): RepairSession =
        if (stage == OrderStage.Available) RepairSession(stage = OrderStage.Active) else this

    fun remove(part: ComputerPart): RepairSession =
        if (stage == OrderStage.Active) copy(assembly = assembly.remove(part)) else this

    fun install(part: ComputerPart): RepairSession {
        if (stage != OrderStage.Active || !assembly.canInstall(part) || part !in parts) return this
        return copy(
            assembly = assembly.install(part),
            graphicsCableReseated = graphicsCableReseated || part == ComputerPart.GraphicsCable,
        )
    }

    fun turnScrew(part: ComputerPart, index: Int, action: ScrewAction): RepairSession =
        if (stage == OrderStage.Active) copy(assembly = assembly.turnScrew(part, index, action)) else this

    fun deliveryRequirements(): DeliveryRequirements {
        val partsIntact = StarterOrder.originalParts.all { (part, original) ->
            val current = parts[part] ?: return@all false
            assembly.isInstalled(part) && current.working &&
                current.specification.meets(original.specification) &&
                assembly.screwsRemaining(part) == part.screws.size
        }
        val startsNormally = graphicsCableReseated &&
            requiredStartupParts.all(::hasWorkingPart) &&
            (hasWorkingPart(ComputerPart.MemoryLeft) || hasWorkingPart(ComputerPart.MemoryRight))
        return DeliveryRequirements(partsIntact, startsNormally)
    }

    private fun hasWorkingPart(part: ComputerPart): Boolean =
        assembly.isInstalled(part) && parts[part]?.working == true

    fun deliver(): RepairSession =
        if (stage == OrderStage.Active && deliveryRequirements().ready) {
            RepairSession(stage = OrderStage.Delivered)
        } else {
            this
        }

    fun save(): List<String> = listOf(stage.name, graphicsCableReseated.toString()) + assembly.save()

    companion object {
        fun restore(entries: List<String>): RepairSession {
            val stage = OrderStage.entries.firstOrNull { it.name == entries.firstOrNull() } ?: return RepairSession()
            if (stage != OrderStage.Active) return RepairSession(stage = stage)
            return RepairSession(
                stage = stage,
                assembly = ComputerAssembly.restore(entries.drop(2)),
                graphicsCableReseated = entries.getOrNull(1) == "true",
            )
        }
    }
}
