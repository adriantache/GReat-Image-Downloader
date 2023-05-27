package com.example.greatimagedownloader.ui.di

import com.example.greatimagedownloader.ui.MainScreenViewModel
import com.example.greatimagedownloader.ui.wifi.WifiUtil
import com.example.greatimagedownloader.ui.wifi.WifiUtilImpl
import org.koin.dsl.module

val uiModule = module {
    factory { MainScreenViewModel(get()) }

    factory<WifiUtil> { WifiUtilImpl(get()) }
}
