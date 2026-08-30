package com.adriantache.greatimagedownloader.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WifiStorageImplTest {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var wifiStorage: WifiStorageImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPreferences = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)
        wifiStorage = WifiStorageImpl(sharedPreferences)
    }

    @Test
    fun `getWifiSsid returns null initially`() {
        val result = wifiStorage.getWifiSsid()
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `saveWifiSsid and getWifiSsid work correctly`() {
        wifiStorage.saveWifiSsid("TestSSID")
        val result = wifiStorage.getWifiSsid()
        assertThat(result.getOrNull()).isEqualTo("TestSSID")
    }

    @Test
    fun `getWifiPassword returns null initially`() {
        val result = wifiStorage.getWifiPassword()
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `saveWifiPassword and getWifiPassword work correctly`() {
        wifiStorage.saveWifiPassword("TestPass")
        val result = wifiStorage.getWifiPassword()
        assertThat(result.getOrNull()).isEqualTo("TestPass")
    }

    @Test
    fun `getWifiBssid returns null initially`() {
        val result = wifiStorage.getWifiBssid()
        assertThat(result.isSuccess).isTrue
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `saveWifiBssid and getWifiBssid work correctly`() {
        wifiStorage.saveWifiBssid("TestBSSID")
        val result = wifiStorage.getWifiBssid()
        assertThat(result.getOrNull()).isEqualTo("TestBSSID")
    }
}
