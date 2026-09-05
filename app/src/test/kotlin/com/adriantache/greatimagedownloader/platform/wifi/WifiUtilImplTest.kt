package com.adriantache.greatimagedownloader.platform.wifi

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowWifiManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@OptIn(ExperimentalCoroutinesApi::class)
class WifiUtilImplTest {
    private lateinit var context: Context
    private lateinit var wifiUtil: WifiUtilImpl
    private lateinit var wifiManager: WifiManager
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var shadowWifiManager: ShadowWifiManager
    private lateinit var shadowConnectivityManager: ShadowConnectivityManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowWifiManager = shadowOf(wifiManager)
        shadowConnectivityManager = shadowOf(connectivityManager)
        wifiUtil = WifiUtilImpl(context)
    }

    @Test
    fun `isWifiDisabled returns correct value`() {
        @Suppress("DEPRECATION")
        wifiManager.isWifiEnabled = true
        assertThat(wifiUtil.isWifiDisabled).isFalse

        @Suppress("DEPRECATION")
        wifiManager.isWifiEnabled = false
        assertThat(wifiUtil.isWifiDisabled).isTrue
    }

    @Test
    fun `cleanup unbinds network`() {
        // Arrange
        val network = mockk<Network>()
        connectivityManager.bindProcessToNetwork(network)
        assertThat(connectivityManager.boundNetworkForProcess).isEqualTo(network)

        // Act
        wifiUtil.cleanup()

        // Assert
        assertThat(connectivityManager.boundNetworkForProcess).isNull()
    }

    @Test
    fun `suggestNetwork returns RICOH SSID when found in scan results`() = runTest {
        // Arrange
        // ScanResult fields are public, we can use a real instance in Robolectric
        val scanResult = ScanResult()
        @Suppress("DEPRECATION")
        scanResult.SSID = "RICOH_GRIII_123456"
        scanResult.capabilities = "[WPA2-PSK-CCMP][ESS]"
        scanResult.level = -50
        
        shadowWifiManager.setScanResults(listOf(scanResult))
        shadowWifiManager.setStartScanSucceeds(true)

        // Act
        val resultJob = launch {
            val result = wifiUtil.suggestNetwork()
            assertThat(result).isEqualTo("RICOH_GRIII_123456")
        }
        
        // Trigger the broadcast
        context.sendBroadcast(Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        advanceUntilIdle()
        resultJob.cancel()
    }

    @Test
    fun `suggestNetwork returns RICOH SSID without quotes when SSID is enclosed in quotes`() = runTest {
        // Arrange
        val scanResult = ScanResult()
        @Suppress("DEPRECATION")
        scanResult.SSID = "\"RICOH_GRIII_654321\""
        scanResult.capabilities = "[WPA2-PSK-CCMP][ESS]"
        scanResult.level = -40

        shadowWifiManager.setScanResults(listOf(scanResult))
        shadowWifiManager.setStartScanSucceeds(true)

        // Act
        val resultJob = launch {
            val result = wifiUtil.suggestNetwork()
            assertThat(result).isEqualTo("RICOH_GRIII_654321")
        }

        // Trigger the broadcast
        context.sendBroadcast(Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        advanceUntilIdle()
        resultJob.cancel()
    }

    @Test
    fun `suggestNetwork returns ERROR when no camera SSID is found`() = runTest {
        // Arrange
        val scanResult = ScanResult()
        @Suppress("DEPRECATION")
        scanResult.SSID = "MyHomeRouter"
        scanResult.capabilities = "[WPA2-PSK-CCMP][ESS]"
        scanResult.level = -30

        shadowWifiManager.setScanResults(listOf(scanResult))
        shadowWifiManager.setStartScanSucceeds(true)

        // Act
        val resultJob = launch {
            val result = wifiUtil.suggestNetwork()
            assertThat(result).isEqualTo("ERROR")
        }

        // Trigger the broadcast
        context.sendBroadcast(Intent(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        advanceUntilIdle()
        resultJob.cancel()
    }
}
