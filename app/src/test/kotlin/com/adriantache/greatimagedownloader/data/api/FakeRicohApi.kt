package com.adriantache.greatimagedownloader.data.api

import com.adriantache.greatimagedownloader.data.api.model.PhotoInfo
import com.adriantache.greatimagedownloader.data.api.model.RicohConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakeRicohApi : RicohApi {
    var pingResponse: Response<Unit> = Response.success(Unit)
    var photosResponse: Response<PhotoInfo> = Response.success(PhotoInfo(null, null, emptyList()))
    var configResponse: Response<RicohConfig> = Response.success(
        RicohConfig(
            errCode = null, errMsg = null, manufacturer = null, model = null,
            serialNo = null, firmware = null, macAddress = null, channelList = null,
            hot = null, battery = null, operationModeList = null, stillFormatList = null
        )
    )
    var photoResponse: ResponseBody = "fake image data".toResponseBody("image/jpeg".toMediaType())
    
    var pingCalledCount = 0
    var getPhotosCalledCount = 0
    var finishCalled = false
    var pingShouldThrow: Throwable? = null
    var finishShouldThrow: Throwable? = null
    var getPhotosShouldThrow: Throwable? = null
    var getPhotosFailureCountBeforeSuccess = 0
    var getPhotoShouldThrow: Throwable? = null
    var lastRequestedPhotoSize: String? = null

    override suspend fun ping(): Response<Unit> {
        pingCalledCount++
        pingShouldThrow?.let { throw it }
        return pingResponse
    }

    override suspend fun getPhotos(): Response<PhotoInfo> {
        getPhotosCalledCount++
        if (getPhotosFailureCountBeforeSuccess > 0) {
            getPhotosFailureCountBeforeSuccess--
            throw java.io.IOException("Transient routing not ready")
        }
        getPhotosShouldThrow?.let { throw it }
        return photosResponse
    }

    override suspend fun getConfig(): Response<RicohConfig> = configResponse

    override suspend fun getPhoto(directory: String, file: String, size: String?): ResponseBody {
        lastRequestedPhotoSize = size
        getPhotoShouldThrow?.let { throw it }
        return photoResponse
    }

    override suspend fun finish() {
        finishCalled = true
        finishShouldThrow?.let { throw it }
    }
}
