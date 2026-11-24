package com.gamedraft.preview

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.awaitHorizontalTouchSlopOrCancellation
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Constraints
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

private data class InventoryDrag(val item: InventoryItem, val position: Offset)
private val ToolSlotSize = 56.dp
private val ToolGap = 8.dp
private val RailPadding = 8.dp
private val TabletButtonWidth = 42.dp
private enum class BenchTool { Driver, Paste }

@Composable
internal fun WorkshopGame(
    storage: SaveStorage,
    onExitGame: () -> Unit,
    onStartGame: () -> Unit = {},
    registerBackHandler: @Composable (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    var started by rememberSaveable { mutableStateOf(false) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var exited by rememberSaveable { mutableStateOf(false) }
    var loadGeneration by remember { mutableStateOf(0) }
    val focus = remember { FocusRequester() }
    registerBackHandler(!exited) { menuOpen = !menuOpen }
    var tabletOpen by rememberSaveable { mutableStateOf(false) }
    var tabletPage by rememberSaveable { mutableStateOf(TabletPage.Orders) }
    val sessionState = rememberSaveable(stateSaver = SessionSaver) { mutableStateOf(RepairSession()) }
    val font = FontFamily(Font(Res.font.screen_labels, FontWeight.Bold))

    CompositionLocalProvider(GameFont provides font) {
        Box(
            Modifier.fillMaxSize().background(Background),
            contentAlignment = Alignment.Center,
        ) {
            LandscapeSurface {
                LaunchedEffect(exited, menuOpen) { focus.requestFocus() }
                Box(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                    if (event.key != Key.Escape || exited) return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyUp) menuOpen = !menuOpen
                    true
                }.focusRequester(focus).focusable()) {
                    if (exited) {
                        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            GameText("已退出游戏", fontSize = 24)
                            GameButton("重新进入", { exited = false })
                        }
                    } else {
                        if (started) key(loadGeneration) { WorkbenchScreen(sessionState, menuOpen, tabletOpen) }
                        else StartScreen(onStart = {
                            if (sessionState.value.stage == OrderStage.Delivered) {
                                sessionState.value = sessionState.value.copy(stage = OrderStage.Available)
                            }
                            onStartGame()
                            started = true
                        })
                        if (tabletOpen) {
                            Box(Modifier.fillMaxSize().background(Background)) {
                                Box(Modifier.matchParentSize().clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null) {})
                                WorkshopTablet(sessionState.value, tabletPage, { tabletPage = it },
                                    onClose = { tabletOpen = false },
                                    onBuy = { sessionState.value = sessionState.value.buy(it) },
                                    onReceive = { sessionState.value = sessionState.value.receive(it) },
                                    onAccept = {
                                        sessionState.value = sessionState.value.accept()
                                        if (!started) onStartGame()
                                        started = true
                                        tabletOpen = false
                                    },
                                    onDeliver = { sessionState.value = sessionState.value.deliver() })
                            }
                        }
                        FloatingMenuButton { menuOpen = true }
                        if (menuOpen) GameMenu(storage, sessionState.value, onClose = { menuOpen = false }, onHome = {
                            tabletOpen = false
                            started = false
                            menuOpen = false
                        }, onLoaded = {
                            tabletOpen = false
                            sessionState.value = it
                            loadGeneration++
                            started = true
                            menuOpen = false
                        }, onExit = {
                            started = false
                            menuOpen = false
                            sessionState.value = RepairSession()
                            tabletOpen = false
                            exited = true
                            onExitGame()
                        })
                        if (!tabletOpen || menuOpen) {
                            Box(Modifier.align(Alignment.TopEnd)
                                .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
                                .padding(end = RailPadding, top = RailPadding)
                                .size(TabletButtonWidth).background(SlotBackground).border(1.dp, Border)
                                .semantics { contentDescription = "平板电脑" }.clickable(role = Role.Button) {
                                    menuOpen = false
                                    if (!tabletOpen) {
                                        tabletPage = if (sessionState.value.stage == OrderStage.Active) TabletPage.Delivery else TabletPage.Orders
                                        tabletOpen = true
                                    }
                                }, contentAlignment = Alignment.Center) { TabletThumbnail(Modifier.size(27.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeSurface(content: @Composable () -> Unit) {
    Layout(content = { Box(Modifier.fillMaxSize()) { content() } },
        modifier = Modifier.fillMaxSize()) { children, bounds ->
        val turn = bounds.maxHeight > bounds.maxWidth
        val gameBounds = if (turn) Constraints.fixed(bounds.maxHeight, bounds.maxWidth) else bounds
        val game = children.single().measure(gameBounds)
        layout(bounds.maxWidth, bounds.maxHeight) {
            if (turn) game.placeWithLayer(bounds.maxWidth, 0) {
                transformOrigin = TransformOrigin(0f, 0f)
                rotationZ = 90f
            } else game.place(0, 0)
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
private fun WorkbenchScreen(sessionState: androidx.compose.runtime.MutableState<RepairSession>, paused: Boolean, tabletOpen: Boolean) {
    val artwork = loadWorkbenchArtwork()
    var session by sessionState
    var monitorOpen by remember(session.stage) { mutableStateOf(false) }
    val power = remember(session.stage, session.assembly, session.parts[ComputerPart.Processor]?.hasPaste) { ComputerPower() }
    RunComputer(power, session, paused, onPassed = { session = session.recordSuccessfulStartup() },
        onInstall = { disk -> session = session.finishInstallation(disk) })
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedItemId by rememberSaveable { mutableStateOf<Int?>(null) }
    var tool by rememberSaveable(session.stage) { mutableStateOf<BenchTool?>(null) }
    val screwdriverSelected = tool == BenchTool.Driver
    var holdingTarget by remember { mutableStateOf<WorkbenchHold?>(null) }
    var draggedPart by remember { mutableStateOf<InventoryDrag?>(null) }
    var holdPoint by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var workbench by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val assembly = session.assembly
    val hasMachine = session.stage == OrderStage.Active
    val selection = ComputerPart.entries.firstOrNull { it.name == selectedName }
    val currentDrag = draggedPart
    val hoveredSlot = currentDrag?.let { placementSlot(it.item.type, assembly, viewport, it.position) }

    fun selectPart(part: ComputerPart) {
        selectedItemId = null
        selectedName = part.name
    }

    fun canHold(target: WorkbenchHold): Boolean {
        if (tabletOpen || session.stage != OrderStage.Active) return false
        val screw = target.screw ?: return session.assembly.canRemove(target.part)
        return screwdriverSelected && session.assembly.screwAction(target.part, screw.index) == screw.action
    }

    fun changeHold(target: WorkbenchHold, point: Offset?) {
        if (point == null) {
            if (holdingTarget == target) holdingTarget = null
            return
        }
        if (draggedPart == null && holdingTarget == null) {
            selectPart(target.part)
            holdPoint = point
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
        } else {
            session = session.turnScrew(part, screw.index, screw.action)
        }
    }

    fun selectItem(item: InventoryItem) {
        selectedItemId = item.id
        selectedName = null
    }

    fun moveInventoryPart(item: InventoryItem, position: Offset) {
        val active = draggedPart
        if (!hasMachine || tabletOpen || (active != null && active.item.id != item.id)) return
        if (active == null) selectItem(item)
        draggedPart = InventoryDrag(item, position)
    }

    fun finishInventoryDrag(item: InventoryItem, released: Boolean) {
        val active = draggedPart?.takeIf { it.item.id == item.id } ?: return
        draggedPart = null
        if (!released || tabletOpen) return
        val slot = placementSlot(item.type, session.assembly, viewport, active.position) ?: return
        session = session.install(item.id, slot)
        selectedItemId = null
        selectedName = slot.name.takeIf { slot.screws.isNotEmpty() }
    }

    if (monitorOpen) {
        StartupMonitor(power, session, onClose = { monitorOpen = false })
        return
    }

    Box(
        Modifier.fillMaxSize().onGloballyPositioned { workbench = it },
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxSize()) {
                if (artwork.ready) WorkbenchScene(artwork, assembly.takeIf { hasMachine }, selection,
                    session.inventory.firstOrNull { it.id == selectedItemId }?.type,
                    screwdriverSelected, tool == BenchTool.Paste, session.parts[ComputerPart.Processor]?.hasPaste == true,
                    { session = session.applyPasteToMountedCpu() }, hoveredSlot, { viewport = it }, ::selectPart, ::changeHold, ::finishHold,
                    Modifier.fillMaxSize())

            }
            WorkshopControlRail("开机检测", if (hasMachine) ({ monitorOpen = true }) else null) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(ToolGap)) {
                    ToolSlot("螺丝刀", { tool = if (screwdriverSelected) null else BenchTool.Driver }, hasMachine, screwdriverSelected) {
                        ScrewdriverThumbnail(artwork, Modifier.size(38.dp))
                    }
                    ToolSlot("硅脂", { tool = if (tool == BenchTool.Paste) null else BenchTool.Paste },
                        hasMachine || session.inventory.any { it.type == ComputerPart.Processor }, tool == BenchTool.Paste) {
                        PasteThumbnail(Modifier.size(34.dp))
                    }
                }
                Inventory(artwork, session.inventory, selectedItemId,
                    { item ->
                        selectItem(item)
                        if (tool == BenchTool.Paste) session = session.applyPasteToItem(item.id)
                    }, ::moveInventoryPart, ::finishInventoryDrag)
            }
        }
        holdingTarget?.let { target ->
            key(target, holdPoint) {
                val progress = remember { Animatable(0f) }
                LaunchedEffect(Unit) { progress.animateTo(1f, tween(target.durationMillis, easing = LinearEasing)) }
                Canvas(Modifier.matchParentSize()) {
                    val r = 12.dp.toPx()
                    val touch = workbench?.takeIf { it.isAttached }?.windowToLocal(holdPoint) ?: return@Canvas
                    val x = if (touch.x + 3*r < size.width) touch.x + 2*r else touch.x - 2*r
                    val y = if (touch.y > 3*r) touch.y - 2*r else touch.y + 2*r
                    val at = Offset(x.coerceIn(r,size.width-r), y.coerceIn(r,size.height-r))
                    drawCircle(Background.copy(alpha=.9f), r, at)
                    drawCircle(Border, r, at, style = Stroke(3.dp.toPx()))
                    drawArc(Amber, -90f, 360f*progress.value, false, at - Offset(r,r),
                        Size(2*r,2*r), style = Stroke(3.dp.toPx()))
                }
            }
        }
        if (currentDrag != null) {
            Canvas(Modifier.matchParentSize()) {
                drawPartPreview(
                    artwork,
                    currentDrag.item.type,
                    workbench?.takeIf { it.isAttached }?.windowToLocal(currentDrag.position) ?: return@Canvas,
                    Size(95.dp.toPx(), 76.dp.toPx()),
                )
            }
        }
    }
}

@Composable
internal fun WorkshopControlRail(
    switchLabel: String,
    onSwitch: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val width = ToolSlotSize * 2 + ToolGap + RailPadding * 2
    Column(Modifier.background(Panel).windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.End))
        .width(width).fillMaxHeight().border(1.dp, Border).padding(RailPadding),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().height(TabletButtonWidth), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (onSwitch != null) {
                Box(Modifier.weight(1f).fillMaxHeight().background(SlotBackground).border(1.dp, Border)
                    .clickable(role = Role.Button, onClick = onSwitch), contentAlignment = Alignment.Center) {
                    GameText(switchLabel, fontSize = 14)
                }
            }
            Spacer(Modifier.width(TabletButtonWidth))
        }
        content()
    }
}

@Composable
private fun ToolSlot(label: String, onClick: () -> Unit, enabled: Boolean = true,
    active: Boolean = false, icon: @Composable () -> Unit) {
    Box(Modifier.size(ToolSlotSize).background(if (active) Color(0xFF392E22) else SlotBackground)
        .border(if (active) 2.dp else 1.dp, if (active) Amber else Border)
        .semantics { contentDescription = label; selected = active }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) { icon() }
}

@Composable
private fun Inventory(
    artwork: WorkbenchArtwork,
    parts: List<InventoryItem>,
    selectedId: Int?,
    onSelect: (InventoryItem) -> Unit,
    onMove: (InventoryItem, Offset) -> Unit,
    onFinish: (InventoryItem, Boolean) -> Unit,
) {
    Column(
        Modifier.fillMaxWidth(),
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().background(SlotBackground).border(1.dp, Border).padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(parts, key = { it.id }) { item ->
                InventorySlot(artwork, item, item.id == selectedId, onSelect, onMove, onFinish)
            }
        }
    }
}

@Composable
private fun InventorySlot(
    artwork: WorkbenchArtwork,
    item: InventoryItem,
    active: Boolean,
    onSelect: (InventoryItem) -> Unit,
    onMove: (InventoryItem, Offset) -> Unit,
    onFinish: (InventoryItem, Boolean) -> Unit,
) {
    var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val currentMove by rememberUpdatedState(onMove)
    val currentFinish by rememberUpdatedState(onFinish)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .background(if (active) Color(0xFF302820) else SlotBackground)
            .border(if (active) 2.dp else 1.dp, if (active) Amber else Border)
            .onGloballyPositioned { coordinates = it }
            .pointerInput(item.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Drag across the tray to place a part, or along it to scroll.
                    val start = (if (down.type == PointerType.Mouse) {
                        awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    } else {
                        awaitHorizontalTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                    }) ?: return@awaitEachGesture
                    val layout = coordinates?.takeIf { it.isAttached } ?: return@awaitEachGesture

                    var released = false
                    try {
                        currentMove(item, layout.localToWindow(start.position))
                        released = drag(start.id) { change ->
                            if (layout.isAttached) currentMove(item, layout.localToWindow(change.position))
                            change.consume()
                        }
                        val release = currentEvent.changes.firstOrNull { it.id == start.id }
                        if (released && release != null && layout.isAttached) {
                            currentMove(item, layout.localToWindow(release.position))
                        }
                    } finally {
                        currentFinish(item, released)
                    }
                }
            }
            .clickable(role = Role.Button) { onSelect(item) }
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PartThumbnail(artwork, item.type, Modifier.size(width = 44.dp, height = 22.dp))
        Spacer(Modifier.height(2.dp))
        GameText(item.label, fontSize = 12)
        if (item.part.hasPaste) GameText("已涂硅脂", fontSize = 11, color = Amber)
        GameText("耐久 ${item.part.durability}", fontSize = 11, color = if (item.part.working) MutedText else Amber)
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
