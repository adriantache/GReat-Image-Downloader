package com.adriantache.greatimagedownloader

import android.app.Application
import com.adriantache.greatimagedownloader.data.di.dataModule
import com.adriantache.greatimagedownloader.domain.di.domainModule
import com.adriantache.greatimagedownloader.platform.di.platformModule
import com.adriantache.greatimagedownloader.service.serviceModule
import com.adriantache.greatimagedownloader.ui.di.uiModule
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
                    platformModule,
                    serviceModule,
                )
            )
        }
    }
}
