package com.adriantache.greatimagedownloader.data.api

import com.adriantache.greatimagedownloader.data.api.model.PhotoInfo
import com.adriantache.greatimagedownloader.data.api.model.RicohConfig
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface RicohApi {
    @GET("v1/ping")
    suspend fun ping(): Response<Unit>

    @GET("v1/photos")
    suspend fun getPhotos(): Response<PhotoInfo>

    // TODO: implement this to check battery level and see what else it returns
    @GET("v1/props")
    suspend fun getConfig(): Response<RicohConfig>

    @Streaming
    @GET("v1/photos/{dir}/{file}")
    suspend fun getPhoto(
        @Path("dir") directory: String,
        @Path("file") file: String,
        @Query("size") size: String? = null,
    ): ResponseBody

    @POST("v1/device/finish")
    suspend fun finish()
}
