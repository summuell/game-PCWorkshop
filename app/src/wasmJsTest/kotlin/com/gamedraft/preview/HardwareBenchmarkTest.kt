package com.gamedraft.preview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HardwareBenchmarkTest {
    @Test
    fun scoresInstalledComponentsAndKeepsTheSystemDiskInTheComposite() {
        val baseline = build(Product.Gt1030, Product.ValueRam8, Product.Hdd1T)
        val upgraded = build(Product.Rtx4070, Product.Dimm16, Product.Nvme512)
            .mount(Product.Dimm16, ComputerPart.MemoryThird)
        val low = baseline.runHardwareBenchmark()
        val high = upgraded.runHardwareBenchmark()

        assertEquals(low, baseline.runHardwareBenchmark())
        assertEquals(low.section(BenchmarkTest.Processor), high.section(BenchmarkTest.Processor))
        for (test in listOf(BenchmarkTest.Graphics, BenchmarkTest.Memory, BenchmarkTest.Storage)) {
            assertTrue(high.section(test).score > low.section(test).score)
        }
        assertTrue(high.score > low.score)
        assertEquals(2, high.section(BenchmarkTest.Memory).devices.size)

        val extra = upgraded.mount(Product.Pro990_2T, ComputerPart.SolidStateDrive2).runHardwareBenchmark()
        val storage = extra.section(BenchmarkTest.Storage)
        assertEquals(high.score, extra.score)
        assertEquals(2, storage.devices.size)
        assertTrue(storage.devices.first().score!! > storage.devices.last().score!!)
        assertTrue(storage.devices.last().details.contains("3255 MB/s"))

        val secondGpu = upgraded.mount(Product.Rtx3060, ComputerPart.GraphicsCard2)
        val graphics = secondGpu.runHardwareBenchmark().section(BenchmarkTest.Graphics)
        assertEquals(2, graphics.devices.size)
        assertNull(graphics.devices.last().score)
        val powered = secondGpu.mount(Product.VideoLead, ComputerPart.GraphicsCable2).runHardwareBenchmark()
        assertTrue(powered.section(BenchmarkTest.Graphics).devices.last().score!! > 0)
        assertEquals(high.score, powered.score)
    }

    @Test
    fun distinguishesChannelPairingAndReportsAnUnconnectedDrive() {
        val base = build(Product.Gt1030, Product.Dimm8, Product.Nvme512)
        val single = base.mount(Product.Dimm8, ComputerPart.MemoryRight)
        val dual = base.mount(Product.Dimm8, ComputerPart.MemoryThird)
        val mixed = base.mount(Product.ValueRam8, ComputerPart.MemoryThird)
        assertTrue(dual.runHardwareBenchmark().section(BenchmarkTest.Memory).score >
            single.runHardwareBenchmark().section(BenchmarkTest.Memory).score)
        assertTrue(mixed.runHardwareBenchmark().section(BenchmarkTest.Memory).devices.all {
            it.details.contains("2666 MT/s")
        })
        val storage = dual.mount(Product.Hdd1T).runHardwareBenchmark().section(BenchmarkTest.Storage)
        assertNull(storage.devices.last().score)
    }

    private fun build(gpu: Product, memory: Product, disk: Product): RepairSession {
        var session = RepairSession().mount(Product.Air903).mount(Product.Z590Master)
            .mount(Product.I5).applyPasteToMountedCpu().mount(Product.CpuCooler)
            .mount(Product.Psu550).mount(Product.BoardLead).mount(gpu).mount(memory).mount(disk)
        if (gpu.graphics?.powerPins != 0) session = session.mount(Product.VideoLead)
        if (disk.slot == ComputerPart.HardDrive) session = session.mount(Product.DriveLead)
        return session.finishInstallation(disk.slot).also { assertEquals(disk.slot, it.bootDisk) }
    }

    private fun BenchmarkReport.section(test: BenchmarkTest): BenchmarkSection = sections.single { it.test == test }

    private fun RepairSession.mount(product: Product, slot: ComputerPart = product.slot): RepairSession {
        val purchased = buy(product)
        val item = purchased.inventory.first()
        assertTrue(purchased.canInstall(item, slot))
        return purchased.install(item.id, slot)
    }
}
