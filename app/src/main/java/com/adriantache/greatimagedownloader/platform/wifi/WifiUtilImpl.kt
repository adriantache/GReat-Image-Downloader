@file:SuppressLint("MissingPermission")
// All permissions required by this class (ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES, CHANGE_WIFI_STATE)
// are handled at the UI layer (in the use case) before any of these functions are called.

package com.adriantache.greatimagedownloader.platform.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.MacAddress
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import com.adriantache.greatimagedownloader.domain.wifi.WifiUtil
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class WifiUtilImpl(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WifiUtil {
    private val wifiManager by lazy { context.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    private var networkCallback: NetworkCallback? = null

    override val isWifiDisabled: Boolean
        get() = !wifiManager.isWifiEnabled

    override suspend fun connectToWifi(
        ssid: String,
        password: String,
        bssid: String?, // The new, optional BSSID for the "fast path"
    ): Pair<Boolean, String?> { // Returns success status and the new BSSID if found
        return withTimeoutOrNull(15_000L) { // Overall 15 second timeout
            suspendCancellableCoroutine { continuation ->
                val specifierBuilder = WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)

                // If we have a BSSID, add it to the specifier for a faster connection
                bssid?.let {
                    specifierBuilder.setBssid(MacAddress.fromString(it))
                }

                val request = NetworkRequest.Builder()
                    .addTransportType(android.net.NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifierBuilder.build())
                    .build()

                val callback = object : NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        super.onAvailable(network)
                        connectivityManager.bindProcessToNetwork(network)

                        // On success, get the BSSID from the network capabilities
                        val capabilities = connectivityManager.getNetworkCapabilities(network)
                        val wifiInfo = capabilities?.transportInfo as? WifiInfo
                        val newBssid = wifiInfo?.bssid

                        if (continuation.isActive) {
                            continuation.resume(Pair(true, newBssid))
                        }
                    }

                    override fun onLost(network: android.net.Network) {
                        super.onLost(network)
                        cleanup()
                    }

                    override fun onUnavailable() {
                        super.onUnavailable()
                        if (continuation.isActive) {
                            continuation.resume(Pair(false, null))
                        }
                    }
                }

                continuation.invokeOnCancellation {
                    runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                }

                connectivityManager.requestNetwork(request, callback, 15_000)
            }
        } ?: Pair(false, null) // Timeout case
    }

    override fun cleanup() {
        networkCallback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
        }?.also { networkCallback = null }
        connectivityManager.bindProcessToNetwork(null)
    }

    override suspend fun suggestNetwork(): String = withContext(dispatcher) {
        try {
            scanNetworks()
                .firstOrNull { isLikelyRicohCamera(it) }
                ?.let { extractSsid(it) }
                ?: "ERROR"
        } catch (e: Exception) {
            e.printStackTrace()
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

        @Suppress("DEPRECATION")
        if (!wifiManager.startScan()) {
            channel.close(RuntimeException("WiFi scan failed"))
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                /* Ignore */
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
