package com.example.greatimagedownloader.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.utils.model.Kbps
import com.example.greatimagedownloader.ui.util.KeepScreenOn
import java.text.DecimalFormat

@Composable
fun DownloadingView(
    modifier: Modifier = Modifier,
    currentPhoto: Int,
    totalPhotos: Int,
    photoDownloadInfo: List<PhotoDownloadInfo>,
    downloadSpeed: Kbps,
) {
    val boxShape = RoundedCornerShape(8.dp)
    val shimmerBrush = ShimmerBrush()

    KeepScreenOn()

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val downloadSpeedDisplay = remember(downloadSpeed) {
            getDownloadSpeed(downloadSpeed.value)
        }
        val downloadSpeedImage = remember(downloadSpeed) {
            getDownloadSpeedDisplay(downloadSpeed.value)
        }

        Text(
            text = "Downloading photos: ${currentPhoto}/${totalPhotos}" +
                    "\n" +
                    downloadSpeedDisplay +
                    "\n" +
                    downloadSpeedImage,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = currentPhoto.toFloat() / totalPhotos
        )

        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(items = photoDownloadInfo) { mediaInfo ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.primary, boxShape),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        !mediaInfo.isFinished -> Text(
                            text = "${mediaInfo.downloadProgress}%",
                            color = MaterialTheme.colorScheme.onPrimary,
                        )

                        mediaInfo.isImage -> AsyncImage(
                            model = mediaInfo.uri,
                            modifier = Modifier.clip(boxShape),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            placeholder = painterResource(R.drawable.baseline_landscape_24),
                        )

                        else -> Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(MaterialTheme.colorScheme.primary, boxShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.movie),
                                contentDescription = null,
                                modifier = Modifier.requiredSize(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            items(items = (0 until totalPhotos - photoDownloadInfo.size).toList()) {
                ProgressShimmer(shimmerBrush, boxShape)
            }
        }
    }
}

@Composable
private fun ProgressShimmer(shimmerBrush: Brush, boxShape: RoundedCornerShape) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(shimmerBrush, boxShape)
    )
}

private fun getDownloadSpeed(downloadSpeedKb: Double): String {
    val downloadSpeedMb = downloadSpeedKb / 1024
    val decimalFormat = DecimalFormat().apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    val formattedSpeed = decimalFormat.format(downloadSpeedMb)

    return "($formattedSpeed MB/s)"
}

private fun getDownloadSpeedDisplay(downloadSpeedKb: Double): String {
    val downloadSpeedMb = downloadSpeedKb / 1024
    val roundedSpeedMb = (downloadSpeedMb).toInt()

    return "|".repeat(roundedSpeedMb.coerceAtMost(5)) +
            ".".repeat((5 - roundedSpeedMb).coerceIn(0..5)) +
            if (downloadSpeedMb > 6) "+" else " "
}

@Preview(showBackground = true)
@Composable
private fun DownloadingViewPreview() {
    DownloadingView(
        currentPhoto = 3,
        totalPhotos = 100,
        photoDownloadInfo = emptyList(),
        downloadSpeed = Kbps(21.22),
    )
}
