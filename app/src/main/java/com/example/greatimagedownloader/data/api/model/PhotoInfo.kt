package com.example.greatimagedownloader.data.api.model

data class PhotoInfo(
    val errorCode: Float,
    val errMsg: String,
    val dirs: List<Dir>,
)

data class Dir(
    val name: String,
    val files: List<String>,
)

// TODO: implement directories?
//{errCode=200.0, errMsg=OK, dirs=[{name=101RICOH, files=[R0000683.JPG]}]}
