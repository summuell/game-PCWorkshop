package com.gamedraft.preview

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

// All four consumers (PCB silkscreen, component sprites, drop guides and fasteners)
// use these same physical socket positions.
internal class BoardGeometry(val layout: BoardLayout) {
    val pcb = Rect(155f, 365f, if (layout.dimms == 4) 666f else 640f,
        if (layout.form == "ATX") 988f else 949f)
    val sockets = buildMap {
        for ((i, slot) in ComputerPart.memorySlots.take(layout.dimms).withIndex()) {
            val x = 502f + i * 35f
            put(slot, Rect(x, 395f, x + 29, 655f))
        }
        layout.m2Rows.forEachIndexed { i, y ->
            put(ComputerPart.diskSlots[i], Rect(255f, y - 17f, 495f, y + 17f))
        }
        layout.graphicsRows.forEachIndexed { i, y ->
            put(ComputerPart.graphicsSlots[i], Rect(132f, y - 23f, 661f, y + 23f))
            put(ComputerPart.graphicsLeads[i], Rect(651f, y - 14f, 700f + 15 * i, y + 14f))
        }
    }

    fun screws(slot: ComputerPart): List<MountingScrew> {
        val area = sockets[slot] ?: return slot.screws
        return when (slot) {
            in ComputerPart.diskSlots -> listOf(MountingScrew(area.right.toInt() - 7, area.center.y.toInt(), 8))
            in ComputerPart.graphicsSlots -> listOf(MountingScrew(area.left.toInt() + 10, area.center.y.toInt(), 8))
            else -> slot.screws
        }
    }

    companion object {
        private val layouts = BoardLayout.entries.associateWith(::BoardGeometry)
        operator fun get(layout: BoardLayout) = layouts.getValue(layout)
    }
}

private val BoardInk = Color(0xFF14191E)
private val BoardMetal = Color(0xFFB0BAC2)
private val BoardGold = Color(0xFFC3A46D)
private fun DrawScope.tile(color: Color, x: Int, y: Int, w: Int, h: Int) =
    drawRect(color, Offset(x.toFloat(), y.toFloat()), Size(w.toFloat(), h.toFloat()))

internal fun DrawScope.drawBoard(board: BoardGeometry) {
    val layout = board.layout
    val pcb = board.pcb
    val substrate = when (layout) {
        BoardLayout.Steel -> Color(0xFF424954)
        BoardLayout.PrimeB560, BoardLayout.PrimeH510 -> Color(0xFF443D38)
        else -> Color(0xFF292C33)
    }
    val armor = when (layout) {
        BoardLayout.Aorus -> Color(0xFF545D68)
        BoardLayout.Steel -> Color(0xFFCBD1D1)
        BoardLayout.Tuf -> Color(0xFF65686B)
        BoardLayout.ProVdh -> Color(0xFF484D52)
        else -> Color(0xFF949CA0)
    }
    val accent = when (layout) {
        BoardLayout.Tuf -> Color(0xFFCCA551)
        BoardLayout.Aorus -> Color(0xFFBE8055)
        BoardLayout.Steel -> Color(0xFF8093A2)
        else -> Color(0xFFB3B8BD)
    }
    drawRect(BoardInk, pcb.topLeft, pcb.size)
    drawRect(substrate, pcb.deflate(5f).topLeft, pcb.deflate(5f).size)
    val trace = Color(0xFF616262)
    for (i in 0..14) {
        val x = 196 + i * 7
        tile(trace, x, 650 + i * 2, 3, 47)
        tile(trace, x, 696 + i * 2, 50, 3)
        tile(trace, 516 + i * 6, 675, 2, 28 + i * 5)
    }
    // Rear I/O stack, VRM chokes, eight-pin EPS, 24-pin ATX and the LGA retention frame.
    for (y in 397..590 step 47) {
        tile(BoardMetal, 150, y, 33, 35)
        tile(BoardInk, 151, y + 8, 8, 18)
    }
    for (i in 0..6) {
        tile(BoardInk, 284+i*25, 434, 21, 22)
        tile(BoardMetal, 287+i*25, 437, 15, 14)
        tile(BoardInk, 268, 461+i*25, 20, 21)
        tile(BoardMetal, 271, 464+i*25, 14, 14)
    }
    tile(BoardInk, 183, 372, 52, 20)
    for (i in 0..3) tile(BoardGold, 188+i*12, 377, 5, 8)
    tile(BoardMetal, 304, 479, 141, 154)
    tile(BoardInk, 315, 493, 119, 126)
    for (i in 0..10) for (j in 0..9) tile(BoardGold, 320+i*10, 499+j*12, 3, 4)
    tile(BoardMetal, 445, 495, 5, 130)
    tile(BoardInk, pcb.right.toInt()-19, 470, 16, 132)
    for (i in 0..11) tile(BoardGold, pcb.right.toInt()-14, 474+i*10, 5, 4)

    // Larger models have a rear I/O shroud and heavier VRM heatsinks.
    val vrmWidth = when (layout) {
        BoardLayout.Aorus -> 72
        BoardLayout.Steel, BoardLayout.Tuf -> 62
        BoardLayout.ProVdh -> 46
        BoardLayout.PrimeB560 -> 32
        BoardLayout.PrimeH510 -> 0
    }
    if (vrmWidth > 0) {
        tile(armor, 184, 389, vrmWidth, 221)
        for (y in 399..588 step 16) tile(BoardInk, 190, y, vrmWidth-12, 5)
        tile(accent, 184, 599, vrmWidth, 8)
        if (layout.form == "ATX") {
            tile(armor, 268, 382, 210, 46)
            for (x in 278..456 step 15) tile(BoardInk, x, 387, 5, 30)
        }
    }
    tile(armor, 535, 820, 87, 76)
    for (i in 0..4) tile(accent, 542+i*13, 830+i*4, 7, 40)
    if (layout == BoardLayout.Steel) {
        for (i in 0..4) tile(BoardMetal, 183+i*12, 861+i*10, 35, 9)
    }
    for (i in 0..3) {
        tile(BoardInk, 552+i*18, 909, 15, 23)
        tile(BoardMetal, 555+i*18, 912, 9, 4)
    }
    // Coin cell, audio capacitors and short PCIe connectors flank the expansion area.
    drawCircle(BoardInk, 20f, Offset(555f, 778f))
    drawCircle(BoardMetal, 15f, Offset(555f, 778f))
    tile(armor, 548, 776, 12, 3)
    for (i in 0..4) {
        tile(BoardGold, 164, 774+i*30, 11, 14)
        tile(BoardMetal, 166, 775+i*30, 7, 3)
    }
    for (y in layout.shortPcieRows) {
        tile(Color(0xFF666A70), 181, y-7, 56, 14)
        tile(BoardInk, 185, y-3, 46, 6)
    }
    // The Master uses fin stacks, the TUF has yellow corner markings, and the
    // Steel Legend uses broken light/dark camouflage blocks across the covers.
    when (layout) {
        BoardLayout.Aorus -> {
            for (x in 275..465 step 8) tile(BoardMetal, x, 385, 2, 34)
            for (i in 0..4) tile(accent, 540+i*10, 842-i*3, 10, 6)
        }
        BoardLayout.Tuf -> {
            for (i in 0..3) tile(accent, 191+i*9, 573-i*6, 6, 16)
            tile(accent, 539, 872, 55, 5)
        }
        BoardLayout.Steel -> {
            for (i in 0..3) {
                tile(substrate, 193+(i%2)*17, 420+i*44, 25, 13)
                tile(BoardMetal, 545+(i%2)*31, 826+i*17, 20, 9)
            }
        }
        else -> Unit
    }
    for ((slot, area) in board.sockets) {
        val x = area.left.toInt()
        val y = area.center.y.toInt()
        when (slot) {
            in ComputerPart.memorySlots -> {
                val color = if (ComputerPart.memorySlots.indexOf(slot) % 2 == 0) BoardInk else Color(0xFF52555A)
                drawRect(color, area.topLeft, area.size)
                tile(BoardGold, x+13, area.top.toInt()+12, 3, 226)
                for (end in listOf(area.top, area.bottom-10)) tile(BoardMetal, x+2,end.toInt(),25,9)
            }
            in ComputerPart.diskSlots -> {
                tile(BoardInk, x-3, y-15, 16, 30)
                tile(BoardGold, x+9, y-12, 4, 23)
                for (hole in listOf(418, 451, 488)) {
                    tile(BoardMetal, hole-4, y-4, 8, 8)
                    tile(BoardInk, hole-2, y-2, 4, 4)
                }
            }
            in ComputerPart.graphicsSlots -> {
                tile(if (slot == ComputerPart.GraphicsCard && vrmWidth >= 46) BoardMetal else Color(0xFF54595B),
                    178, y-11, 305, 22)
                tile(BoardInk, 184, y-5, 285, 9)
                tile(BoardGold, 207, y-5, 3, 9)
                tile(BoardMetal, 477, y-8, 12, 15)
            }
            else -> Unit
        }
    }
}

internal fun DrawScope.drawGraphicsLead(slot: ComputerPart, board: BoardGeometry) {
    val area = board.sockets.getValue(slot)
    val i = ComputerPart.graphicsLeads.indexOf(slot)
    val x = area.right - 4
    val y = area.center.y
    val path = listOf(Offset(area.left,y), Offset(x,y), Offset(x,974f + i*8),
        Offset(466f+i*9,1000f+i*7), Offset(449f+i*5,1101f))
    for ((a,b) in path.zipWithNext()) drawLine(BoardInk,a,b,10f)
    for ((a,b) in path.zipWithNext()) drawLine(BoardGold,a,b,3f)
    drawRect(Color(0xFF3C4145), area.topLeft, Size(20f, area.height))
}

@Composable
internal fun BoardThumbnail(layout: BoardLayout, modifier: Modifier) {
    Canvas(modifier) {
        val board = BoardGeometry[layout]
        val scale = minOf(size.width / board.pcb.width, size.height / board.pcb.height)
        withTransform({
            translate(center.x-board.pcb.center.x*scale, center.y-board.pcb.center.y*scale)
            scale(scale, scale, pivot = Offset.Zero)
        }) { drawBoard(board) }
    }
}
