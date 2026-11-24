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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

internal enum class TabletPage { Orders, Delivery, Shop, Purchases }

@Composable
internal fun WorkshopTablet(
    session: RepairSession,
    page: TabletPage,
    onPageChanged: (TabletPage) -> Unit,
    onClose: () -> Unit,
    onAccept: () -> Unit,
    onDeliver: () -> Unit,
    onBuy: (Product) -> Unit,
    onReceive: (Int) -> Unit,
) {
    var category by rememberSaveable { mutableStateOf(ShopCategory.Board) }
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
            GameButton("工作台", onClose)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GameButton("接单", { onPageChanged(TabletPage.Orders) }, Modifier.weight(1f), primary = page == TabletPage.Orders)
            GameButton("交单", { onPageChanged(TabletPage.Delivery) }, Modifier.weight(1f), primary = page == TabletPage.Delivery)
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
                TabletPage.Orders -> OrderPage(session.stage, onAccept)
                TabletPage.Delivery -> DeliveryPage(session, onDeliver)
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
                            Column(Modifier.weight(1f)) {
                                GameText(product.model)
                                GameText(product.detail, fontSize = 13, color = MutedText)
                            }
                            GameText("测试价 ¥0", fontSize = 14, color = Amber)
                            GameButton("购买", { onBuy(product) })
                        }
                    }
                }
                TabletPage.Purchases -> {
                    if (session.purchases.isEmpty()) GameText("暂无购物订单", color = MutedText)
                    for (order in session.purchases.asReversed()) {
                        Row(Modifier.fillMaxWidth().background(Panel).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                GameText(order.product.model)
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

@Composable
private fun OrderPage(stage: OrderStage, onAccept: () -> Unit) {
    val status = when (stage) {
        OrderStage.Available -> "待接订单"
        OrderStage.Active -> "当前订单"
        OrderStage.Delivered -> "已交付"
    }
    GameText(status, fontSize = 14, color = MutedText)
    GameText(StarterOrder.title, fontSize = 24, modifier = Modifier.semantics { heading() })
    GameText(StarterOrder.request)
    if (stage == OrderStage.Available) {
        GameButton("接下订单", onAccept, Modifier.fillMaxWidth(), primary = true)
    }
}

@Composable
private fun DeliveryPage(session: RepairSession, onDeliver: () -> Unit) {
    when (session.stage) {
        OrderStage.Available -> GameText("接单后可交付主机", color = MutedText)
        OrderStage.Delivered -> {
            GameText("交付完成", fontSize = 24, color = Color(0xFF8CD9CF), modifier = Modifier.semantics { heading() })
            GameText("主机已交还，工作台已清空。", color = MutedText)
        }
        OrderStage.Active -> {
            val requirements = session.deliveryRequirements()
            DeliveryRequirement("系统已重装", requirements.reinstalled)
            DeliveryRequirement("配件完好", requirements.partsIntact)
            DeliveryRequirement("正常启动", requirements.startsNormally)
            GameButton("确认交付", onDeliver, Modifier.fillMaxWidth(), primary = true, enabled = requirements.ready)
        }
    }
}

@Composable
private fun DeliveryRequirement(label: String, completed: Boolean) {
    GameText(
        label,
        modifier = Modifier.semantics { stateDescription = if (completed) "已完成" else "未完成" },
        color = if (completed) Color(0xFF89929A) else Color(0xFF7CE0A6),
        textDecoration = if (completed) TextDecoration.LineThrough else null,
    )
}
