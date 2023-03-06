package com.example.greatimagedownloader.data.di

import com.example.greatimagedownloader.data.RepositoryImpl
import com.example.greatimagedownloader.data.api.getApi
import com.example.greatimagedownloader.data.storage.FilesStorage
import com.example.greatimagedownloader.data.storage.FilesStorageImpl
import com.example.greatimagedownloader.data.storage.WifiStorage
import com.example.greatimagedownloader.data.storage.WifiStorageImpl
import com.example.greatimagedownloader.domain.data.Repository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    factory<Repository> { RepositoryImpl(get(), get(), get()) }

    factory<WifiStorage> { WifiStorageImpl(androidContext()) }
    factory<FilesStorage> { FilesStorageImpl(androidContext()) }

    single { getApi() }
}
