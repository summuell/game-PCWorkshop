package com.gamedraft.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import com.gamedraft.preview.resources.Res
import com.gamedraft.preview.resources.workbench_assembled
import com.gamedraft.preview.resources.workbench_board
import com.gamedraft.preview.resources.workbench_empty
import com.gamedraft.preview.resources.workbench_lid
import org.jetbrains.compose.resources.imageResource

private const val ArtworkWidth = 1024
private const val ArtworkHeight = 1536
private const val WorkbenchTop = 106
private const val WorkbenchHeight = 1386

internal data class WorkbenchArtwork(
    val emptyCase: ImageBitmap,
    val assembled: ImageBitmap,
    val board: ImageBitmap,
    val lid: ImageBitmap,
) {
    val ready: Boolean
        get() = listOf(emptyCase, assembled, board, lid).all { it.width > 1 && it.height > 1 }
}

@Composable
internal fun loadWorkbenchArtwork() = WorkbenchArtwork(
    emptyCase = imageResource(Res.drawable.workbench_empty),
    assembled = imageResource(Res.drawable.workbench_assembled),
    board = imageResource(Res.drawable.workbench_board),
    lid = imageResource(Res.drawable.workbench_lid),
)

private data class PartArtwork(
    val bounds: Rect,
    val outline: Path? = null,
    val hitArea: Rect = bounds,
)

private fun bounds(left: Int, top: Int, right: Int, bottom: Int) =
    Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())

private fun outline(first: Pair<Int, Int>, vararg remaining: Pair<Int, Int>) = Path().apply {
    moveTo(first.first.toFloat(), first.second.toFloat())
    for ((x, y) in remaining) lineTo(x.toFloat(), y.toFloat())
    close()
}

// Coordinates are measured in the same 1024 by 1536 view for every artwork layer.
private val partArtwork = mapOf(
    ComputerPart.Motherboard to PartArtwork(
        bounds(155, 367, 652, 960),
        outline(170 to 367, 631 to 367, 649 to 378, 649 to 945, 637 to 959, 177 to 959, 164 to 946,
            164 to 737, 155 to 721, 155 to 404, 170 to 404),
    ),
    ComputerPart.Processor to PartArtwork(bounds(315, 492, 435, 621)),
    ComputerPart.FrontFanUpper to PartArtwork(
        bounds(715, 341, 815, 632),
        outline(764 to 341, 806 to 341, 813 to 357, 813 to 604, 804 to 629, 727 to 629,
            720 to 615, 720 to 366),
    ),
    ComputerPart.FrontFanLower to PartArtwork(
        bounds(715, 635, 816, 934),
        outline(761 to 635, 808 to 635, 814 to 650, 814 to 913, 805 to 932, 727 to 932,
            720 to 913, 720 to 654),
    ),
    ComputerPart.PowerSupply to PartArtwork(bounds(154, 1004, 447, 1243)),
    ComputerPart.HardDrive to PartArtwork(
        bounds(531, 1039, 790, 1224),
        outline(564 to 1039, 781 to 1039, 790 to 1051, 790 to 1212, 781 to 1224, 553 to 1224,
            545 to 1208, 545 to 1175, 531 to 1175, 531 to 1101, 545 to 1101, 545 to 1057),
    ),
    ComputerPart.SolidStateDrive to PartArtwork(
        bounds(258, 865, 503, 932),
        outline(259 to 865, 492 to 865, 492 to 881, 502 to 885, 502 to 908, 492 to 912,
            492 to 931, 259 to 931, 259 to 904, 269 to 904, 269 to 893, 259 to 893),
        hitArea = bounds(248, 854, 511, 942),
    ),
    ComputerPart.MemoryLeft to PartArtwork(
        bounds(498, 374, 537, 701),
        hitArea = bounds(489, 369, 538, 704),
    ),
    ComputerPart.MemoryRight to PartArtwork(
        bounds(542, 374, 580, 701),
        hitArea = bounds(538, 369, 586, 704),
    ),
    ComputerPart.Cooler to PartArtwork(
        bounds(242, 401, 495, 663),
        outline(244 to 426, 258 to 406, 285 to 401, 307 to 420, 429 to 420, 452 to 401,
            480 to 408, 493 to 433, 478 to 461, 489 to 497, 495 to 549, 480 to 596, 493 to 622,
            480 to 653, 451 to 663, 424 to 648, 307 to 648, 280 to 663, 254 to 653, 242 to 625,
            257 to 594, 244 to 547, 246 to 506, 258 to 463),
    ),
    ComputerPart.GraphicsCard to PartArtwork(
        bounds(130, 716, 695, 851),
        outline(130 to 716, 695 to 716, 695 to 769, 657 to 769, 657 to 836,
            635 to 851, 164 to 851, 164 to 829, 130 to 829),
        hitArea = bounds(131, 721, 659, 850),
    ),
    ComputerPart.MotherboardCable to PartArtwork(
        bounds(191, 304, 705, 722),
        outline(193 to 370, 194 to 348, 204 to 329, 227 to 317, 272 to 310, 283 to 304,
            609 to 304, 647 to 312, 675 to 332, 692 to 363, 700 to 394, 702 to 721,
            656 to 721, 656 to 383, 650 to 360, 636 to 345, 608 to 341, 310 to 341,
            276 to 351, 266 to 342, 226 to 352, 221 to 370, 238 to 370, 238 to 407, 192 to 407),
        hitArea = bounds(196, 299, 695, 361),
    ),
    ComputerPart.GraphicsCable to PartArtwork(
        bounds(444, 735, 705, 1120),
        outline(627 to 735, 691 to 735, 701 to 763, 702 to 914, 693 to 949, 670 to 980,
            640 to 990, 533 to 990, 500 to 1008, 499 to 1060, 482 to 1106, 444 to 1120,
            444 to 1096, 466 to 1084, 468 to 1020, 477 to 996, 510 to 964, 629 to 947,
            650 to 926, 653 to 791, 627 to 777),
        hitArea = bounds(653, 746, 708, 951),
    ),
    ComputerPart.DriveCable to PartArtwork(
        bounds(442, 1102, 549, 1245),
        outline(443 to 1245, 443 to 1169, 450 to 1146, 472 to 1123, 506 to 1111, 507 to 1103,
            549 to 1103, 549 to 1174, 510 to 1174, 510 to 1155, 489 to 1162, 480 to 1175,
            480 to 1245),
        hitArea = bounds(441, 1124, 512, 1244),
    ),
    ComputerPart.SidePanel to PartArtwork(bounds(153, 294, 822, 1246)),
)

@Composable
internal fun WorkbenchScene(
    artwork: WorkbenchArtwork,
    assembly: ComputerAssembly,
    selectedPart: ComputerPart?,
    onSelect: (ComputerPart) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val aspectRatio = ArtworkWidth.toFloat() / WorkbenchHeight
        val sceneWidth = minOf(maxWidth, maxHeight * aspectRatio)
        val sceneHeight = sceneWidth / aspectRatio

        Box(Modifier.size(sceneWidth, sceneHeight).clipToBounds()) {
            Canvas(Modifier.matchParentSize()) {
                val scale = size.width / ArtworkWidth
                withTransform({
                    translate(0f, -WorkbenchTop * scale)
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    drawSheet(artwork.emptyCase)
                    if (assembly.isInstalled(ComputerPart.SidePanel)) {
                        drawPart(artwork, ComputerPart.SidePanel)
                    } else {
                        for (part in ComputerPart.entries) {
                            if (assembly.isInstalled(part)) drawPart(artwork, part)
                        }
                    }
                    if (selectedPart != null) {
                        val installed = assembly.isInstalled(selectedPart)
                        if (assembly.isVisible(selectedPart) || (!installed && assembly.blockerFor(selectedPart) == null)) {
                            drawPartHighlight(selectedPart, installed)
                        }
                    }
                }
            }
            // The input targets are stacked in the same order as their corresponding artwork.
            for (part in ComputerPart.entries) {
                if (!assembly.isVisible(part)) continue
                val hit = partArtwork.getValue(part).hitArea
                Box(
                    Modifier
                        .offset(
                            x = sceneWidth * (hit.left / ArtworkWidth),
                            y = sceneHeight * ((hit.top - WorkbenchTop) / WorkbenchHeight),
                        )
                        .size(
                            width = sceneWidth * (hit.width / ArtworkWidth),
                            height = sceneHeight * (hit.height / WorkbenchHeight),
                        )
                        .semantics { contentDescription = part.label }
                        .clickable(interactionSource = null, indication = null, role = Role.Button) { onSelect(part) },
                )
            }
        }
    }
}

@Composable
internal fun PartThumbnail(artwork: WorkbenchArtwork, part: ComputerPart, modifier: Modifier) {
    Canvas(modifier) {
        val bounds = partArtwork.getValue(part).bounds
        val scale = minOf(size.width / bounds.width, size.height / bounds.height)
        val left = (size.width - bounds.width * scale) / 2 - bounds.left * scale
        val top = (size.height - bounds.height * scale) / 2 - bounds.top * scale
        withTransform({
            translate(left, top)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPart(artwork, part)
        }
    }
}

private fun DrawScope.drawSheet(image: ImageBitmap) {
    drawImage(image = image, dstSize = IntSize(ArtworkWidth, ArtworkHeight), filterQuality = FilterQuality.None)
}

private fun DrawScope.drawPart(artwork: WorkbenchArtwork, part: ComputerPart) {
    val art = partArtwork.getValue(part)
    if (part == ComputerPart.Processor) {
        drawProcessor(art.bounds)
        return
    }
    val image = when (part) {
        ComputerPart.Motherboard -> artwork.board
        ComputerPart.SidePanel -> artwork.lid
        else -> artwork.assembled
    }
    clipRect(art.bounds.left, art.bounds.top, art.bounds.right, art.bounds.bottom) {
        if (art.outline == null) {
            drawSheet(image)
        } else {
            clipPath(art.outline) { drawSheet(image) }
        }
    }
}

private fun DrawScope.drawProcessor(bounds: Rect) {
    drawRect(Color(0xFF07352F), bounds.topLeft, bounds.size)
    drawRect(
        Color(0xFFABA6A0),
        topLeft = bounds.topLeft + Offset(8f, 8f),
        size = Size(bounds.width - 16, bounds.height - 16),
    )
    drawRect(
        Color(0xFFD2CDC0),
        topLeft = bounds.topLeft + Offset(13f, 13f),
        size = Size(bounds.width - 30, bounds.height - 36),
    )
    drawRect(
        Color(0xFFE4C974),
        topLeft = bounds.topLeft + Offset(9f, bounds.height - 20),
        size = Size(13f, 12f),
    )
}

private fun DrawScope.drawPartHighlight(part: ComputerPart, installed: Boolean) {
    val art = partArtwork.getValue(part)
    val color = if (installed) Color(0xFFFFC064) else Color(0xFF7ED5C9)
    val stroke = Stroke(width = 6f)
    val shape = art.outline
    if (shape == null) {
        if (!installed) drawRect(color.copy(alpha = 0.18f), art.bounds.topLeft, art.bounds.size)
        drawRect(color, art.bounds.topLeft, art.bounds.size, style = stroke)
    } else {
        if (!installed) drawPath(shape, color.copy(alpha = 0.18f))
        drawPath(shape, color, style = stroke)
    }
}
