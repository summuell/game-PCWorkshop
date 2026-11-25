package com.gamedraft.preview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardSlotsTest {
    @Test
    fun assembleEveryBoardAndRestoreItsOccupiedSockets() {
        // Full-length PCIe, storage M.2 and DIMM counts from the six manufacturers' spec sheets.
        val catalog = mapOf(
            Product.Z590Master to Triple(3, 3, 4), Product.TufZ590 to Triple(2, 3, 4),
            Product.H570Steel to Triple(2, 3, 4), Product.B560ProVdh to Triple(1, 2, 4),
            Product.B560 to Triple(1, 2, 2), Product.H510 to Triple(1, 1, 2),
        )
        assertEquals(catalog.keys, Product.entries.filter { it.category == ShopCategory.Board }.toSet())
        for ((board, counts) in catalog) {
            var session = RepairSession()
            fun install(product: Product, slot: ComputerPart = product.slot) {
                session = session.buy(product)
                val item = session.inventory.first()
                assertTrue(session.canInstall(item, slot))
                session = session.install(item.id, slot)
                assertEquals(product, session.parts.getValue(slot).product)
                for (index in slot.screws.indices) session = session.turnScrew(slot, index, ScrewAction.Tighten)
            }
            fun fill(product: Product, count: Int) {
                val candidate = InventoryItem(-1, product.slot, product.newPart())
                val sockets = product.slot.compatibleSlots
                assertEquals(sockets.take(count), sockets.filter { session.canInstall(candidate, it) })
                for (slot in sockets.take(count).reversed()) install(product, slot)
                assertFalse(sockets.any { session.canInstall(candidate, it) })
            }
            install(Product.H5Flow)
            install(board)
            install(Product.Psu550)
            install(Product.BoardLead)
            install(Product.I5)
            session = session.applyPasteToMountedCpu()
            install(Product.CpuCooler)
            fill(Product.Dimm16, counts.third)
            fill(Product.Gtx1660, counts.first)
            fill(Product.VideoLead, counts.first)
            fill(Product.Nvme512, counts.second)
            assertEquals(counts.second, session.availableDisks.size)
            val boot = session.availableDisks.last()
            session = session.finishInstallation(boot)
            assertEquals(boot, session.bootDisk)
            assertEquals(null, session.firstStartupFailure())
            assertTrue(session.hardwareReport().any { it.second.contains("${counts.third * 16} GB DDR4") })
            assertEquals(counts.first, session.hardwareReport().count { it.first.startsWith("显示适配器") })
            val stored = session.storeComputer()
            val restored = RepairSession.restore(stored.save()).install(stored.inventory.first().id, ComputerPart.Chassis)
            assertEquals(session.parts, restored.parts)
            assertEquals(session.assembly, restored.assembly)
            assertEquals(boot, restored.bootDisk)
            for (slot in ComputerPart.graphicsLeads.take(counts.first - 1)) session = session.remove(slot)
            assertEquals(null, session.firstStartupFailure())
            session = session.remove(ComputerPart.graphicsLeads[counts.first - 1])
            assertEquals(StartupStage.Video, session.firstStartupFailure())
        }
    }
}
