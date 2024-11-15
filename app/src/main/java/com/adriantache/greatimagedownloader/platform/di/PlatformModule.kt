package com.adriantache.greatimagedownloader.platform.di

import com.adriantache.greatimagedownloader.domain.wifi.WifiUtil
import com.adriantache.greatimagedownloader.platform.wifi.WifiScanRateLimiter
import com.adriantache.greatimagedownloader.platform.wifi.WifiUtilImpl
import org.koin.dsl.module

val platformModule = module {
    single<WifiUtil> { WifiUtilImpl(get(), get()) }
    single { WifiScanRateLimiter() }
}
