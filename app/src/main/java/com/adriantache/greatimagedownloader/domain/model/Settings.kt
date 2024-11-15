package com.adriantache.greatimagedownloader.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Settings(
    val rememberLastDownloadedPhotos: Boolean? = null,
)
