package com.gamedraft.preview

import kotlin.math.roundToInt

internal enum class BenchmarkTest(val label: String, val workload: String, val samples: Int, val weight: Int) {
    Processor("处理器", "单核与多核渲染", 32, 30),
    Graphics("显卡", "3D 场景渲染", 38, 35),
    Memory("内存", "带宽与延迟测试", 24, 20),
    Storage("存储", "顺序读取与随机访问", 30, 15),
}

internal data class DeviceBenchmark(val name: String, val details: String, val score: Int?)
internal data class BenchmarkSection(val test: BenchmarkTest, val score: Int, val devices: List<DeviceBenchmark>)

internal data class BenchmarkReport(val sections: List<BenchmarkSection>) {
    val score: Int = (sections.sumOf { it.score * it.test.weight } / 100.0).roundToInt()
}

internal fun RepairSession.runHardwareBenchmark(): BenchmarkReport {
    val cpu = detectedPart(ComputerPart.Processor)
    val cpuResult = cpu?.let {
        val (single, multi) = when (it.product) {
            Product.I5 -> 1550 to 12_800
            // Old saves may have a specification without a catalog model.
            else -> 1500 to 12_000
        }
        DeviceBenchmark(
            it.product?.model ?: ComputerPart.Processor.label,
            "单核 $single · 多核 $multi",
            (single * 2 + multi * 3) / 5,
        )
    }

    val graphics = ComputerPart.graphicsSlots.mapIndexedNotNull { index, slot ->
        if (!assembly.isInstalled(slot)) return@mapIndexedNotNull null
        val card = parts[slot] ?: return@mapIndexedNotNull null
        val score = if (graphicsPowered(index)) graphicsScore(card) else null
        val vram = graphicsMemoryGb(card)
        val details = listOfNotNull(slot.socketName, if (vram > 0) "$vram GB" else null).joinToString(" · ")
        DeviceBenchmark(card.product?.model ?: slot.label, details, score)
    }

    val memorySlots = ComputerPart.memorySlots.take(boardLayout.dimms)
    val modules = memorySlots.mapNotNull(::detectedPart)
    val capacity = modules.sumOf { it.specification.capacityGb }
    val rate = modules.effectiveMemoryRate()
    // The first half of the slots are on channel A, the rest on channel B.
    val channelA = memorySlots.take(memorySlots.size / 2).mapNotNull(::detectedPart)
        .sumOf { it.specification.capacityGb }
    val pairedCapacity = 2 * minOf(channelA, capacity - channelA)
    val pairedFraction = if (capacity > 0) pairedCapacity.toDouble() / capacity else 0.0
    val memory = memorySlots.mapNotNull { slot ->
        if (!assembly.isInstalled(slot)) return@mapNotNull null
        val module = parts[slot] ?: return@mapNotNull null
        val size = module.specification.capacityGb
        DeviceBenchmark(
            module.product?.model ?: slot.label, "${slot.socketName} · $size GB · $rate MT/s",
            if (module.working) memoryScore(size, rate) else null,
        )
    }

    val detectedDisks = availableDisks
    val systemDisk = bootDisk
    val disks = (ComputerPart.diskSlots + ComputerPart.HardDrive).mapNotNull { slot ->
        if (!assembly.isInstalled(slot)) return@mapNotNull null
        val drive = parts[slot] ?: return@mapNotNull null
        val measured = if (slot in detectedDisks) storageBenchmark(drive, slot) else null
        val label = if (slot == systemDisk) "${slot.socketName} · 系统盘" else slot.socketName
        val speed = measured?.let { " · ${it.first} MB/s" }.orEmpty()
        slot to DeviceBenchmark(
            drive.product?.model ?: slot.label, "$label · ${drive.specification.capacityGb} GB$speed", measured?.second,
        )
    }

    return BenchmarkReport(listOf(
        BenchmarkSection(BenchmarkTest.Processor, cpuResult?.score ?: 0, listOfNotNull(cpuResult)),
        // A desktop workload uses one adapter and one system disk. Extra devices are tested separately.
        BenchmarkSection(BenchmarkTest.Graphics, graphics.firstNotNullOfOrNull { it.score } ?: 0, graphics),
        BenchmarkSection(BenchmarkTest.Memory, memoryScore(capacity, rate, pairedFraction), memory),
        BenchmarkSection(BenchmarkTest.Storage, disks.firstOrNull { it.first == systemDisk }?.second?.score ?: 0,
            disks.map { it.second }),
    ))
}

private fun graphicsScore(card: HardwarePart): Int = when (card.product) {
    Product.Gt1030 -> 1150
    Product.Gtx1660 -> 6250
    Product.Rx6600 -> 8100
    Product.Rtx3060 -> 8900
    Product.Rtx4060 -> 10_550
    Product.Rtx4070 -> 17_600
    else -> card.specification.performanceTier.coerceIn(1, 6) * 1150
}

private fun memoryScore(capacity: Int, rate: Int, pairedFraction: Double = 0.0): Int {
    if (capacity <= 0) return 0
    val bandwidth = rate * 1.3 * (1 + 0.65 * pairedFraction)
    return (bandwidth + minOf(capacity, 32) * 52).roundToInt()
}

private fun RepairSession.storageBenchmark(drive: HardwarePart, slot: ComputerPart): Pair<Int, Int> {
    val model = drive.product?.drive
    val connection = model?.connection ?: if (slot in ComputerPart.diskSlots) DriveInterface.Nvme else DriveInterface.SataHdd
    val readLimit = when (connection) {
        DriveInterface.Nvme -> if (slot == ComputerPart.SolidStateDrive && boardLayout != BoardLayout.PrimeH510) 7600 else 3500
        DriveInterface.SataSsd -> 560
        DriveInterface.SataHdd -> 260
    }
    val baseline = when (connection) {
        DriveInterface.Nvme -> 1800
        DriveInterface.SataSsd -> 460
        DriveInterface.SataHdd -> 160
    }
    val read = (minOf(model?.readSpeedMb ?: baseline, readLimit) * 0.93).roundToInt()
    // SSDs retain an advantage from random I/O even when the sequential read numbers are closer.
    val score = when (connection) {
        DriveInterface.Nvme -> 1200 + read * 7 / 10
        DriveInterface.SataSsd -> 750 + read * 2
        DriveInterface.SataHdd -> 110 + read
    }
    return read to score
}
