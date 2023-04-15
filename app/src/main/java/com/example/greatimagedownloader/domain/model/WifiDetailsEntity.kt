package com.example.greatimagedownloader.domain.model

import com.example.greatimagedownloader.domain.ui.model.WifiDetails
import com.example.greatimagedownloader.domain.data.model.WifiDetails as WifiDetailsData

private const val WIFI_PASS_MIN_LENGTH = 8

data class WifiDetailsEntity(
    val ssid: String?,
    val password: String?,
) {
    val isValid = !ssid.isNullOrBlank() &&
            !password.isNullOrBlank() &&
            password.length >= WIFI_PASS_MIN_LENGTH

    fun toUi(): WifiDetails {
        require(isValid)

        return WifiDetails(
            ssid = requireNotNull(ssid),
            password = requireNotNull(password),
        )
    }

    fun toData(): WifiDetailsData {
        return WifiDetailsData(
            ssid = requireNotNull(ssid),
            password = requireNotNull(password),
        )
    }
}
