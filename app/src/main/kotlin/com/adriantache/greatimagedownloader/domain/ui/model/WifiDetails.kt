package com.adriantache.greatimagedownloader.domain.ui.model

import com.adriantache.greatimagedownloader.domain.model.WifiDetailsEntity

data class WifiDetails(
    val ssid: String,
    val password: String,
    val bssid: String?,
) {
    fun toEntity(): WifiDetailsEntity {
        return WifiDetailsEntity(
            ssid = ssid,
            password = password,
            bssid = bssid,
        )
    }
}
