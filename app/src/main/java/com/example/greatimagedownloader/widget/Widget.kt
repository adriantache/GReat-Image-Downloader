package com.example.greatimagedownloader.widget

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.text.Text
import androidx.glance.text.TextDefaults.defaultTextStyle
import androidx.glance.unit.FixedColorProvider
import com.example.greatimagedownloader.domain.DownloadPhotosUseCase
import com.example.greatimagedownloader.domain.model.States
import org.koin.compose.koinInject

class Widget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data needed to render the AppWidget.
        // Use `withContext` to switch to another thread for long running
        // operations.

        provideContent {
            // create your AppWidget here
            MyContent()
        }
    }

    // TODO: continue with https://developer.android.com/jetpack/compose/glance/user-interaction once I've moved the logic to the service

    @Composable
    private fun MyContent(
        downloadPhotoUseCase: DownloadPhotosUseCase = koinInject(),
    ) {
        val state by downloadPhotoUseCase.state.collectAsState()

        var statusText by remember { mutableStateOf("Get photos") }
        var shouldDownload by remember { mutableStateOf(false) }

        LaunchedEffect(state, shouldDownload) {
            when (val state = state) {
                is States.Init -> {
                    shouldDownload = false
                    state.onInit()
                }

                is States.ChangeSettings -> Unit
                is States.ConnectWifi -> if (!shouldDownload) {
                    return@LaunchedEffect
                } else if (state.isSoftTimeout) {
                    state.onSoftTimeoutRetry()
                } else {
                    state.onConnect()
                }

                is States.DownloadPhotos -> shouldDownload = false
                States.GetPhotos -> Unit
                is States.RequestPermissions -> Unit
                is States.RequestWifiCredentials -> Unit
                is States.SelectFolders -> state.onFoldersSelect(state.folderInfo.folders.keys.toList())
            }

            statusText = when (state) {
                is States.ChangeSettings -> error("Unsupported state")
                is States.ConnectWifi -> "Connecting..."
                is States.DownloadPhotos -> "Downloading"
                States.GetPhotos -> "Sync..."
                is States.Init -> "Init"
                is States.RequestPermissions -> error("Unsupported state")
                is States.RequestWifiCredentials -> error("Unsupported state")
                is States.SelectFolders -> "Autoselect folders"
            }
        }

        Column(
            modifier = GlanceModifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.primary),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "GR", style = defaultTextStyle.copy(color = FixedColorProvider(Color.White)))

            Spacer(modifier = GlanceModifier.height(8.dp))

            Button(
                text = statusText,
                onClick = { shouldDownload = true }
            )
        }
    }
}
