package com.example.greatimagedownloader.ui.view

import android.graphics.ImageDecoder
import android.net.Uri
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.ui.util.KeepScreenOn


@Composable
fun DownloadingView(
    modifier: Modifier = Modifier,
    currentPhoto: Int,
    totalPhotos: Int,
    photoUris: List<String?>,
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver

    KeepScreenOn()

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Downloading photos: ${currentPhoto}/${totalPhotos}")

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
            itemsIndexed(items = photoUris) { index, photoUri ->
                val bgColor = when {
                    photoUri == null -> Color.Red
                    index < currentPhoto -> MaterialTheme.colorScheme.primary
                    else -> Color.LightGray
                }
                val boxShape = RoundedCornerShape(8.dp)


                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(bgColor, boxShape),
                    contentAlignment = Alignment.Center,
                ) {
                    val uri = Uri.parse(photoUri)
                    val source = ImageDecoder.createSource(contentResolver, uri)
                    val bitmap = ImageDecoder.decodeBitmap(source).asImageBitmap()

                    Image(
                        modifier = Modifier.clip(boxShape),
                        bitmap = bitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
            }

            items(items = (0 until totalPhotos - photoUris.size).toList()) {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .background(Color.LightGray, RoundedCornerShape(8.dp)),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadingViewPreview() {
    DownloadingView(
        currentPhoto = 33,
        totalPhotos = 100,
        photoUris = emptyList()
    )
}
