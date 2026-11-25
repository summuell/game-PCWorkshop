package com.gamedraft.preview

internal enum class ShopCategory(val label: String) {
    Cases("机箱"), Board("主板"), Cpu("CPU"), Cooling("散热"), Ram("内存"), Graphics("显卡"), Power("电源"),
    Drives("硬盘"), Cables("线材"), Tools("工具")
}

internal enum class CaseStyle { Mesh, Glass, Slats, Solid, Perforated }

internal enum class DriveInterface(val label: String) {
    Nvme("M.2 2280 · NVMe"), SataSsd("2.5 英寸 · SATA SSD"), SataHdd("3.5 英寸 · SATA HDD")
}

internal data class DriveModel(val connection: DriveInterface, val readSpeedMb: Int)
internal data class GraphicsModel(val boardWatts: Int, val powerPins: Int, val memoryType: String, val fans: Int = 2)

internal const val StartingBalance = 50_000

internal fun yuan(amount: Int): String =
    "¥" + amount.toString().reversed().chunked(3).joinToString(",").reversed()

internal enum class Product(
    val category: ShopCategory,
    val slot: ComputerPart,
    val shopDescription: String,
    val model: String,
    private val notes: String,
    val price: Int,
    val spec: HardwareSpecification = HardwareSpecification(),
    val caseStyle: CaseStyle? = null,
    val board: BoardLayout? = null,
    val drive: DriveModel? = null,
    val graphics: GraphicsModel? = null,
) {
    Air903(ShopCategory.Cases, ComputerPart.Chassis, "君主-AIR 903 BASE-黑色", "AIR 903", "黑色 · 网孔前板", 399, caseStyle = CaseStyle.Mesh),
    H5Flow(ShopCategory.Cases, ComputerPart.Chassis, "恩杰-H5 Flow-白色", "H5 Flow", "白色 · 玻璃侧板", 499, caseStyle = CaseStyle.Glass),
    North(ShopCategory.Cases, ComputerPart.Chassis, "分形工艺-North-炭黑色", "North", "炭黑色 · 胡桃木格栅", 999, caseStyle = CaseStyle.Slats),
    PureBase500(ShopCategory.Cases, ComputerPart.Chassis, "德商必酷-Pure Base 500-深灰色", "Pure Base 500", "深灰色 · 封闭式面板", 449, caseStyle = CaseStyle.Solid),
    Frame4000D(ShopCategory.Cases, ComputerPart.Chassis, "海盗船-FRAME 4000D-白色", "FRAME 4000D", "白色 · 三角孔前板", 599, caseStyle = CaseStyle.Perforated),
    Z590Master(ShopCategory.Board, ComputerPart.Motherboard, "技嘉-Z590 AORUS MASTER", "Z590 AORUS MASTER",
        BoardLayout.Aorus.description, 1499, board = BoardLayout.Aorus),
    TufZ590(ShopCategory.Board, ComputerPart.Motherboard, "华硕-TUF GAMING Z590-PLUS WIFI", "TUF GAMING Z590-PLUS WIFI",
        BoardLayout.Tuf.description, 999, board = BoardLayout.Tuf),
    H570Steel(ShopCategory.Board, ComputerPart.Motherboard, "华擎-H570 Steel Legend", "H570 Steel Legend",
        BoardLayout.Steel.description, 749, board = BoardLayout.Steel),
    B560ProVdh(ShopCategory.Board, ComputerPart.Motherboard, "微星-B560M PRO-VDH", "B560M PRO-VDH",
        BoardLayout.ProVdh.description, 519, board = BoardLayout.ProVdh),
    B560(ShopCategory.Board, ComputerPart.Motherboard, "华硕-PRIME B560M-K", "ASUS PRIME B560M-K",
        BoardLayout.PrimeB560.description, 449, board = BoardLayout.PrimeB560),
    H510(ShopCategory.Board, ComputerPart.Motherboard, "华硕-PRIME H510M-K", "ASUS PRIME H510M-K",
        BoardLayout.PrimeH510.description, 329, board = BoardLayout.PrimeH510),
    I5(ShopCategory.Cpu, ComputerPart.Processor, "英特尔-酷睿-i5-11400F", "Intel Core i5-11400F", "6 核 12 线程 · LGA1200", 499),
    CpuCooler(ShopCategory.Cooling, ComputerPart.Cooler, "英特尔-LGA1200-原装散热器", "Intel LGA1200", "下压式风冷 · LGA1200", 59),
    Dimm4(ShopCategory.Ram, ComputerPart.MemoryLeft, "英睿达-DDR4-2666-4GB", "Crucial CT4G4DFS8266",
        "台式机 DIMM · 单条", 49, memory(4, 2666)),
    ValueRam8(ShopCategory.Ram, ComputerPart.MemoryLeft, "金士顿-ValueRAM-DDR4-2666-8GB", "Kingston KVR26N19S8/8",
        "台式机 DIMM · 单条", 89, memory(8, 2666)),
    Dimm8(ShopCategory.Ram, ComputerPart.MemoryLeft, "金士顿-FURY Beast-DDR4-3200-8GB", "Kingston KF432C16BB/8",
        "台式机 DIMM · 单条", 119, memory(8, 3200)),
    Fury16_2666(ShopCategory.Ram, ComputerPart.MemoryLeft, "金士顿-FURY Beast-DDR4-2666-16GB", "Kingston KF426C16BB/16",
        "台式机 DIMM · 单条", 169, memory(16, 2666)),
    Dimm16(ShopCategory.Ram, ComputerPart.MemoryLeft, "金士顿-FURY Beast-DDR4-3200-16GB", "Kingston KF432C16BB/16",
        "台式机 DIMM · 单条", 199, memory(16, 3200)),
    Fury32_3200(ShopCategory.Ram, ComputerPart.MemoryLeft, "金士顿-FURY Beast-DDR4-3200-32GB", "Kingston KF432C16BB/32",
        "台式机 DIMM · 单条", 369, memory(32, 3200)),
    Fury32_3600(ShopCategory.Ram, ComputerPart.MemoryLeft, "金士顿-FURY Beast-DDR4-3600-32GB", "Kingston KF436C18BB/32",
        "台式机 DIMM · 单条", 429, memory(32, 3600)),
    Gt1030(ShopCategory.Graphics, ComputerPart.GraphicsCard, "微星-NVIDIA-GT 1030-2GB", "MSI GT 1030 2G LP OC",
        "PCIe 3.0 ×4", 329, graphicsMemory(1, 2), graphics = GraphicsModel(30, 0, "GDDR5", fans = 1)),
    Gtx1660(ShopCategory.Graphics, ComputerPart.GraphicsCard, "七彩虹-NVIDIA-GTX 1660 SUPER-6GB", "七彩虹 战斧 GTX 1660 SUPER",
        "PCIe 3.0 ×16", 699, graphicsMemory(2, 6), graphics = GraphicsModel(125, 8, "GDDR6")),
    Rx6600(ShopCategory.Graphics, ComputerPart.GraphicsCard, "蓝宝石-AMD-RX 6600-8GB", "Sapphire PULSE RX 6600",
        "PCIe 4.0 ×8", 1099, graphicsMemory(3, 8), graphics = GraphicsModel(140, 8, "GDDR6")),
    Rtx3060(ShopCategory.Graphics, ComputerPart.GraphicsCard, "微星-NVIDIA-RTX 3060-12GB", "MSI RTX 3060 VENTUS 2X 12G OC",
        "PCIe 4.0 ×16", 1399, graphicsMemory(4, 12), graphics = GraphicsModel(170, 8, "GDDR6")),
    Rtx4060(ShopCategory.Graphics, ComputerPart.GraphicsCard, "微星-NVIDIA-RTX 4060-8GB", "MSI RTX 4060 VENTUS 2X BLACK 8G OC",
        "PCIe 4.0 ×8", 1899, graphicsMemory(5, 8), graphics = GraphicsModel(115, 8, "GDDR6")),
    Rtx4070(ShopCategory.Graphics, ComputerPart.GraphicsCard, "微星-NVIDIA-RTX 4070-12GB", "MSI RTX 4070 VENTUS 2X 12G OC",
        "PCIe 4.0 ×16", 3299, graphicsMemory(6, 12), graphics = GraphicsModel(200, 8, "GDDR6X")),
    Cx450(ShopCategory.Power, ComputerPart.PowerSupply, "海盗船-ATX-CX450", "Corsair CX450",
        "80 PLUS 铜牌 · 非模组", 239, supply(450)),
    Psu550(ShopCategory.Power, ComputerPart.PowerSupply, "航嘉-ATX-550W", "航嘉 ATX 550 W",
        "非模组", 279, supply(550)),
    Cx650(ShopCategory.Power, ComputerPart.PowerSupply, "海盗船-ATX-CX650", "Corsair CX650",
        "80 PLUS 铜牌 · 非模组", 369, supply(650)),
    Rm750x(ShopCategory.Power, ComputerPart.PowerSupply, "海盗船-ATX-RM750x", "Corsair RM750x",
        "80 PLUS 金牌 · 全模组", 649, supply(750)),
    Rm850x(ShopCategory.Power, ComputerPart.PowerSupply, "海盗船-ATX-RM850x", "Corsair RM850x",
        "80 PLUS 金牌 · 全模组", 759, supply(850)),
    Focus1000(ShopCategory.Power, ComputerPart.PowerSupply, "海韵-ATX-FOCUS GX-1000", "Seasonic FOCUS GX-1000",
        "80 PLUS 金牌 · 全模组", 999, supply(1000)),
    Hdd1T(ShopCategory.Drives, ComputerPart.HardDrive, "希捷-HDD-1TB-酷鱼", "Seagate BarraCuda ST1000DM010",
        "7200 RPM", 219, disk(1, 1000), drive = DriveModel(DriveInterface.SataHdd, 210)),
    Nv2_250(ShopCategory.Drives, ComputerPart.SolidStateDrive, "金士顿-NVMe SSD-250GB-NV2", "Kingston NV2",
        "PCIe 4.0 ×4", 169, disk(3, 250), drive = DriveModel(DriveInterface.Nvme, 3000)),
    Sa510_500(ShopCategory.Drives, ComputerPart.HardDrive, "西部数据-SATA SSD-500GB-蓝盘 SA510", "WD Blue SA510",
        "", 249, disk(2, 500), drive = DriveModel(DriveInterface.SataSsd, 560)),
    Nvme512(ShopCategory.Drives, ComputerPart.SolidStateDrive, "金士顿-NVMe SSD-512GB-KC3000", "Kingston KC3000",
        "PCIe 4.0 ×4", 299, disk(4, 512), drive = DriveModel(DriveInterface.Nvme, 7000)),
    IronWolf2T(ShopCategory.Drives, ComputerPart.HardDrive, "希捷-HDD-2TB-酷狼", "Seagate IronWolf ST2000VN004",
        "5900 RPM", 439, disk(1, 2000), drive = DriveModel(DriveInterface.SataHdd, 180)),
    Kc3000_1T(ShopCategory.Drives, ComputerPart.SolidStateDrive, "金士顿-NVMe SSD-1TB-KC3000", "Kingston KC3000",
        "PCIe 4.0 ×4", 479, disk(4, 1024), drive = DriveModel(DriveInterface.Nvme, 7000)),
    BlueHdd4T(ShopCategory.Drives, ComputerPart.HardDrive, "西部数据-HDD-4TB-蓝盘", "WD Blue WD40EZAX",
        "5400 RPM", 599, disk(1, 4000), drive = DriveModel(DriveInterface.SataHdd, 185)),
    Pro990_2T(ShopCategory.Drives, ComputerPart.SolidStateDrive, "三星-NVMe SSD-2TB-990 PRO", "Samsung 990 PRO",
        "PCIe 4.0 ×4", 999, disk(5, 2000), drive = DriveModel(DriveInterface.Nvme, 7450)),
    VideoLead(ShopCategory.Cables, ComputerPart.GraphicsCable, "通用-显卡供电线-PCIe", "显卡供电线", "PCIe · 6+2 针", 19),
    DriveLead(ShopCategory.Cables, ComputerPart.DriveCable, "通用-硬盘连接线-SATA", "硬盘连接线", "SATA 硬盘线组", 15),
    BoardLead(ShopCategory.Cables, ComputerPart.MotherboardCable, "通用-主板供电线-ATX", "主板供电线", "ATX · 24 针", 25);

    val shopName: String get() = "${category.label}-$shopDescription"

    val detail: String
        get() {
            drive?.let {
                return listOf(it.connection.label, notes, "最高读取 ${it.readSpeedMb} MB/s")
                    .filter(String::isNotEmpty).joinToString(" · ")
            }
            graphics?.let {
                val connector = if (it.powerPins == 0) "插槽供电" else "${it.powerPins} 针供电"
                return "${spec.capacityGb} GB ${it.memoryType} · $notes · ${it.boardWatts} W · $connector"
            }
            if (category == ShopCategory.Power) return "${spec.powerWatts} W · $notes"
            return notes
        }

    fun newPart() = HardwarePart(spec, product = this)
}

private fun memory(capacity: Int, rate: Int) =
    HardwareSpecification(capacityGb = capacity, memoryRateMt = rate)

private fun graphicsMemory(tier: Int, vram: Int) =
    HardwareSpecification(performanceTier = tier, capacityGb = vram)

private fun disk(tier: Int, capacity: Int) =
    HardwareSpecification(performanceTier = tier, capacityGb = capacity)

private fun supply(watts: Int) = HardwareSpecification(powerWatts = watts)

internal data class InventoryItem(
    val id: Int,
    val type: ComputerPart,
    val part: HardwarePart,
    val storedComputer: StoredComputer? = null,
) {
    val label: String get() = when (type) {
        in ComputerPart.memorySlots -> "内存 ${part.specification.capacityGb}G ${part.specification.memoryRateMt}"
        in ComputerPart.diskSlots, ComputerPart.HardDrive -> {
            val capacity = part.specification.capacityGb
            val capacityLabel = if (capacity >= 1000) "${capacity / 1000}TB" else "${capacity}GB"
            val kind = when (part.product?.drive?.connection) {
                DriveInterface.SataSsd -> "SATA SSD"
                DriveInterface.SataHdd -> "HDD"
                DriveInterface.Nvme -> "NVMe"
                null -> type.label
            }
            "$kind $capacityLabel"
        }
        in ComputerPart.graphicsSlots -> part.product?.shopDescription?.substringAfter('-')?.substringAfter('-')
            ?.substringBeforeLast('-') ?: type.label
        ComputerPart.PowerSupply -> "电源 ${part.specification.powerWatts}W"
        ComputerPart.Chassis, ComputerPart.Motherboard -> part.product?.model ?: type.label
        ComputerPart.SidePanel -> part.product?.model?.let { "$it 侧板" } ?: type.label
        else -> type.label
    }
}

private fun graphicsMemoryGb(part: HardwarePart): Int =
    part.specification.capacityGb.takeIf { it > 0 } ?: part.product?.spec?.capacityGb ?: 0

internal fun RepairSession.hardwareReport(): List<Pair<String, String>> = buildList {
    fun detected(slot: ComputerPart) = parts[slot]?.takeIf { it.working && assembly.isInstalled(slot) }
    fun model(slot: ComputerPart) = detected(slot)?.let { it.product?.model ?: slot.label }
    model(ComputerPart.Processor)?.let { add("处理器" to it) }
    model(ComputerPart.Motherboard)?.let { add("主板" to it) }
    val modules = ComputerPart.memorySlots.mapNotNull(::detected)
    if (modules.isNotEmpty()) {
        val capacity = modules.sumOf { it.specification.capacityGb }
        val speed = modules.minOf { it.specification.memoryRateMt }.coerceAtMost(3200)
        add("物理内存" to "${capacity} GB DDR4 · ${speed} MT/s · 已用插槽 ${modules.size}/${boardLayout.dimms}")
    }
    for ((index, slot) in ComputerPart.graphicsSlots.withIndex()) {
        val part = detected(slot)?.takeIf { graphicsPowered(index) } ?: continue
        val graphics = part.product?.graphics
        val vram = if (graphics == null) "" else " · ${graphics.memoryType} ${graphicsMemoryGb(part)} GB"
        add("显示适配器 · PCIe ${index + 1}" to "${part.product?.model ?: slot.label}$vram")
    }
    for (drive in availableDisks) {
        val device = parts.getValue(drive)
        val bus = when (device.product?.drive?.connection) {
            DriveInterface.Nvme -> "NVMe SSD"
            DriveInterface.SataSsd -> "SATA SSD"
            DriveInterface.SataHdd -> "SATA HDD"
            null -> if (drive in ComputerPart.diskSlots) "NVMe" else "SATA"
        }
        add(drive.socketName to "${device.product?.model ?: drive.label} · ${device.specification.capacityGb} GB · $bus")
    }
}
