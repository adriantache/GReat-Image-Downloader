package com.example.greatimagedownloader.service

import org.koin.dsl.module

val serviceModule = module {
    single { DataTransferTool() }
}
