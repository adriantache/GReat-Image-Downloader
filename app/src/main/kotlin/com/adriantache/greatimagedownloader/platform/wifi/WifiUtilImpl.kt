@file:SuppressLint("MissingPermission")
// All permissions required by this class (ACCESS_FINE_LOCATION, NEARBY_WIFI_DEVICES, CHANGE_WIFI_STATE)
// are handled at the UI layer (in the use case) before any of these functions are called.

package com.adriantache.greatimagedownloader.platform.wifi

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.MacAddress
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
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
import kotlin.time.Duration.Companion.seconds

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
        if (isAlreadyConnected(ssid)) {
            Log.d("WifiUtilImpl", "Already connected to $ssid")
            return Pair(true, bssid)
        }

        cleanup()

        return withTimeoutOrNull(15.seconds) { // Overall 15 second timeout
            suspendCancellableCoroutine { continuation ->
                val specifierBuilder = WifiNetworkSpecifier.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)

                // If we have a BSSID, add it to the specifier for a faster connection
                // Only use it if it's not the dummy "02:00:00:00:00:00" address
                if (isValidBssid(bssid)) {
                    specifierBuilder.setBssid(MacAddress.fromString(bssid!!))
                }

                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(specifierBuilder.build())
                    .build()

                val callback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    object : NetworkCallback(FLAG_INCLUDE_LOCATION_INFO) {
                        override fun onAvailable(network: android.net.Network) {
                            handleOnAvailable(network, null, continuation)
                        }

                        override fun onCapabilitiesChanged(
                            network: android.net.Network,
                            networkCapabilities: NetworkCapabilities,
                        ) {
                            handleOnAvailable(network, networkCapabilities, continuation)
                        }

                        override fun onLost(network: android.net.Network) {
                            handleOnLost(network)
                        }

                        override fun onUnavailable() {
                            handleOnUnavailable(continuation)
                        }
                    }
                } else {
                    object : NetworkCallback() {
                        override fun onAvailable(network: android.net.Network) {
                            handleOnAvailable(network, null, continuation)
                        }

                        override fun onCapabilitiesChanged(
                            network: android.net.Network,
                            networkCapabilities: NetworkCapabilities,
                        ) {
                            handleOnAvailable(network, networkCapabilities, continuation)
                        }

                        override fun onLost(network: android.net.Network) {
                            handleOnLost(network)
                        }

                        override fun onUnavailable() {
                            handleOnUnavailable(continuation)
                        }
                    }
                }

                networkCallback = callback

                continuation.invokeOnCancellation {
                    cleanup()
                }

                connectivityManager.requestNetwork(request, callback, 15_000)
            }
        } ?: Pair(false, null) // Timeout case
    }

    private fun handleOnAvailable(
        network: android.net.Network,
        callbackCapabilities: NetworkCapabilities?,
        continuation: kotlinx.coroutines.CancellableContinuation<Pair<Boolean, String?>>,
    ) {
        connectivityManager.bindProcessToNetwork(network)

        // Prefer capabilities delivered directly to onCapabilitiesChanged (which carry unredacted location info)
        val capabilities = callbackCapabilities ?: connectivityManager.getNetworkCapabilities(network)
        val wifiInfo = capabilities?.transportInfo as? WifiInfo
        val newBssid = wifiInfo?.bssid?.takeIf { isValidBssid(it) }

        if (continuation.isActive) {
            continuation.resume(Pair(true, newBssid))
        }
    }

    private fun handleOnLost(network: android.net.Network) {
        if (connectivityManager.boundNetworkForProcess == network) {
            connectivityManager.bindProcessToNetwork(null)
        }
    }

    private fun handleOnUnavailable(
        continuation: kotlinx.coroutines.CancellableContinuation<Pair<Boolean, String?>>,
    ) {
        if (continuation.isActive) {
            continuation.resume(Pair(false, null))
        }
    }

    @Suppress("DEPRECATION")
    private fun isAlreadyConnected(targetSsid: String): Boolean {
        val cleanTargetSsid = targetSsid.removeSurrounding("\"")
        val candidateNetworks = buildList {
            connectivityManager.boundNetworkForProcess?.let { add(it) }
            connectivityManager.activeNetwork?.let { add(it) }
            addAll(connectivityManager.allNetworks)
        }.distinct()

        for (network in candidateNetworks) {
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: continue
            if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) continue

            val wifiInfo = capabilities.transportInfo as? WifiInfo
            val currentSsid = wifiInfo?.ssid?.removeSurrounding("\"")
            if (currentSsid == cleanTargetSsid) {
                connectivityManager.bindProcessToNetwork(network)
                return true
            }
        }
        return false
    }

    private fun isValidBssid(bssid: String?): Boolean {
        return !bssid.isNullOrBlank() && bssid != "02:00:00:00:00:00"
    }

    override fun ensureBound(): Boolean {
        val currentBound = connectivityManager.boundNetworkForProcess
        if (currentBound != null) {
            val caps = connectivityManager.getNetworkCapabilities(currentBound)
            if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                return true
            }
        }

        val wifiNetwork = connectivityManager.allNetworks.firstOrNull { network ->
            val caps = connectivityManager.getNetworkCapabilities(network)
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

        if (wifiNetwork != null) {
            connectivityManager.bindProcessToNetwork(wifiNetwork)
            return true
        }
        return false
    }

    override fun cleanup() {
        networkCallback?.let {
            runCatching { connectivityManager.unregisterNetworkCallback(it) }
        }
        networkCallback = null

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
            ssid.startsWith("RICOH", ignoreCase = true) -> true
            ssid.startsWith("GR", ignoreCase = true) -> true
            ssid.startsWith("PENTAX", ignoreCase = true) -> true
            ssid.contains("ricoh", ignoreCase = true) -> true
            ssid.contains("pentax", ignoreCase = true) -> true
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

        ContextCompat.registerReceiver(
            context,
            receiver,
            android.content.IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            ContextCompat.RECEIVER_EXPORTED,
        )

        @Suppress("DEPRECATION")
        if (!wifiManager.startScan()) {
            // If Android throttles active scans, fall back to cached scan results
            val cachedResults = wifiManager.scanResults.orEmpty().sortedByDescending { it.level }
            if (cachedResults.isNotEmpty()) {
                cachedResults.forEach { trySend(it) }
                channel.close()
            } else {
                channel.close(RuntimeException("WiFi scan failed"))
            }
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
        val rawSsid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            scanResult.wifiSsid?.toString()
        } else {
            @Suppress("DEPRECATION")
            scanResult.SSID
        }
        return rawSsid?.removeSurrounding("\"")
    }
}
