package com.example.greatimagedownloader.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.example.greatimagedownloader.domain.utils.model.Kbps
import com.example.greatimagedownloader.ui.util.KeepScreenOn
import kotlinx.coroutines.delay
import java.text.DecimalFormat

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DownloadingView(
    modifier: Modifier = Modifier,
    currentPhoto: Int,
    totalPhotos: Int,
    photoDownloadInfo: List<PhotoDownloadInfo>,
    downloadSpeed: Kbps,
    isStopping: Boolean,
    onClose: () -> Unit,
) {
    val boxShape = RoundedCornerShape(8.dp)
    val scrollState = rememberScrollState()

    val downloadSpeedDisplay = remember(downloadSpeed) {
        getDownloadSpeed(downloadSpeed.value)
    }
    val downloadSpeedImage = remember(downloadSpeed) {
        getDownloadSpeedDisplay(downloadSpeed.value)
    }

    var photosSize by remember { mutableIntStateOf(0) }

    KeepScreenOn()

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

        AnimatedVisibility(isStopping) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Stopping download...",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }

                Spacer(Modifier.height(16.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary, boxShape)
                .padding(16.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Downloading photos: ${currentPhoto}/${totalPhotos}" +
                        "\n" +
                        downloadSpeedDisplay +
                        "\n" +
                        downloadSpeedImage,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }

        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(progress = { if (totalPhotos == 0) 0f else currentPhoto.toFloat() / totalPhotos })

        Spacer(Modifier.height(16.dp))

        val density = LocalDensity.current
        var totalFlowWidth by remember { mutableStateOf(0.dp) }

        ContextualFlowRow(
            modifier = Modifier
                .safeDrawingPadding()
                .fillMaxWidth(1f)
                .onGloballyPositioned { totalFlowWidth = with(density) { it.size.width.toDp() } }
                .wrapContentHeight(align = Alignment.CenterVertically)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            itemCount = photoDownloadInfo.size,
            maxItemsInEachRow = 3,
        ) { index ->
            val mediaInfo = photoDownloadInfo[index]
            val width = (totalFlowWidth - 16.dp) / 3

            Box(
                modifier = Modifier
                    .requiredWidth(width)
                    .background(MaterialTheme.colorScheme.primary, boxShape),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    !mediaInfo.isFinished -> Box(
                        modifier = Modifier
                            .requiredWidth(width)
                            .requiredHeight(width / 2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${mediaInfo.downloadProgress}%",
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }

                    mediaInfo.isImage -> AsyncImage(
                        model = mediaInfo.uri,
                        modifier = Modifier.clip(boxShape),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.baseline_landscape_24),
                    )

                    else -> Box(
                        modifier = Modifier
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
    }
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
        isStopping = false,
        onClose = {},
    )
}
