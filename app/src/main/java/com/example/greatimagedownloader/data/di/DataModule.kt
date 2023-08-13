package com.example.greatimagedownloader.data.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.example.greatimagedownloader.data.RepositoryImpl
import com.example.greatimagedownloader.data.api.getApi
import com.example.greatimagedownloader.data.storage.FilesStorage
import com.example.greatimagedownloader.data.storage.FilesStorageImpl
import com.example.greatimagedownloader.data.storage.PreferencesStorage
import com.example.greatimagedownloader.data.storage.PreferencesStorageImpl
import com.example.greatimagedownloader.data.storage.WifiStorage
import com.example.greatimagedownloader.data.storage.WifiStorageImpl
import com.example.greatimagedownloader.domain.data.Repository
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private const val SHARED_PREFERENCES_FILE = "preferences"

val dataModule = module {
    factory<Repository> { RepositoryImpl(get(), get(), get(), get()) }

    factory<WifiStorage> { WifiStorageImpl(get()) }
    factory<FilesStorage> { FilesStorageImpl(androidContext()) }
    factory<PreferencesStorage> { PreferencesStorageImpl(get()) }

    single { get<Context>().getSharedPreferences(SHARED_PREFERENCES_FILE, MODE_PRIVATE) }

    single { getApi() }
}
