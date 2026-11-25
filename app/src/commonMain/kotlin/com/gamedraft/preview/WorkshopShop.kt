package com.gamedraft.preview

internal enum class ShopCategory(val label: String) {
    Cases("机箱"), Board("主板"), Cpu("CPU"), Cooling("散热"), Ram("内存"), Graphics("显卡"), Power("电源"),
    Drives("硬盘"), Cables("线材"), Tools("工具")
}

internal enum class CaseStyle { Mesh, Glass, Slats, Solid, Perforated }

internal enum class Product(
    val category: ShopCategory,
    val slot: ComputerPart,
    val shopDescription: String,
    val model: String,
    val detail: String,
    val spec: HardwareSpecification = HardwareSpecification(),
    val caseStyle: CaseStyle? = null,
    val board: BoardLayout? = null,
) {
    Air903(ShopCategory.Cases, ComputerPart.Chassis, "君主-AIR 903 BASE-黑色", "AIR 903", "黑色 · 网孔前板", caseStyle = CaseStyle.Mesh),
    H5Flow(ShopCategory.Cases, ComputerPart.Chassis, "恩杰-H5 Flow-白色", "H5 Flow", "白色 · 玻璃侧板", caseStyle = CaseStyle.Glass),
    North(ShopCategory.Cases, ComputerPart.Chassis, "分形工艺-North-炭黑色", "North", "炭黑色 · 胡桃木格栅", caseStyle = CaseStyle.Slats),
    PureBase500(ShopCategory.Cases, ComputerPart.Chassis, "德商必酷-Pure Base 500-深灰色", "Pure Base 500", "深灰色 · 封闭式面板", caseStyle = CaseStyle.Solid),
    Frame4000D(ShopCategory.Cases, ComputerPart.Chassis, "海盗船-FRAME 4000D-白色", "FRAME 4000D", "白色 · 三角孔前板", caseStyle = CaseStyle.Perforated),
    Z590Master(ShopCategory.Board, ComputerPart.Motherboard, "技嘉-Z590 AORUS MASTER", "Z590 AORUS MASTER",
        BoardLayout.Aorus.description, board = BoardLayout.Aorus),
    TufZ590(ShopCategory.Board, ComputerPart.Motherboard, "华硕-TUF GAMING Z590-PLUS WIFI", "TUF GAMING Z590-PLUS WIFI",
        BoardLayout.Tuf.description, board = BoardLayout.Tuf),
    H570Steel(ShopCategory.Board, ComputerPart.Motherboard, "华擎-H570 Steel Legend", "H570 Steel Legend",
        BoardLayout.Steel.description, board = BoardLayout.Steel),
    B560ProVdh(ShopCategory.Board, ComputerPart.Motherboard, "微星-B560M PRO-VDH", "B560M PRO-VDH",
        BoardLayout.ProVdh.description, board = BoardLayout.ProVdh),
    B560(ShopCategory.Board, ComputerPart.Motherboard, "华硕-PRIME B560M-K", "ASUS PRIME B560M-K",
        BoardLayout.PrimeB560.description, board = BoardLayout.PrimeB560),
    H510(ShopCategory.Board, ComputerPart.Motherboard, "华硕-PRIME H510M-K", "ASUS PRIME H510M-K",
        BoardLayout.PrimeH510.description, board = BoardLayout.PrimeH510),
    I5(ShopCategory.Cpu, ComputerPart.Processor, "英特尔-酷睿-i5-11400F", "Intel Core i5-11400F", "6 核 12 线程 · LGA1200"),
    CpuCooler(ShopCategory.Cooling, ComputerPart.Cooler, "英特尔-LGA1200-原装散热器", "Intel LGA1200", "下压式风冷 · LGA1200"),
    Dimm8(ShopCategory.Ram, ComputerPart.MemoryLeft, "金士顿-DDR4-3200-8GB", "DDR4-3200 8 GB", "台式机 DIMM · 单条",
        HardwareSpecification(capacityGb = 8, memoryRateMt = 3200)),
    Dimm16(ShopCategory.Ram, ComputerPart.MemoryLeft, "金士顿-DDR4-3200-16GB", "DDR4-3200 16 GB", "台式机 DIMM · 单条",
        HardwareSpecification(capacityGb = 16, memoryRateMt = 3200)),
    Gtx1660(ShopCategory.Graphics, ComputerPart.GraphicsCard, "七彩虹-NVIDIA-GTX 1660 SUPER-6GB", "GeForce GTX 1660 SUPER", "6 GB GDDR6 · PCIe",
        HardwareSpecification(performanceTier = 2)),
    Psu550(ShopCategory.Power, ComputerPart.PowerSupply, "航嘉-ATX-550W", "ATX 550 W", "额定功率 550 W", HardwareSpecification(powerWatts = 550)),
    Nvme512(ShopCategory.Drives, ComputerPart.SolidStateDrive, "金士顿-SSD-512GB-KC3000", "Kingston KC3000", "512 GB · NVMe SSD",
        HardwareSpecification(performanceTier = 2, capacityGb = 512)),
    Hdd1T(ShopCategory.Drives, ComputerPart.HardDrive, "希捷-HDD-1TB-酷鱼", "Seagate BarraCuda", "1000 GB · SATA HDD",
        HardwareSpecification(capacityGb = 1000)),
    IronWolf2T(ShopCategory.Drives, ComputerPart.HardDrive, "希捷-HDD-2TB-酷狼", "Seagate IronWolf", "2000 GB · SATA HDD",
        HardwareSpecification(capacityGb = 2000)),
    VideoLead(ShopCategory.Cables, ComputerPart.GraphicsCable, "通用-显卡供电线-PCIe", "显卡供电线", "PCIe · 6+2 针"),
    DriveLead(ShopCategory.Cables, ComputerPart.DriveCable, "通用-硬盘连接线-SATA", "硬盘连接线", "SATA 硬盘线组"),
    BoardLead(ShopCategory.Cables, ComputerPart.MotherboardCable, "通用-主板供电线-ATX", "主板供电线", "ATX · 24 针");

    val shopName: String get() = "${category.label}-$shopDescription"

    fun newPart() = HardwarePart(spec, product = this)
}

internal data class InventoryItem(
    val id: Int,
    val type: ComputerPart,
    val part: HardwarePart,
    val storedComputer: StoredComputer? = null,
) {
    val label: String get() = when (type) {
        in ComputerPart.memorySlots -> "内存 ${part.specification.capacityGb}G"
        ComputerPart.HardDrive -> "硬盘 ${part.specification.capacityGb / 1000}TB"
        ComputerPart.Chassis, ComputerPart.Motherboard -> part.product?.model ?: type.label
        ComputerPart.SidePanel -> part.product?.model?.let { "$it 侧板" } ?: type.label
        else -> type.label
    }
}

internal fun RepairSession.hardwareReport(): List<Pair<String, String>> = buildList {
    fun detected(slot: ComputerPart) = parts[slot]?.takeIf { it.working && assembly.isInstalled(slot) }
    fun model(slot: ComputerPart) = detected(slot)?.let { it.product?.model ?: slot.label }
    model(ComputerPart.Processor)?.let { add("处理器" to it) }
    model(ComputerPart.Motherboard)?.let { add("主板" to it) }
    val modules = ComputerPart.memorySlots.mapNotNull(::detected)
    if (modules.isNotEmpty()) {
        val capacity = modules.sumOf { it.specification.capacityGb }
        val speed = modules.minOf { it.specification.memoryRateMt }
        add("物理内存" to "${capacity} GB DDR4 · ${speed} MT/s · 已用插槽 ${modules.size}/${boardLayout.dimms}")
    }
    for ((index, slot) in ComputerPart.graphicsSlots.withIndex()) {
        model(slot)?.let { add("显示适配器 · PCIe ${index + 1}" to it) }
    }
    for (drive in availableDisks) {
        val device = parts.getValue(drive)
        val bus = if (drive in ComputerPart.diskSlots) "NVMe" else "SATA"
        add(drive.socketName to "${device.product?.model ?: drive.label} · ${device.specification.capacityGb} GB · $bus")
    }
}
