package com.gamedraft.preview

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val MonitorFrame = Color(0xFF303F49)
private val MonitorEdge = Color(0xFF71808A)
private val MonitorGlass = Color(0xFF0C141B)
private val StartupGreen = Color(0xFF7CE0A6)

@Composable
internal fun StartupMonitor(
    stopAt: StartupStage?,
    onClose: () -> Unit,
    onPassed: () -> Unit,
) {
    var stage by remember { mutableStateOf(StartupStage.Power) }
    var stalled by remember { mutableStateOf(false) }
    val finishStartup by rememberUpdatedState(onPassed)

    LaunchedEffect(stopAt) {
        for (step in StartupStage.entries) {
            stage = step
            delay(step.durationMillis)
            if (step == stopAt) {
                stalled = true
                return@LaunchedEffect
            }
        }
        finishStartup()
    }

    Column(
        Modifier.fillMaxSize().padding(12.dp).background(Panel).border(2.dp, Border).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameText("显示器", fontSize = 22, modifier = Modifier.semantics { heading() })
            GameButton("返回工作台", onClose)
        }
        Box(
            Modifier.fillMaxWidth().weight(1f).background(SlotBackground).border(1.dp, Border),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.widthIn(max = 480.dp).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Monitor(stage, stalled)
                if (stalled || stage == StartupStage.Desktop) {
                    GameText(
                        when (stage) {
                            StartupStage.Power -> "主机电源灯未亮"
                            StartupStage.SelfTest -> "自检指示灯常亮"
                            StartupStage.Video -> "主机风扇运转中"
                            StartupStage.Desktop -> "开机检测通过"
                            else -> "启动已暂停"
                        },
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        color = if (stalled) MutedText else StartupGreen,
                    )
                }
            }
        }
    }
}

@Composable
private fun Monitor(stage: StartupStage, stalled: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.fillMaxWidth().background(MonitorFrame).border(2.dp, MonitorEdge)
                .padding(start = 7.dp, top = 7.dp, end = 7.dp, bottom = 2.dp),
        ) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 10f).background(MonitorGlass)
                    .border(2.dp, Background),
                contentAlignment = Alignment.Center,
            ) {
                when (stage) {
                    StartupStage.Power, StartupStage.SelfTest, StartupStage.Video -> if (stalled) {
                        GameText(
                            "无信号",
                            modifier = Modifier.background(Panel).border(1.dp, Border)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 14,
                            color = MutedText,
                        )
                    }
                    StartupStage.BootDrive -> GameText(
                        if (stalled) "未找到启动设备" else "固件自检完成",
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                        fontSize = 14,
                        color = MutedText,
                    )
                    StartupStage.SystemLoading -> SystemLoading()
                    StartupStage.Welcome -> Column(
                        Modifier.fillMaxSize().background(Brush.verticalGradient(
                            listOf(Color(0xFF294D69), Color(0xFF163746)),
                        )),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    ) {
                        Canvas(Modifier.size(30.dp)) {
                            drawCircle(MutedText, size.width * .19f, Offset(center.x, size.height * .26f))
                            drawArc(MutedText, 180f, 180f, true, Offset(0f, size.height * .52f),
                                Size(size.width, size.height * .75f))
                        }
                        GameText("欢迎", fontSize = 16)
                    }
                    StartupStage.Desktop -> Desktop()
                }
            }
            Box(
                Modifier.fillMaxWidth().height(12.dp).padding(end = 4.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                val led = if (stage >= StartupStage.BootDrive) StartupGreen else Amber
                Box(Modifier.size(5.dp).background(led))
            }
        }
        Box(Modifier.size(32.dp, 14.dp).background(MonitorFrame))
        Box(Modifier.size(106.dp, 7.dp).background(MonitorEdge))
    }
}

@Composable
private fun SystemLoading() {
    val transition = rememberInfiniteTransition()
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing)),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Canvas(Modifier.size(38.dp)) {
            // Stepped mountain silhouette used as the system's boot emblem.
            val tile = size.width / 10
            for (row in 0..4) {
                drawRect(Color(0xFF8FD9E1), Offset((4 - row) * tile, (2 + row) * tile),
                    Size((1 + 2 * row) * tile, tile))
            }
            drawRect(Color(0xFF397789), Offset(4 * tile, 5 * tile), Size(4 * tile, tile))
        }
        Canvas(Modifier.size(18.dp)) {
            for (dot in 0..5) {
                val angle = (rotation - dot * 32) * PI / 180
                drawCircle(Color.White.copy(alpha = 1f - dot * .14f), size.width * .075f,
                    Offset(center.x + cos(angle).toFloat() * size.width * .39f,
                        center.y + sin(angle).toFloat() * size.height * .39f))
            }
        }
    }
}

@Composable
private fun Desktop() {
    Canvas(Modifier.fillMaxSize()) {
        val pixel = size.width / 64
        fun block(x: Int, y: Int, width: Int, height: Int, color: Color) {
            drawRect(color, Offset(x * pixel, y * pixel), Size(width * pixel, height * pixel))
        }

        drawRect(Color(0xFF326673))
        val hillside = Color(0xFF224B5B)
        block(49, 12, 15, 25, hillside)
        block(42, 17, 7, 20, hillside)
        block(35, 22, 7, 15, hillside)
        block(26, 29, 9, 8, hillside)
        block(0, 32, 26, 5, Color(0xFF285767))

        block(4, 4, 3, 1, Amber)
        block(4, 5, 7, 4, Amber)
        block(5, 6, 5, 2, Color(0xFFFFD29E))
        block(4, 12, 5, 6, MutedText)
        block(5, 14, 3, 1, MonitorFrame)
        block(5, 16, 3, 1, MonitorFrame)

        block(0, 37, 64, 3, MonitorGlass)
        block(2, 38, 2, 1, StartupGreen)
        block(6, 38, 2, 1, MutedText)
        block(57, 38, 1, 1, StartupGreen)
        block(60, 38, 2, 1, MutedText)
    }
}
