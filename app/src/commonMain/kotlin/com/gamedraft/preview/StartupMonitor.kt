package com.gamedraft.preview

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.flow.first
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.clipToBounds
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

internal enum class BootSource { Disk, Usb }

internal class ComputerPower {
    var cycle by mutableStateOf(0L)
        private set
    var requested by mutableStateOf(false)
        private set
    var powered by mutableStateOf(false)
    var stage by mutableStateOf(StartupStage.Power)
    var stalled by mutableStateOf(false)
    var usb by mutableStateOf(false)
        private set
    val choosing: Boolean
        get() = powered && stage == StartupStage.BootDrive && usb && source == null
    var source by mutableStateOf<BootSource?>(BootSource.Disk)
        private set
    var installer by mutableStateOf(false)
    var destination by mutableStateOf<ComputerPart?>(null)
    var percent by mutableStateOf(0)

    fun toggleUsb() {
        usb = !usb
        if (!usb) {
            if (requested && source == BootSource.Usb) stop()
            source = BootSource.Disk
        }
    }

    fun choose(device: BootSource) {
        if (choosing) source = device
    }

    fun start(device: BootSource? = null) {
        stop()
        requested = true
        source = if (usb) device else BootSource.Disk
    }

    fun stop() {
        cycle++
        requested = false
        powered = false
        stage = StartupStage.Power
        stalled = false
        source = BootSource.Disk
        installer = false
        destination = null
        percent = 0
    }
}

private suspend fun gameDelay(milliseconds: Long, paused: androidx.compose.runtime.State<Boolean>) {
    var remaining = milliseconds
    while (true) {
        androidx.compose.runtime.snapshotFlow { paused.value }.first { !it }
        if (remaining <= 0) return
        val start = kotlin.time.TimeSource.Monotonic.markNow()
        val interrupted = kotlinx.coroutines.withTimeoutOrNull(remaining) {
            androidx.compose.runtime.snapshotFlow { paused.value }.first { it }
        }
        if (interrupted == null) return
        remaining -= start.elapsedNow().inWholeMilliseconds
    }
}

@Composable
internal fun RunComputer(power: ComputerPower, session: RepairSession, paused: Boolean, onPassed: () -> Unit,
    onInstall: (ComputerPart) -> Unit) {
    val menuPaused = rememberUpdatedState(paused)
    val finishStartup by rememberUpdatedState(onPassed)
    val finishInstallation by rememberUpdatedState(onInstall)
    val cycle = power.cycle
    LaunchedEffect(power, cycle) {
        if (!power.requested) return@LaunchedEffect
        val failure = session.firstStartupFailure()
        if (failure == StartupStage.Power) {
            power.stop()
            return@LaunchedEffect
        }
        power.powered = true
        if (!session.coolingAvailable) launch {
            gameDelay(ThermalShutdownMillis, menuPaused)
            if (power.cycle == cycle) power.stop()
        }
        for (step in StartupStage.entries) {
            if (power.cycle != cycle) return@LaunchedEffect
            power.stage = step
            if (step == StartupStage.BootDrive && power.choosing) {
                androidx.compose.runtime.snapshotFlow { power.choosing }.first { !it }
            }
            gameDelay(step.durationMillis, menuPaused)
            val usbBoot = power.source == BootSource.Usb && power.usb
            if (step == failure && !(usbBoot && step == StartupStage.BootDrive)) {
                power.stalled = true
                return@LaunchedEffect
            }
            if (usbBoot && step == StartupStage.SystemLoading) {
                power.installer = true
                return@LaunchedEffect
            }
        }
        if (power.cycle == cycle && session.coolingAvailable) finishStartup()
    }
    LaunchedEffect(power, cycle, power.destination) {
        val disk = power.destination ?: return@LaunchedEffect
        if (!power.installer || !power.usb || disk !in session.availableDisks) return@LaunchedEffect
        for (tick in 1..50) {
            gameDelay(140, menuPaused)
            if (cycle != power.cycle || !power.usb) return@LaunchedEffect
            if (tick == 50) finishInstallation(disk)
            power.percent = tick * 2
        }
    }
}

@Composable
internal fun StartupMonitor(power: ComputerPower, session: RepairSession, onClose: () -> Unit) {
    Row(Modifier.fillMaxSize().background(Background)) {
        Monitor(power, session, Modifier.weight(1f).fillMaxHeight().padding(8.dp))
        WorkshopControlRail("工作台", onClose) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(42.dp).widthIn(min=74.dp)
                    .background(if (power.powered) Color(0xFF193D38) else Panel)
                    .border(2.dp, if (power.powered) StartupGreen else Border)
                    .semantics { stateDescription = if (power.powered) "已开机" else "已关机" }
                    .clickable(role = Role.Button) { if (power.requested) power.stop() else power.start() }
                    .padding(horizontal=8.dp), contentAlignment=Alignment.Center) {
                    Row(verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        Canvas(Modifier.size(14.dp)) {
                            val color = if (power.powered) StartupGreen else MutedText
                            drawArc(color,-55f,290f,false,Offset(1f,size.height*.15f),Size(size.width-2f,size.height*.82f),
                                style=androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                            drawLine(color,Offset(center.x,0f),center,2.dp.toPx())
                        }
                        GameText("电源", fontSize=14, color=if(power.powered) StartupGreen else MutedText)
                    }
                }
                GameButton("重启", { power.start(null) }, enabled = power.powered)
                GameButton(if (power.usb) "拔出 U 盘" else "插入 U 盘", power::toggleUsb)
            }
        }
    }
}

@Composable
private fun Monitor(power: ComputerPower, session: RepairSession, modifier: Modifier) {
    Column(modifier.background(MonitorFrame).border(2.dp, MonitorEdge).padding(5.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().background(MonitorGlass).clipToBounds(), contentAlignment=Alignment.Center) {
            when {
                !power.powered -> Unit
                power.choosing ->
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp),
                        verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        GameText("选择启动设备",fontSize=20)
                        GameButton("U 盘启动", { power.choose(BootSource.Usb) })
                        GameButton("硬盘启动", { power.choose(BootSource.Disk) })
                    }
                power.installer -> SystemInstaller(power, session)
                power.stalled -> GameText(if (power.stage == StartupStage.BootDrive) "找不到启动设备" else "无信号", color=MutedText)
                power.stage == StartupStage.SystemLoading -> SystemLoading()
                power.stage == StartupStage.Welcome -> GameText("欢迎",fontSize=22)
                power.stage == StartupStage.Desktop -> ComputerDesktop(session,power.usb)
            }
        }
        Box(Modifier.fillMaxWidth().height(6.dp),contentAlignment=Alignment.CenterEnd) {
            Box(Modifier.size(4.dp).background(if(power.powered) StartupGreen else Border))
        }
    }
}

@Composable
private fun SystemInstaller(power: ComputerPower, session: RepairSession) {
    Column(Modifier.fillMaxSize().background(Color(0xFF183446)).verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement=Arrangement.spacedBy(10.dp)) {
        GameText("系统安装",fontSize=22)
        if (power.destination == null) {
            GameText(if(session.availableDisks.isEmpty()) "未找到可用磁盘" else "选择系统安装位置",color=MutedText)
            for ((index, disk) in session.availableDisks.withIndex()) {
                val capacity = session.parts.getValue(disk).specification.capacityGb
                GameButton("磁盘 $index · ${session.parts.getValue(disk).product?.model ?: disk.label} · ${capacity} GB", { power.destination = disk })
            }
        } else if (power.percent < 100) {
            GameText(when(power.percent) {
                in 0..29 -> "正在复制系统文件"
                in 30..69 -> "正在展开系统文件"
                in 70..89 -> "正在安装组件"
                else -> "正在配置启动文件"
            })
            Box(Modifier.fillMaxWidth().height(10.dp).background(Background)) {
                Box(Modifier.fillMaxWidth(power.percent/100f).fillMaxHeight().background(StartupGreen))
            }
            GameText("${power.percent}%",color=StartupGreen)
        } else {
            GameText("安装完成",color=StartupGreen)
            GameButton("从硬盘重新启动", { power.start(BootSource.Disk) },primary=true)
        }
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
