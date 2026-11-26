package com.gamedraft.preview

internal interface SaveStorage {
    fun read(slot: Int): String?
    fun write(slot: Int, value: String)
    fun timestamp(): String
}

internal data class GameSave(val savedAt: String, val session: RepairSession) {
    fun encode(): String = (listOf("PCWorkshop/1", savedAt) + session.save()).joinToString("\n")

    companion object {
        fun decode(value: String): GameSave {
            val lines = value.lines()
            require(lines.size >= 4 && lines[0] == "PCWorkshop/1" && lines[1].isNotBlank())
            val data = lines.drop(2)
            val legacy = data.first() in setOf("Available", "Active", "Delivered") && "ShopInventory" in data
            require(data.first() == "FreeBuild" || legacy)
            return GameSave(lines[1], RepairSession.restore(data))
        }
    }
}
