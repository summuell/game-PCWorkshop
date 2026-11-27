package com.gamedraft.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val BenchmarkFace = Color(0xFF132730)
private val BenchmarkPanel = Color(0xFF1C343E)
private val BenchmarkAccent = Color(0xFF95DFCB)
private const val BenchmarkSampleMillis = 120L
private val BenchmarkSamples = BenchmarkTest.entries.sumOf { it.samples }

@Composable
internal fun BenchmarkWindow(session: RepairSession, paused: Boolean) {
    var report by remember(session.assembly, session.parts) { mutableStateOf<BenchmarkReport?>(null) }
    var sample by remember { mutableIntStateOf(0) }
    var cancelled by remember { mutableStateOf(false) }
    val running = report != null && sample < BenchmarkSamples
    val isPaused = rememberUpdatedState(paused)
    LaunchedEffect(running, report) {
        if (!running) return@LaunchedEffect
        while (sample < BenchmarkSamples) {
            gameDelay(BenchmarkSampleMillis, isPaused)
            sample++
        }
    }

    val current = report
    when {
        running -> BenchmarkProgress(sample, paused, cancel = {
            report = null
            cancelled = true
        })
        current != null -> BenchmarkResults(current, runAgain = {
            sample = 0
            report = session.runHardwareBenchmark()
        })
        else -> Column(
            Modifier.fillMaxWidth().background(BenchmarkFace).border(1.dp, Border).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GameText("整机性能测试", fontSize = 18, color = BenchmarkAccent)
            GameText("处理器 · 显卡 · 内存 · 存储", fontSize = 13, color = MutedText)
            WorkloadPreview(BenchmarkTest.Graphics, .22f)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameButton("开始跑分", {
                    sample = 0
                    cancelled = false
                    report = session.runHardwareBenchmark()
                }, primary = true)
                GameText(if (cancelled) "测试已取消" else "预计 15 秒", fontSize = 13, color = MutedText)
            }
        }
    }
}

@Composable
private fun BenchmarkProgress(sample: Int, paused: Boolean, cancel: () -> Unit) {
    var offset = 0
    val test = BenchmarkTest.entries.first {
        if (sample < offset + it.samples) true else {
            offset += it.samples
            false
        }
    }
    val progress = sample.toFloat() / BenchmarkSamples
    val stageProgress = (sample - offset).toFloat() / test.samples
    Column(
        Modifier.fillMaxWidth().background(BenchmarkFace).border(1.dp, Border).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                GameText(if (paused) "测试已暂停" else test.workload, fontSize = 16, color = BenchmarkAccent)
                GameText("第 ${test.ordinal + 1} / ${BenchmarkTest.entries.size} 项 · ${test.label}", fontSize = 12, color = MutedText)
            }
            GameButton("取消", cancel)
        }
        WorkloadPreview(test, stageProgress)
        Row(
            Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f).height(8.dp).background(BenchmarkPanel).semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
            }) {
                Box(Modifier.fillMaxWidth(progress).height(8.dp).background(BenchmarkAccent))
            }
            GameText("${sample * 100 / BenchmarkSamples}%", color = BenchmarkAccent, fontSize = 14)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (stage in BenchmarkTest.entries) {
                val color = when {
                    stage.ordinal < test.ordinal -> BenchmarkAccent
                    stage == test -> Amber
                    else -> MutedText
                }
                GameText(stage.label, fontSize = 12, color = color)
            }
        }
    }
}

@Composable
private fun BenchmarkResults(report: BenchmarkReport, runAgain: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth().background(BenchmarkFace).border(1.dp, Border).padding(9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                GameText("综合得分", color = BenchmarkAccent, fontSize = 13)
                GameText("${report.score} 分", color = Amber, fontSize = 28)
            }
            GameButton("再次跑分", runAgain)
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val groups = if (maxWidth >= 430.dp) report.sections.chunked(2) else report.sections.map { listOf(it) }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (group in groups) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (section in group) BenchmarkResultSection(section, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BenchmarkResultSection(section: BenchmarkSection, modifier: Modifier) {
    Column(
        modifier.background(BenchmarkPanel).border(1.dp, Border).padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GameText("${section.test.label}得分", fontSize = 14, color = BenchmarkAccent)
            GameText("${section.score} 分", fontSize = 14, color = Amber)
        }
        for (device in section.devices) {
            Row(
                Modifier.fillMaxWidth().background(BenchmarkFace).padding(5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    GameText(device.name, fontSize = 12)
                    GameText(device.details, fontSize = 11, color = MutedText)
                }
                val score = device.score
                GameText(if (score == null) "未检测到" else "$score 分", fontSize = 12,
                    color = if (score == null) MutedText else BenchmarkAccent)
            }
        }
    }
}

@Composable
private fun WorkloadPreview(test: BenchmarkTest, progress: Float) {
    Canvas(Modifier.fillMaxWidth().height(88.dp).background(Color(0xFF0E1C24))) {
        val grid = Color(0xFF243D47)
        for (column in 1..11) {
            val x = size.width * column / 12
            drawLine(grid, Offset(x, 0f), Offset(x, size.height))
        }
        for (row in 1..5) {
            val y = size.height * row / 6
            drawLine(grid, Offset(0f, y), Offset(size.width, y))
        }
        when (test) {
            BenchmarkTest.Processor -> {
                val unit = minOf(size.height / 7, size.width / 12)
                val left = center.x - 6 * unit
                for (tile in 0 until 60) {
                    val x = tile % 12
                    val y = tile / 12
                    val isRendered = tile < progress * 60
                    val glow = .3f + .7f * (1 - (kotlin.math.abs(x - 6) + kotlin.math.abs(y - 2)) / 9f)
                    val color = if (isRendered) BenchmarkAccent.copy(alpha = glow) else grid
                    drawRect(color, Offset(left + x * unit, (y + 1) * unit), Size(unit - 2, unit - 2))
                }
            }
            BenchmarkTest.Graphics -> {
                val radius = minOf(size.height * .37f, size.width / 5)
                val rotation = progress * 2 * PI
                fun point(i: Int, phase: Double): Offset {
                    val angle = 2 * PI * i / 18 + rotation
                    return center + Offset(
                        (cos(angle) * radius * 2.3).toFloat(),
                        (sin(angle + phase) * radius + sin(phase) * radius * .28).toFloat(),
                    )
                }
                for (i in 0 until 18) {
                    val first = point(i, 0.0)
                    val second = point(i, 1.0)
                    drawLine(BenchmarkAccent, first, point(i + 1, 0.0), strokeWidth = 2f)
                    drawLine(Amber.copy(alpha = .72f), second, point(i + 1, 1.0), strokeWidth = 2f)
                    drawLine(BenchmarkAccent.copy(alpha = .45f), first, second)
                }
            }
            BenchmarkTest.Memory -> {
                val unit = minOf(size.height / 5, size.width / 16)
                val left = center.x - 8 * unit
                val active = (progress * 42).toInt() % 16
                for (column in 0 until 16) {
                    val color = if (column in active..active + 2) Amber else BenchmarkAccent.copy(alpha = .32f)
                    repeat(3) { row ->
                        drawRect(color, Offset(left + column * unit, (row + 1) * unit), Size(unit - 3, unit - 3))
                    }
                }
            }
            BenchmarkTest.Storage -> {
                val unit = size.width / 28
                for (column in 1..26) {
                    val intensity = .22f + .54f * ((column * 11 + (progress * 45).toInt()) % 17) / 16f
                    val height = size.height * intensity
                    drawRect(BenchmarkAccent.copy(alpha = .38f + intensity * .6f),
                        Offset(column * unit, size.height - height - 4), Size(unit * .7f, height))
                }
            }
        }
    }
}
