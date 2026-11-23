package com.gamedraft.preview

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.net.ssl.HttpsURLConnection

internal actual suspend fun thermalShutdownDelayMillis(): Long = withContext(Dispatchers.IO) {
    val connection = URL(ThermalTimingUrl).openConnection() as HttpsURLConnection
    try {
        connection.useCaches = false
        connection.connectTimeout = 6000
        connection.readTimeout = 6000
        check(connection.responseCode == 200)
        val millis = connection.inputStream.bufferedReader().use { it.readLine().trim().toLong() }
        check(millis in 5000L..10000L)
        millis
    } finally {
        connection.disconnect()
    }
}
