package com.example.greatimagedownloader.data.di

import com.example.greatimagedownloader.domain.data.Repository
import org.koin.dsl.module

val dataModule = module {
    factory<Repository> { RepositoryImpl() }
}
