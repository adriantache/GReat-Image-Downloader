package com.example.greatimagedownloader.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class AppService : Service() {
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            Actions.START.name -> start()
            Actions.STOP.name -> stopSelf()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun start() {
        val notification = getNotification(this)
        startForeground(1, notification)
    }

    enum class Actions {
        START, STOP
    }
}
