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
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val MonitorFrame = Color(0xFF303F49)
private val MonitorEdge = Color(0xFF71808A)
private val MonitorGlass = Color(0xFF0C141B)
private val StartupGreen = Color(0xFF7CE0A6)
private const val ThermalShutdownMillis = 5000L

internal class ComputerPower {
    var cycle by mutableStateOf(0L)
        private set
    var requested by mutableStateOf(false)
        private set
    var powered by mutableStateOf(false)
    var stage by mutableStateOf(StartupStage.Power)
    var stalled by mutableStateOf(false)

    fun start() {
        stop()
        requested = true
    }

    fun stop() {
        cycle++
        requested = false
        powered = false
        stage = StartupStage.Power
        stalled = false
    }
}

@Composable
internal fun RunComputer(power: ComputerPower, session: RepairSession, onPassed: () -> Unit) {
    val finishStartup by rememberUpdatedState(onPassed)
    val cycle = power.cycle
    LaunchedEffect(power, cycle) {
        if (!power.requested) return@LaunchedEffect
        val failure = session.firstStartupFailure()
        if (failure == StartupStage.Power) {
            power.stop()
            return@LaunchedEffect
        }
        power.powered = true
        if (!session.coolingAvailable) {
            launch {
                delay(ThermalShutdownMillis)
                if (power.cycle == cycle) power.stop()
            }
        }
        for (step in StartupStage.entries) {
            if (power.cycle != cycle) return@LaunchedEffect
            power.stage = step
            delay(step.durationMillis)
            if (step == failure) {
                if (power.cycle == cycle) power.stalled = true
                return@LaunchedEffect
            }
        }
        if (power.cycle == cycle && session.coolingAvailable) finishStartup()
    }
}

@Composable
internal fun StartupMonitor(power: ComputerPower, onClose: () -> Unit) {
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
                Monitor(power)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.height(46.dp).background(if (power.powered) Color(0xFF193D38) else Panel)
                            .border(2.dp, if (power.powered) StartupGreen else Border)
                            .semantics { stateDescription = if (power.powered) "已开机" else "已关机" }
                            .clickable(role = Role.Button) { if (power.requested) power.stop() else power.start() }
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Canvas(Modifier.size(16.dp)) {
                            val color = if (power.powered) StartupGreen else MutedText
                            if (power.powered) drawCircle(StartupGreen.copy(alpha = .2f))
                            drawArc(color, -55f, 290f, false, Offset(size.width * .1f, size.height * .16f),
                                Size(size.width * .8f, size.height * .8f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                            drawLine(color, Offset(center.x, 0f), Offset(center.x, center.y), 2.dp.toPx())
                        }
                        GameText("电源", color = if (power.powered) StartupGreen else MutedText)
                    }
                    GameButton("重启", power::start, enabled = power.powered)
                }
            }
        }
    }
}

@Composable
private fun Monitor(power: ComputerPower) {
    val stage = power.stage
    val stalled = power.stalled
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
                if (power.powered) when (stage) {
                    StartupStage.Power, StartupStage.SelfTest, StartupStage.Video -> if (stalled) {
                        GameText(
                            "无信号",
                            modifier = Modifier.background(Panel).border(1.dp, Border)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            fontSize = 14,
                            color = MutedText,
                        )
                    }
                    StartupStage.BootDrive -> if (stalled) {
                        Box(Modifier.align(Alignment.TopStart).padding(12.dp).size(8.dp, 2.dp).background(MutedText))
                    }
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
                val led = if (power.powered && stage >= StartupStage.BootDrive) StartupGreen else Border
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
