package com.example.greatimagedownloader.domain.di

import com.example.greatimagedownloader.domain.DownloadPhotosUseCase
import com.example.greatimagedownloader.domain.DownloadPhotosUseCaseImpl
import org.koin.dsl.module

val domainModule = module {
    factory<DownloadPhotosUseCase> { DownloadPhotosUseCaseImpl(get()) }
}
