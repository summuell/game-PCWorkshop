package com.gamedraft.preview

internal data class MountingScrew(val x: Int, val y: Int, val radius: Int = 11)

internal enum class ScrewAction { Loosen, Tighten }

internal enum class ComputerPart(val label: String, val screws: List<MountingScrew> = emptyList()) {
    Chassis("机箱"),
    Motherboard(
        "主板",
        listOf(MountingScrew(622, 387), MountingScrew(228, 710), MountingScrew(213, 940), MountingScrew(538, 942)),
    ),
    Processor("处理器"),
    PowerSupply(
        "电源",
        listOf(MountingScrew(191, 1039), MountingScrew(390, 1039), MountingScrew(191, 1227), MountingScrew(392, 1225)),
    ),
    HardDrive(
        "硬盘",
        listOf(MountingScrew(579, 1056), MountingScrew(768, 1056), MountingScrew(577, 1199), MountingScrew(768, 1207)),
    ),
    SolidStateDrive("固态硬盘", listOf(MountingScrew(493, 898, 10))),
    SolidStateDrive2("固态硬盘", listOf(MountingScrew(493, 898, 10))),
    SolidStateDrive3("固态硬盘", listOf(MountingScrew(493, 898, 10))),
    MemoryLeft("内存条"),
    MemoryRight("内存条"),
    MemoryThird("内存条"),
    MemoryFourth("内存条"),
    Cooler(
        "散热器",
        listOf(MountingScrew(269, 423, 12), MountingScrew(469, 423, 12),
            MountingScrew(270, 641, 12), MountingScrew(469, 641, 12)),
    ),
    GraphicsCard("显卡", listOf(MountingScrew(143, 736, 9))),
    GraphicsCard2("显卡", listOf(MountingScrew(143, 736, 9))),
    GraphicsCard3("显卡", listOf(MountingScrew(143, 736, 9))),
    MotherboardCable("主板供电线"),
    GraphicsCable("显卡供电线"),
    GraphicsCable2("显卡供电线"),
    GraphicsCable3("显卡供电线"),
    DriveCable("硬盘连接线"),
    SidePanel(
        "机箱盖",
        listOf(MountingScrew(183, 320, 14), MountingScrew(790, 320, 14),
            MountingScrew(183, 1204, 14), MountingScrew(790, 1204, 14)),
    );

    val compatibleSlots: List<ComputerPart>
        get() = when (this) {
            in memorySlots -> memorySlots
            in diskSlots -> diskSlots
            in graphicsSlots -> graphicsSlots
            in graphicsLeads -> graphicsLeads
            else -> listOf(this)
        }

    val socketName: String
        get() = when (this) {
            in diskSlots -> "M.2_${diskSlots.indexOf(this) + 1}"
            in graphicsSlots -> "PCIe ${graphicsSlots.indexOf(this) + 1}"
            in graphicsLeads -> "PCIe ${graphicsLeads.indexOf(this) + 1} 供电"
            in memorySlots -> "DIMM ${memorySlots.indexOf(this) + 1}"
            HardDrive -> "SATA"
            else -> label
        }

    val mounts: List<ComputerPart>
        get() = when (this) {
            Processor, in diskSlots, in memorySlots, in graphicsSlots -> listOf(Motherboard)
            Cooler -> listOf(Processor)
            MotherboardCable -> listOf(Motherboard, PowerSupply)
            in graphicsLeads -> listOf(graphicsSlots[graphicsLeads.indexOf(this)], PowerSupply)
            DriveCable -> listOf(HardDrive, PowerSupply, Motherboard)
            Chassis -> emptyList()
            else -> listOf(Chassis)
        }

    companion object {
        val memorySlots = listOf(MemoryLeft, MemoryRight, MemoryThird, MemoryFourth)
        val diskSlots = listOf(SolidStateDrive, SolidStateDrive2, SolidStateDrive3)
        val graphicsSlots = listOf(GraphicsCard, GraphicsCard2, GraphicsCard3)
        val graphicsLeads = listOf(GraphicsCable, GraphicsCable2, GraphicsCable3)
    }

}

internal data class AssemblyBlocker(val part: ComputerPart, val removeFirst: Boolean) {
    val instruction: String
        get() = if (removeFirst) "先拆下${part.label}" else "先装回${part.label}"
}

internal data class ComputerAssembly(
    val installed: Set<ComputerPart> = emptySet(),
    val loosenedScrews: Map<ComputerPart, Set<Int>> = emptyMap(),
) {
    fun isInstalled(part: ComputerPart): Boolean = part in installed

    fun isVisible(part: ComputerPart, throughPanel: Boolean = false): Boolean {
        if (!isInstalled(part)) return false
        if (!throughPanel && isInstalled(ComputerPart.SidePanel)) return part == ComputerPart.SidePanel
        return part != ComputerPart.Processor || !isInstalled(ComputerPart.Cooler)
    }

    fun blockerFor(part: ComputerPart): AssemblyBlocker? {
        if (part != ComputerPart.SidePanel && isInstalled(ComputerPart.SidePanel)) {
            return AssemblyBlocker(ComputerPart.SidePanel, removeFirst = true)
        }
        if (isInstalled(part)) {
            val mountedPart = ComputerPart.entries.lastOrNull { isInstalled(it) && part in it.mounts }
            return mountedPart?.let { AssemblyBlocker(it, removeFirst = true) }
        }
        val emptyMount = part.mounts.firstOrNull { !isInstalled(it) }
        return emptyMount?.let { AssemblyBlocker(it, removeFirst = false) }
    }

    fun hasScrew(part: ComputerPart, index: Int): Boolean =
        isInstalled(part) && index in part.screws.indices && index !in loosenedScrews[part].orEmpty()

    fun screwsRemaining(part: ComputerPart): Int = part.screws.indices.count { hasScrew(part, it) }

    fun screwAction(part: ComputerPart, index: Int): ScrewAction? {
        if (!isVisible(part) || index !in part.screws.indices) return null
        if (!hasScrew(part, index)) return ScrewAction.Tighten
        return ScrewAction.Loosen.takeIf { blockerFor(part) == null }
    }

    fun turnScrew(part: ComputerPart, index: Int, action: ScrewAction): ComputerAssembly {
        if (screwAction(part, index) != action) return this
        val loosened = loosenedScrews[part].orEmpty()
        val updated = when (action) {
            ScrewAction.Loosen -> loosened + index
            ScrewAction.Tighten -> loosened - index
        }
        val updatedScrews = if (updated.isEmpty()) loosenedScrews - part else loosenedScrews + (part to updated)
        return copy(loosenedScrews = updatedScrews)
    }

    fun canRemove(part: ComputerPart): Boolean =
        isVisible(part) && blockerFor(part) == null && screwsRemaining(part) == 0

    fun remove(part: ComputerPart): ComputerAssembly {
        if (!canRemove(part)) return this
        return copy(installed = installed - part, loosenedScrews = loosenedScrews - part)
    }

    fun canInstall(part: ComputerPart): Boolean = !isInstalled(part) && blockerFor(part) == null

    fun install(part: ComputerPart): ComputerAssembly {
        if (!canInstall(part)) return this
        val emptyHoles = part.screws.indices.toSet()
        val updatedScrews = if (emptyHoles.isEmpty()) loosenedScrews else loosenedScrews + (part to emptyHoles)
        return copy(installed = installed + part, loosenedScrews = updatedScrews)
    }

    fun saveScrews(): List<String> = loosenedScrews.flatMap { (part, screws) ->
        screws.map { "Screw|${part.name}|$it" }
    }
}
