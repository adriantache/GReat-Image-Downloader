package com.adriantache.greatimagedownloader.platform.wifi

private const val RATE_LIMIT_RESET_TIME = 2_000L
private const val MAX_SCANS = 4

class WifiScanRateLimiter {
    private var scanTimes = mutableListOf<Long>()

    val canScan: Boolean
        get() {
            removeStaleEntries()

            val result = scanTimes.size < MAX_SCANS

            return result.also { canScan ->
                if (canScan) registerScan() // We assume that the consumer of this variable will trigger a scan if they can.
            }
        }

    private fun registerScan() {
        scanTimes.add(System.currentTimeMillis())
    }

    @Synchronized
    private fun removeStaleEntries() {
        val now = System.currentTimeMillis()

        scanTimes.removeIf { now - it > RATE_LIMIT_RESET_TIME }
    }
}
