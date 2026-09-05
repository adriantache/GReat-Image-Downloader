package com.adriantache.greatimagedownloader.data.api.model

import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import kotlinx.serialization.Serializable

@Serializable
data class PhotoInfo(
    val errCode: Float? = null,
    val errMsg: String? = null,
    val dirs: List<Dir> = emptyList(),
)

@Serializable
data class Dir(
    val name: String = "",
    val files: List<String> = emptyList(),
) {
    fun toPhotoInfoList(): List<PhotoFile> {
        return files.map { fileName ->
            PhotoFile(
                directory = name,
                name = fileName,
            )
        }
    }
}

// TODO: implement directories?
//{errCode=200.0, errMsg=OK, dirs=[{name=101RICOH, files=[R0000683.JPG]}]}
