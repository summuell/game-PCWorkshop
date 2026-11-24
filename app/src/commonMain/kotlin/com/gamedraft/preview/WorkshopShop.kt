package com.gamedraft.preview

internal enum class ShopCategory(val label: String) {
    Tools("工具"), Board("主板"), Cpu("CPU"), Ram("内存"), Graphics("显卡"), Power("电源"), Drives("硬盘")
}

internal enum class Product(
    val category: ShopCategory,
    val slot: ComputerPart,
    val model: String,
    val detail: String,
    val spec: HardwareSpecification = HardwareSpecification(),
) {
    B560(ShopCategory.Board, ComputerPart.Motherboard, "ASUS PRIME B560M-K", "LGA1200 · 2 个 DDR4 插槽"),
    I5(ShopCategory.Cpu, ComputerPart.Processor, "Intel Core i5-11400F", "6 核 12 线程 · LGA1200"),
    Dimm8(ShopCategory.Ram, ComputerPart.MemoryLeft, "DDR4-3200 8 GB", "台式机 DIMM · 单条",
        HardwareSpecification(capacityGb = 8, memoryRateMt = 3200)),
    Dimm16(ShopCategory.Ram, ComputerPart.MemoryLeft, "DDR4-3200 16 GB", "台式机 DIMM · 单条",
        HardwareSpecification(capacityGb = 16, memoryRateMt = 3200)),
    Gtx1660(ShopCategory.Graphics, ComputerPart.GraphicsCard, "GeForce GTX 1660 SUPER", "6 GB GDDR6 · PCIe",
        HardwareSpecification(performanceTier = 2)),
    Psu550(ShopCategory.Power, ComputerPart.PowerSupply, "ATX 550 W", "额定功率 550 W", HardwareSpecification(powerWatts = 550)),
    Nvme512(ShopCategory.Drives, ComputerPart.SolidStateDrive, "Kingston KC3000", "512 GB · NVMe SSD",
        HardwareSpecification(performanceTier = 2, capacityGb = 512)),
    Hdd1T(ShopCategory.Drives, ComputerPart.HardDrive, "Seagate BarraCuda", "1000 GB · SATA HDD",
        HardwareSpecification(capacityGb = 1000));

    fun newPart() = CustomerPart(spec, product = this)
}

internal data class Purchase(val number: Int, val product: Product, val received: Boolean = false)

internal data class InventoryItem(val id: Int, val type: ComputerPart, val part: CustomerPart) {
    val label: String get() = when (type) {
        ComputerPart.MemoryLeft, ComputerPart.MemoryRight -> "内存 ${part.specification.capacityGb}G"
        else -> type.label
    }
}

internal fun RepairSession.hardwareReport(): List<Pair<String, String>> = buildList {
    fun detected(slot: ComputerPart) = parts[slot]?.takeIf { it.working && assembly.isInstalled(slot) }
    fun model(slot: ComputerPart) = detected(slot)?.let { it.product?.model ?: slot.label }
    model(ComputerPart.Processor)?.let { add("处理器" to it) }
    model(ComputerPart.Motherboard)?.let { add("主板" to it) }
    val modules = listOf(ComputerPart.MemoryLeft, ComputerPart.MemoryRight).mapNotNull(::detected)
    if (modules.isNotEmpty()) {
        val capacity = modules.sumOf { it.specification.capacityGb }
        val speed = modules.minOf { it.specification.memoryRateMt }
        add("物理内存" to "${capacity} GB DDR4 · ${speed} MT/s · 已用插槽 ${modules.size}/2")
    }
    model(ComputerPart.GraphicsCard)?.let { add("显示适配器" to it) }
    for (drive in availableDisks) {
        val device = parts.getValue(drive)
        val bus = if (drive == ComputerPart.SolidStateDrive) "NVMe" else "SATA"
        add(drive.label to "${device.product?.model ?: drive.label} · ${device.specification.capacityGb} GB · $bus")
    }
}
