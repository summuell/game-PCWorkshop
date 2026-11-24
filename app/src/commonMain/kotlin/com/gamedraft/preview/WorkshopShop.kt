package com.gamedraft.preview

internal enum class ShopCategory(val label: String) {
    Tools("工具"), Board("主板"), Cpu("CPU"), Ram("内存"), Graphics("显卡"), Power("电源"), Drives("硬盘"), Cables("线材")
}

internal enum class DriveBrand { Seagate, Kingston }

internal enum class Product(
    val category: ShopCategory,
    val slot: ComputerPart,
    val shopDescription: String,
    val model: String,
    val detail: String,
    val spec: HardwareSpecification = HardwareSpecification(),
    val driveBrand: DriveBrand? = null,
) {
    B560(ShopCategory.Board, ComputerPart.Motherboard, "华硕-B560M-K", "ASUS PRIME B560M-K", "LGA1200 · 2 个 DDR4 插槽"),
    I5(ShopCategory.Cpu, ComputerPart.Processor, "英特尔-酷睿-i5-11400F", "Intel Core i5-11400F", "6 核 12 线程 · LGA1200"),
    Dimm8(ShopCategory.Ram, ComputerPart.MemoryLeft, "金士顿-DDR4-3200-8GB", "DDR4-3200 8 GB", "台式机 DIMM · 单条",
        HardwareSpecification(capacityGb = 8, memoryRateMt = 3200)),
    Dimm16(ShopCategory.Ram, ComputerPart.MemoryLeft, "金士顿-DDR4-3200-16GB", "DDR4-3200 16 GB", "台式机 DIMM · 单条",
        HardwareSpecification(capacityGb = 16, memoryRateMt = 3200)),
    Gtx1660(ShopCategory.Graphics, ComputerPart.GraphicsCard, "七彩虹-NVIDIA-GTX 1660 SUPER-6GB", "GeForce GTX 1660 SUPER", "6 GB GDDR6 · PCIe",
        HardwareSpecification(performanceTier = 2)),
    Psu550(ShopCategory.Power, ComputerPart.PowerSupply, "航嘉-ATX-550W", "ATX 550 W", "额定功率 550 W", HardwareSpecification(powerWatts = 550)),
    Nvme512(ShopCategory.Drives, ComputerPart.SolidStateDrive, "金士顿-SSD-512GB-KC3000", "Kingston KC3000", "512 GB · NVMe SSD",
        HardwareSpecification(performanceTier = 2, capacityGb = 512), DriveBrand.Kingston),
    Hdd1T(ShopCategory.Drives, ComputerPart.HardDrive, "希捷-HDD-1TB-酷鱼", "Seagate BarraCuda", "1000 GB · SATA HDD",
        HardwareSpecification(capacityGb = 1000), DriveBrand.Seagate),
    IronWolf2T(ShopCategory.Drives, ComputerPart.HardDrive, "希捷-HDD-2TB-酷狼", "Seagate IronWolf", "2000 GB · SATA HDD",
        HardwareSpecification(capacityGb = 2000), DriveBrand.Seagate),
    VideoLead(ShopCategory.Cables, ComputerPart.GraphicsCable, "通用-显卡供电线-PCIe", "显卡供电线", "PCIe · 6+2 针"),
    DriveLead(ShopCategory.Cables, ComputerPart.DriveCable, "通用-硬盘连接线-SATA", "硬盘连接线", "SATA 硬盘线组"),
    BoardLead(ShopCategory.Cables, ComputerPart.MotherboardCable, "通用-主板供电线-ATX", "主板供电线", "ATX · 24 针");

    val shopName: String get() = "${category.label}-$shopDescription"

    fun newPart() = CustomerPart(spec, product = this)
}

internal data class Purchase(val number: Int, val product: Product, val received: Boolean = false)

internal data class InventoryItem(val id: Int, val type: ComputerPart, val part: CustomerPart) {
    val label: String get() = when (type) {
        ComputerPart.MemoryLeft, ComputerPart.MemoryRight -> "内存 ${part.specification.capacityGb}G"
        ComputerPart.HardDrive -> "硬盘 ${part.specification.capacityGb / 1000}TB"
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
