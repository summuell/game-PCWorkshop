package com.gamedraft.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runSkikoComposeUiTest
import androidx.compose.ui.test.swipe
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WorkbenchDragTest {
    @Test
    fun dropAfterShoppingInLandscape() = buyAndDrop(Size(900f, 400f), Offset(340f, 190f))

    @Test
    fun dropAfterShoppingInRotatedWorkbench() = buyAndDrop(Size(400f, 900f), Offset(210f, 340f))

    private fun buyAndDrop(size: Size, target: Offset) = runSkikoComposeUiTest(size = size) {
        val storage = object : SaveStorage {
            override fun read(slot: Int): String? = null
            override fun write(slot: Int, value: String) = Unit
            override fun timestamp() = "test"
        }
        setContent { WorkshopGame(storage, {}) }
        onNodeWithText("开始游戏").performClick()
        onNodeWithContentDescription("平板电脑").performClick()
        onAllNodesWithText("购买")[0].performClick()
        onNodeWithText("返回").performClick()

        val root = onRoot()
        val origin = onNodeWithText("AIR 903").fetchSemanticsNode().boundsInRoot.center
        root.performTouchInput { down(origin) }
        for (step in 1..10) {
            root.performTouchInput { moveTo(origin + (target - origin) * (step / 10f)) }
            waitForIdle()
        }
        root.performTouchInput { up() }
        onNodeWithText("收纳机箱").performClick()

        val stored = onNodeWithText("AIR 903").fetchSemanticsNode().boundsInRoot.center
        root.performTouchInput { swipe(stored, target, 100) }
        onNodeWithText("收纳机箱").assertExists()
    }
}
