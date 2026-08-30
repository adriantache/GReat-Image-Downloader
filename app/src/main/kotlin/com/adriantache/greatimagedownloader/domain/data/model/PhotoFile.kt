package com.adriantache.greatimagedownloader.domain.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PhotoFile(
    val directory: String,
    val name: String,
)
