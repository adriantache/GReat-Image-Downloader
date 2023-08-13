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
import com.example.greatimagedownloader.domain.model.WifiDetailsEntity
import com.example.greatimagedownloader.domain.utils.model.delay
import com.example.greatimagedownloader.domain.wifi.WifiUtil
import com.example.greatimagedownloader.platform.wifi.WifiStatus.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val CONNECT_RETRY_INTERVAL = 5_000
private const val SCAN_WAIT_MS = 5_000

class WifiUtilImpl(
    context: Context,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WifiUtil {
    private val scope = CoroutineScope(dispatcher)

    private var status: WifiStatus = DISCONNECTED

    private var wifiScanRateLimiter = WifiScanRateLimiter()

    override val isWifiDisabled: Boolean
        get() = !wifiManager.isWifiEnabled || wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun connectToWifi(
        wifiDetails: WifiDetailsEntity,
        connectTimeoutMs: Int,
        onTimeout: () -> Unit,
        onWifiConnected: () -> Unit,
        onWifiDisconnected: () -> Unit,
    ) {
        scanForWifi()

        val connectionAttemptTime = System.currentTimeMillis()

        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(wifiDetails.ssid!!)
            .setWpa2Passphrase(wifiDetails.password!!)
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
                    onWifiConnected()
                }
            }

            override fun onLost(network: Network) {
                super.onLost(network)

                disconnectFromWifi()
                onWifiDisconnected()
            }
        }

        scope.launch {
            while (status == SCANNING) {
                if (System.currentTimeMillis() - connectionAttemptTime >= connectTimeoutMs) {
                    onTimeout()
                    return@launch
                }

                connectivityManager.requestNetwork(networkRequest, networkCallback, CONNECT_RETRY_INTERVAL)

                delay(CONNECT_RETRY_INTERVAL)
            }
        }
    }

    override fun disconnectFromWifi() {
        // This is to stop the looping request for OnePlus & Xiaomi models.
        connectivityManager.bindProcessToNetwork(null)

        status = DISCONNECTED
    }

    override fun suggestNetwork(): String {
        if (isWifiDisabled) {
            return "Wifi is off!"
        }

        scanForWifi()

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

    private fun scanForWifi() {
        status = SCANNING

        scope.launch {
            while (status == SCANNING) {
                val now = System.currentTimeMillis()

                // We currently cannot scan more often than 4 times per 2 minutes.
                if (wifiScanRateLimiter.canScan) {
                    @Suppress("DEPRECATION")
                    wifiManager.startScan()

                    delay(wifiScanRateLimiter.getScanDelay())
                } else {
                    delay(SCAN_WAIT_MS)
                }
            }
        }
    }
}

private enum class WifiStatus {
    DISCONNECTED, CONNECTED, SCANNING
}
