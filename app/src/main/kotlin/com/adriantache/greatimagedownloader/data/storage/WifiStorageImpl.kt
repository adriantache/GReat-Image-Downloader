package com.adriantache.greatimagedownloader.data.storage

import android.content.SharedPreferences
import androidx.core.content.edit
import com.adriantache.greatimagedownloader.data.storage.error.WifiStorageException

private const val WIFI_SSID = "WIFI_NAME"
private const val WIFI_PASS = "WIFI_PASS"
private const val WIFI_BSSID = "WIFI_BSSID"

class WifiStorageImpl(
    private val sharedPreferences: SharedPreferences,
) : WifiStorage {
    override fun getWifiSsid(): Result<String?> = runCatching {
        sharedPreferences.getString(WIFI_SSID, null)
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(WifiStorageException.Unknown(it)) }
    )

    override fun getWifiPassword(): Result<String?> = runCatching {
        sharedPreferences.getString(WIFI_PASS, null)
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(WifiStorageException.Unknown(it)) }
    )

    override fun getWifiBssid(): Result<String?> = runCatching {
        sharedPreferences.getString(WIFI_BSSID, null)
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(WifiStorageException.Unknown(it)) }
    )

    override fun saveWifiSsid(ssid: String): Result<Unit> = runCatching {
        sharedPreferences.edit {
            putString(WIFI_SSID, ssid)
        }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(WifiStorageException.Unknown(it)) }
    )

    override fun saveWifiPassword(password: String): Result<Unit> = runCatching {
        sharedPreferences.edit {
            putString(WIFI_PASS, password)
        }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(WifiStorageException.Unknown(it)) }
    )

    override fun saveWifiBssid(bssid: String?): Result<Unit> = runCatching {
        sharedPreferences.edit {
            if (bssid == null) {
                remove(WIFI_BSSID)
            } else {
                putString(WIFI_BSSID, bssid)
            }
        }
    }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(WifiStorageException.Unknown(it)) }
    )
}
