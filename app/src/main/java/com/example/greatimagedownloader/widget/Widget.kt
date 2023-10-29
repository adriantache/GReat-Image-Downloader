package com.example.greatimagedownloader.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartService
import androidx.glance.appwidget.provideContent
import com.example.greatimagedownloader.service.AppService

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
    private fun MyContent() {
//        val context = LocalContext.current
//        val intent = Intent(context.applicationContext, AppService::class.java).apply {
//            action = AppService.Actions.START.name
//        }

//        Column(
//            modifier = GlanceModifier.fillMaxSize()
//                .background(MaterialTheme.colorScheme.primary),
//            verticalAlignment = Alignment.Top,
//            horizontalAlignment = Alignment.CenterHorizontally,
//        ) {
        Button(
            text = "Get photos",
            actionStartService<AppService>(
                isForegroundService = true,
            )
        )
//        }
    }
}
