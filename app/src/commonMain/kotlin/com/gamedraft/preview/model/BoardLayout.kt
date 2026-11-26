package com.gamedraft.preview

// Positions use the workbench artwork grid. A graphics row denotes a full-length connector,
// independent of its electrical lane count. M.2 rows are storage sockets (Key M).
internal enum class BoardLayout(
    val form: String,
    val dimms: Int,
    val graphicsRows: List<Int>,
    val m2Rows: List<Int>,
    val lanes: String,
    val shortPcieRows: List<Int>,
) {
    Aorus("ATX", 4, listOf(736, 843, 954), listOf(682, 794, 899), "x16/x0 或 x8/x8，末槽 x4", emptyList()),
    Tuf("ATX", 4, listOf(740, 917), listOf(685, 804, 858), "x16 + x4", listOf(797, 860)),
    Steel("ATX", 4, listOf(740, 856), listOf(687, 801, 922), "x16 + x4", listOf(800, 912, 951)),
    ProVdh("mATX", 4, listOf(746), listOf(687, 868), "x16", listOf(825, 896)),
    PrimeB560("mATX", 2, listOf(748), listOf(686, 891), "x16", listOf(824, 887)),
    PrimeH510("mATX", 2, listOf(754), listOf(695), "x16", listOf(861));

    val description: String get() = "$form · LGA1200 · DDR4 ×$dimms · M.2 ×${m2Rows.size} · 全长 PCIe ×${graphicsRows.size} ($lanes)"

    fun supports(slot: ComputerPart): Boolean = when (slot) {
        in ComputerPart.memorySlots -> ComputerPart.memorySlots.indexOf(slot) < dimms
        in ComputerPart.diskSlots -> ComputerPart.diskSlots.indexOf(slot) < m2Rows.size
        in ComputerPart.graphicsSlots -> ComputerPart.graphicsSlots.indexOf(slot) < graphicsRows.size
        in ComputerPart.graphicsLeads -> ComputerPart.graphicsLeads.indexOf(slot) < graphicsRows.size
        else -> true
    }
}
