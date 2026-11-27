package com.gamedraft.preview

import kotlin.random.Random

internal val InventoryItem.repairChance: Double
    get() = if (storedComputer == null) part.durability.coerceIn(0, 100) / 2.0 else 0.0

internal fun InventoryItem.mergedDurability(other: InventoryItem): Int? {
    val product = part.product ?: return null
    if (id == other.id || type != other.type || storedComputer != null || other.storedComputer != null ||
        product != other.part.product || part.maintenanceCapacity != other.part.maintenanceCapacity
    ) return null
    val points = part.durability.coerceIn(10, 100) + other.part.durability.coerceIn(10, 100)
    return (points * 80 / 100).coerceAtMost(100)
}

private val HardwarePart.maintenanceCapacity: Int
    get() = specification.capacityGb.takeIf { it > 0 } ?: product?.spec?.capacityGb ?: 0

internal fun RepairSession.repairPart(id: Int, random: Random = Random.Default): RepairSession {
    val item = inventory.firstOrNull { it.id == id } ?: return this
    val chance = item.repairChance
    if (chance <= 0.0) return this
    val durability = if (random.nextDouble(100.0) < chance) 100 else 0
    return copy(inventory = inventory.map { if (it.id == id) it.withDurability(durability) else it })
}

internal fun RepairSession.mergeParts(primaryId: Int, donorId: Int): RepairSession {
    val primary = inventory.firstOrNull { it.id == primaryId } ?: return this
    val donor = inventory.firstOrNull { it.id == donorId } ?: return this
    val durability = primary.mergedDurability(donor) ?: return this
    return copy(inventory = inventory.filterNot { it.id == donorId }.map {
        if (it.id == primaryId) it.withDurability(durability) else it
    })
}

private fun InventoryItem.withDurability(durability: Int): InventoryItem =
    copy(part = part.copy(durability = durability))
