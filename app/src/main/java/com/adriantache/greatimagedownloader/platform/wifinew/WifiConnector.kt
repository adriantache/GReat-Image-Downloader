package com.adriantache.greatimagedownloader.platform.wifinew

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.adriantache.greatimagedownloader.domain.wifi.WifiUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WifiConnector(
    private val context: Context,
) : WifiUtil {
    private val wifiManager by lazy { context.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    private var networkCallback: NetworkCallback? = null
    private var connectionJob: Job? = null

    override val isWifiDisabled: Boolean
        get() = !wifiManager.isWifiEnabled

    @SuppressLint("MissingPermission")
    override suspend fun connectToWifi(
        ssid: String,
        password: String,
        onDisconnected: () -> Unit,
        onConnected: () -> Unit,
        onScanning: () -> Unit,
        onThrottled: () -> Unit,
    ) {
        connectionJob?.cancel() // Cancel any existing connection attempts

        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid(ssid)
                .setWpa2Passphrase(password)
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            networkCallback = object : NetworkCallback() {
                override fun onAvailable(network: android.net.Network) {
                    super.onAvailable(network)
                    connectivityManager.bindProcessToNetwork(network)
                    onConnected()
                }

                override fun onLost(network: android.net.Network) {
                    super.onLost(network)
                    onDisconnected()
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    onThrottled()
                }
            }

            onScanning()
            connectivityManager.requestNetwork(request, networkCallback as NetworkCallback)
        }
    }

    override fun cleanup() {
        connectionJob?.cancel()
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
        }
        // TODO: re-enable this or remove after testing
//        connectivityManager.bindProcessToNetwork(null)
    }

    @SuppressLint("MissingPermission")
    override suspend fun suggestNetwork(): String = withContext(Dispatchers.IO) {
        try {
            scanNetworks()
                .firstOrNull { isLikelyRicohCamera(it) }
                ?.let { extractSsid(it) }
                ?: "ERROR"
        } catch (e: Exception) {
            "ERROR"
        }
    }

    private fun isLikelyRicohCamera(scanResult: ScanResult): Boolean {
        val ssid = extractSsid(scanResult) ?: return false
        return when {
            ssid.startsWith("RICOH_GRIII", ignoreCase = true) -> true
            ssid.startsWith("GRIII_", ignoreCase = true) -> true
            ssid.startsWith("GR_", ignoreCase = true) -> true
            ssid.contains("ricoh", ignoreCase = true) -> true
            scanResult.capabilities.contains("WPA2-PSK") -> true
            else -> false
        }
    }

    @SuppressLint("MissingPermission")
    private fun scanNetworks(): Flow<ScanResult> = callbackFlow {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: android.content.Intent) {
                if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    wifiManager.scanResults
                        .sortedByDescending { it.level }
                        .forEach { trySend(it) }
                    channel.close()
                }
            }
        }

        context.registerReceiver(
            receiver,
            android.content.IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )

        if (!wifiManager.startScan()) {
            channel.close(RuntimeException("WiFi scan failed"))
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) { /* Ignore */
            }
        }
    }

    private fun extractSsid(scanResult: ScanResult): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            scanResult.wifiSsid?.toString()
        } else {
            @Suppress("DEPRECATION")
            scanResult.SSID?.removeSurrounding("\"")
        }
    }
}
