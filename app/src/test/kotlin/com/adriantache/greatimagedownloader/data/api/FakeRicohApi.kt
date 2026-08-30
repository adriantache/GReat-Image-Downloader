package com.adriantache.greatimagedownloader.data.api

import com.adriantache.greatimagedownloader.data.api.model.PhotoInfo
import com.adriantache.greatimagedownloader.data.api.model.RicohConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakeRicohApi : RicohApi {
    var photosResponse: Response<PhotoInfo> = Response.success(PhotoInfo(null, null, emptyList()))
    var configResponse: Response<RicohConfig> = Response.success(
        RicohConfig(
            errCode = null, errMsg = null, manufacturer = null, model = null,
            serialNo = null, firmware = null, macAddress = null, channelList = null,
            hot = null, battery = null, operationModeList = null, stillFormatList = null
        )
    )
    var photoResponse: ResponseBody = "fake image data".toResponseBody("image/jpeg".toMediaType())
    
    var finishCalled = false
    var finishShouldThrow: Throwable? = null
    var getPhotosShouldThrow: Throwable? = null
    var getPhotoShouldThrow: Throwable? = null

    override suspend fun getPhotos(): Response<PhotoInfo> {
        getPhotosShouldThrow?.let { throw it }
        return photosResponse
    }

    override suspend fun getConfig(): Response<RicohConfig> = configResponse

    override suspend fun getPhoto(directory: String, file: String): ResponseBody {
        getPhotoShouldThrow?.let { throw it }
        return photoResponse
    }

    override suspend fun finish() {
        finishCalled = true
        finishShouldThrow?.let { throw it }
    }
}
