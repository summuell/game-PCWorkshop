package com.gamedraft.preview

internal const val ThermalTimingUrl =
    "https://www.random.org/integers/?num=1&min=5000&max=10000&col=1&base=10&format=plain&rnd=new"

internal expect suspend fun thermalShutdownDelayMillis(): Long
