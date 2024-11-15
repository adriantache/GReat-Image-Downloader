package com.adriantache.greatimagedownloader.service.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoFileItem(
    val directory: String,
    val name: String,
) : Parcelable
