package com.gamedraft.preview

internal enum class ComputerPart(val label: String) {
    Motherboard("主板"),
    Processor("处理器"),
    FrontFanUpper("上风扇"),
    FrontFanLower("下风扇"),
    PowerSupply("电源"),
    HardDrive("硬盘"),
    SolidStateDrive("固态硬盘"),
    MemoryLeft("内存 A"),
    MemoryRight("内存 B"),
    Cooler("散热器"),
    GraphicsCard("显卡"),
    MotherboardCable("主板供电线"),
    GraphicsCable("显卡供电线"),
    DriveCable("硬盘连接线"),
    SidePanel("机箱盖");

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

internal data class ComputerAssembly(val inventory: List<ComputerPart> = emptyList()) {
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

    fun toggle(part: ComputerPart): ComputerAssembly {
        if (blockerFor(part) != null) return this
        val updatedInventory = if (isInstalled(part)) listOf(part) + inventory else inventory - part
        return copy(inventory = updatedInventory)
    }
}
