package com.adriantache.greatimagedownloader.domain.model

import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile

data class FolderInfo(
    val folders: Map<String, Int>,
) {
    constructor(mediaToDownload: List<PhotoFile>) : this(mediaToDownload.groupBy { it.directory }.mapValues { it.value.size })

    val hasMultipleFolders = folders.size > 1
}
