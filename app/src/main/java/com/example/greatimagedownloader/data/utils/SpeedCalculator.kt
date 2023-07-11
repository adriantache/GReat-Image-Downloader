package com.example.greatimagedownloader.data.utils

// TODO: tweak these values until things look good
private const val EXPIRATION_MS = 10 * 1000L
private const val MAX_SAMPLES = 100

// TODO: hide this behind an interface
class SpeedCalculator {
    private var samples = emptyList<DataPoint>()

    fun registerData(downloadedBytes: Long) {
        val currentTime = System.currentTimeMillis()
        val dataPoint = DataPoint(downloadedBytes = downloadedBytes, timestamp = currentTime)

        removeStaleSamples(currentTime)

        samples += dataPoint
    }

    fun getAverageSpeed(): Double {
        removeStaleSamples()

        if (samples.size < 2) return 0.0

        val speeds = mutableListOf<Double>()

        for (i in 1..<samples.size) {
            val firstSample = samples.getOrNull(i - 1) ?: break
            val secondSample = samples.getOrNull(i) ?: break
            val timeElapsedSeconds = (secondSample.timestamp - firstSample.timestamp) / 1000

            speeds += secondSample.downloadedBytes.toDouble() / timeElapsedSeconds
        }

        return speeds.average()
    }

    private fun removeStaleSamples(currentTime: Long = System.currentTimeMillis()) {
        // We exclude samples that have expired or are too many.
        samples = samples.takeLast(MAX_SAMPLES).filter { it.timestamp <= currentTime - EXPIRATION_MS }
    }
}

private data class DataPoint(
    val downloadedBytes: Long,
    val timestamp: Long,
)
