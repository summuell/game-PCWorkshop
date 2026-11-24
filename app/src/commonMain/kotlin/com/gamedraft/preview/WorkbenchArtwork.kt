package com.gamedraft.preview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.layout.LayoutCoordinates
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
) {
    val ready: Boolean
        get() = listOf(table, emptyCase, assembled, board).all { it.width > 1 && it.height > 1 }
}

@Composable
internal fun loadWorkbenchArtwork() = WorkbenchArtwork(
    table = imageResource(Res.drawable.workbench_clean),
    emptyCase = imageResource(Res.drawable.workbench_empty),
    assembled = imageResource(Res.drawable.workbench_assembled),
    board = imageResource(Res.drawable.workbench_board),
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
    ComputerPart.Chassis to PartArtwork(ChassisBounds),
    ComputerPart.Motherboard to PartArtwork(
        bounds(155, 367, 652, 960),
        outline(170 to 367, 631 to 367, 649 to 378, 649 to 945, 637 to 959, 177 to 959, 164 to 946,
            164 to 737, 155 to 721, 155 to 404, 170 to 404),
    ),
    ComputerPart.Processor to PartArtwork(bounds(315, 492, 435, 621)),
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
        bounds(542, 374, 581, 701),
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
    chassis: Product?,
    selectedPart: ComputerPart?,
    placementSlots: List<ComputerPart>,
    screwdriverSelected: Boolean,
    pasteSelected: Boolean,
    cpuHasPaste: Boolean,
    onPaste: () -> Unit,
    hoveredSlot: ComputerPart?,
    onViewportChanged: (LayoutCoordinates) -> Unit,
    onSelect: (ComputerPart) -> Unit,
    onBeginHold: (WorkbenchHold, PointerId) -> WorkbenchPress?,
    onEndHold: (WorkbenchPress, Boolean) -> Unit,
    onAccessibleHold: (WorkbenchHold) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val visibleParts = ComputerPart.entries.filter { assembly.isVisible(it) }
    val renderParts = if (chassis?.caseStyle == CaseStyle.Glass && assembly.isInstalled(ComputerPart.SidePanel)) {
        ComputerPart.entries.filter { assembly.isVisible(it, throughPanel = true) }
    } else visibleParts

    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val aspectRatio = WorkbenchHeight.toFloat() / ArtworkWidth
        val sceneWidth = minOf(maxWidth, maxHeight * aspectRatio)
        val sceneHeight = sceneWidth / aspectRatio

        // The mat fills the work area. Hardware and its touch targets keep their original proportions.
        Canvas(Modifier.matchParentSize()) {
            withTransform({
                val horizontalScale = size.width / WorkbenchHeight
                val verticalScale = size.height / ArtworkWidth
                translate((WorkbenchTop + WorkbenchHeight) * horizontalScale, 0f)
                scale(horizontalScale, verticalScale, pivot = Offset.Zero)
                rotate(90f, pivot = Offset.Zero)
            }) { drawSheet(artwork.table) }
        }

        fun Modifier.positionOnScene(area: Rect): Modifier =
            offset(
                x = sceneWidth * ((WorkbenchTop + WorkbenchHeight - area.bottom) / WorkbenchHeight),
                y = sceneHeight * (area.left / ArtworkWidth),
            ).size(
                width = sceneWidth * (area.height / WorkbenchHeight),
                height = sceneHeight * (area.width / ArtworkWidth),
            )

        Box(
            Modifier.size(sceneWidth, sceneHeight).clipToBounds().onGloballyPositioned(onViewportChanged),
        ) {
            Canvas(Modifier.matchParentSize()) {
                val scale = size.height / ArtworkWidth
                withTransform({
                    translate((WorkbenchTop + WorkbenchHeight) * scale, 0f)
                    rotate(90f, pivot = Offset.Zero)
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    chassis?.caseStyle?.let { drawChassis(it) }
                    for (part in renderParts) {
                        if (part == ComputerPart.Chassis) continue
                        drawPart(artwork, part, chassis)
                        if (part == ComputerPart.Processor && cpuHasPaste) {
                            drawThermalPaste(partArtwork.getValue(part).bounds.center)
                        }
                        part.screws.forEachIndexed { index, screw ->
                            if (assembly.hasScrew(part, index)) drawScrewHead(screw)
                            else drawScrewHole(screw)
                            if (screwdriverSelected) {
                                assembly.screwAction(part, index)?.let { drawScrewHighlight(screw, it) }
                            }
                        }
                    }
                    if (selectedPart != null && assembly.isVisible(selectedPart)) drawPartHighlight(selectedPart)
                    for (slot in placementSlots) drawPlacementGuide(slot, slot == hoveredSlot)
                }
            }
            // Parts use the same stacking order for drawing and hit testing.
            for (part in visibleParts) {
                key(part) {
                    if (pasteSelected && part == ComputerPart.Processor) {
                        Box(Modifier.positionOnScene(partArtwork.getValue(part).hitArea)
                            .semantics { contentDescription = "给处理器涂硅脂" }
                            .clickable(role = Role.Button, onClick = onPaste))
                    } else HoldZone(
                        target = WorkbenchHold(part),
                        modifier = Modifier.positionOnScene(partArtwork.getValue(part).hitArea),
                        onSelect = onSelect,
                        onBeginHold = onBeginHold,
                        onEndHold = onEndHold,
                        onAccessibleHold = onAccessibleHold,
                    )
                }
            }
            if (screwdriverSelected) {
                for (part in visibleParts) {
                    part.screws.forEachIndexed { index, screw ->
                        val action = assembly.screwAction(part, index)
                        if (action != null) {
                            key(part, index, action) {
                                HoldZone(
                                    target = WorkbenchHold(part, WorkbenchHold.Screw(index, action)),
                                    modifier = Modifier.positionOnScene(screwArea(screw, 34)),
                                    onSelect = onSelect,
                                    onBeginHold = onBeginHold,
                                    onEndHold = onEndHold,
                                    onAccessibleHold = onAccessibleHold,
                                )
                            }
                        }
                    }
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
    onBeginHold: (WorkbenchHold, PointerId) -> WorkbenchPress?,
    onEndHold: (WorkbenchPress, Boolean) -> Unit,
    onAccessibleHold: (WorkbenchHold) -> Boolean,
) {
    val begin by rememberUpdatedState(onBeginHold)
    val end by rememberUpdatedState(onEndHold)
    Box(
        modifier.semantics {
            contentDescription = target.label
            role = Role.Button
            onClick { onSelect(target.part); true }
            onLongClick { onAccessibleHold(target) }
        }.pointerInput(target) {
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                val press = begin(target, down.id) ?: return@awaitEachGesture
                var finished = false
                try {
                    // Both an early release and a cancelled gesture end this wait before its deadline.
                    finished = withTimeoutOrNull(target.durationMillis.toLong()) {
                        waitForUpOrCancellation()?.consume()
                        true
                    } == null
                } finally {
                    end(press, finished)
                }
            }
        },
    )
}

internal fun placementSlot(
    slots: List<ComputerPart>,
    workbench: LayoutCoordinates?,
    mat: LayoutCoordinates?,
    viewport: LayoutCoordinates?,
    position: Offset,
): ComputerPart? {
    if (workbench == null || !workbench.isAttached || slots.isEmpty()) return null
    if (ComputerPart.Chassis in slots) {
        if (mat == null || !mat.isAttached) return null
        val local = mat.localPositionOf(workbench, position)
        return ComputerPart.Chassis.takeIf { local.x >= 0f && local.y >= 0f && local.x < mat.size.width && local.y < mat.size.height }
    }
    if (viewport == null || !viewport.isAttached) return null
    val local = viewport.localPositionOf(workbench, position)
    val width = viewport.size.width.toFloat()
    val height = viewport.size.height.toFloat()
    if (width <= 0 || height <= 0 || local.x !in 0f..width || local.y !in 0f..height) return null
    val point = Offset(local.y * ArtworkWidth / height, WorkbenchTop + WorkbenchHeight * (1f - local.x / width))
    return slots.firstOrNull { placementArea(it).contains(point) }
}

private fun DrawScope.drawThermalPaste(center: Offset) {
    val shadow = Color(0xFF727B83)
    val paste = Color(0xFFE5E9EC)
    val highlight = Color(0xFFF9FBFC)
    drawRect(shadow, center + Offset(-45f, -26f), Size(90f, 54f))
    drawRect(shadow, center + Offset(-32f, -39f), Size(64f, 80f))
    drawRect(paste, center + Offset(-41f, -24f), Size(82f, 46f))
    drawRect(paste, center + Offset(-28f, -34f), Size(55f, 68f))
    drawRect(highlight, center + Offset(-26f, -25f), Size(42f, 19f))
    drawRect(highlight, center + Offset(-30f, -6f), Size(22f, 10f))
}

@Composable
internal fun PartThumbnail(artwork: WorkbenchArtwork, part: ComputerPart, product: Product?, modifier: Modifier) {
    Canvas(modifier) {
        drawPartPreview(artwork, part, product, center, size)
    }
}

internal fun DrawScope.drawPartPreview(artwork: WorkbenchArtwork, part: ComputerPart, product: Product?, position: Offset, available: Size) {
    fitArtwork(partArtwork.getValue(part).bounds, position, available) {
        drawPart(artwork, part, product)
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
internal fun PasteThumbnail(modifier: Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 24
        fun bar(x: Int, y: Int, w: Int, h: Int, color: Color) =
            drawRect(color, Offset(x*u, y*u), Size(w*u, h*u))
        rotate(-40f) {
            val metal = Color(0xFFCDD7DB)
            bar(1, 11, 6, 2, metal)
            bar(6, 9, 3, 6, Color(0xFF93A7AD))
            bar(9, 8, 10, 8, metal)
            bar(11, 8, 5, 8, Color(0xFF498E9C))
            bar(19, 11, 3, 2, metal)
            bar(22, 8, 2, 8, Amber)
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

private fun DrawScope.drawPart(artwork: WorkbenchArtwork, part: ComputerPart, product: Product? = null) {
    if (part == ComputerPart.Chassis || part == ComputerPart.SidePanel) {
        val style = product?.caseStyle ?: return
        if (part == ComputerPart.Chassis) drawChassis(style) else drawCasePanel(style)
        return
    }
    val art = partArtwork.getValue(part)
    if (part == ComputerPart.MemoryRight) {
        val source = partArtwork.getValue(ComputerPart.MemoryLeft).bounds
        withTransform({ translate(art.bounds.left - source.left, art.bounds.top - source.top) }) {
            drawPart(artwork, ComputerPart.MemoryLeft)
        }
        return
    }
    if (part == ComputerPart.Processor) {
        drawProcessor(art.bounds)
        return
    }
    val image = when (part) {
        ComputerPart.Motherboard -> artwork.board
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

private fun placementArea(part: ComputerPart): Rect {
    val area = partArtwork.getValue(part).hitArea
    return if (part == ComputerPart.MemoryLeft || part == ComputerPart.MemoryRight) area else area.inflate(18f)
}

private fun DrawScope.drawPlacementGuide(part: ComputerPart, overGuide: Boolean) {
    val frame = placementArea(part)
    val color = if (overGuide) Color(0xFFAEF3AB) else Color(0xFF7ED5C9)
    drawRect(color.copy(alpha = if (overGuide) 0.24f else 0.13f), frame.topLeft, frame.size)
    val dash = if (overGuide) null else PathEffect.dashPathEffect(floatArrayOf(16f, 10f))
    drawRect(color, frame.topLeft, frame.size, style = Stroke(6f, pathEffect = dash))
}

private fun screwArea(screw: MountingScrew, radius: Int) =
    bounds(screw.x - radius, screw.y - radius, screw.x + radius, screw.y + radius)

private fun DrawScope.drawScrewHead(screw: MountingScrew) {
    val rim = screwArea(screw, screw.radius)
    val head = screwArea(screw, screw.radius - 3)
    val center = head.center
    drawRect(Color(0xFF20232A), rim.topLeft, rim.size)
    drawRect(Color(0xFF90959B), head.topLeft, head.size)
    drawLine(Color(0xFF42474F), Offset(head.left + 2, center.y), Offset(head.right - 2, center.y), 3f)
    drawLine(Color(0xFF42474F), Offset(center.x, head.top + 2), Offset(center.x, head.bottom - 2), 3f)
}

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
