package com.adriantache.greatimagedownloader.data.utils.speedCalculator

import androidx.annotation.VisibleForTesting

private const val EXPIRATION_MS = 5 * 1000L
private const val MAX_SAMPLES = 5000
private const val BYTES_PER_KB = 1024

private data class DataPoint(
    val downloadedKb: Double,
    val timestamp: Long,
)

class SpeedCalculatorImpl : SpeedCalculator {
    private val samples = ArrayDeque<DataPoint>()
    private var totalKbInWindow = 0.0

    @Synchronized
    override fun registerData(
        downloadedBytes: Long,
        currentTime: Long,
    ) {
        val downloadedKb = downloadedBytes.toDouble() / BYTES_PER_KB
        val dataPoint = DataPoint(downloadedKb = downloadedKb, timestamp = currentTime)

        samples.addLast(dataPoint)
        totalKbInWindow += downloadedKb

        while (samples.size > MAX_SAMPLES) {
            totalKbInWindow -= samples.removeFirst().downloadedKb
        }
    }

    @Synchronized
    override fun getAverageSpeedKbps(
        @VisibleForTesting
        currentTime: Long,
    ): Double {
        removeStaleSamples(currentTime)

        if (samples.size < 2) return 0.0

        val initialTimestamp = samples.first().timestamp
        val durationMs = currentTime - initialTimestamp

        if (durationMs < 500) return 0.0

        return (totalKbInWindow / durationMs) * 1000.0
    }

    @Synchronized
    override fun reset() {
        samples.clear()
        totalKbInWindow = 0.0
    }

    @VisibleForTesting
    @Synchronized
    fun clearData() {
        reset()
    }

    private fun removeStaleSamples(currentTime: Long = System.currentTimeMillis()) {
        val threshold = currentTime - EXPIRATION_MS
        while (samples.isNotEmpty() && samples.first().timestamp < threshold) {
            totalKbInWindow -= samples.removeFirst().downloadedKb
        }
    }
}
