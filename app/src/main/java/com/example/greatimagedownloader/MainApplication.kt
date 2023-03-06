package com.example.greatimagedownloader

import android.app.Application
import com.example.greatimagedownloader.data.di.dataModule
import com.example.greatimagedownloader.domain.di.domainModule
import com.example.greatimagedownloader.ui.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)

            modules(
                listOf(
                    uiModule,
                    domainModule,
                    dataModule,
                )
            )
        }
    }
}
