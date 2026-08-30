package com.adriantache.greatimagedownloader.ui.view

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadingView(
    modifier: Modifier = Modifier,
    currentPhoto: Int,
    totalPhotos: Int,
    photoDownloadInfo: List<PhotoDownloadInfo>,
    downloadSpeed: Kbps,
    onClose: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var photosSize by remember { mutableIntStateOf(0) }

    LaunchedEffect(photoDownloadInfo) {
        if (photosSize != photoDownloadInfo.size) {
            photosSize = photoDownloadInfo.size
            delay(1000) // required to let the bitmap inflate
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CloseIcon(onClose)

        DownloadDashboard(
            currentPhoto = currentPhoto,
            totalPhotos = totalPhotos,
            speedKbps = downloadSpeed.value
        )

        Spacer(Modifier.height(24.dp))

        SmartFlowRow(
            modifier = Modifier.fillMaxWidth(),
            photoDownloadInfo = photoDownloadInfo,
        )
    }
}

@Composable
private fun DownloadDashboard(
    currentPhoto: Int,
    totalPhotos: Int,
    speedKbps: Double,
) {
    val speedMbps = speedKbps / 1024
    val progress = if (totalPhotos == 0) 0f else currentPhoto.toFloat() / totalPhotos
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    val status = when {
        speedMbps < 1.0 -> ConnectionStatus(
            label = "LOW SPEED",
            color = MaterialTheme.colorScheme.error,
            bars = 1
        )

        speedMbps >= 5.0 -> ConnectionStatus(
            label = "ULTRA SPEED",
            color = Color(0xFF2E7D32), // Green
            bars = 5
        )

        else -> ConnectionStatus(
            label = "STABLE SPEED",
            color = MaterialTheme.colorScheme.primary,
            bars = 3
        )
    }

    val statusColor by animateColorAsState(targetValue = status.color, label = "statusColor")

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Speed indicator with icons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(5) { i ->
                        val active = i < status.bars
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (active) statusColor else statusColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(if (active) 36.dp else 24.dp)
                        )
                    }
                }

                Text(
                    text = "${"%.2f".format(speedMbps)} MB/s",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )

                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = status.label,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor
                    )
                }
            }

            // Progress section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp)),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$currentPhoto / $totalPhotos photos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                }
            }
        }
    }
}

private data class ConnectionStatus(
    val label: String,
    val color: Color,
    val bars: Int,
)

@Preview(showBackground = true)
@Composable
private fun DownloadingViewPreview() {
    DownloadingView(
        currentPhoto = 45,
        totalPhotos = 100,
        photoDownloadInfo = emptyList(),
        downloadSpeed = Kbps(8500.0), // ~8.3 MB/s
        onClose = {},
    )
}
