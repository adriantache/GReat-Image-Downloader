package com.example.greatimagedownloader.ui.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.R

@Composable
fun DownloadButton(
    onClick: () -> Unit,
) {
    val bgColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .requiredSize(200.dp)
            .clickable(
                enabled = true,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = size.width / 2

            drawCircle(
                color = bgColor,
                center = Offset(center, center),
                radius = center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painterResource(id = R.drawable.tap_and_play),
                contentDescription = null,
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.connect_and_start_download),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DownloadButtonPreview() {
    DownloadButton {}
}
