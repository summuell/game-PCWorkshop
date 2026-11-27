package com.gamedraft.preview

private const val MinimumWorkingDurability = 50

internal data class HardwareSpecification(
    val performanceTier: Int = 1,
    val capacityGb: Int = 0,
    val memoryRateMt: Int = 0,
    val powerWatts: Int = 0,
)

internal enum class DiskSystem { Blank, Original, Reinstalled }

internal data class HardwarePart(
    val specification: HardwareSpecification,
    val durability: Int = 100,
    val system: DiskSystem = DiskSystem.Blank,
    val product: Product? = null,
    val hasPaste: Boolean = false,
) {
    val working: Boolean
        get() = durability >= MinimumWorkingDurability
}

internal enum class StartupStage(val durationMillis: Long) {
    Power(420),
    SelfTest(640),
    Video(540),
    BootDrive(900),
    SystemLoading(2100),
    Welcome(950),
    Desktop(0),
}

internal data class StoredComputer(
    val assembly: ComputerAssembly,
    val parts: Map<ComputerPart, HardwarePart>,
    val preferredDisk: ComputerPart,
    val detachedPanel: HardwarePart? = null,
)

internal data class RepairSession(
    val assembly: ComputerAssembly = ComputerAssembly(),
    val preferredDisk: ComputerPart = ComputerPart.HardDrive,
    val parts: Map<ComputerPart, HardwarePart> = emptyMap(),
    val warehouse: List<InventoryItem> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val balance: Int = StartingBalance,
) {
    val boardLayout: BoardLayout get() = parts[ComputerPart.Motherboard]?.product?.board ?: BoardLayout.PrimeB560
    val chassis: Product? get() = parts[ComputerPart.Chassis]?.product

    fun buy(product: Product): RepairSession {
        if (balance < product.price) return this
        return receive(product).copy(balance = balance - product.price)
    }

    private fun receive(product: Product): RepairSession =
        copy(inventory = listOf(newItem(product.slot, product.newPart())) + inventory)

    private fun newItem(type: ComputerPart, part: HardwarePart): InventoryItem {
        val largest = maxOf(inventory.maxOfOrNull { it.id } ?: 0, warehouse.maxOfOrNull { it.id } ?: 0)
        return InventoryItem(largest + 1, type, part)
    }

    fun deposit(id: Int): RepairSession {
        val item = inventory.firstOrNull { it.id == id } ?: return this
        return copy(inventory = inventory - item, warehouse = listOf(item) + warehouse)
    }

    fun withdraw(id: Int): RepairSession {
        val item = warehouse.firstOrNull { it.id == id } ?: return this
        return copy(inventory = listOf(item) + inventory, warehouse = warehouse - item)
    }

    fun remove(part: ComputerPart): RepairSession {
        if (!assembly.canRemove(part)) return this
        if (part == ComputerPart.Chassis) return storeComputer()
        val instance = parts[part] ?: return this
        val remaining = parts - part
        val cpu = remaining[ComputerPart.Processor]
        return copy(
            assembly = assembly.remove(part),
            parts = if (part == ComputerPart.Cooler && cpu != null) {
                remaining + (ComputerPart.Processor to cpu.copy(hasPaste = false))
            } else remaining,
            inventory = listOf(newItem(part.compatibleSlots.first(), instance)) + inventory,
        )
    }

    fun storeComputer(): RepairSession {
        val case = parts[ComputerPart.Chassis] ?: return this
        val panel = if (assembly.isInstalled(ComputerPart.SidePanel)) null else inventory.firstOrNull {
            it.type == ComputerPart.SidePanel && it.part.product == case.product
        }
        val snapshot = StoredComputer(assembly, parts, preferredDisk, panel?.part)
        val item = newItem(ComputerPart.Chassis, case).copy(storedComputer = snapshot)
        return RepairSession(
            warehouse = warehouse,
            inventory = listOf(item) + inventory.filterNot { it.id == panel?.id },
            balance = balance,
        )
    }

    fun canInstall(item: InventoryItem, slot: ComputerPart): Boolean =
        slot in item.type.compatibleSlots && boardLayout.supports(slot) && assembly.canInstall(slot) &&
            (slot != ComputerPart.SidePanel || item.part.product == chassis) &&
            (slot !in ComputerPart.graphicsLeads || needsGraphicsLead(ComputerPart.graphicsLeads.indexOf(slot)))

    fun install(itemId: Int, slot: ComputerPart): RepairSession {
        val item = inventory.firstOrNull { it.id == itemId } ?: return this
        if (!canInstall(item, slot)) return this
        val remaining = inventory.filterNot { it.id == itemId }
        item.storedComputer?.let { stored ->
            val panel = stored.detachedPanel?.let { listOf(newItem(ComputerPart.SidePanel, it)) }.orEmpty()
            return copy(
                assembly = stored.assembly,
                parts = stored.parts,
                preferredDisk = stored.preferredDisk,
                inventory = panel + remaining,
            )
        }
        val loosePanel = if (slot == ComputerPart.Chassis) listOf(newItem(ComputerPart.SidePanel, item.part)) else emptyList()
        return copy(
            assembly = assembly.install(slot),
            parts = parts + (slot to item.part),
            inventory = loosePanel + remaining,
        )
    }

    fun turnScrew(part: ComputerPart, index: Int, action: ScrewAction): RepairSession =
        copy(assembly = assembly.turnScrew(part, index, action))

    fun firstStartupFailure(): StartupStage? = when {
        !hasWorkingPart(ComputerPart.PowerSupply) ||
            !hasWorkingPart(ComputerPart.MotherboardCable) ||
            !hasEnoughPower() -> StartupStage.Power
        !hasWorkingPart(ComputerPart.Motherboard) ||
            !hasWorkingPart(ComputerPart.Processor) ||
            ComputerPart.memorySlots.none(::hasWorkingPart) -> StartupStage.SelfTest
        ComputerPart.graphicsSlots.indices.none(::graphicsPowered) -> StartupStage.Video
        bootDisk == null -> StartupStage.BootDrive
        else -> null
    }

    private fun needsGraphicsLead(index: Int): Boolean =
        parts[ComputerPart.graphicsSlots[index]]?.product?.graphics?.powerPins != 0

    fun graphicsPowered(index: Int): Boolean =
        hasWorkingPart(ComputerPart.graphicsSlots[index]) &&
            (!needsGraphicsLead(index) || hasWorkingPart(ComputerPart.graphicsLeads[index]))

    private fun hasEnoughPower(): Boolean {
        val rated = parts[ComputerPart.PowerSupply]?.specification?.powerWatts ?: return true
        if (rated <= 0) return true // Older saves may have no rated wattage.
        // Approximate load for the current CPU, board, cooling and attached devices.
        val baseLoad = if (hasWorkingPart(ComputerPart.Processor)) 110 else 45
        val graphicsLoad = ComputerPart.graphicsSlots.indices.filter(::graphicsPowered).sumOf {
            parts[ComputerPart.graphicsSlots[it]]?.product?.graphics?.boardWatts ?: 125
        }
        val memoryLoad = ComputerPart.memorySlots.count(::hasWorkingPart) * 3
        val diskLoad = availableDisks.map { disk ->
            val connection = parts[disk]?.product?.drive?.connection
            if (connection == DriveInterface.SataHdd || (connection == null && disk == ComputerPart.HardDrive)) 10 else 6
        }.sum()
        return baseLoad + graphicsLoad + memoryLoad + diskLoad <= rated
    }

    val availableDisks: List<ComputerPart>
        get() = (ComputerPart.diskSlots + ComputerPart.HardDrive).filter {
            hasWorkingPart(it) && (it != ComputerPart.HardDrive || hasWorkingPart(ComputerPart.DriveCable))
        }

    val bootDisk: ComputerPart?
        get() = (listOf(preferredDisk) + availableDisks).firstOrNull {
            it in availableDisks && parts.getValue(it).system != DiskSystem.Blank
        }

    fun finishInstallation(disk: ComputerPart): RepairSession {
        if (disk !in availableDisks || !coolingAvailable ||
            firstStartupFailure()?.let { it != StartupStage.BootDrive } == true) return this
        return copy(parts = parts + (disk to parts.getValue(disk).copy(system = DiskSystem.Reinstalled)),
            preferredDisk = disk)
    }

    fun applyPasteToItem(id: Int): RepairSession {
        val cpu = inventory.firstOrNull { it.id == id && it.type == ComputerPart.Processor && !it.part.hasPaste } ?: return this
        return copy(inventory = inventory.map { if (it.id == id) cpu.copy(part = cpu.part.copy(hasPaste = true)) else it })
    }

    fun applyPasteToMountedCpu(): RepairSession {
        val slot = ComputerPart.Processor
        val cpu = parts[slot] ?: return this
        if (!assembly.isVisible(slot) || cpu.hasPaste) return this
        return copy(parts = parts + (slot to cpu.copy(hasPaste = true)))
    }

    val thermalShutdownMillis: Long?
        get() = when {
            !hasWorkingPart(ComputerPart.Cooler) -> 5_000L
            parts[ComputerPart.Processor]?.hasPaste != true -> 10_000L
            else -> null
        }

    val coolingAvailable: Boolean get() = thermalShutdownMillis == null

    fun detectedPart(slot: ComputerPart): HardwarePart? =
        parts[slot]?.takeIf { assembly.isInstalled(slot) && it.working }

    private fun hasWorkingPart(slot: ComputerPart): Boolean = detectedPart(slot) != null

    fun save(): List<String> = buildList {
        add("FreeBuild")
        add("Balance=$balance")
        addAll(StoredComputer(assembly, parts, preferredDisk).encode())
        for ((location, items) in listOf("Item" to inventory, "Warehouse" to warehouse)) {
            for (item in items) {
                add("$location|${item.id}|${item.type.name}|${item.part.encode()}")
                item.storedComputer?.encode()?.forEach { add("Stored|${item.id}|$it") }
            }
        }
    }

    companion object {
        fun restore(entries: List<String>): RepairSession {
            val freeBuild = entries.firstOrNull() == "FreeBuild"
            val pending = mutableListOf<Product>()
            val warehouse = mutableListOf<InventoryItem>()
            val inventory = mutableListOf<InventoryItem>()
            val stored = mutableMapOf<Int, MutableList<String>>()
            for (entry in entries) {
                val fields = entry.split('|')
                when (fields.first()) {
                    "Purchase" -> if (!fields[3].toBoolean()) pending += Product.valueOf(fields[2])
                    "Item", "Warehouse" -> {
                        val type = ComputerPart.entries.firstOrNull { it.name == fields[2] } ?: continue
                        if (freeBuild || type != ComputerPart.SidePanel) {
                            val destination = if (fields.first() == "Item") inventory else warehouse
                            destination += InventoryItem(fields[1].toInt(), type, decodePart(fields, 3))
                        }
                    }
                    "Stored" -> if (freeBuild) stored.getOrPut(fields[1].toInt()) { mutableListOf() }
                        .add(entry.substringAfter('|').substringAfter('|'))
                }
            }
            val active = decodeComputer(if (freeBuild) entries else emptyList())
            fun restoreItem(item: InventoryItem) = item.copy(storedComputer = stored[item.id]?.let(::decodeComputer))
            val session = RepairSession(
                assembly = active.assembly,
                preferredDisk = active.preferredDisk,
                parts = active.parts,
                inventory = inventory.map(::restoreItem),
                warehouse = warehouse.map(::restoreItem),
                balance = entries.firstOrNull { it.startsWith("Balance=") }
                    ?.substringAfter('=')?.toIntOrNull()?.takeIf { it >= 0 } ?: StartingBalance,
            )
            return pending.fold(session) { current, product -> current.receive(product) }
        }
    }
}

private fun StoredComputer.encode(): List<String> = buildList {
    add("Preferred=" + preferredDisk.name)
    for ((slot, part) in parts) add("Mounted|${slot.name}|${part.encode()}")
    addAll(assembly.saveScrews())
    detachedPanel?.let { add("Panel|${it.encode()}") }
}

private fun decodeComputer(entries: List<String>): StoredComputer {
    val parts = mutableMapOf<ComputerPart, HardwarePart>()
    val screws = mutableMapOf<ComputerPart, MutableSet<Int>>()
    var detachedPanel: HardwarePart? = null
    for (entry in entries) {
        val fields = entry.split('|')
        when (fields.first()) {
            "Mounted" -> parts[ComputerPart.valueOf(fields[1])] = decodePart(fields, 2)
            "Screw" -> screws.getOrPut(ComputerPart.valueOf(fields[1])) { mutableSetOf() }.add(fields[2].toInt())
            "Panel" -> detachedPanel = decodePart(fields, 1)
        }
    }
    val preferred = ComputerPart.entries.firstOrNull { "Preferred=" + it.name in entries } ?: ComputerPart.HardDrive
    return StoredComputer(ComputerAssembly(parts.keys.toSet(), screws), parts, preferred, detachedPanel)
}

private fun HardwarePart.encode(): String = with(specification) {
    listOf(product?.name.orEmpty(), durability, system.name, performanceTier, capacityGb, memoryRateMt, powerWatts, hasPaste).joinToString("|")
}

private fun decodePart(fields: List<String>, start: Int): HardwarePart {
    val spec = HardwareSpecification(fields[start+3].toInt(), fields[start+4].toInt(), fields[start+5].toInt(), fields[start+6].toInt())
    return HardwarePart(spec, fields[start+1].toInt(), DiskSystem.valueOf(fields[start+2]), Product.entries.firstOrNull { it.name == fields[start] },
        fields.getOrNull(start+7)?.toBoolean() ?: false)
}
