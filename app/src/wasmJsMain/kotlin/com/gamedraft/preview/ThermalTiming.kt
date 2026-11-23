package com.gamedraft.preview

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("""(url, done) => {
    fetch(url, {cache: "no-store", credentials: "omit", referrerPolicy: "no-referrer", signal: AbortSignal.timeout(6000)})
        .then(response => response.ok ? response.text() : "")
        .then(text => done(Number(text.trim()) || 0), () => done(0));
}""")
private external fun requestThermalDelay(url: String, done: (Int) -> Unit)

internal actual suspend fun thermalShutdownDelayMillis(): Long {
    val millis = suspendCancellableCoroutine<Int> { continuation ->
        requestThermalDelay(ThermalTimingUrl) { value ->
            if (continuation.isActive) continuation.resume(value)
        }
    }
    check(millis in 5000..10000)
    return millis.toLong()
}
