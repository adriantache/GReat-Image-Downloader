package com.adriantache.greatimagedownloader.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.adriantache.greatimagedownloader.R
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo

@Composable
fun SmartFlowRow(
    modifier: Modifier = Modifier,
    photoDownloadInfo: List<PhotoDownloadInfo>,
) {
    val density = LocalDensity.current
    val state = rememberLazyGridState()

    var totalFlowWidth by remember { mutableStateOf(0.dp) }
    var latestRememberedFinishedImage by remember { mutableIntStateOf(0) }
    val latestFinishedImage = remember(photoDownloadInfo) {
        derivedStateOf { photoDownloadInfo.indexOfLast { it.isFinished } }
    }.value

    LaunchedEffect(latestFinishedImage) {
        if (latestRememberedFinishedImage != latestFinishedImage) {
            state.animateScrollBy(9999f)
            latestRememberedFinishedImage = latestFinishedImage
        }
    }

    LazyVerticalGrid(
        modifier = modifier.onGloballyPositioned {
            if (totalFlowWidth != 0.dp) return@onGloballyPositioned
            totalFlowWidth = with(density) { it.size.width.toDp() - 8.dp - 4.dp }
        },
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        columns = GridCells.Adaptive((totalFlowWidth / 2).coerceAtLeast(2.dp)),
        state = state,
    ) {
        items(photoDownloadInfo) { info ->
            SmartFlowRowItem(info, totalFlowWidth = totalFlowWidth)
        }
    }
}

@Composable
fun SmartFlowRowItem(
    mediaInfo: PhotoDownloadInfo,
    totalFlowWidth: Dp,
) {
    val context = LocalContext.current
    val boxShape = RoundedCornerShape(8.dp)

    val widthMultiplier = when {
        !mediaInfo.isFinished -> 0.5f
        !mediaInfo.isImage -> 0.5f
        else -> if (mediaInfo.isLandscape(context)) 1f else 0.5f
    }
    val requiredWidth = totalFlowWidth * widthMultiplier

    Box(
        modifier = Modifier
            .requiredWidth(requiredWidth)
            .background(MaterialTheme.colorScheme.primary, boxShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !mediaInfo.isFinished -> Box(contentAlignment = Alignment.Center) {
                Text(
                    modifier = Modifier.height(24.dp),
                    text = "${mediaInfo.downloadProgress}%",
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }

            mediaInfo.isImage -> AsyncImage(
                model = mediaInfo.uri,
                modifier = Modifier
                    .requiredWidth(requiredWidth)
                    .clip(boxShape),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                placeholder = painterResource(R.drawable.baseline_landscape_24),
            )

            else -> Box(
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, boxShape),
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
