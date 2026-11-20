package com.gamedraft.preview

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gamedraft.preview.resources.Res
import com.gamedraft.preview.resources.screen_labels
import com.gamedraft.preview.resources.workbench_assembled
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.imageResource

private val Background = Color(0xFF10171F)
private val Panel = Color(0xFF1C2832)
private val SlotBackground = Color(0xFF141D25)
private val Border = Color(0xFF455764)
private val TextColor = Color(0xFFF3EAE0)
private val MutedText = Color(0xFFB7C3CA)
private val Amber = Color(0xFFFFBC70)
private val DarkInk = Color(0xFF302012)
private val GameFont = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }

private val AssemblySaver = listSaver<ComputerAssembly, String>(
    save = { assembly -> assembly.inventory.map { it.name } },
    restore = { names -> ComputerAssembly(names.map(ComputerPart::valueOf)) },
)

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
            imageResource(Res.drawable.workbench_assembled),
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
    var assembly by rememberSaveable(stateSaver = AssemblySaver) { mutableStateOf(ComputerAssembly()) }
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selection = selectedName?.let(ComputerPart::valueOf)

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
                assembly = assembly,
                selectedPart = selection,
                onSelect = { selectedName = it.name },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            PartControls(
                assembly = assembly,
                selection = selection,
                onSelect = { selectedName = it.name },
                onToggle = {
                    if (selectedName == it.name) {
                        assembly = assembly.toggle(it)
                        selectedName = null
                    }
                },
            )
            Inventory(
                artwork = artwork,
                parts = assembly.inventory,
                selection = selection,
                onSelect = { selectedName = it.name },
            )
        } else {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                GameText("正在载入工作台", color = MutedText)
            }
        }
    }
}

@Composable
private fun PartControls(
    assembly: ComputerAssembly,
    selection: ComputerPart?,
    onSelect: (ComputerPart) -> Unit,
    onToggle: (ComputerPart) -> Unit,
) {
    val blocker = selection?.let(assembly::blockerFor)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 66.dp).background(Panel).padding(horizontal = 14.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (selection == null) {
                val hint = if (assembly.isInstalled(ComputerPart.SidePanel)) {
                    "点选机箱盖开始拆装"
                } else {
                    "点选零件拆卸或从物品栏装回"
                }
                GameText(hint, color = MutedText)
            } else {
                GameText(selection.label, fontSize = 18)
                if (blocker != null) GameText(blocker.instruction, fontSize = 14, color = Amber)
            }
        }
        if (selection != null) {
            if (blocker != null) {
                GameButton("定位", onClick = { onSelect(blocker.part) })
            } else {
                val action = if (assembly.isInstalled(selection)) "拆卸" else "装回"
                GameButton(action, onClick = { onToggle(selection) }, primary = true)
            }
        }
    }
}

@Composable
private fun Inventory(
    artwork: WorkbenchArtwork,
    parts: List<ComputerPart>,
    selection: ComputerPart?,
    onSelect: (ComputerPart) -> Unit,
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
                    val active = part == selection
                    Column(
                        Modifier
                            .width(92.dp)
                            .height(72.dp)
                            .background(if (active) Color(0xFF302820) else SlotBackground)
                            .border(if (active) 2.dp else 1.dp, if (active) Amber else Border)
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
            }
        }
    }
}

@Composable
private fun GameButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    Box(
        modifier
            .heightIn(min = if (primary) 46.dp else 42.dp)
            .widthIn(min = 70.dp)
            .background(if (primary) Amber else SlotBackground)
            .border(1.dp, if (primary) Color(0xFFC88D52) else Border)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        GameText(label, color = if (primary) DarkInk else TextColor)
    }
}

@Composable
private fun GameText(text: String, modifier: Modifier = Modifier, fontSize: Int = 16, color: Color = TextColor) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontFamily = GameFont.current,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 5).sp,
        ),
    )
}
