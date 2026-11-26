package com.gamedraft.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

private val Ink = Color(0xFF151B1E)
private val DarkMetal = Color(0xFF283039)
private val Steel = Color(0xFFADB5B9)
private val Gold = Color(0xFFC8A359)
private val Label = Color(0xFFE3E4DD)
private val Blue = Color(0xFF4085B1)
private val Green = Color(0xFF366D56)
private val Red = Color(0xFFAE4543)

internal val Product.hasComponentArtwork: Boolean
    get() = category in listOf(ShopCategory.Drives, ShopCategory.Ram, ShopCategory.Graphics, ShopCategory.Power)

internal fun DrawScope.drawComponent(product: Product, bounds: Rect) {
    // Each sprite uses an integer grid with the aspect ratio of its mounting area.
    val (width, height) = when (product.category) {
        ShopCategory.Ram -> 16f to 128f
        ShopCategory.Graphics -> 128f to 32f
        ShopCategory.Power -> 96f to 80f
        ShopCategory.Drives -> when (product.drive?.connection) {
            DriveInterface.Nvme -> 96f to 26f
            else -> 96f to 68f
        }
        else -> return
    }
    withTransform({
        translate(bounds.left, bounds.top)
        scale(bounds.width / width, bounds.height / height, pivot = Offset.Zero)
    }) {
        when (product.category) {
            ShopCategory.Ram -> drawMemory(product)
            ShopCategory.Graphics -> drawGraphics(product)
            ShopCategory.Power -> drawSupply(product)
            ShopCategory.Drives -> drawDrive(product)
            else -> Unit
        }
    }
}

private fun DrawScope.pixels(color: Color, x: Int, y: Int, w: Int, h: Int) =
    drawRect(color, Offset(x.toFloat(), y.toFloat()), Size(w.toFloat(), h.toFloat()))

private fun DrawScope.drawMemory(product: Product) {
    val bare = product == Product.Dimm4 || product == Product.ValueRam8
    pixels(Ink, 1, 1, 14, 126)
    pixels(if (bare) Green else DarkMetal, 2, 2, 11, 124)
    for (y in 5..122 step 4) if (y !in 61..66) pixels(Gold, 13, y, 2, 2)
    if (bare) {
        for (y in 9..112 step 14) pixels(Ink, 3, y, 8, 9)
        pixels(Label, 3, if (product == Product.Dimm4) 18 else 76, 7, 23)
    } else {
        val edge = when (product) {
            Product.Dimm8 -> Color(0xFF83898C)
            Product.Fury16_2666 -> Color(0xFF5D7078)
            Product.Dimm16 -> Steel
            Product.Fury32_3200 -> Color(0xFF909DA9)
            else -> Color(0xFFE0E3E7)
        }
        pixels(edge, 1, 8, 2, 108)
        for (y in 12..105 step 25) {
            pixels(Ink, 3, y, 6, 18)
            pixels(edge, 4, y + 2, 1, 13)
        }
        pixels(Label, 6, 55, 3, 17)
    }
    pixels(Ink, 12, 62, 4, 4)
}

private fun DrawScope.drawDrive(product: Product) {
    when (product.drive?.connection) {
        DriveInterface.Nvme -> {
            val board = if (product == Product.Nv2_250) Blue else DarkMetal
            pixels(Ink, 1, 1, 91, 24)
            pixels(board, 3, 2, 87, 22)
            for (x in 10..65 step 14) pixels(Ink, x, 5, 10, 16)
            val labelColor = if (product == Product.Nv2_250) Label else Color(0xFF555C61)
            pixels(labelColor, 28, 5, 49, 16)
            pixels(if (product == Product.Pro990_2T) Red else Steel, 30, 8, 17, 3)
            pixels(labelColor.copy(alpha = 0.55f), 51, 14, 18, 3)
            for (y in 3..23 step 3) if (y != 18) pixels(Gold, 2, y, 7, 2)
            pixels(Ink, 84, 9, 9, 8)
            pixels(Steel, 86, 10, 4, 6)
        }
        DriveInterface.SataSsd -> {
            // The smaller 2.5-inch drive is seated in the existing chassis tray.
            pixels(DarkMetal, 7, 4, 83, 59)
            pixels(Steel, 9, 5, 4, 56)
            pixels(Steel, 85, 5, 4, 56)
            pixels(Ink, 5, 12, 75, 45)
            pixels(Color(0xFF777E83), 8, 14, 68, 39)
            pixels(Label, 18, 19, 51, 29)
            pixels(Blue, 18, 19, 51, 12)
            pixels(DarkMetal, 22, 38, 25, 3)
            pixels(Gold, 3, 25, 5, 13)
        }
        DriveInterface.SataHdd -> {
            val stripe = when (product) {
                Product.IronWolf2T -> Red
                Product.BlueHdd4T -> Blue
                else -> Green
            }
            pixels(Ink, 5, 1, 88, 66)
            pixels(Steel, 8, 2, 83, 62)
            pixels(Color(0xFF798389), 12, 6, 75, 54)
            pixels(Color(0xFFBBC1C3), 17, 9, 65, 48)
            pixels(Label, 21, 12, 57, 26)
            pixels(stripe, 21, 12, 57, 8)
            for (y in 24..34 step 4) pixels(DarkMetal, 26, y, 18 + (y % 3) * 5, 1)
            for (x in 59..73 step 3) pixels(Ink, x, 26, 1, 9)
            pixels(DarkMetal, 2, 25, 9, 17)
            pixels(Gold, 2, 28, 5, 10)
        }
        null -> Unit
    }
}

private fun DrawScope.drawGraphics(product: Product) {
    val card = product.graphics ?: return
    val bodyWidth = if (card.fans == 1) 72 else 119
    val trim = when (product) {
        Product.Gtx1660, Product.Rx6600 -> Red
        Product.Rtx3060 -> Steel
        Product.Rtx4070 -> Color(0xFFC1C7CA)
        Product.Gt1030 -> Color(0xFF52636B)
        else -> Color(0xFF767E85)
    }
    pixels(Steel, 1, 3, 6, 24)
    pixels(Ink, 3, 6, 3, 5)
    pixels(Ink, 3, 17, 3, 5)
    pixels(Green, 8, 5, bodyWidth, 24)
    pixels(Ink, 11, 2, bodyWidth - 5, 25)
    pixels(DarkMetal, 15, 4, bodyWidth - 12, 21)
    pixels(trim, 16, 4, bodyWidth - 15, 3)
    pixels(trim, 16, 22, bodyWidth - 15, 2)
    for (i in 0 until card.fans) {
        val x = if (card.fans == 1) 43 else 41 + i * 52
        drawPixelFan(x, 14, 10)
    }
    if (card.powerPins > 0) {
        pixels(Ink, 116, 7, 10, 13)
        for (y in 9..17 step 3) pixels(Gold, 122, y, 3, 1)
    }
    for (x in 19..minOf(82, bodyWidth) step 3) if (x != 31) pixels(Gold, x, 28, 2, 2)
}

private fun DrawScope.drawSupply(product: Product) {
    val modular = product.spec.powerWatts >= 750
    val trim = when (product) {
        Product.Psu550 -> Blue
        Product.Cx450 -> Color(0xFF8E9B9F)
        Product.Cx650 -> Color(0xFFAAA6A2)
        Product.Focus1000 -> Color(0xFFD3DADF)
        else -> Gold
    }
    pixels(Ink, 1, 1, 94, 78)
    pixels(if (product == Product.Focus1000) Color(0xFF454D54) else DarkMetal, 3, 3, 90, 74)
    pixels(trim, 8, 7, 77, 3)
    pixels(Ink, 8, 14, 63, 59)
    drawPixelFan(39, 43, 28)
    for (y in 17..70 step 5) pixels(Color(0xFF879095), 10, y, 59, 1)
    if (modular) {
        for (y in 16..62 step 12) pixels(Ink, 77, y, 14, 8)
    } else {
        pixels(Ink, 78, 27, 14, 25)
        for (y in 30..49 step 4) pixels(trim, 83, y, 9, 1)
    }
    if (product == Product.Rm850x) pixels(Gold, 75, 71, 15, 3)
}

private fun DrawScope.drawPixelFan(x: Int, y: Int, radius: Int) {
    val inset = radius / 3
    pixels(Ink, x - radius, y - radius + inset, radius * 2, (radius - inset) * 2)
    pixels(Ink, x - radius + inset, y - radius, (radius - inset) * 2, radius * 2)
    val blade = Color(0xFF67717A)
    pixels(blade, x - radius + inset, y - radius + 2, inset, radius)
    pixels(blade, x, y - radius + inset, radius - 2, inset)
    pixels(blade, x + 1, y, inset, radius - 2)
    pixels(blade, x - radius + 2, y + 2, radius, inset)
    pixels(Steel, x - inset / 2, y - inset / 2, inset, inset)
}
