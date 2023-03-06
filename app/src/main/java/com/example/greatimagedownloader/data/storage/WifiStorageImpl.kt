package com.example.greatimagedownloader.data.storage

import android.content.Context
import androidx.core.content.edit
import com.example.greatimagedownloader.data.utils.getString

private const val PREFERENCES_FILE = "prefs"
private const val WIFI_SSID = "WIFI_NAME"
private const val WIFI_PASS = "WIFI_PASS"

class WifiStorageImpl(
    private val context: Context,
) : WifiStorage {
    private val preferences = context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)

    override fun getWifiSsid(): String? {
        return preferences.getString(WIFI_SSID)
    }

    override fun getWifiPassword(): String? {
        return preferences.getString(WIFI_PASS)
    }

    override fun saveWifiSsid(ssid: String) {
        preferences.edit {
            putString(WIFI_SSID, ssid)
        }
    }

    override fun saveWifiPassword(password: String) {
        preferences.edit {
            putString(WIFI_PASS, password)
        }
    }
}
