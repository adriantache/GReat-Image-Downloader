package com.example.greatimagedownloader.domain.ui.model

import com.example.greatimagedownloader.domain.model.WifiDetailsEntity

data class WifiDetails(
    val ssid: String,
    val password: String,
) {
    fun toEntity(): WifiDetailsEntity {
        return WifiDetailsEntity(
            ssid = ssid,
            password = password,
        )
    }
}
