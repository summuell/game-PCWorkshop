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

internal enum class TabletPage { Shop, Purchases }

@Composable
internal fun WorkshopTablet(
    session: RepairSession,
    page: TabletPage,
    onPageChanged: (TabletPage) -> Unit,
    onClose: () -> Unit,
    onBuy: (Product) -> Unit,
    onReceive: (Int) -> Unit,
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
            val pending = session.purchases.count { !it.received }
            GameButton(if (pending > 0) "购物订单 ($pending)" else "购物订单", { onPageChanged(TabletPage.Purchases) },
                Modifier.weight(1.3f), primary = page == TabletPage.Purchases)
        }
        val scroll = key(page, if (page == TabletPage.Shop) category else null) { rememberScrollState() }
        Column(
            Modifier.weight(1f).fillMaxWidth().background(SlotBackground)
                .border(1.dp, Border).verticalScroll(scroll).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (page) {
                TabletPage.Shop -> {
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
                TabletPage.Purchases -> {
                    if (session.purchases.isEmpty()) GameText("暂无购物订单", color = MutedText)
                    for (order in session.purchases.asReversed()) {
                        Row(Modifier.fillMaxWidth().background(Panel).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                GameText(order.product.shopName)
                                GameText("订单 #${order.number} · " + if (order.received) "已签收" else "已到货",
                                    fontSize = 13, color = MutedText)
                            }
                            if (!order.received) GameButton("签收", { onReceive(order.number) }, primary = true)
                        }
                    }
                }
            }
        }
    }
}
