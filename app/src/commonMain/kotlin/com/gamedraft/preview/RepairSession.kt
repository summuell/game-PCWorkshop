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

internal data class RepairSession(
    val assembly: ComputerAssembly = ComputerAssembly(),
    val preferredDisk: ComputerPart = ComputerPart.HardDrive,
    val parts: Map<ComputerPart, HardwarePart> = emptyMap(),
    val purchases: List<Purchase> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
) {
    val chassis: Product? get() = parts[ComputerPart.Chassis]?.product

    fun buy(product: Product): RepairSession {
        val number = (purchases.lastOrNull()?.number ?: 0) + 1
        return copy(purchases = purchases + Purchase(number, product))
    }

    private fun newItem(type: ComputerPart, part: HardwarePart) =
        InventoryItem((inventory.maxOfOrNull { it.id } ?: 0) + 1, type, part)

    fun receive(number: Int): RepairSession {
        val order = purchases.firstOrNull { it.number == number && !it.received } ?: return this
        return copy(
            purchases = purchases.map { if (it.number == number) it.copy(received = true) else it },
            inventory = listOf(newItem(order.product.slot, order.product.newPart())) + inventory,
        )
    }

    fun remove(part: ComputerPart): RepairSession {
        if (!assembly.canRemove(part)) return this
        val instance = parts[part] ?: return this
        val panel = if (part == ComputerPart.Chassis) inventory.firstOrNull {
            it.type == ComputerPart.SidePanel && it.part.product == chassis
        } else null
        val loosePart = if (part == ComputerPart.Processor) instance.copy(hasPaste = false) else instance
        return copy(
            assembly = assembly.remove(part),
            parts = parts - part,
            inventory = listOf(newItem(part, loosePart)) + inventory.filterNot { it.id == panel?.id },
        )
    }

    fun canInstall(item: InventoryItem, slot: ComputerPart): Boolean =
        slot in item.type.compatibleSlots && assembly.canInstall(slot) &&
            (slot != ComputerPart.SidePanel || item.part.product == chassis)

    fun install(itemId: Int, slot: ComputerPart): RepairSession {
        val item = inventory.firstOrNull { it.id == itemId } ?: return this
        if (!canInstall(item, slot)) return this
        val remaining = inventory.filterNot { it.id == itemId }
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
            !hasWorkingPart(ComputerPart.MotherboardCable) -> StartupStage.Power
        !hasWorkingPart(ComputerPart.Motherboard) ||
            !hasWorkingPart(ComputerPart.Processor) ||
            !(hasWorkingPart(ComputerPart.MemoryLeft) || hasWorkingPart(ComputerPart.MemoryRight)) -> StartupStage.SelfTest
        !hasWorkingPart(ComputerPart.GraphicsCard) ||
            !hasWorkingPart(ComputerPart.GraphicsCable) -> StartupStage.Video
        bootDisk == null -> StartupStage.BootDrive
        else -> null
    }

    val availableDisks: List<ComputerPart>
        get() = listOf(ComputerPart.SolidStateDrive, ComputerPart.HardDrive).filter {
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

    private fun hasWorkingPart(part: ComputerPart): Boolean =
        assembly.isInstalled(part) && parts[part]?.working == true

    fun save(): List<String> = buildList {
        add("FreeBuild")
        add("Preferred=" + preferredDisk.name)
        for ((slot, part) in parts) add("Mounted|${slot.name}|${part.encode()}")
        for (item in inventory) add("Item|${item.id}|${item.type.name}|${item.part.encode()}")
        for (order in purchases) add("Purchase|${order.number}|${order.product.name}|${order.received}")
        addAll(assembly.saveScrews())
    }

    companion object {
        fun restore(entries: List<String>): RepairSession {
            val freeBuild = entries.firstOrNull() == "FreeBuild"
            val purchases = mutableListOf<Purchase>()
            val inventory = mutableListOf<InventoryItem>()
            val parts = mutableMapOf<ComputerPart, HardwarePart>()
            val screws = mutableMapOf<ComputerPart, MutableSet<Int>>()
            for (entry in entries) {
                val fields = entry.split('|')
                when (fields.first()) {
                    "Purchase" -> purchases += Purchase(fields[1].toInt(), Product.valueOf(fields[2]), fields[3].toBoolean())
                    "Item" -> {
                        val type = ComputerPart.entries.firstOrNull { it.name == fields[2] } ?: continue
                        if (freeBuild || type != ComputerPart.SidePanel) {
                            inventory += InventoryItem(fields[1].toInt(), type, decodePart(fields, 3))
                        }
                    }
                    "Mounted" -> if (freeBuild) parts[ComputerPart.valueOf(fields[1])] = decodePart(fields, 2)
                    "Screw" -> if (freeBuild) {
                        val slot = ComputerPart.valueOf(fields[1])
                        screws.getOrPut(slot) { mutableSetOf() }.add(fields[2].toInt())
                    }
                }
            }
            return RepairSession(
                assembly = ComputerAssembly(parts.keys.toSet(), screws),
                preferredDisk = ComputerPart.entries.firstOrNull { "Preferred=" + it.name in entries } ?: ComputerPart.HardDrive,
                parts = parts, purchases = purchases, inventory = inventory,
            )
        }
    }
}

private fun HardwarePart.encode(): String = with(specification) {
    listOf(product?.name.orEmpty(), durability, system.name, performanceTier, capacityGb, memoryRateMt, powerWatts, hasPaste).joinToString("|")
}

private fun decodePart(fields: List<String>, start: Int): HardwarePart {
    val spec = HardwareSpecification(fields[start+3].toInt(), fields[start+4].toInt(), fields[start+5].toInt(), fields[start+6].toInt())
    return HardwarePart(spec, fields[start+1].toInt(), DiskSystem.valueOf(fields[start+2]), Product.entries.firstOrNull { it.name == fields[start] },
        fields.getOrNull(start+7)?.toBoolean() ?: false)
}
