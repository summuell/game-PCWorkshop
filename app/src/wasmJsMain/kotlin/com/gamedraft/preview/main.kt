package com.gamedraft.preview

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@JsFun("""() => {
    if (!document.fullscreenElement && document.fullscreenEnabled) {
        document.documentElement.requestFullscreen().catch(() => {});
    }
}""")
private external fun requestGameFullscreen()

@JsFun("() => new Date().toLocaleString('zh-CN', {hour12: false})")
private external fun localTimestamp(): String

@JsFun("""() => {
    if (document.fullscreenElement) document.exitFullscreen().catch(() => {});
    if (window.opener) window.close();
}""")
private external fun exitGame()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val storage = object : SaveStorage {
        override fun read(slot: Int) = kotlinx.browser.window.localStorage.getItem("pc-workshop.save.$slot")
        override fun write(slot: Int, value: String) {
            kotlinx.browser.window.localStorage.setItem("pc-workshop.save.$slot", value)
        }
        override fun timestamp() = localTimestamp()
    }
    ComposeViewport(document.body!!) {
        WorkshopGame(storage, onExitGame = ::exitGame, onStartGame = ::requestGameFullscreen)
    }
}
