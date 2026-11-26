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

internal val ChassisBounds = Rect(96f, 227f, 916f, 1312f)
private val Ink = Color(0xFF11151B)
private val Tray = Color(0xFF292F38)
private val Recess = Color(0xFF1B2027)
private val Flange = Color(0xFF414953)

private data class CaseFinish(val frame: Color, val shadow: Color, val edge: Color)

private fun CaseStyle.finish() = when (this) {
    CaseStyle.Mesh -> CaseFinish(Color(0xFF333A43), Color(0xFF222831), Color(0xFF5D6774))
    CaseStyle.Glass -> CaseFinish(Color(0xFFD9DDE1), Color(0xFF9CA7B2), Color(0xFFEFF2F5))
    CaseStyle.Slats -> CaseFinish(Color(0xFF373A3C), Color(0xFF25282A), Color(0xFF666769))
    CaseStyle.Solid -> CaseFinish(Color(0xFF606770), Color(0xFF434951), Color(0xFF8A939E))
    CaseStyle.Perforated -> CaseFinish(Color(0xFFC7CBCD), Color(0xFF858F98), Color(0xFFE8EBED))
}

private fun DrawScope.tile(color: Color, x: Int, y: Int, width: Int, height: Int) =
    drawRect(color, Offset(x.toFloat(), y.toFloat()), Size(width.toFloat(), height.toFloat()))

internal fun DrawScope.drawChassis(style: CaseStyle) {
    val finish = style.finish()

    // Feet, folded steel edges, and the full-width motherboard and drive tray.
    for (x in listOf(140, 706)) {
        tile(Ink, x, 227, 120, 32)
        tile(Ink, x, 1277, 120, 32)
    }
    tile(Ink, 96, 243, 820, 1052)
    tile(Ink, 112, 227, 780, 1078)
    tile(finish.shadow, 104, 249, 800, 1038)
    tile(finish.frame, 112, 235, 775, 1048)
    tile(finish.edge, 120, 244, 746, 9)
    tile(finish.edge, 120, 244, 9, 1020)
    tile(Ink, 139, 277, 696, 982)
    tile(Tray, 147, 285, 677, 964)
    tile(Flange, 147, 285, 677, 8)
    tile(Flange, 147, 952, 677, 13)
    tile(Recess, 169, 365, 483, 578)
    tile(Tray, 178, 374, 465, 560)
    tile(Recess, 189, 420, 260, 246)
    tile(Flange, 189, 420, 260, 7)

    for (screw in ComputerPart.Motherboard.screws) {
        tile(Recess, screw.x - 9, screw.y - 9, 18, 18)
        tile(Color(0xFF9F8052), screw.x - 4, screw.y - 4, 8, 8)
    }
    for (y in 384..912 step 64) {
        tile(Ink, 747, y, 38, 42)
        tile(Flange, 750, y + 39, 35, 4)
    }
    for (y in 702..894 step 32) {
        tile(Flange, 128, y, 32, 19)
        tile(Ink, 134, y + 4, 20, 10)
    }
    for (bay in listOf(Rect(155f, 995f, 446f, 1242f), Rect(528f, 1027f, 790f, 1231f))) {
        drawRect(Flange, bay.topLeft, bay.size)
        val inside = bay.deflate(9f)
        drawRect(Recess, inside.topLeft, inside.size)
    }
    for (part in listOf(ComputerPart.PowerSupply, ComputerPart.HardDrive)) {
        for (screw in part.screws) tile(Flange, screw.x - 6, screw.y - 6, 12, 12)
    }

    // Each fascia has its own surface treatment and controls.
    tile(finish.shadow, 835, 251, 69, 1028)
    tile(finish.frame, 847, 258, 49, 1004)
    tile(finish.edge, 847, 258, 6, 1004)
    when (style) {
        CaseStyle.Mesh -> {
            tile(Ink, 858, 317, 33, 856)
            for (y in 324..1164 step 14) for (x in 863..883 step 10) {
                tile(finish.frame, x, y, 5, 7)
            }
        }
        CaseStyle.Glass -> {
            tile(Ink, 859, 332, 28, 830)
            for (y in 342..1152 step 21) tile(finish.shadow, 861, y, 22, 8)
        }
        CaseStyle.Slats -> {
            tile(Ink, 855, 308, 38, 895)
            val wood = Color(0xFF89674D)
            for (x in 857..885 step 9) {
                tile(wood, x, 312, 5, 887)
                tile(Color(0xFFAD8A66), x, 312, 2, 887)
            }
        }
        CaseStyle.Solid -> {
            tile(finish.edge, 855, 339, 5, 840)
            tile(Recess, 880, 338, 12, 843)
            for (y in 354..1166 step 16) tile(finish.shadow, 881, y, 8, 7)
        }
        CaseStyle.Perforated -> {
            tile(finish.edge, 856, 330, 34, 839)
            for (y in 341..1150 step 21) for (x in 860..875 step 15) {
                tile(finish.shadow, x + 3, y, 4, 4)
                tile(finish.shadow, x + 1, y + 4, 8, 4)
                tile(finish.shadow, x, y + 8, 10, 3)
            }
        }
    }
    tile(Ink, 862, 274, 19, 18)
    tile(finish.edge, 868, 279, 7, 6)
    tile(Ink, 860, 300, 23, 8)
}

internal fun DrawScope.drawCasePanel(style: CaseStyle) {
    val finish = style.finish()
    if (style == CaseStyle.Glass) {
        tile(finish.frame, 153, 294, 669, 43)
        tile(finish.frame, 153, 1198, 669, 48)
        tile(finish.frame, 153, 337, 35, 861)
        tile(finish.frame, 788, 337, 34, 861)
    } else {
        tile(Ink, 153, 294, 669, 952)
        tile(finish.frame, 160, 301, 655, 937)
    }
    tile(finish.edge, 169, 310, 637, 8)
    tile(finish.shadow, 799, 318, 8, 910)
    if (style == CaseStyle.Glass) {
        tile(Color(0x70182630), 188, 337, 600, 861)
        tile(Color(0xFF3C4C57), 193, 344, 6, 848)
        tile(Color(0xFF5B747F), 204, 347, 565, 7)
        tile(Color(0xFF394D59), 204, 361, 390, 5)
    } else {
        tile(finish.shadow, 186, 337, 599, 862)
        tile(finish.frame, 192, 342, 585, 848)
        if (style == CaseStyle.Mesh || style == CaseStyle.Perforated) {
            for (y in 403..441 step 19) for (x in 243..703 step 19) {
                tile(finish.shadow, x, y, 9, 8)
            }
        }
    }
}

@Composable
internal fun CaseShopThumbnail(product: Product, modifier: Modifier) {
    val style = product.caseStyle ?: return
    Canvas(modifier) {
        val scale = minOf(size.height / ChassisBounds.height, size.width / ChassisBounds.width)
        withTransform({
            translate(center.x - ChassisBounds.center.x * scale, center.y - ChassisBounds.center.y * scale)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawChassis(style)
            drawCasePanel(style)
        }
    }
}
