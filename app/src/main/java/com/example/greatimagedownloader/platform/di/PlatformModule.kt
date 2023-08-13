package com.example.greatimagedownloader.platform.di

import com.example.greatimagedownloader.domain.wifi.WifiUtil
import com.example.greatimagedownloader.platform.wifi.WifiUtilImpl
import org.koin.dsl.module

val platformModule = module {
    single<WifiUtil> { WifiUtilImpl(get()) }
}
