package com.example.greatimagedownloader.domain.model

import com.example.greatimagedownloader.domain.data.model.PhotoFile

data class FolderInfo(
    val folders: Map<String, Int>,
) {
    constructor(mediaToDownload: List<PhotoFile>) : this(mediaToDownload.groupBy { it.directory }.mapValues { it.value.size })

    val hasMultipleFolders = folders.size > 1
}
