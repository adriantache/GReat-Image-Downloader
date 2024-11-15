package com.adriantache.greatimagedownloader.domain.data.model

import com.adriantache.greatimagedownloader.domain.model.WifiDetailsEntity

data class WifiDetails(
    val ssid: String?,
    val password: String?,
) {
    fun toEntity(): WifiDetailsEntity {
        return WifiDetailsEntity(
            ssid = ssid,
            password = password,
        )
    }
}
