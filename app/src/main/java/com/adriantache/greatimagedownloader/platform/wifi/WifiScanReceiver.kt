package com.adriantache.greatimagedownloader.platform.wifi

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager

class WifiScanReceiver(private val onScanResultsAvailable: (scanResults: List<ScanResult>) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
            val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

            @SuppressLint("MissingPermission") // TODO: verify permission?
            val scanResults = wifiManager.scanResults
            onScanResultsAvailable(scanResults)
        }
    }
}
