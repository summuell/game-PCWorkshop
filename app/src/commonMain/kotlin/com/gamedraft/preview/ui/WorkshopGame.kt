package com.gamedraft.preview

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
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
import kotlin.math.abs

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

internal class WorkbenchPress(val target: WorkbenchHold, val point: Offset, val pointerId: PointerId) {
    private val started = kotlin.time.TimeSource.Monotonic.markNow()
    val fraction: Float get() = (started.elapsedNow().inWholeMilliseconds.toFloat() / target.durationMillis).coerceIn(0f, 1f)
    var completed by mutableStateOf(false)
        private set

    fun complete() { completed = true }
}

private data class InventoryContact(val item: InventoryItem, val id: PointerId, val start: Offset)
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
    var tabletPage by rememberSaveable { mutableStateOf(TabletPage.Shop) }
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
                                    onDeposit = { sessionState.value = sessionState.value.deposit(it) },
                                    onWithdraw = { sessionState.value = sessionState.value.withdraw(it) })
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
                                    tabletOpen = true
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
    var monitorOpen by remember { mutableStateOf(false) }
    // Inventory cells survive tablet/menu transitions and can retain their press callback.
    val acceptsWorkbenchInput by rememberUpdatedState(!paused && !tabletOpen && !monitorOpen)
    val power = remember(session.assembly, session.parts[ComputerPart.Processor]?.hasPaste) { ComputerPower() }
    RunComputer(power, session, paused, onInstall = { disk -> session = session.finishInstallation(disk) })
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedItemId by rememberSaveable { mutableStateOf<Int?>(null) }
    var tool by rememberSaveable { mutableStateOf<BenchTool?>(null) }
    val screwdriverSelected = tool == BenchTool.Driver
    var press by remember { mutableStateOf<WorkbenchPress?>(null) }
    val contactPoints = remember { mutableMapOf<PointerId, Offset>() }
    LaunchedEffect(monitorOpen, tabletOpen, paused) { press = null }
    var draggedPart by remember { mutableStateOf<InventoryDrag?>(null) }
    var inventoryContact by remember { mutableStateOf<InventoryContact?>(null) }
    var workbench by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var mat by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val assembly = session.assembly
    val hasMachine = session.chassis != null
    val selection = ComputerPart.entries.firstOrNull { it.name == selectedName }
    val currentDrag = draggedPart
    val placementItem = currentDrag?.item ?: session.inventory.firstOrNull { it.id == selectedItemId }
    val placementSlots = placementItem?.let { item ->
        item.type.compatibleSlots.filter { session.canInstall(item, it) }
    }.orEmpty()
    val hoveredSlot = currentDrag?.let { placementSlot(placementSlots, BoardGeometry[session.boardLayout], workbench, mat, it.position) }

    fun selectPart(part: ComputerPart) {
        selectedItemId = null
        selectedName = part.name
    }

    fun canHold(target: WorkbenchHold): Boolean {
        if (!acceptsWorkbenchInput) return false
        val screw = target.screw ?: return session.assembly.canRemove(target.part)
        return screwdriverSelected && session.assembly.screwAction(target.part, screw.index) == screw.action
    }

    fun beginHold(target: WorkbenchHold, id: PointerId): WorkbenchPress? {
        if (draggedPart != null || press != null) return null
        selectPart(target.part)
        if (!canHold(target)) return null
        val point = contactPoints[id] ?: return null
        return WorkbenchPress(target, point, id).also { press = it }
    }

    fun performHold(target: WorkbenchHold): Boolean {
        if (draggedPart != null || !canHold(target)) return false
        val part = target.part
        val screw = target.screw
        if (screw == null) {
            session = session.remove(part)
            selectedName = null
        } else {
            selectPart(part)
            session = session.turnScrew(part, screw.index, screw.action)
        }
        return true
    }

    fun endHold(gesture: WorkbenchPress, finished: Boolean) {
        if (press !== gesture) return
        if (finished && performHold(gesture.target)) gesture.complete() else press = null
    }

    fun selectItem(item: InventoryItem) {
        selectedItemId = item.id
        selectedName = null
    }

    fun armInventoryDrag(item: InventoryItem, id: PointerId) {
        val start = contactPoints[id] ?: return
        if (acceptsWorkbenchInput && press == null && inventoryContact == null) {
            inventoryContact = InventoryContact(item, id, start)
        }
    }

    fun dropItem(item: InventoryItem, point: Offset) {
        val available = item.type.compatibleSlots.filter { session.canInstall(item, it) }
        val slot = placementSlot(available, BoardGeometry[session.boardLayout], workbench, mat, point) ?: return
        session = session.install(item.id, slot)
        selectedItemId = null
        selectedName = slot.name.takeIf { slot.screws.isNotEmpty() }
    }

    if (monitorOpen) {
        StartupMonitor(power, session, paused || tabletOpen, onClose = { monitorOpen = false })
        return
    }

    Box(
        Modifier.fillMaxSize().onGloballyPositioned { workbench = it }.pointerInput(paused, tabletOpen) {
            // Observe before tiny screw zones consume the press. These coordinates are shared with the overlay.
            try {
                awaitPointerEventScope {
                    while (true) {
                        for (change in awaitPointerEvent(PointerEventPass.Initial).changes) {
                            if (change.pressed && !change.previousPressed) {
                                contactPoints[change.id] = change.position
                            }
                            val contact = inventoryContact
                            if (contact != null && change.id == contact.id) {
                                val delta = change.position - contact.start
                                if (draggedPart == null && change.pressed) {
                                    val horizontal = abs(delta.x)
                                    val vertical = abs(delta.y)
                                    if (horizontal >= 2.dp.toPx() && horizontal >= vertical * 0.7f) {
                                        selectItem(contact.item)
                                        draggedPart = InventoryDrag(contact.item, change.position)
                                    } else if (vertical >= viewConfiguration.touchSlop) {
                                        inventoryContact = null
                                    }
                                }
                                if (draggedPart != null) {
                                    if (change.pressed) draggedPart = InventoryDrag(contact.item, change.position)
                                    else if (!change.isConsumed) dropItem(contact.item, change.position)
                                    change.consume()
                                }
                                if (!change.pressed) {
                                    draggedPart = null
                                    inventoryContact = null
                                }
                            }
                            if (!change.pressed) {
                                contactPoints.remove(change.id)
                                if (press?.pointerId == change.id) press = null
                            }
                        }
                    }
                }
            } finally {
                inventoryContact = null
                draggedPart = null
                contactPoints.clear()
                press = null
            }
        },
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxSize().onGloballyPositioned { mat = it }) {
                if (artwork.ready) WorkbenchScene(artwork, assembly, session.parts, BoardGeometry[session.boardLayout], selection, placementSlots,
                    screwdriverSelected, tool == BenchTool.Paste, session.parts[ComputerPart.Processor]?.hasPaste == true,
                    { session = session.applyPasteToMountedCpu() }, hoveredSlot, ::selectPart, ::beginHold, ::endHold, ::performHold,
                    Modifier.fillMaxSize())
                if (hasMachine) {
                    GameButton("收纳机箱", {
                        session = session.storeComputer()
                        selectedName = null
                        selectedItemId = null
                    }, Modifier.align(Alignment.BottomStart).padding(8.dp),
                        enabled = !paused && !tabletOpen && press == null && draggedPart == null)
                }
            }
            WorkshopControlRail("开机检测", { monitorOpen = true }) {
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
                    }, ::armInventoryDrag)
            }
        }
        press?.let { gesture ->
            key(gesture) { HoldRing(gesture, Modifier.matchParentSize()) }
        }
        if (currentDrag != null) {
            Canvas(Modifier.matchParentSize()) {
                drawPartPreview(
                    artwork,
                    currentDrag.item.type,
                    currentDrag.item.part.product,
                    currentDrag.position,
                    Size(95.dp.toPx(), 76.dp.toPx()),
                )
            }
        }
    }
}

@Composable
private fun HoldRing(press: WorkbenchPress, modifier: Modifier) {
    var fraction by remember { mutableFloatStateOf(press.fraction) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(press.completed) {
        if (press.completed) {
            scale.animateTo(1.16f, tween(110))
            scale.animateTo(1f, tween(170))
        } else {
            while (fraction < 1f) withFrameNanos { fraction = press.fraction }
        }
    }

    Canvas(modifier) {
        val center = press.point
        val radius = 36.dp.toPx() * scale.value
        val stroke = 4.dp.toPx() * scale.value
        drawCircle(Background.copy(alpha = .18f), radius, center)
        drawCircle(Background.copy(alpha = .88f), radius, center, style = Stroke(stroke + 2.dp.toPx()))
        drawCircle(Border, radius, center, style = Stroke(stroke))
        if (press.completed) {
            drawCircle(Color(0xFF7CE0A6), radius, center, style = Stroke(stroke))
        } else {
            drawArc(Amber, -90f, 360f * fraction, false, center - Offset(radius, radius),
                Size(2 * radius, 2 * radius), style = Stroke(stroke))
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
    onPressItem: (InventoryItem, PointerId) -> Unit,
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
                InventorySlot(artwork, item, item.id == selectedId, onSelect, onPressItem)
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
    onPressItem: (InventoryItem, PointerId) -> Unit,
) {
    val pressItem by rememberUpdatedState(onPressItem)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .background(if (active) Color(0xFF302820) else SlotBackground)
            .border(if (active) 2.dp else 1.dp, if (active) Amber else Border)
            .pointerInput(item) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    pressItem(item, down.id)
                }
            }
            .clickable(role = Role.Button) { onSelect(item) }
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PartThumbnail(artwork, item.type, item.part.product, Modifier.size(width = 44.dp, height = 28.dp))
        Spacer(Modifier.height(2.dp))
        GameText(item.label, fontSize = 12)
        item.storedComputer?.let { computer ->
            GameText("已收纳 · ${computer.parts.size - 1} 件", fontSize = 11, color = Amber)
        }
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
