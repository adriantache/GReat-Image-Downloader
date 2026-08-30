package com.adriantache.greatimagedownloader.ui.view

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.adriantache.greatimagedownloader.R
import com.adriantache.greatimagedownloader.ui.util.KeepScreenOn

@Composable
fun SyncView() {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation Animation"
    )
    val bgColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.primary,
        targetValue = Color.Black,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Background Animation"
    )

    KeepScreenOn()

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(248.dp)) // padding (8) + button size (40) + spacer (200)

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            WifiDecorator {
                Box(modifier = Modifier.rotate(rotation)) {
                    ActionButton(
                        bgColor = bgColor,
                        iconPainter = painterResource(id = R.drawable.sync),
                        text = "Synchronizing...",
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SyncViewPreview() {
    SyncView()
}
