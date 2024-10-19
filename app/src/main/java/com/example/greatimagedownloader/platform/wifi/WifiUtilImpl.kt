package com.example.greatimagedownloader.platform.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.example.greatimagedownloader.domain.utils.model.delay
import com.example.greatimagedownloader.domain.wifi.WifiUtil
import com.example.greatimagedownloader.platform.wifi.WifiStatus.CONNECTED
import com.example.greatimagedownloader.platform.wifi.WifiStatus.DISCONNECTED
import com.example.greatimagedownloader.platform.wifi.WifiStatus.SCANNING
import com.example.greatimagedownloader.platform.wifi.WifiStatus.THROTTLED

private const val CONNECT_RETRY_INTERVAL = 5_000
private const val SCAN_WAIT_MS = 2_000

class WifiUtilImpl(
    private val wifiScanRateLimiter: WifiScanRateLimiter,
    context: Context,
) : WifiUtil {
    var status: WifiStatus? = null

    override val isWifiDisabled: Boolean
        get() = !wifiManager.isWifiEnabled || wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override suspend fun connectToWifi(
        ssid: String,
        password: String,
        onDisconnected: () -> Unit,
        onConnected: () -> Unit,
        onScanning: () -> Unit,
        onThrottled: () -> Unit,
    ) {
        scanForWifi(onScanning = onScanning, onThrottled = onThrottled)

        // Delay to allow system to scan for Wi-Fi networks before trying to connect.
        delay(SCAN_WAIT_MS)
        requestConnect(ssid = ssid, password = password, onConnected = onConnected, onDisconnected = onDisconnected)
    }

    // TODO: improve user feedback for this feature
    override suspend fun suggestNetwork(): String {
        if (isWifiDisabled) {
            return "Wifi is off!"
        }

        // We ignore states for this for now.
        scanForWifi(onScanning = {}, onThrottled = {})

        // Ensure that Wi-Fi is enabled
        @SuppressLint("MissingPermission")
        val scanResults = wifiManager.scanResults.map {
            @Suppress("DEPRECATION")
            val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) it.wifiSsid.toString() else it.SSID
            ssid.replace("\"", "")
        }

        status = DISCONNECTED

        return scanResults.find { it.startsWith("GR_") }.orEmpty()
    }

    private suspend fun scanForWifi(
        onScanning: () -> Unit,
        onThrottled: () -> Unit,
    ) {
        // We currently are not allowed by the system to scan more often than 4 times per 2 minutes.
        if (wifiScanRateLimiter.canScan) {
            @Suppress("DEPRECATION")
            wifiManager.startScan()

            status = SCANNING
            onScanning()
        } else {
            status = THROTTLED
            onThrottled()

            delay(CONNECT_RETRY_INTERVAL)
            scanForWifi(onScanning = onScanning, onThrottled = onThrottled)
        }
    }

    private fun requestConnect(
        ssid: String,
        password: String,
        onConnected: () -> Unit,
        onDisconnected: () -> Unit,
    ) {
        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // To make sure that requests don't go over mobile data.
                connectivityManager.bindProcessToNetwork(network)

                // Avoid triggering connection success multiple times.
                if (status == SCANNING) {
                    status = CONNECTED
                    onConnected()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)

                // TODO: check if this is needed
                // This is to stop the looping request for OnePlus & Xiaomi models.
                // connectivityManager.bindProcessToNetwork(null)

                status = DISCONNECTED
                onDisconnected()
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback, CONNECT_RETRY_INTERVAL)
    }
}
