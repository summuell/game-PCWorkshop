package com.gamedraft.preview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamedraft.preview.resources.Res
import com.gamedraft.preview.resources.screen_labels
import com.gamedraft.preview.resources.workbench_clean
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.imageResource

internal val Background = Color(0xFF10171F)
internal val Panel = Color(0xFF1C2832)
internal val SlotBackground = Color(0xFF141D25)
internal val Border = Color(0xFF455764)
private val TextColor = Color(0xFFF3EAE0)
internal val MutedText = Color(0xFFB7C3CA)
internal val Amber = Color(0xFFFFBC70)
private val DarkInk = Color(0xFF302012)
private val GameFont = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }

private val SessionSaver = listSaver<RepairSession, String>(
    save = { it.save() },
    restore = RepairSession::restore,
)

private data class InventoryDrag(val part: ComputerPart, val position: Offset)

@Composable
fun WorkshopGame() {
    var started by rememberSaveable { mutableStateOf(false) }
    val font = FontFamily(Font(Res.font.screen_labels, FontWeight.Bold))

    CompositionLocalProvider(GameFont provides font) {
        Box(
            Modifier.fillMaxSize().background(Background).safeDrawingPadding(),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.widthIn(max = 620.dp).fillMaxSize()) {
                if (started) {
                    WorkbenchScreen(onHome = { started = false })
                } else {
                    StartScreen(onStart = { started = true })
                }
            }
        }
    }
}

@Composable
private fun StartScreen(onStart: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(
            imageResource(Res.drawable.workbench_clean),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            filterQuality = FilterQuality.None,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.47f to Color.Transparent,
                    0.79f to Background.copy(alpha = 0.88f),
                    1f to Background,
                ),
            ),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GameText("电脑维修", fontSize = 38, modifier = Modifier.semantics { heading() })
            GameButton("开始游戏", onStart, Modifier.fillMaxWidth(), primary = true)
        }
    }
}

@Composable
private fun WorkbenchScreen(onHome: () -> Unit) {
    val artwork = loadWorkbenchArtwork()
    var session by rememberSaveable(stateSaver = SessionSaver) { mutableStateOf(RepairSession()) }
    var tabletOpen by rememberSaveable { mutableStateOf(false) }
    var tabletPage by rememberSaveable { mutableStateOf(TabletPage.Orders) }
    var monitorOpen by remember { mutableStateOf(false) }
    val power = remember(session.stage, session.assembly, session.parts, session.graphicsCableReseated) { ComputerPower() }
    RunComputer(power, session) { session = session.recordSuccessfulStartup() }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var screwdriverSelected by rememberSaveable { mutableStateOf(false) }
    var holdingTarget by remember { mutableStateOf<WorkbenchHold?>(null) }
    var draggedPart by remember { mutableStateOf<InventoryDrag?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var viewport by remember { mutableStateOf<Rect?>(null) }
    var screenOrigin by remember { mutableStateOf(Offset.Zero) }
    val assembly = session.assembly
    val hasMachine = session.stage == OrderStage.Active
    val selection = selectedName?.let(ComputerPart::valueOf)
    val currentDrag = draggedPart
    val hoveredSlot = currentDrag?.let { placementSlot(it.part, assembly, viewport, it.position) }

    fun selectPart(part: ComputerPart) {
        selectedName = part.name
        feedback = null
    }

    fun canHold(target: WorkbenchHold): Boolean {
        if (tabletOpen || session.stage != OrderStage.Active) return false
        val screw = target.screw ?: return session.assembly.canRemove(target.part)
        return screwdriverSelected && session.assembly.screwAction(target.part, screw.index) == screw.action
    }

    fun changeHold(target: WorkbenchHold, pressed: Boolean) {
        if (!pressed) {
            if (holdingTarget == target) holdingTarget = null
            return
        }
        if (draggedPart == null && holdingTarget == null) {
            selectPart(target.part)
            holdingTarget = target.takeIf(::canHold)
        }
    }

    fun finishHold(target: WorkbenchHold) {
        if (draggedPart != null || (holdingTarget != null && holdingTarget != target)) return
        holdingTarget = null
        val part = target.part
        selectPart(part)
        if (!canHold(target)) return

        val screw = target.screw
        if (screw == null) {
            session = session.remove(part)
            selectedName = null
            feedback = "${part.label}已放入物品栏"
        } else {
            session = session.turnScrew(part, screw.index, screw.action)
        }
    }

    fun moveInventoryPart(part: ComputerPart, position: Offset) {
        val active = draggedPart
        if (tabletOpen || session.assembly.isInstalled(part) || (active != null && active.part != part)) return
        if (active == null) selectPart(part)
        draggedPart = InventoryDrag(part, position)
    }

    fun finishInventoryDrag(part: ComputerPart, released: Boolean) {
        val active = draggedPart?.takeIf { it.part == part } ?: return
        draggedPart = null
        if (!released || tabletOpen) return
        val blocker = session.assembly.blockerFor(part)
        val slot = placementSlot(part, session.assembly, viewport, active.position)
        when {
            blocker != null -> feedback = blocker.instruction
            slot == null -> feedback = "拖到提示框内再松手"
            else -> {
                val installed = session.install(part, slot)
                if (installed !== session) {
                    session = installed
                    selectedName = part.name.takeIf { part.screws.isNotEmpty() }
                    feedback = if (part.screws.isEmpty()) {
                        "${part.label}已装回"
                    } else {
                        "零件已就位，用螺丝刀长按空孔"
                    }
                }
            }
        }
    }

    if (monitorOpen) {
        StartupMonitor(power, onClose = { monitorOpen = false })
        return
    }

    if (tabletOpen) {
        WorkshopTablet(
            session = session,
            page = tabletPage,
            onPageChanged = { tabletPage = it },
            onClose = { tabletOpen = false },
            onAccept = {
                val accepted = session.accept()
                if (accepted !== session) {
                    session = accepted
                    tabletOpen = false
                    feedback = "主机已送到工作台"
                }
            },
            onDeliver = {
                val delivered = session.deliver()
                if (delivered !== session) {
                    session = delivered
                    selectedName = null
                    screwdriverSelected = false
                    feedback = null
                }
            },
        )
        return
    }

    Box(
        Modifier.fillMaxSize().onGloballyPositioned { screenOrigin = it.positionInRoot() },
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().height(52.dp).background(Panel).padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameText("维修工作台", fontSize = 20, modifier = Modifier.semantics { heading() })
                GameButton("主页", onHome)
            }
            if (artwork.ready) {
                WorkbenchScene(
                    artwork = artwork,
                    assembly = assembly.takeIf { hasMachine },
                    selectedPart = selection,
                    screwdriverSelected = screwdriverSelected,
                    hoveredSlot = hoveredSlot,
                    onViewportChanged = { viewport = it },
                    computerOn = power.requested,
                    onPower = {
                        holdingTarget = null
                        draggedPart = null
                        if (!power.requested) power.start()
                        monitorOpen = true
                    },
                    onSelect = ::selectPart,
                    onHoldChanged = ::changeHold,
                    onHoldComplete = ::finishHold,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                ToolStrip(
                    artwork = artwork,
                    hasMachine = hasMachine,
                    screwdriverSelected = screwdriverSelected,
                    selection = selection,
                    hint = when (session.stage) {
                        OrderStage.Available -> "打开平板接单"
                        OrderStage.Delivered -> "订单已交付"
                        OrderStage.Active -> feedback ?: workbenchHint(assembly, selection, screwdriverSelected, holdingTarget)
                    },
                    holding = holdingTarget,
                    onScrewdriver = {
                        screwdriverSelected = !screwdriverSelected
                        holdingTarget = null
                        feedback = null
                    },
                    onTablet = {
                        holdingTarget = null
                        draggedPart = null
                        tabletPage = if (session.stage == OrderStage.Available) TabletPage.Orders else TabletPage.Delivery
                        tabletOpen = true
                    },
                )
                Inventory(
                    artwork = artwork,
                    parts = assembly.inventory,
                    selection = selection,
                    onSelect = ::selectPart,
                    onMove = ::moveInventoryPart,
                    onFinish = ::finishInventoryDrag,
                )
            } else {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    GameText("正在载入工作台", color = MutedText)
                }
            }
        }
        if (currentDrag != null) {
            Canvas(Modifier.matchParentSize()) {
                drawPartPreview(
                    artwork,
                    currentDrag.part,
                    currentDrag.position - screenOrigin,
                    Size(95.dp.toPx(), 76.dp.toPx()),
                )
            }
        }
    }
}

private fun workbenchHint(
    assembly: ComputerAssembly,
    selection: ComputerPart?,
    screwdriverSelected: Boolean,
    holding: WorkbenchHold?,
): String {
    if (holding != null) {
        return when (holding.screw?.action) {
            ScrewAction.Loosen -> "继续按住，拧下螺丝"
            ScrewAction.Tighten -> "继续按住，拧紧螺丝"
            null -> "继续按住，取下零件"
        }
    }
    if (selection == null) {
        return when {
            screwdriverSelected -> "长按螺丝卸下，长按空孔装回"
            assembly.isInstalled(ComputerPart.SidePanel) -> "选中螺丝刀，长按盖板螺丝"
            else -> "长按零件拆下，从物品栏拖回"
        }
    }
    assembly.blockerFor(selection)?.let { return it.instruction }
    if (!assembly.isInstalled(selection)) return "拖到提示框内松手"
    val screws = assembly.screwsRemaining(selection)
    return if (screws > 0) {
        if (screwdriverSelected) "长按螺丝卸下" else "用螺丝刀卸下螺丝"
    } else if (screwdriverSelected && selection.screws.isNotEmpty()) {
        "长按部件取下，或长按空孔装螺丝"
    } else {
        "长按零件拆下"
    }
}

@Composable
private fun ToolStrip(
    artwork: WorkbenchArtwork,
    hasMachine: Boolean,
    screwdriverSelected: Boolean,
    selection: ComputerPart?,
    hint: String,
    holding: WorkbenchHold?,
    onScrewdriver: () -> Unit,
    onTablet: () -> Unit,
) {
    val holdProgress by animateFloatAsState(
        targetValue = if (holding != null) 1f else 0f,
        animationSpec = holding?.let { tween(it.durationMillis, easing = LinearEasing) } ?: snap(),
    )
    Row(
        Modifier.fillMaxWidth().height(90.dp).background(Panel).padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            GameText("工具栏", fontSize = 14, color = MutedText)
            ToolSlot(
                label = "螺丝刀",
                onClick = onScrewdriver,
                enabled = hasMachine,
                active = screwdriverSelected,
            ) {
                ScrewdriverThumbnail(artwork, Modifier.size(width = 29.dp, height = 32.dp))
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (selection != null) {
                GameText(selection.label, fontSize = 16)
            } else {
                Spacer(Modifier.height(3.dp))
            }
            GameText(hint, fontSize = 14, color = if (holding != null) Amber else MutedText)
            Box(Modifier.fillMaxWidth().height(3.dp)) {
                if (holding != null) {
                    Box(Modifier.fillMaxSize().background(SlotBackground))
                    Box(Modifier.fillMaxWidth(holdProgress).height(3.dp).background(Amber))
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Spacer(Modifier.height(19.dp))
            ToolSlot(label = "平板", onClick = onTablet) {
                TabletThumbnail(Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun ToolSlot(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    active: Boolean = false,
    icon: @Composable () -> Unit,
) {
    Row(
        Modifier
            .height(43.dp)
            .background(if (active) Color(0xFF302820) else SlotBackground)
            .border(if (active) 2.dp else 1.dp, if (active) Amber else Border)
            .semantics { selected = active }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        val color = when {
            !enabled -> MutedText
            active -> Amber
            else -> TextColor
        }
        GameText(label, fontSize = 14, color = color)
    }
}

@Composable
private fun Inventory(
    artwork: WorkbenchArtwork,
    parts: List<ComputerPart>,
    selection: ComputerPart?,
    onSelect: (ComputerPart) -> Unit,
    onMove: (ComputerPart, Offset) -> Unit,
    onFinish: (ComputerPart, Boolean) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().background(Panel).padding(start = 14.dp, end = 14.dp, top = 5.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameText("物品栏", fontSize = 16, modifier = Modifier.semantics { heading() })
            GameText("${parts.size} 件", fontSize = 14, color = MutedText)
        }
        if (parts.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(72.dp).background(SlotBackground).border(1.dp, Border),
                contentAlignment = Alignment.Center,
            ) {
                GameText("拆下的零件会放在这里", fontSize = 14, color = MutedText)
            }
        } else {
            LazyRow(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(parts, key = { it.name }) { part ->
                    InventorySlot(artwork, part, part == selection, onSelect, onMove, onFinish)
                }
            }
        }
    }
}

@Composable
private fun InventorySlot(
    artwork: WorkbenchArtwork,
    part: ComputerPart,
    active: Boolean,
    onSelect: (ComputerPart) -> Unit,
    onMove: (ComputerPart, Offset) -> Unit,
    onFinish: (ComputerPart, Boolean) -> Unit,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentMove by rememberUpdatedState(onMove)
    val currentFinish by rememberUpdatedState(onFinish)
    Column(
        Modifier
            .width(92.dp)
            .height(72.dp)
            .background(if (active) Color(0xFF302820) else SlotBackground)
            .border(if (active) 2.dp else 1.dp, if (active) Amber else Border)
            .onGloballyPositioned { coordinates = it }
            .pointerInput(part) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // A sideways touch scrolls the inventory. Pulling upward starts placement.
                    val start = if (down.type == PointerType.Mouse) {
                        awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    } else {
                        awaitVerticalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    } ?: return@awaitEachGesture
                    val layout = coordinates?.takeIf { it.isAttached } ?: return@awaitEachGesture

                    var released = false
                    try {
                        currentMove(part, layout.localToRoot(start.position))
                        released = drag(start.id) { change ->
                            if (layout.isAttached) currentMove(part, layout.localToRoot(change.position))
                            change.consume()
                        }
                        val release = currentEvent.changes.firstOrNull { it.id == start.id }
                        if (released && release != null && layout.isAttached) {
                            currentMove(part, layout.localToRoot(release.position))
                        }
                    } finally {
                        currentFinish(part, released)
                    }
                }
            }
            .clickable(role = Role.Button) { onSelect(part) }
            .padding(horizontal = 5.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PartThumbnail(artwork, part, Modifier.size(width = 60.dp, height = 33.dp))
        Spacer(Modifier.height(5.dp))
        GameText(part.label, fontSize = 14)
    }
}

@Composable
internal fun GameButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    val highlighted = primary && enabled
    Box(
        modifier
            .heightIn(min = if (primary) 46.dp else 42.dp)
            .widthIn(min = 70.dp)
            .background(if (highlighted) Amber else SlotBackground)
            .border(1.dp, if (highlighted) Color(0xFFC88D52) else Border)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        val color = when {
            !enabled -> MutedText
            highlighted -> DarkInk
            else -> TextColor
        }
        GameText(label, color = color)
    }
}

@Composable
internal fun GameText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: Int = 16,
    color: Color = TextColor,
    textDecoration: TextDecoration? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontFamily = GameFont.current,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 5).sp,
            textDecoration = textDecoration,
        ),
    )
}
