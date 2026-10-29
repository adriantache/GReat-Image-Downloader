package com.adriantache.greatimagedownloader.data.api

import kotlinx.serialization.json.Json
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

private const val RICOH_BASE_URL = "http://192.168.0.1/"

fun getApi(): RicohApi {
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(maxIdleConnections = 0, keepAliveDuration = 1, TimeUnit.SECONDS))
        .protocols(listOf(Protocol.HTTP_1_1)) // Cameras often have issues with HTTP/2 negotiation.
        .retryOnConnectionFailure(true)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Connection", "close")
                .build()
            chain.proceed(request)
        }
        .build()

    val contentType = "application/json".toMediaType()
    val json = Json { ignoreUnknownKeys = true }

    val retrofit = Retrofit.Builder()
        .baseUrl(RICOH_BASE_URL)
        .addConverterFactory(json.asConverterFactory(contentType))
        .client(okHttpClient)
        .build()

    return retrofit.create(RicohApi::class.java)
}
