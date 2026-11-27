package com.gamedraft.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private enum class MaintenanceMode(val label: String) { Repair("维修"), Merge("合并") }
private val MaintenanceGreen = Color(0xFFA6DBC1)

@Composable
internal fun PartMaintenancePage(
    sessionState: MutableState<RepairSession>,
    artwork: WorkbenchArtwork,
    modifier: Modifier,
) {
    var session by sessionState
    var mode by remember { mutableStateOf(MaintenanceMode.Repair) }
    var primaryId by remember { mutableStateOf<Int?>(null) }
    var donorId by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val parts = session.inventory.filter { it.storedComputer == null }
    val primary = parts.firstOrNull { it.id == primaryId }
    val donor = parts.firstOrNull { it.id == donorId }
    val preview = if (primary != null && donor != null) primary.mergedDurability(donor) else null
    val repairMode = mode == MaintenanceMode.Repair
    val chance = primary?.repairChance ?: 0.0

    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight().background(SlotBackground).border(1.dp, Border).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GameText("物品栏 · ${parts.size} 件", fontSize = 15)
            if (parts.isEmpty()) {
                GameText("把拆下或从仓库取出的部件放入物品栏。", fontSize = 13, color = MutedText)
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(parts, key = { it.id }) { item ->
                    val isPrimary = primary?.id == item.id
                    val isDonor = donor?.id == item.id
                    val enabled = repairMode || primary == null || isPrimary || primary.mergedDurability(item) != null
                    val role = when {
                        isPrimary -> if (repairMode) "已选" else "主件"
                        isDonor -> "副件"
                        else -> null
                    }
                    MaintenancePartRow(item, artwork, role, enabled) {
                        feedback = null
                        when {
                            repairMode -> primaryId = item.id
                            isPrimary -> {
                                primaryId = null
                                donorId = null
                            }
                            primary == null -> {
                                primaryId = item.id
                                donorId = null
                            }
                            isDonor -> donorId = null
                            else -> donorId = item.id
                        }
                    }
                }
            }
        }

        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (option in MaintenanceMode.entries) {
                    GameButton(option.label, {
                        mode = option
                        donorId = null
                        feedback = null
                    }, Modifier.weight(1f), primary = mode == option)
                }
            }
            val detailsScroll = key(mode, primaryId, donorId) { rememberScrollState() }
            Column(Modifier.weight(1f).fillMaxWidth().background(SlotBackground).border(1.dp, Border)
                .verticalScroll(detailsScroll).padding(7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                feedback?.let {
                    GameText(it, Modifier.semantics { liveRegion = LiveRegionMode.Polite }, fontSize = 15, color = Amber)
                }
                when {
                    repairMode && primary == null ->
                        GameText("选择一个部件。成功率为当前耐久的一半。", fontSize = 13, color = MutedText)
                    repairMode && chance == 0.0 ->
                        GameText("耐久为 0，无法再次维修，可用于合并。", fontSize = 13, color = MutedText)
                    repairMode -> {
                        val percent = if (chance % 1 == 0.0) chance.toInt().toString() else chance.toString()
                        GameText("成功率 $percent%", fontSize = 15, color = MaintenanceGreen)
                        GameText("成功恢复至 100，失败归 0 且无法再修。", fontSize = 13, color = MutedText)
                    }
                    primary == null ->
                        GameText("选择一个要保留的主件。", fontSize = 13, color = MutedText)
                    preview == null ->
                        GameText("选择同品牌、同型号、同容量的另一件作为副件。", fontSize = 13, color = MutedText)
                    else -> {
                        GameText("预计耐久 $preview · 成功率 100%", fontSize = 15, color = MaintenanceGreen)
                        GameText("保留主件，消耗副件。", fontSize = 13, color = MutedText)
                    }
                }
                if (!repairMode) {
                    if (primary != null) GameText("主件 ${primary.part.durability}", fontSize = 13)
                    if (donor != null) GameText("副件 ${donor.part.durability}", fontSize = 13)
                    GameText("两件相加取 80%。低于 10 的按 10 计，结果最高 100。", fontSize = 12, color = MutedText)
                }
            }
            GameButton(if (repairMode) "开始维修" else "确认合并", {
                val selected = primary ?: return@GameButton
                val before = session
                val next = if (repairMode) before.repairPart(selected.id)
                    else donor?.let { before.mergeParts(selected.id, it.id) } ?: before
                if (next === before) return@GameButton
                val durability = next.inventory.first { it.id == selected.id }.part.durability
                session = next
                if (repairMode) {
                    feedback = if (durability == 100) "维修成功 · 耐久 100" else "维修失败 · 耐久 0"
                } else {
                    donorId = null
                    feedback = "合并完成 · 耐久 $durability"
                }
            }, modifier = Modifier.fillMaxWidth(), primary = true,
                enabled = if (repairMode) chance > 0.0 else preview != null)
        }
    }
}

@Composable
private fun MaintenancePartRow(
    item: InventoryItem,
    artwork: WorkbenchArtwork,
    role: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val active = role != null
    val durabilityColor = if (item.part.working) MaintenanceGreen else Amber
    Row(
        Modifier.fillMaxWidth().heightIn(min = 46.dp)
            .background(if (active) Color(0xFF302820) else Panel)
            .border(1.dp, if (active) Amber else Border)
            .semantics { selected = active }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PartThumbnail(artwork, item.type, item.part.product, Modifier.size(width = 44.dp, height = 29.dp))
        Column(Modifier.weight(1f)) {
            val name = if (item.type == ComputerPart.SidePanel) item.label else item.part.product?.shopName ?: item.label
            GameText(name, fontSize = 12, color = if (enabled) TextColor else MutedText)
            GameText("耐久 ${item.part.durability}", fontSize = 11, color = if (enabled) durabilityColor else MutedText)
        }
        if (role != null) {
            Box(Modifier.background(SlotBackground).padding(horizontal = 4.dp, vertical = 2.dp)) {
                GameText(role, fontSize = 11, color = Amber)
            }
        }
    }
}
