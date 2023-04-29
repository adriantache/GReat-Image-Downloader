package com.example.greatimagedownloader.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.ui.model.ProcessedDownloadInfo
import com.example.greatimagedownloader.ui.model.ProcessedDownloadInfo.Companion.toProcessedDownloadInfo
import com.example.greatimagedownloader.ui.util.KeepScreenOn

@Composable
fun DownloadingView(
    modifier: Modifier = Modifier,
    currentPhoto: Int,
    totalPhotos: Int,
    photoDownloadInfo: List<PhotoDownloadInfo>,
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    val boxShape = RoundedCornerShape(8.dp)

    var processedDownloadInfo by remember { mutableStateOf(emptyList<ProcessedDownloadInfo>()) }

    KeepScreenOn()

    LaunchedEffect(photoDownloadInfo) {
        processedDownloadInfo = photoDownloadInfo.map { it.toProcessedDownloadInfo(contentResolver, processedDownloadInfo) }
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Downloading photos: ${currentPhoto}/${totalPhotos}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = currentPhoto.toFloat() / totalPhotos
        )

        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 48.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(items = processedDownloadInfo) { processedDownloadInfo ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.primary, boxShape),
                    contentAlignment = Alignment.Center,
                ) {
                    when (processedDownloadInfo) {
                        is ProcessedDownloadInfo.Pending -> Text(
                            text = "${processedDownloadInfo.downloadProgress}%",
                            color = MaterialTheme.colorScheme.onPrimary,
                        )

                        is ProcessedDownloadInfo.Finished -> if (processedDownloadInfo.bitmap != null) {
                            Image(
                                modifier = Modifier.clip(boxShape),
                                bitmap = processedDownloadInfo.bitmap,
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .background(Color.Green)
                            )
                        }
                    }
                }
            }

            items(items = (0 until totalPhotos - processedDownloadInfo.size).toList()) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(ShimmerBrush(), boxShape)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadingViewPreview() {
    DownloadingView(
        currentPhoto = 3,
        totalPhotos = 100,
        photoDownloadInfo = emptyList(),
    )
}
