package com.adriantache.greatimagedownloader

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.adriantache.greatimagedownloader.domain.DownloadPhotosUseCase
import com.adriantache.greatimagedownloader.logging.AppLogger
import com.adriantache.greatimagedownloader.service.DataTransferTool
import com.adriantache.greatimagedownloader.ui.MainScreenStateMachine
import com.adriantache.greatimagedownloader.ui.theme.GReatImageDownloaderTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val downloadPhotosUseCase: DownloadPhotosUseCase by inject()
    private val dataTransferTool: DataTransferTool by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logActivityLaunch("onCreate", intent)
        handleQuickDownloadIntent(intent)
        enableEdgeToEdge()
        setContent {
            GReatImageDownloaderTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    MainScreenStateMachine()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        logActivityLaunch("onNewIntent", intent)
        handleQuickDownloadIntent(intent)
    }

    private fun logActivityLaunch(lifecycleMethod: String, intent: Intent?) {
        AppLogger.d(
            "MainActivity",
            "$lifecycleMethod: action=${intent?.action}, " +
                "autoConnect=${intent?.getBooleanExtra(EXTRA_AUTO_CONNECT, false)}, " +
                "serviceState=${dataTransferTool.serviceStateFlow.value}, " +
                "widgetState=${dataTransferTool.widgetStateFlow.value}, " +
                "stage=${dataTransferTool.connectionStageFlow.value}, " +
                "uiState=${downloadPhotosUseCase.state.value::class.simpleName}",
        )
    }

    private fun handleQuickDownloadIntent(intent: Intent?) {
        if (intent?.action == ACTION_QUICK_DOWNLOAD ||
            intent?.getBooleanExtra(EXTRA_AUTO_CONNECT, false) == true
        ) {
            intent.removeExtra(EXTRA_AUTO_CONNECT)
            downloadPhotosUseCase.triggerAutoConnect()
        }
    }

    companion object {
        const val ACTION_QUICK_DOWNLOAD = "com.adriantache.greatimagedownloader.action.QUICK_DOWNLOAD"
        const val EXTRA_AUTO_CONNECT = "extra_auto_connect"
    }
}
