package com.adriantache.greatimagedownloader.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.adriantache.greatimagedownloader.R
import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo

@Composable
fun SmartFlowRow(modifier: Modifier = Modifier) {

}

@Composable
fun RowScope.SmartFlowRowItem(
    mediaInfo: PhotoDownloadInfo,
) {
    val context = LocalContext.current

    val weight = if (mediaInfo.isLandscape(context)) 1f else 0.5f

    Box(
        modifier = Modifier
            .weight(weight)
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
