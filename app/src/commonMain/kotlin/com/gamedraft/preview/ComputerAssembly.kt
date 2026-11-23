package com.gamedraft.preview

internal data class MountingScrew(val x: Int, val y: Int, val radius: Int = 11)

internal enum class ScrewAction { Loosen, Tighten }

internal enum class ComputerPart(val label: String, val screws: List<MountingScrew> = emptyList()) {
    Motherboard(
        "主板",
        listOf(MountingScrew(622, 387), MountingScrew(228, 710), MountingScrew(213, 940), MountingScrew(538, 942)),
    ),
    Processor("处理器"),
    FrontFanUpper(
        "上风扇",
        listOf(MountingScrew(733, 371, 8), MountingScrew(793, 354, 8),
            MountingScrew(733, 607, 8), MountingScrew(788, 616, 8)),
    ),
    FrontFanLower(
        "下风扇",
        listOf(MountingScrew(733, 652, 8), MountingScrew(792, 648, 8),
            MountingScrew(733, 911, 8), MountingScrew(788, 910, 8)),
    ),
    PowerSupply(
        "电源",
        listOf(MountingScrew(191, 1039), MountingScrew(390, 1039), MountingScrew(191, 1227), MountingScrew(392, 1225)),
    ),
    HardDrive(
        "硬盘",
        listOf(MountingScrew(579, 1056), MountingScrew(768, 1056), MountingScrew(577, 1199), MountingScrew(768, 1207)),
    ),
    SolidStateDrive("固态硬盘", listOf(MountingScrew(493, 898, 10))),
    MemoryLeft("内存条"),
    MemoryRight("内存条"),
    Cooler(
        "散热器",
        listOf(MountingScrew(269, 423, 12), MountingScrew(469, 423, 12),
            MountingScrew(270, 641, 12), MountingScrew(469, 641, 12)),
    ),
    GraphicsCard("显卡", listOf(MountingScrew(143, 736, 9))),
    MotherboardCable("主板供电线"),
    GraphicsCable("显卡供电线"),
    DriveCable("硬盘连接线"),
    SidePanel(
        "机箱盖",
        listOf(MountingScrew(183, 320, 14), MountingScrew(790, 320, 14),
            MountingScrew(183, 1204, 14), MountingScrew(790, 1204, 14)),
    );

    val compatibleSlots: List<ComputerPart>
        get() = when (this) {
            MemoryLeft, MemoryRight -> listOf(MemoryLeft, MemoryRight)
            else -> listOf(this)
        }

    val mounts: List<ComputerPart>
        get() = when (this) {
            Processor, SolidStateDrive, MemoryLeft, MemoryRight, GraphicsCard -> listOf(Motherboard)
            Cooler -> listOf(Processor)
            MotherboardCable -> listOf(Motherboard, PowerSupply)
            GraphicsCable -> listOf(GraphicsCard, PowerSupply)
            DriveCable -> listOf(HardDrive, PowerSupply, Motherboard)
            else -> emptyList()
        }
}

internal data class AssemblyBlocker(val part: ComputerPart, val removeFirst: Boolean) {
    val instruction: String
        get() = if (removeFirst) "先拆下${part.label}" else "先装回${part.label}"
}

internal data class ComputerAssembly(
    val inventory: List<ComputerPart> = emptyList(),
    val loosenedScrews: Map<ComputerPart, Set<Int>> = emptyMap(),
) {
    fun isInstalled(part: ComputerPart): Boolean = part !in inventory

    fun isVisible(part: ComputerPart): Boolean {
        if (!isInstalled(part)) return false
        if (isInstalled(ComputerPart.SidePanel)) return part == ComputerPart.SidePanel
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
        return copy(inventory = listOf(part) + inventory, loosenedScrews = loosenedScrews - part)
    }

    fun canInstall(part: ComputerPart): Boolean = !isInstalled(part) && blockerFor(part) == null

    fun install(part: ComputerPart): ComputerAssembly {
        if (!canInstall(part)) return this
        val emptyHoles = part.screws.indices.toSet()
        val updatedScrews = if (emptyHoles.isEmpty()) loosenedScrews else loosenedScrews + (part to emptyHoles)
        return copy(inventory = inventory - part, loosenedScrews = updatedScrews)
    }

    fun save(): List<String> = inventory.map { it.name } + loosenedScrews.flatMap { (part, screws) ->
        screws.map { "${part.name}:$it" }
    }

    companion object {
        fun restore(entries: List<String>): ComputerAssembly {
            val inventory = entries.filterNot { ':' in it }.map(ComputerPart::valueOf)
            val screws = entries.filter { ':' in it }.groupBy(
                keySelector = { ComputerPart.valueOf(it.substringBefore(':')) },
                valueTransform = { it.substringAfter(':').toInt() },
            )
            return ComputerAssembly(inventory, screws.mapValues { it.value.toSet() })
        }
    }
}
