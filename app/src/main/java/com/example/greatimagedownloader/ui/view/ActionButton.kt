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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.ui.util.conditional

@Composable
fun ActionButton(
    bgColor: Color = MaterialTheme.colorScheme.primary,
    iconPainter: Painter,
    text: String,
    onClick: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .requiredSize(200.dp)
            .clip(CircleShape)
            .conditional(onClick != null) {
                clickable(
                    enabled = true,
                    onClick = requireNotNull(onClick),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shadowElevation = 8.dp,
            shape = CircleShape,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size.width / 2

                drawCircle(
                    color = bgColor,
                    center = Offset(center, center),
                    radius = center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                iconPainter,
                contentDescription = null,
                tint = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = text,
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
    Box(Modifier.padding(16.dp)) {
        ActionButton(iconPainter = painterResource(id = R.drawable.wifi_off), text = "Test")
    }
}
