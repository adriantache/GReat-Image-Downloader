package com.adriantache.greatimagedownloader.platform.di

import com.adriantache.greatimagedownloader.domain.wifi.WifiUtil
import com.adriantache.greatimagedownloader.platform.wifi.WifiConnector
import org.koin.dsl.module

val platformModule = module {
    single<WifiUtil> { WifiConnector(get()) }
}
