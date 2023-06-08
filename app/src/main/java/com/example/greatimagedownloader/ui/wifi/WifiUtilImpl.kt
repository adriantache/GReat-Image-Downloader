package com.example.greatimagedownloader.ui.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import com.example.greatimagedownloader.domain.ui.model.WifiDetails
import kotlinx.coroutines.delay

const val CONNECT_TIMEOUT_MS = 10_000

// TODO: refactor this class
// TODO: handle situation where wifi is not turned on using isWifiDisabled
class WifiUtilImpl(context: Context) : WifiUtil {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val isWifiDisabled: Boolean
        get() = !wifiManager.isWifiEnabled || wifiManager.wifiState != WifiManager.WIFI_STATE_ENABLED

    @get:Synchronized
    @set:Synchronized
    private var state: WifiState = WifiState.IDLE

    override suspend fun connectToWifi(
        wifiDetails: WifiDetails,
        onConnectionSuccess: () -> Unit,
        onConnectionLost: () -> Unit,
    ) {
        scanForWifi()

        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(wifiDetails.ssid)
            .setWpa2Passphrase(wifiDetails.password)
            .build()

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        // TODO: only keep the callbacks we actually need and remove the logs
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i("WifiConnection", "onAvailable: $network")

                super.onAvailable(network)

                // To make sure that requests don't go over mobile data.
                connectivityManager.bindProcessToNetwork(network)

                // Avoid triggering connection success multiple times.
                if (state == WifiState.SCANNING) {
                    state = WifiState.FOUND
                    onConnectionSuccess()
                }
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                Log.i("WifiConnection", "onLosing: $network -> $maxMsToLive")

                super.onLosing(network, maxMsToLive)
            }

            override fun onUnavailable() {
                Log.i("WifiConnection", "onUnavailable")

                // TODO: implement for when we can't find the network we're searching for

                super.onUnavailable()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                Log.i("WifiConnection", "onCapabilitiesChanged: $network -> $networkCapabilities")

                super.onCapabilitiesChanged(network, networkCapabilities)
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                Log.i("WifiConnection", "onLinkPropertiesChanged: $network -> $linkProperties")

                super.onLinkPropertiesChanged(network, linkProperties)
            }

            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                Log.i("WifiConnection", "onBlockedStatusChanged: $network -> $blocked")

                super.onBlockedStatusChanged(network, blocked)
            }

            override fun onLost(network: Network) {
                Log.i("WifiConnection", "onLost: $network")

                super.onLost(network)
                // This is to stop the looping request for OnePlus & Xiaomi models.
                connectivityManager.bindProcessToNetwork(null)

                state = WifiState.LOST
                onConnectionLost()

                // Here you can have a fallback option to show a 'Please connect manually' page with an Intent to the Wifi settings
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback, CONNECT_TIMEOUT_MS)
    }

    @SuppressLint("MissingPermission")
    override suspend fun suggestNetwork(): String {
        if (isWifiDisabled) {
            return "Wifi is off!"
        }

        scanForWifi()

        // Ensure that Wi-Fi is enabled
        val scanResults = wifiManager.scanResults.map {
            @Suppress("DEPRECATION")
            val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) it.wifiSsid.toString() else it.SSID
            ssid.replace("\"", "")
        }

        state = WifiState.IDLE

        return scanResults.find { it.startsWith("GR_") }.orEmpty()
    }

    // TODO: fix this scan code
    private suspend fun scanForWifi() {
        state = WifiState.SCANNING

        wifiManager.startScan()
        delay(2000) // Wait for the scan to be performed


//            val wifiScanReceiver = object : BroadcastReceiver() {
//                override fun onReceive(context: Context, intent: Intent) {
//                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
//                    if (success) {
//                        continuation.resume(Unit)
//                    } else {
//                        continuation.resume(Unit)
//                    }
//                }
//            }
//
//            val intentFilter = IntentFilter()
//            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
//            context.registerReceiver(wifiScanReceiver, intentFilter)
//
//            val success = wifiManager.startScan()
//            if (!success) {
//                continuation.resume(Unit)
//            }
    }
}

private enum class WifiState {
    IDLE, SCANNING, FOUND, LOST
}
