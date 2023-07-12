package com.example.greatimagedownloader.data.utils.speedCalculator

import androidx.annotation.VisibleForTesting
import com.example.greatimagedownloader.data.utils.mode

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
    private var samples = mutableListOf<DataPoint>()

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

        val groupedSamples = samples.groupBy {
            (it.timestamp - initialTimestamp) / 1000
        }

        val summedSamples = groupedSamples.mapValues { entry ->
            entry.value.sumOf { it.downloadedKb }
        }

        val average = summedSamples.values.average()
        val mode = summedSamples.values.toList().mode()

        return (average + mode) / 2
    }

    @VisibleForTesting
    fun clearData() {
        samples.clear()
    }

    private fun removeStaleSamples(currentTime: Long = System.currentTimeMillis()) {
        // We exclude samples that have expired or are too many.
        samples = samples.takeLast(MAX_SAMPLES).filter { it.timestamp > currentTime - EXPIRATION_MS }.toMutableList()
    }
}
