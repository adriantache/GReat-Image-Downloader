package com.example.greatimagedownloader.ui.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.util.Log
import com.example.greatimagedownloader.domain.ui.model.WifiDetails

private const val CONNECT_TIMEOUT_MS = 10_000

class WifiUtilImpl(
    private val context: Context,
) : WifiUtil {
    override fun connectToWifi(
        wifiDetails: WifiDetails,
        onConnectionSuccess: () -> Unit,
        onConnectionLost: () -> Unit
    ) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        // TODO: fix wifi scan not starting automatically
        wifiManager.startScan()

        val wifiNetworkSpecifier = WifiNetworkSpecifier.Builder()
            .setSsid(wifiDetails.ssid)
            .setWpa2Passphrase(wifiDetails.password)
            .build()

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(wifiNetworkSpecifier)
            .build()

        // TODO: only keep the callbacks we actually need
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i("WifiConnection", "onAvailable: $network")

                super.onAvailable(network)

                // To make sure that requests don't go over mobile data
                connectivityManager.bindProcessToNetwork(network)
                onConnectionSuccess()
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                Log.i("WifiConnection", "onLosing: $network -> $maxMsToLive")

                super.onLosing(network, maxMsToLive)
            }

            override fun onUnavailable() {
                Log.i("WifiConnection", "onUnavailable")

                super.onUnavailable()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
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
                // This is to stop the looping request for OnePlus & Xiaomi models
                connectivityManager.bindProcessToNetwork(null)

                onConnectionLost()

                // Here you can have a fallback option to show a 'Please connect manually' page with an Intent to the Wifi settings
            }
        }

        connectivityManager.requestNetwork(networkRequest, networkCallback, CONNECT_TIMEOUT_MS)
    }
}
