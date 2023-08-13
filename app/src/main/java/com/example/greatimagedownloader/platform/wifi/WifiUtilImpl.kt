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
import com.example.greatimagedownloader.domain.wifi.WifiUtil
import com.example.greatimagedownloader.platform.wifi.WifiStatus.*

class WifiUtilImpl(context: Context) : WifiUtil {
    private var status: WifiStatus = DISCONNECTED

    override val isWifiDisabled: Boolean
        get() = !wifiManager.isWifiEnabled || wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun connectToWifi(
        wifiDetails: WifiDetailsEntity,
        connectTimeoutMs: Int,
        onWifiConnected: () -> Unit,
        onWifiDisconnected: () -> Unit,
    ) {
        scanForWifi()

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

        connectivityManager.requestNetwork(networkRequest, networkCallback, connectTimeoutMs)
    }

    override fun disconnectFromWifi() {
        // This is to stop the looping request for OnePlus & Xiaomi models.
//        connectivityManager.bindProcessToNetwork(null)

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

        return scanResults.find { it.startsWith("GR_") }.orEmpty()
    }

    private fun scanForWifi() {
        status = SCANNING

        @Suppress("DEPRECATION")
        wifiManager.startScan()
    }
}

private enum class WifiStatus {
    DISCONNECTED, CONNECTED, SCANNING
}
