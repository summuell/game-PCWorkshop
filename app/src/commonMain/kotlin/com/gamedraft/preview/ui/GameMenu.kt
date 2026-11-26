package com.gamedraft.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun FloatingMenuButton(onOpen: () -> Unit) {
    var x by rememberSaveable { mutableStateOf(.5f) }
    var y by rememberSaveable { mutableStateOf(0f) }
    BoxWithConstraints(Modifier.fillMaxSize().displayCutoutPadding().padding(4.dp)) {
        val buttonSize = 44.dp
        val buttonPixels = with(androidx.compose.ui.platform.LocalDensity.current) { buttonSize.roundToPx() }
        val travelX = (constraints.maxWidth - buttonPixels).coerceAtLeast(0)
        val travelY = (constraints.maxHeight - buttonPixels).coerceAtLeast(0)
        Box(Modifier.offset { IntOffset((x * travelX).roundToInt(), (y * travelY).roundToInt()) }.size(buttonSize)
            .pointerInput(travelX, travelY) {
                detectDragGestures { change, delta ->
                    change.consume()
                    if (travelX > 0) x = (x + delta.x / travelX).coerceIn(0f, 1f)
                    if (travelY > 0) y = (y + delta.y / travelY).coerceIn(0f, 1f)
                }
            }.semantics { contentDescription = "游戏菜单" }.clickable(role = Role.Button, onClick = onOpen),
            contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(34.dp).background(Panel.copy(alpha = .9f)).border(1.dp, Border).padding(8.dp)) {
                for (line in 0..2) {
                    val height = size.height * (.18f + line * .32f)
                    drawLine(Amber, Offset(0f, height), Offset(size.width, height), 2.dp.toPx())
                }
            }
        }
    }
}

internal enum class MenuPage { Main, Save, Load, Exit }

@Composable
internal fun GameMenu(
    storage: SaveStorage, session: RepairSession, onClose: () -> Unit, onHome: () -> Unit,
    onLoaded: (RepairSession) -> Unit, onExit: () -> Unit,
) {
    var page by remember { mutableStateOf(MenuPage.Main) }
    var overwrite by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf("") }
    var revision by remember { mutableStateOf(0) }

    fun go(target: MenuPage) {
        page = target
        feedback = ""
        overwrite = null
    }

    fun save(slot: Int) {
        runCatching { storage.write(slot, GameSave(storage.timestamp(), session).encode()) }.fold(
            onSuccess = {
                overwrite = null
                revision++
                feedback = "已保存到存档 $slot"
            },
            onFailure = { feedback = "保存失败，请检查本地存储空间或权限。" },
        )
    }

    Box(Modifier.fillMaxSize().background(Background.copy(alpha = .88f))
        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        contentAlignment = Alignment.Center) {
        Column(Modifier.displayCutoutPadding().padding(8.dp).widthIn(max = 430.dp).fillMaxWidth()
            .background(Panel).border(1.dp, Border).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                GameText(when(page) {
                    MenuPage.Main -> "游戏菜单"
                    MenuPage.Save -> "保存进度"
                    MenuPage.Load -> "载入存档"
                    MenuPage.Exit -> "退出游戏"
                }, fontSize = 20)
                GameButton(if (page == MenuPage.Main) "继续" else "返回",
                    { if (page == MenuPage.Main) onClose() else go(MenuPage.Main) })
            }
            when (page) {
                MenuPage.Main -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        GameButton("保存进度", { go(MenuPage.Save) }, Modifier.weight(1f))
                        GameButton("载入存档", { go(MenuPage.Load) }, Modifier.weight(1f))
                    }
                    GameButton("返回主界面", onHome, Modifier.fillMaxWidth())
                    GameButton("退出游戏", { go(MenuPage.Exit) }, Modifier.fillMaxWidth())
                }
                MenuPage.Exit -> {
                    GameText("退出后将丢失未保存的进度。", color = MutedText)
                    GameButton("确认退出", onExit, Modifier.fillMaxWidth())
                }
                MenuPage.Save, MenuPage.Load -> {
                    GameText("载入后回到工作台，主机为关机状态。", fontSize = 13, color = MutedText)
                    for (slot in 1..3) {
                        val result = remember(slot, revision, page) { runCatching { storage.read(slot)?.let(GameSave::decode) } }
                        val snapshot = result.getOrNull()
                        Row(Modifier.fillMaxWidth().background(SlotBackground).padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                GameText("存档 $slot", fontSize = 14)
                                GameText(snapshot?.savedAt ?: if (result.isFailure) "无法读取" else "空存档",
                                    fontSize = 12, color = MutedText)
                            }
                            if (page == MenuPage.Load) {
                                GameButton("载入", {
                                    runCatching { GameSave.decode(requireNotNull(storage.read(slot))) }.fold(
                                        onSuccess = { onLoaded(it.session) }, onFailure = { feedback = "无法读取该存档" })
                                }, enabled = snapshot != null)
                            } else {
                                GameButton(if (overwrite == slot) "确认覆盖" else "保存", {
                                    if (overwrite == slot || (snapshot == null && result.isSuccess)) save(slot)
                                    else overwrite = slot
                                })
                            }
                        }
                    }
                    if (feedback.isNotEmpty()) GameText(feedback, fontSize = 14, color = Amber)
                }
            }
        }
    }
}
