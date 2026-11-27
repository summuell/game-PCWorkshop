package com.gamedraft.preview

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PartMaintenanceTest {
    @Test
    fun repairUsesHalfTheDurabilityAndLocksOutZero() {
        val disk = item(1, Product.Nvme512, 81).let { it.copy(part = it.part.copy(system = DiskSystem.Reinstalled)) }
        val session = RepairSession(inventory = listOf(disk))
        assertEquals(40.5, disk.repairChance)
        val success = session.repairPart(1, FixedRoll(40.49))
        assertEquals(100, success.inventory.single().part.durability)
        assertEquals(DiskSystem.Reinstalled, success.inventory.single().part.system)
        val failure = session.repairPart(1, FixedRoll(40.5))
        assertEquals(0, failure.inventory.single().part.durability)
        val restored = GameSave.decode(GameSave("now", failure).encode()).session
        assertSame(restored, restored.repairPart(1, FixedRoll(0.0)))
    }

    @Test
    fun mergeConsumesOnlyTheMatchingDonorAndPreservesThePrimaryDiskSystem() {
        val primary = item(1, Product.Nvme512, 0).let { it.copy(part = it.part.copy(system = DiskSystem.Reinstalled)) }
        val donor = item(2, Product.Nvme512, 9)
        val differentSize = item(3, Product.Kc3000_1T, 70)
        val wrongCapacity = item(4, Product.Nvme512, 60).let {
            it.copy(part = it.part.copy(specification = it.part.specification.copy(capacityGb = 1024)))
        }
        val wrongModel = item(5, Product.Sa510_500, 100)
        val session = RepairSession(inventory = listOf(primary, donor, differentSize, wrongCapacity, wrongModel))
        assertSame(session, session.mergeParts(1, 1))
        for (id in listOf(3, 4, 5)) assertSame(session, session.mergeParts(1, id))
        assertEquals(16, primary.mergedDurability(donor))
        val merged = session.mergeParts(1, 2)
        assertEquals(listOf(1, 3, 4, 5), merged.inventory.map { it.id })
        assertEquals(16, merged.inventory.first().part.durability)
        assertEquals(DiskSystem.Reinstalled, merged.inventory.first().part.system)
        assertEquals(100, item(6, Product.I5, 90).mergedDurability(item(7, Product.I5, 90)))
        assertEquals(63, item(8, Product.I5, 39).mergedDurability(item(9, Product.I5, 40)))
    }

    @Test
    fun protectsStoredComputersAndSeparatesCasePanels() {
        val loose = item(1, Product.Air903, 80)
        val stored = loose.copy(id = 2, storedComputer = StoredComputer(
            ComputerAssembly(setOf(ComputerPart.Chassis)), mapOf(ComputerPart.Chassis to loose.part), ComputerPart.HardDrive))
        val panel = loose.copy(id = 3, type = ComputerPart.SidePanel)
        val legacy = item(4, Product.Air903, 60).copy(part = loose.part.copy(product = null))
        val session = RepairSession(inventory = listOf(loose, stored, panel, legacy))
        assertSame(session, session.repairPart(2, FixedRoll(0.0)))
        assertNull(loose.mergedDurability(stored))
        assertNull(loose.mergedDurability(panel))
        assertNull(loose.mergedDurability(legacy))
    }

    private fun item(id: Int, product: Product, durability: Int) =
        InventoryItem(id, product.slot, product.newPart().copy(durability = durability))

    private class FixedRoll(private val roll: Double) : Random() {
        override fun nextBits(bitCount: Int): Int = error("Expected one percentage roll")
        override fun nextDouble(until: Double): Double {
            assertEquals(100.0, until)
            return roll
        }
    }
}
