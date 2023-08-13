package com.example.greatimagedownloader.platform.wifi

private const val RATE_LIMIT_RESET_TIME = 2_000L
private const val MAX_SCANS = 4

class WifiScanRateLimiter {
    private var scanTimes = mutableListOf<Long>()

    val canScan: Boolean
        get() {
            removeStaleEntries()

            return scanTimes.size < MAX_SCANS
        }

    private fun registerScan() {
        scanTimes.add(System.currentTimeMillis())
    }

    fun getScanDelay(): Int {
        removeStaleEntries()

        val delay = when {
            scanTimes.size < 2 -> 15_000
            scanTimes.size == 3 -> 30_000
            else -> 60_000
        }

        // Also register the current scan here, to keep things simple.
        registerScan()

        return delay
    }

    private fun removeStaleEntries() {
        val now = System.currentTimeMillis()

        scanTimes.removeIf { now - it > RATE_LIMIT_RESET_TIME }
    }
}
