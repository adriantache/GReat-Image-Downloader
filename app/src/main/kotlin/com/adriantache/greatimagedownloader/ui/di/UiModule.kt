package com.adriantache.greatimagedownloader.ui.di

import com.adriantache.greatimagedownloader.ui.MainScreenViewModel
import org.koin.dsl.module

val uiModule = module {
    factory { MainScreenViewModel(get()) }
}
