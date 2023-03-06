package com.example.greatimagedownloader.ui.di

import com.example.greatimagedownloader.ui.MainScreenViewModel
import org.koin.dsl.module

val uiModule = module {
    factory { MainScreenViewModel(get()) }
}
