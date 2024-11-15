package com.adriantache.greatimagedownloader.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.adriantache.greatimagedownloader.data.utils.getString

private const val WIFI_SSID = "WIFI_NAME"
private const val WIFI_PASS = "WIFI_PASS"

class WifiStorageImpl(
    private val sharedPreferences: SharedPreferences,
) : WifiStorage {
    override fun getWifiSsid(): String? {
        return sharedPreferences.getString(WIFI_SSID)
    }

    override fun getWifiPassword(): String? {
        return sharedPreferences.getString(WIFI_PASS)
    }

    override fun saveWifiSsid(ssid: String) {
        sharedPreferences.edit {
            putString(WIFI_SSID, ssid)
        }
    }

    override fun saveWifiPassword(password: String) {
        sharedPreferences.edit {
            putString(WIFI_PASS, password)
        }
    }
}
