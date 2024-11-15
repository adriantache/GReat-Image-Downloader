package com.adriantache.greatimagedownloader.data.utils.speedCalculator

import androidx.annotation.VisibleForTesting

interface SpeedCalculator {
    fun registerData(
        downloadedBytes: Long,
        currentTime: Long = System.currentTimeMillis(),
    )

    fun getAverageSpeedKbps(
        @VisibleForTesting
        currentTime: Long = System.currentTimeMillis(),
    ): Double
}
