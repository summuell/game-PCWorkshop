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
    const val title = "显卡线路松动"
    const val request = "搬动主机后，显卡供电线接触不良。请重新拔插显卡供电线，再把电脑组装好交还。"

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

internal enum class DeliveryProblemType { UnresolvedFault, MissingParts, LooseScrews, BrokenParts, DowngradedParts }

internal data class DeliveryProblem(val type: DeliveryProblemType, val parts: List<ComputerPart> = emptyList())

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

    fun deliveryProblems(): List<DeliveryProblem> {
        val requiredParts = StarterOrder.originalParts
        val installed = requiredParts.keys.filter { assembly.isInstalled(it) && it in parts }
        return buildList {
            if (!graphicsCableReseated || ComputerPart.GraphicsCable !in installed) {
                add(DeliveryProblem(DeliveryProblemType.UnresolvedFault))
            }
            val missing = requiredParts.keys - installed.toSet()
            if (missing.isNotEmpty()) add(DeliveryProblem(DeliveryProblemType.MissingParts, missing.toList()))

            val unfastened = installed.filter { assembly.screwsRemaining(it) < it.screws.size }
            if (unfastened.isNotEmpty()) add(DeliveryProblem(DeliveryProblemType.LooseScrews, unfastened))

            val broken = installed.filter { !parts.getValue(it).working }
            if (broken.isNotEmpty()) add(DeliveryProblem(DeliveryProblemType.BrokenParts, broken))

            val downgraded = installed.filter { part ->
                !parts.getValue(part).specification.meets(requiredParts.getValue(part).specification)
            }
            if (downgraded.isNotEmpty()) add(DeliveryProblem(DeliveryProblemType.DowngradedParts, downgraded))
        }
    }

    fun deliver(): RepairSession =
        if (stage == OrderStage.Active && deliveryProblems().isEmpty()) {
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
