package com.adriantache.greatimagedownloader.domain.model

import com.adriantache.greatimagedownloader.domain.data.model.WifiDetails as WifiDetailsData

private const val WIFI_PASS_MIN_LENGTH = 8

data class WifiDetailsEntity(
    val ssid: String?,
    val password: String?,
    val bssid: String?,
) {
    val isValid = !ssid.isNullOrBlank() &&
            !password.isNullOrBlank() &&
            password.length >= WIFI_PASS_MIN_LENGTH

    fun toData(): WifiDetailsData {
        return WifiDetailsData(
            ssid = requireNotNull(ssid),
            password = requireNotNull(password),
            bssid = bssid,
        )
    }
}
