package com.adriantache.greatimagedownloader.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.adriantache.greatimagedownloader.MainActivity
import com.adriantache.greatimagedownloader.R
import com.adriantache.greatimagedownloader.domain.data.Repository
import com.adriantache.greatimagedownloader.service.DataTransferTool
import com.adriantache.greatimagedownloader.service.DataTransferTool.WidgetState
import com.adriantache.greatimagedownloader.service.PhotoDownloadService
import com.adriantache.greatimagedownloader.ui.permissions.PermissionsHelper
import org.koin.core.component.KoinComponent

class QuickDownloadWidget : GlanceAppWidget(), KoinComponent {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dataTransferTool = getKoin().getOrNull<DataTransferTool>()

        provideContent {
            val widgetState by dataTransferTool?.widgetStateFlow?.collectAsState()
                ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(WidgetState.Idle) }

            GlanceTheme {
                QuickDownloadWidgetContent(context, widgetState)
            }
        }
    }

    @Composable
    private fun QuickDownloadWidgetContent(
        context: Context,
        widgetState: WidgetState,
    ) {
        val (iconRes, statusText) = when (widgetState) {
            WidgetState.Idle -> R.drawable.tap_and_play to "Sync"
            is WidgetState.Connecting -> R.drawable.tap_and_play to widgetState.stageText
            WidgetState.Checking -> R.drawable.tap_and_play to "Sync…"
            is WidgetState.Downloading -> R.drawable.ic_stat_gr to "${widgetState.current}/${widgetState.total}"
            WidgetState.Stopping -> R.drawable.ic_stat_gr to "Stop…"
            is WidgetState.Done -> R.drawable.ic_stat_gr to if (widgetState.count == 0) "0 new" else "Done (${widgetState.count})"
            WidgetState.Error -> R.drawable.ic_stat_gr to "Error"
        }

        val contentColor = when (widgetState) {
            WidgetState.Error -> Color(0xFFBA1A1A)
            else -> Color(0xFF1C1917)
        }

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(16.dp)
                .background(Color(0xD9FFCA28))
                .clickable(actionRunCallback<QuickDownloadActionCallback>())
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(iconRes),
                    contentDescription = context.getString(R.string.connect_and_start_download),
                    colorFilter = ColorFilter.tint(ColorProvider(contentColor)),
                    modifier = GlanceModifier.size(24.dp),
                )

                Spacer(modifier = GlanceModifier.height(2.dp))

                Text(
                    text = statusText,
                    style = TextStyle(
                        color = ColorProvider(contentColor),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 1,
                )
            }
        }
    }
}

class QuickDownloadActionCallback : ActionCallback, KoinComponent {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val dataTransferTool = getKoin().getOrNull<DataTransferTool>()
        val repository = getKoin().getOrNull<Repository>()

        val currentState = dataTransferTool?.widgetStateFlow?.value ?: WidgetState.Idle
        val isBusy = currentState is WidgetState.Connecting ||
            currentState is WidgetState.Checking ||
            currentState is WidgetState.Downloading

        if (isBusy) {
            dataTransferTool?.widgetStateFlow?.value = WidgetState.Stopping
            QuickDownloadWidget().updateAll(context)
            val stopIntent = Intent(context, PhotoDownloadService::class.java).apply {
                action = PhotoDownloadService.Actions.STOP.name
            }
            ContextCompat.startForegroundService(context, stopIntent)
            return
        }

        if (!PermissionsHelper.hasAllRequiredPermissions(context)) {
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_AUTO_CONNECT, true)
            }
            context.startActivity(activityIntent)
            return
        }

        val wifiDetails = repository?.getWifiDetails()?.getOrNull()
        if (wifiDetails == null || wifiDetails.ssid.isNullOrBlank() || wifiDetails.password.isNullOrBlank()) {
            // If Wi-Fi credentials have not been set up yet, open MainActivity once for initial setup.
            val activityIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_AUTO_CONNECT, true)
            }
            context.startActivity(activityIntent)
            return
        }

        dataTransferTool?.widgetStateFlow?.value = WidgetState.Connecting("BT…")
        QuickDownloadWidget().updateAll(context)

        val serviceIntent = Intent(context, PhotoDownloadService::class.java).apply {
            action = PhotoDownloadService.Actions.QUICK_DOWNLOAD.name
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
