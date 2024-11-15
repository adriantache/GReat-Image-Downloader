package com.adriantache.greatimagedownloader.domain.di

import com.adriantache.greatimagedownloader.domain.DownloadPhotosUseCase
import com.adriantache.greatimagedownloader.domain.DownloadPhotosUseCaseImpl
import org.koin.dsl.module

val domainModule = module {
    single<DownloadPhotosUseCase> { DownloadPhotosUseCaseImpl(get(), get()) }
}
