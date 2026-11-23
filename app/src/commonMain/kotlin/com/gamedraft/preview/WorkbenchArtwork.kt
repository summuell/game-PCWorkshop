package com.gamedraft.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import com.gamedraft.preview.resources.Res
import com.gamedraft.preview.resources.workbench_assembled
import com.gamedraft.preview.resources.workbench_board
import com.gamedraft.preview.resources.workbench_clean
import com.gamedraft.preview.resources.workbench_empty
import com.gamedraft.preview.resources.workbench_lid
import org.jetbrains.compose.resources.imageResource

private const val ArtworkWidth = 1024
private const val ArtworkHeight = 1536
private const val WorkbenchTop = 106
private const val WorkbenchHeight = 1386
internal const val PartHoldMillis = 650
internal const val StarterScrewdriverHoldMillis = 900

internal data class WorkbenchHold(val part: ComputerPart, val screw: Screw? = null) {
    data class Screw(val index: Int, val action: ScrewAction)

    val durationMillis: Int
        get() = if (screw == null) PartHoldMillis else StarterScrewdriverHoldMillis

    val label: String
        get() {
            val fastener = screw ?: return part.label
            val suffix = if (fastener.action == ScrewAction.Tighten) "螺丝孔" else "螺丝"
            return "${part.label}$suffix ${fastener.index + 1}"
        }
}

internal data class WorkbenchArtwork(
    val table: ImageBitmap,
    val emptyCase: ImageBitmap,
    val assembled: ImageBitmap,
    val board: ImageBitmap,
    val lid: ImageBitmap,
) {
    val ready: Boolean
        get() = listOf(table, emptyCase, assembled, board, lid).all { it.width > 1 && it.height > 1 }
}

@Composable
internal fun loadWorkbenchArtwork() = WorkbenchArtwork(
    table = imageResource(Res.drawable.workbench_clean),
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

private val ComputerCaseOutline = outline(
    105 to 233, 882 to 233, 911 to 280, 911 to 1233, 890 to 1273, 861 to 1293,
    811 to 1293, 794 to 1305, 714 to 1305, 704 to 1293, 238 to 1293, 225 to 1305,
    151 to 1305, 144 to 1293, 105 to 1293,
)

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
    assembly: ComputerAssembly?,
    selectedPart: ComputerPart?,
    screwdriverSelected: Boolean,
    overPlacementGuide: Boolean,
    onViewportChanged: (Rect) -> Unit,
    onPower: () -> Unit,
    onSelect: (ComputerPart) -> Unit,
    onHoldChanged: (WorkbenchHold, Boolean) -> Unit,
    onHoldComplete: (WorkbenchHold) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleParts = assembly?.let { machine -> ComputerPart.entries.filter(machine::isVisible) }.orEmpty()

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val aspectRatio = ArtworkWidth.toFloat() / WorkbenchHeight
        val sceneWidth = minOf(maxWidth, maxHeight * aspectRatio)
        val sceneHeight = sceneWidth / aspectRatio

        fun Modifier.positionOnScene(area: Rect): Modifier =
            offset(
                x = sceneWidth * (area.left / ArtworkWidth),
                y = sceneHeight * ((area.top - WorkbenchTop) / WorkbenchHeight),
            ).size(
                width = sceneWidth * (area.width / ArtworkWidth),
                height = sceneHeight * (area.height / WorkbenchHeight),
            )

        Box(
            Modifier.size(sceneWidth, sceneHeight).clipToBounds().onGloballyPositioned { coordinates ->
                onViewportChanged(
                    Rect(
                        coordinates.positionInRoot(),
                        Size(coordinates.size.width.toFloat(), coordinates.size.height.toFloat()),
                    ),
                )
            },
        ) {
            Canvas(Modifier.matchParentSize()) {
                val scale = size.width / ArtworkWidth
                withTransform({
                    translate(0f, -WorkbenchTop * scale)
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    drawSheet(artwork.table)
                    if (assembly != null) {
                        clipPath(ComputerCaseOutline) { drawSheet(artwork.emptyCase) }
                        for (part in visibleParts) {
                            drawPart(artwork, part)
                            part.screws.forEachIndexed { index, screw ->
                                if (!assembly.hasScrew(part, index)) {
                                    drawScrewHole(screw)
                                }
                                if (screwdriverSelected) {
                                    assembly.screwAction(part, index)?.let { drawScrewHighlight(screw, it) }
                                }
                            }
                        }
                        if (selectedPart != null) {
                            if (assembly.isVisible(selectedPart)) {
                                drawPartHighlight(selectedPart)
                            } else if (assembly.canInstall(selectedPart)) {
                                drawPlacementGuide(selectedPart, overPlacementGuide)
                            }
                        }
                    }
                }
            }
            // Parts use the same stacking order for drawing and hit testing.
            for (part in visibleParts) {
                key(part) {
                    HoldZone(
                        target = WorkbenchHold(part),
                        modifier = Modifier.positionOnScene(partArtwork.getValue(part).hitArea),
                        onSelect = onSelect,
                        onHoldChanged = onHoldChanged,
                        onHoldComplete = onHoldComplete,
                    )
                }
            }
            if (assembly != null && screwdriverSelected) {
                for (part in visibleParts) {
                    part.screws.forEachIndexed { index, screw ->
                        val action = assembly.screwAction(part, index)
                        if (action != null) {
                            key(part, index, action) {
                                HoldZone(
                                    target = WorkbenchHold(part, WorkbenchHold.Screw(index, action)),
                                    modifier = Modifier.positionOnScene(screwArea(screw, 34)),
                                    onSelect = onSelect,
                                    onHoldChanged = onHoldChanged,
                                    onHoldComplete = onHoldComplete,
                                )
                            }
                        }
                    }
                }
            }
            if (assembly != null) {
                Row(
                    Modifier.align(Alignment.BottomEnd).padding(4.dp)
                        .height(44.dp).background(Panel).border(2.dp, Border)
                        .clickable(role = Role.Button, onClick = onPower).padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(Modifier.size(16.dp)) {
                        drawArc(Amber, -55f, 290f, false, Offset(size.width * .1f, size.height * .16f),
                            Size(size.width * .8f, size.height * .8f), style = Stroke(2.dp.toPx()))
                        drawLine(Amber, Offset(center.x, 0f), Offset(center.x, center.y), 2.dp.toPx())
                    }
                    GameText("开机", fontSize = 14)
                }
            }
        }
    }
}

@Composable
private fun HoldZone(
    target: WorkbenchHold,
    modifier: Modifier,
    onSelect: (ComputerPart) -> Unit,
    onHoldChanged: (WorkbenchHold, Boolean) -> Unit,
    onHoldComplete: (WorkbenchHold) -> Unit,
) {
    val currentHoldChanged by rememberUpdatedState(onHoldChanged)
    val currentHoldComplete by rememberUpdatedState(onHoldComplete)
    val base = LocalViewConfiguration.current
    val configuration = remember(base, target.durationMillis) {
        object : ViewConfiguration by base {
            override val longPressTimeoutMillis = target.durationMillis.toLong()
        }
    }
    CompositionLocalProvider(LocalViewConfiguration provides configuration) {
        Box(
            modifier
                .semantics {
                    contentDescription = target.label
                    role = Role.Button
                    onClick {
                        onSelect(target.part)
                        true
                    }
                    onLongClick {
                        onHoldComplete(target)
                        true
                    }
                }
                .pointerInput(target) {
                    detectTapGestures(
                        onPress = {
                            currentHoldChanged(target, true)
                            try {
                                tryAwaitRelease()
                            } finally {
                                currentHoldChanged(target, false)
                            }
                        },
                        onLongPress = { currentHoldComplete(target) },
                    )
                },
        )
    }
}

internal fun isOverPlacementGuide(part: ComputerPart, viewport: Rect?, position: Offset): Boolean {
    if (viewport == null || !viewport.contains(position)) return false
    val local = position - viewport.topLeft
    val point = Offset(local.x * ArtworkWidth / viewport.width, WorkbenchTop + local.y * WorkbenchHeight / viewport.height)
    return placementArea(part).contains(point)
}

@Composable
internal fun PartThumbnail(artwork: WorkbenchArtwork, part: ComputerPart, modifier: Modifier) {
    Canvas(modifier) {
        drawPartPreview(artwork, part, center, size)
    }
}

internal fun DrawScope.drawPartPreview(artwork: WorkbenchArtwork, part: ComputerPart, position: Offset, available: Size) {
    fitArtwork(partArtwork.getValue(part).bounds, position, available) {
        drawPart(artwork, part)
    }
}

private val ScrewdriverBounds = bounds(933, 190, 1007, 626)
private val ScrewdriverOutline = outline(
    963 to 190, 976 to 200, 976 to 403, 988 to 412, 996 to 438, 1006 to 450,
    1006 to 578, 994 to 611, 981 to 624, 955 to 624, 936 to 605, 933 to 581,
    935 to 450, 944 to 427, 956 to 410, 956 to 219,
)

@Composable
internal fun ScrewdriverThumbnail(artwork: WorkbenchArtwork, modifier: Modifier) {
    Canvas(modifier) {
        rotate(-55f) {
            fitArtwork(ScrewdriverBounds, center, size) {
                clipPath(ScrewdriverOutline) { drawSheet(artwork.emptyCase) }
            }
        }
    }
}

@Composable
internal fun TabletThumbnail(modifier: Modifier) {
    Canvas(modifier) {
        val pixel = minOf(size.width / 24f, size.height / 24f)
        val origin = Offset((size.width - 24f * pixel) / 2, (size.height - 24f * pixel) / 2)

        fun drawPixels(color: Color, x: Int, y: Int, width: Int, height: Int) {
            drawRect(color, origin + Offset(x * pixel, y * pixel), Size(width * pixel, height * pixel))
        }

        drawPixels(Color(0xFF0A1018), 1, 4, 22, 17)
        drawPixels(Color(0xFF677986), 2, 3, 20, 17)
        drawPixels(Color(0xFF222D38), 3, 4, 18, 15)
        drawPixels(Color(0xFF25465B), 5, 6, 14, 10)
        drawPixels(Color(0xFF416F78), 5, 6, 14, 2)
        drawPixels(Color(0xFF8CD9CF), 7, 10, 7, 1)
        drawPixels(Color(0xFF6C9FAC), 7, 13, 9, 1)
        drawPixels(Color(0xFFFFBC70), 11, 17, 2, 1)
    }
}

private fun DrawScope.fitArtwork(bounds: Rect, position: Offset, available: Size, draw: DrawScope.() -> Unit) {
    val scale = minOf(available.width / bounds.width, available.height / bounds.height)
    withTransform({
        translate(position.x - bounds.center.x * scale, position.y - bounds.center.y * scale)
        scale(scale, scale, pivot = Offset.Zero)
    }, draw)
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

private fun DrawScope.drawPartHighlight(part: ComputerPart) {
    val art = partArtwork.getValue(part)
    val color = Color(0xFFFFC064)
    val stroke = Stroke(width = 6f)
    val shape = art.outline
    if (shape == null) {
        drawRect(color, art.bounds.topLeft, art.bounds.size, style = stroke)
    } else {
        drawPath(shape, color, style = stroke)
    }
}

private fun placementArea(part: ComputerPart): Rect = partArtwork.getValue(part).hitArea.inflate(18f)

private fun DrawScope.drawPlacementGuide(part: ComputerPart, overGuide: Boolean) {
    val frame = placementArea(part)
    val color = if (overGuide) Color(0xFFAEF3AB) else Color(0xFF7ED5C9)
    drawRect(color.copy(alpha = if (overGuide) 0.24f else 0.13f), frame.topLeft, frame.size)
    val dash = if (overGuide) null else PathEffect.dashPathEffect(floatArrayOf(16f, 10f))
    drawRect(color, frame.topLeft, frame.size, style = Stroke(6f, pathEffect = dash))
}

private fun screwArea(screw: MountingScrew, radius: Int) =
    bounds(screw.x - radius, screw.y - radius, screw.x + radius, screw.y + radius)

private fun DrawScope.drawScrewHole(screw: MountingScrew) {
    val rim = screwArea(screw, screw.radius)
    val hole = screwArea(screw, screw.radius - 4)
    drawRect(Color(0xFF34313A), rim.topLeft, rim.size)
    drawRect(Color(0xFF100F13), hole.topLeft, hole.size)
}

private fun DrawScope.drawScrewHighlight(screw: MountingScrew, action: ScrewAction) {
    val ring = screwArea(screw, screw.radius + 8)
    val color = if (action == ScrewAction.Tighten) Color(0xFF7ED5C9) else Color(0xFFFFC064)
    drawRect(color.copy(alpha = 0.25f), ring.topLeft, ring.size)
    drawRect(color, ring.topLeft, ring.size, style = Stroke(width = 5f))
}
