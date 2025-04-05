package com.adriantache.greatimagedownloader.data.utils.speedCalculator

import androidx.annotation.VisibleForTesting

private const val EXPIRATION_MS = 5 * 1000L
private const val MAX_SAMPLES = 10000
private const val BYTES_PER_KB = 1024

private data class DataPoint(
    val downloadedKb: Double,
    val timestamp: Long,
)

class SpeedCalculatorImpl : SpeedCalculator {
    @get:Synchronized
    @set:Synchronized
    private var samples = emptyList<DataPoint>()

    override fun registerData(
        downloadedBytes: Long,
        currentTime: Long,
    ) {
        val dataPoint = DataPoint(downloadedKb = downloadedBytes.toDouble() / BYTES_PER_KB, timestamp = currentTime)

        samples += dataPoint
    }

    override fun getAverageSpeedKbps(
        @VisibleForTesting
        currentTime: Long,
    ): Double {
        removeStaleSamples(currentTime)

        if (samples.size < 2) return 0.0

        val initialTimestamp = samples[0].timestamp
        val durationSeconds = (currentTime - initialTimestamp) / 1000

        if (durationSeconds < 1) return 0.0

        return samples.sumOf { it.downloadedKb } / durationSeconds
    }

    @VisibleForTesting
    fun clearData() {
        samples = emptyList()
    }

    private fun removeStaleSamples(currentTime: Long = System.currentTimeMillis()) {
        // We exclude samples that have expired or are too many.
        samples = samples.takeLast(MAX_SAMPLES).filter { it.timestamp > currentTime - EXPIRATION_MS }
    }
}
