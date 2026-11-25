package com.gamedraft.preview

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ShopCatalogTest {
    @Test
    fun chargesEachPurchaseOnceAndPersistsTheWallet() {
        val fresh = RepairSession()
        assertEquals(StartingBalance, fresh.balance)
        assertTrue(Product.entries.all { it.price > 0 })
        assertTrue(Product.entries.sumOf { it.price } < fresh.balance)

        val cart = Product.entries.fold(fresh, RepairSession::buy)
        assertEquals(Product.entries.size, cart.inventory.size)
        assertEquals(fresh.balance - Product.entries.sumOf { it.price }, cart.balance)

        val case = cart.inventory.first { it.type == ComputerPart.Chassis }
        val stored = cart.install(case.id, ComputerPart.Chassis).storeComputer()
        assertEquals(cart.balance, stored.balance)
        val saved = GameSave("today", stored)
        assertEquals(saved, GameSave.decode(saved.encode()))
        assertEquals(StartingBalance, RepairSession.restore(stored.save().filterNot { it.startsWith("Balance=") }).balance)
        val legacy = RepairSession.restore(listOf("Available", "ShopInventory", "Purchase|1|Air903|false"))
        assertEquals(StartingBalance, legacy.balance)
        assertEquals(Product.Air903, legacy.inventory.single().part.product)

        val empty = fresh.copy(balance = Product.I5.price).buy(Product.I5)
        assertEquals(0, empty.balance)
        assertSame(empty, empty.buy(Product.BoardLead))
        assertEquals(0, RepairSession.restore(empty.save()).balance)
    }

    @Test
    fun respectsDiskInterfacesAndMixedDdr4Rates() {
        var session = RepairSession()
            .withPart(Product.Air903)
            .withPart(Product.H510)
            .withPart(Product.I5)
            .withPart(Product.Cx450)
            .withPart(Product.BoardLead)
            .withPart(Product.Gt1030)
            .withPart(Product.Fury32_3600)
        assertTrue(session.hardwareReport().any { "32 GB DDR4 · 3200 MT/s" in it.second })
        session = session.withPart(Product.ValueRam8, ComputerPart.MemoryRight)
        assertTrue(session.hardwareReport().any { "40 GB DDR4 · 2666 MT/s · 已用插槽 2/2" in it.second })
        session = session.buy(Product.Sa510_500)
        val sata = session.inventory.first()
        assertFalse(session.canInstall(sata, ComputerPart.SolidStateDrive))
        session = session.install(sata.id, ComputerPart.HardDrive)
        assertFalse(ComputerPart.HardDrive in session.availableDisks)
        session = session.withPart(Product.DriveLead).withPart(Product.Pro990_2T)
        assertEquals(listOf(ComputerPart.SolidStateDrive, ComputerPart.HardDrive), session.availableDisks)
        assertTrue(session.hardwareReport().any { "500 GB · SATA SSD" in it.second })
        assertFalse(session.canInstall(InventoryItem(-1, Product.VideoLead.slot, Product.VideoLead.newPart()), ComputerPart.GraphicsCable))
        session = session.applyPasteToMountedCpu().withPart(Product.CpuCooler)
        assertEquals(StartupStage.BootDrive, session.firstStartupFailure())
        session = session.finishInstallation(ComputerPart.HardDrive)
        assertEquals(ComputerPart.HardDrive, session.bootDisk)
        assertNull(session.firstStartupFailure())
    }

    @Test
    fun poweredGpuLoadDeterminesWhetherSupplyCanStartBuild() {
        var session = RepairSession()
            .withPart(Product.Air903)
            .withPart(Product.Z590Master)
            .withPart(Product.Cx450)
            .withPart(Product.BoardLead)
            .withPart(Product.I5)
            .withPart(Product.Dimm16)
            .withPart(Product.Rtx4070)
            .withPart(Product.Rtx4070, ComputerPart.GraphicsCard2)
        assertEquals(StartupStage.Video, session.firstStartupFailure())
        session = session.withPart(Product.VideoLead)
        assertEquals(StartupStage.BootDrive, session.firstStartupFailure())
        session = session.withPart(Product.VideoLead, ComputerPart.GraphicsCable2)
        assertEquals(StartupStage.Power, session.firstStartupFailure())
        // Return a higher-rated unit to the same configured machine.
        val oldSupply = session.parts.getValue(ComputerPart.PowerSupply)
        val upgraded = session.copy(parts = session.parts + (ComputerPart.PowerSupply to Product.Rm750x.newPart()))
        assertEquals(450, oldSupply.specification.powerWatts)
        assertEquals(StartupStage.BootDrive, upgraded.firstStartupFailure())
        assertEquals(2, upgraded.hardwareReport().count { "GDDR6X 12 GB" in it.second })
    }

    private fun RepairSession.withPart(product: Product, slot: ComputerPart = product.slot): RepairSession {
        val purchased = buy(product)
        val installed = purchased.install(purchased.inventory.first().id, slot)
        assertEquals(product, installed.parts.getValue(slot).product)
        return installed
    }
}
