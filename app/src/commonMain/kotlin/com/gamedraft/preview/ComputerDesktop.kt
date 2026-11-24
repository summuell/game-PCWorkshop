package com.gamedraft.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

private enum class Location(val title: String) {
    Desktop("桌面"), Computer("我的电脑"), Bin("回收站"), System("本地磁盘 (C:)"),
    Data("本地磁盘 (D:)"), Usb("移动磁盘 (U:)"), Hardware("配置检测");

    val parent: Location get() = when (this) {
        System, Data, Usb -> Computer
        Hardware -> Usb
        else -> Desktop
    }
}

private val WindowFace = Color(0xFF233844)
private val WindowAccent = Color(0xFF97DDCB)

@Composable
internal fun ComputerDesktop(session: RepairSession, usb: Boolean) {
    var page by remember { mutableStateOf(Location.Desktop) }
    var denied by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(usb) {
        if (!usb && (page == Location.Usb || page == Location.Hardware)) page = Location.Computer
    }
    Box(Modifier.fillMaxSize()) {
        DesktopWallpaper()
        Column(Modifier.fillMaxSize()) {
            if (page == Location.Desktop) {
                Column(Modifier.weight(1f).padding(10.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (shortcut in listOf(Location.Computer, Location.Bin)) {
                        Column(Modifier.width(86.dp).clickable(role = Role.Button) { page = shortcut }.padding(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            FileIcon(shortcut, Modifier.size(30.dp))
                            GameText(shortcut.title, fontSize = 13)
                        }
                    }
                }
            } else {
                Column(Modifier.weight(1f).fillMaxWidth().padding(6.dp).background(WindowFace).border(1.dp, Border)) {
                    Row(Modifier.fillMaxWidth().background(Color(0xFF152B36)).padding(horizontal = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        WindowAction("返回") { page = page.parent }
                        GameText(page.title, Modifier.weight(1f), fontSize = 15)
                        WindowAction("关闭") { page = Location.Desktop }
                    }
                    key(page) {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            when (page) {
                                Location.Computer -> {
                                    val system = session.bootDisk
                                    if (system != null) FileRow(Location.System, capacity(session, system)) { page = Location.System }
                                    session.availableDisks.firstOrNull { it != system }?.let { disk ->
                                        FileRow(Location.Data, capacity(session, disk)) { page = Location.Data }
                                    }
                                    if (usb) FileRow(Location.Usb) { page = Location.Usb }
                                }
                                Location.System -> listOf("Windows", "Program Files", "Users").forEach { folder ->
                                    FileRow(null, name = folder) { denied = folder }
                                }
                                Location.Usb -> FileRow(Location.Hardware) { page = Location.Hardware }
                                Location.Hardware -> HardwareReport(session)
                                Location.Bin, Location.Data -> Unit
                                Location.Desktop -> Unit
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(24.dp).background(Color(0xFF142933)).padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(8.dp).background(WindowAccent))
                GameText(page.title, fontSize = 11, color = MutedText)
            }
        }
        denied?.let { folder ->
            Box(Modifier.fillMaxSize().background(Background.copy(alpha = .68f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                contentAlignment = Alignment.Center) {
                Column(Modifier.background(WindowFace).border(1.dp, WindowAccent).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GameText("无权限", fontSize = 17, color = Amber)
                    GameText("无法打开 $folder", fontSize = 14)
                    WindowAction("确定") { denied = null }
                }
            }
        }
    }
}

@Composable
private fun WindowAction(label: String, click: () -> Unit) {
    Box(Modifier.heightIn(min = 36.dp).clickable(role = Role.Button, onClick = click).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center) { GameText(label, fontSize = 13, color = WindowAccent) }
}

private fun capacity(session: RepairSession, disk: ComputerPart) =
    "${session.parts.getValue(disk).specification.capacityGb} GB"

@Composable
private fun FileRow(location: Location?, detail: String = "", name: String = location?.title.orEmpty(), click: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 38.dp).background(Color(0xFF29404C))
        .clickable(role = Role.Button, onClick = click).padding(horizontal = 9.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        FileIcon(location, Modifier.size(24.dp))
        GameText(name, Modifier.weight(1f), fontSize = 14)
        if (detail.isNotEmpty()) GameText(detail, fontSize = 12, color = MutedText)
    }
}

@Composable
private fun HardwareReport(session: RepairSession) {
    val ram = listOf(ComputerPart.MemoryLeft, ComputerPart.MemoryRight).filter {
        session.assembly.isInstalled(it) && session.parts[it]?.working == true
    }
    val total = ram.sumOf { session.parts.getValue(it).specification.capacityGb }
    GameText("内存合计  $total GB", fontSize = 16, color = WindowAccent)
    val components = listOf(ComputerPart.Motherboard, ComputerPart.Processor, ComputerPart.GraphicsCard) +
        ram + session.availableDisks + ComputerPart.PowerSupply
    for (slot in components) {
        val part = session.parts[slot] ?: continue
        if (!session.assembly.isInstalled(slot) || !part.working) continue
        val spec = part.specification
        val value = when (slot) {
            ComputerPart.MemoryLeft, ComputerPart.MemoryRight -> "${spec.capacityGb} GB · ${spec.frequencyMhz} MHz"
            ComputerPart.SolidStateDrive, ComputerPart.HardDrive -> "${spec.capacityGb} GB"
            ComputerPart.PowerSupply -> "${spec.powerWatts} W"
            else -> "性能等级 ${spec.performanceTier}"
        }
        Row(Modifier.fillMaxWidth().background(Color(0xFF29404C)).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            GameText(slot.label, fontSize = 13)
            GameText(value, fontSize = 13, color = MutedText)
        }
    }
}

@Composable
private fun FileIcon(location: Location?, modifier: Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 16
        fun tile(x: Int, y: Int, w: Int, h: Int, c: Color) =
            drawRect(c, Offset(x*u,y*u), Size(w*u,h*u))
        val dark = Color(0xFF172C36)
        when (location) {
            Location.Computer -> {
                tile(1,2,14,10,MutedText); tile(3,4,10,6,Color(0xFF498EA6))
                tile(7,12,2,2,MutedText); tile(4,14,8,1,MutedText)
            }
            Location.Bin -> {
                tile(4,4,8,10,MutedText); tile(3,3,10,2,Color.White)
                tile(6,7,1,5,dark); tile(9,7,1,5,dark)
            }
            Location.System, Location.Data, Location.Usb -> {
                tile(2,5,12,7,MutedText); tile(3,6,10,2,Color.White)
                tile(3,10,6,1,dark); tile(11,10,2,1,WindowAccent)
            }
            Location.Hardware -> {
                tile(2,3,12,10,WindowAccent); tile(4,5,8,6,dark)
                tile(5,8,2,2,WindowAccent); tile(9,6,2,4,WindowAccent)
            }
            else -> { tile(1,4,6,3,Amber); tile(1,6,14,7,Amber); tile(2,7,12,1,Color(0xFFFFDBA5)) }
        }
    }
}

@Composable
private fun DesktopWallpaper() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color(0xFF315F79))
        val u = size.width/96
        val v = size.height/56
        drawRect(Color(0xFFEBB781),Offset(68*u,9*v), Size(7*u,7*v))
        for (i in 0..47) {
            val ridge = 16 + kotlin.math.abs(i-33)/2 + kotlin.math.abs(i-23)/4
            drawRect(Color(0xFF527F8A), Offset(i*2*u,ridge*v), Size(2*u+1,size.height-ridge*v))
            val near = 30 + kotlin.math.abs(i-13)/3 - kotlin.math.abs(i-38)/5
            drawRect(Color(0xFF285563), Offset(i*2*u,near*v), Size(2*u+1,size.height-near*v))
            val shore = 41 + kotlin.math.abs(i-36)/4
            drawRect(Color(0xFF183E4B), Offset(i*2*u,shore*v), Size(2*u+1,size.height-shore*v))
        }
    }
}
