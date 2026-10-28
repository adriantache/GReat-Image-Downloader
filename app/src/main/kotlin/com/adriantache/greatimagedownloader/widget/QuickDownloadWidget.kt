package com.adriantache.greatimagedownloader.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import com.adriantache.greatimagedownloader.MainActivity
import com.adriantache.greatimagedownloader.R

class QuickDownloadWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                QuickDownloadWidgetContent(context)
            }
        }
    }

    @Composable
    private fun QuickDownloadWidgetContent(context: Context) {
        val size = LocalSize.current
        val showLabel = size.height >= 84.dp && size.width >= 84.dp

        val launchAction = actionStartActivity<MainActivity>(
            parameters = actionParametersOf(
                ActionParameters.Key<Boolean>(MainActivity.EXTRA_AUTO_CONNECT) to true
            )
        )

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(28.dp)
                .background(GlanceTheme.colors.primary)
                .clickable(launchAction)
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.tap_and_play),
                    contentDescription = context.getString(R.string.connect_and_start_download),
                    colorFilter = ColorFilter.tint(GlanceTheme.colors.onPrimary),
                    modifier = GlanceModifier.size(28.dp),
                )

                if (showLabel) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "GR Sync",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimary,
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
}
