package com.gamedraft.preview

import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.key
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal enum class TabletPage { Shop, Warehouse }

@Composable
internal fun WorkshopTablet(
    session: RepairSession,
    page: TabletPage,
    onPageChanged: (TabletPage) -> Unit,
    onClose: () -> Unit,
    onBuy: (Product) -> Unit,
    onDeposit: (Int) -> Unit,
    onWithdraw: (Int) -> Unit,
) {
    var category by rememberSaveable { mutableStateOf(ShopCategory.Cases) }
    Column(
        Modifier.fillMaxSize().padding(12.dp).background(Panel).border(2.dp, Border).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GameText("平板电脑", fontSize = 22, modifier = Modifier.semantics { heading() })
            GameButton("返回", onClose)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameButton("商城", { onPageChanged(TabletPage.Shop) }, Modifier.weight(1f), primary = page == TabletPage.Shop)
            GameButton("仓库", { onPageChanged(TabletPage.Warehouse) },
                Modifier.weight(1f), primary = page == TabletPage.Warehouse)
        }
        if (page == TabletPage.Warehouse) {
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StorageList("物品栏", session.inventory, "存入", onDeposit, Modifier.weight(1f))
                StorageList("仓库", session.warehouse, "取出", onWithdraw, Modifier.weight(1f))
            }
            return
        }
        val scroll = key(category) { rememberScrollState() }
        Column(
            Modifier.weight(1f).fillMaxWidth().background(SlotBackground)
                .border(1.dp, Border).verticalScroll(scroll).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                for (section in ShopCategory.entries) {
                    GameButton(section.label, { category = section }, primary = section == category)
                }
            }
            if (category == ShopCategory.Tools) GameText("暂无维修或检测工具", color = MutedText)
            for (product in Product.entries.filter { it.category == category }) {
                Row(Modifier.fillMaxWidth().background(Panel).padding(8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (product.caseStyle != null) CaseShopThumbnail(product, Modifier.size(92.dp, 58.dp))
                    Column(Modifier.weight(1f)) {
                        GameText(product.shopName)
                        GameText(product.detail, fontSize = 13, color = MutedText)
                    }
                    GameText("¥0", fontSize = 14, color = Amber)
                    GameButton("购买", { onBuy(product) })
                }
            }
        }
    }
}

@Composable
private fun StorageList(title: String, items: List<InventoryItem>, action: String, transfer: (Int) -> Unit, modifier: Modifier) {
    Column(modifier.background(SlotBackground).border(1.dp, Border).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GameText("$title · ${items.size}", fontSize = 15)
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (item in items) key(item.id) {
                Row(Modifier.fillMaxWidth().background(Panel).padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Column(Modifier.weight(1f)) {
                        GameText(if (item.type == ComputerPart.SidePanel) item.label else item.part.product?.shopName ?: item.label, fontSize = 14)
                        val build = item.storedComputer
                        GameText(if (build == null) "耐久 ${item.part.durability}" else "已收纳 · ${build.parts.size - 1} 件",
                            fontSize = 12, color = MutedText)
                    }
                    GameButton(action, { transfer(item.id) })
                }
            }
        }
    }
}
