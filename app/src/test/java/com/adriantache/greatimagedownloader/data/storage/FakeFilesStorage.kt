package com.adriantache.greatimagedownloader.data.storage

import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import okhttp3.ResponseBody

class FakeFilesStorage : FilesStorage {
    var savedPhotos = mutableListOf<PhotoDownloadInfo>()
    var savedMovies = mutableListOf<String>()
    val savePhotoFlow = MutableSharedFlow<Result<PhotoDownloadInfo>>()
    
    var getSavedPhotosResult: Result<List<PhotoDownloadInfo>> = Result.success(emptyList())
    var getSavedMoviesResult: Result<List<String>> = Result.success(emptyList())
    var deleteMediaResult: Result<Unit> = Result.success(Unit)
    var deleteAllResult: Result<Unit> = Result.success(Unit)

    override fun getSavedPhotos(): Result<List<PhotoDownloadInfo>> = getSavedPhotosResult

    override fun getSavedMovies(): Result<List<String>> = getSavedMoviesResult

    override fun savePhoto(responseBody: ResponseBody, file: PhotoFile): Flow<Result<PhotoDownloadInfo>> = flow {
        savePhotoFlow.collect { emit(it) }
    }

    override fun deleteMedia(uri: String): Result<Unit> = deleteMediaResult

    override suspend fun deleteAll(): Result<Unit> = deleteAllResult
}
