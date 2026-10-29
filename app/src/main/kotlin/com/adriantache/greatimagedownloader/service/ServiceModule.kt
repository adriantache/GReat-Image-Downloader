package com.adriantache.greatimagedownloader.service

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val serviceModule = module {
    single { DataTransferTool() }
    single(createdAtStart = true) { WearableSyncManager(androidContext(), get()) }
}
