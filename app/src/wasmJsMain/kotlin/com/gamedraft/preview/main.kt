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

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        WorkshopGame(onStartGame = ::requestGameFullscreen)
    }
}
