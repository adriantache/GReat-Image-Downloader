package com.adriantache.greatimagedownloader.service.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WifiDetailsItem(
    val ssid: String,
    val password: String,
    val bssid: String? = null,
) : Parcelable
