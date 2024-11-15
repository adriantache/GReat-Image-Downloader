package com.adriantache.greatimagedownloader.data.di

import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.adriantache.greatimagedownloader.data.RepositoryImpl
import com.adriantache.greatimagedownloader.data.api.getApi
import com.adriantache.greatimagedownloader.data.storage.FilesStorage
import com.adriantache.greatimagedownloader.data.storage.FilesStorageImpl
import com.adriantache.greatimagedownloader.data.storage.PreferencesStorage
import com.adriantache.greatimagedownloader.data.storage.PreferencesStorageImpl
import com.adriantache.greatimagedownloader.data.storage.WifiStorage
import com.adriantache.greatimagedownloader.data.storage.WifiStorageImpl
import com.adriantache.greatimagedownloader.domain.data.Repository
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
