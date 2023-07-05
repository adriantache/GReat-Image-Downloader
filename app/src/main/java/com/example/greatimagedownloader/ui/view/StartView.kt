package com.example.greatimagedownloader.ui.view

import android.content.Intent
import android.provider.Settings.ACTION_WIFI_SETTINGS
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.greatimagedownloader.R

// TODO: rename this file and split it into a loading view and a start view
// TODO: make some nice animations between the two states
// TODO: add preview
@Composable
fun StartView(
    onCheckWifiDisabled: () -> Boolean,
    onConnect: () -> Unit,
    onChangeWifiDetails: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    var isLoading by remember { mutableStateOf(false) }
    var isWifiDisabled by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isWifiDisabled = onCheckWifiDisabled()
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        isWifiDisabled = onCheckWifiDisabled()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Spacer(modifier = Modifier.height(200.dp))

            if (isWifiDisabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredSize(200.dp)
                        .clickable {
                            context.startActivity(Intent(ACTION_WIFI_SETTINGS))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shadowElevation = 8.dp,
                        shape = CircleShape,
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = size.width / 2

                            drawCircle(
                                color = Color.Red,
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
                            painter = painterResource(id = R.drawable.wifi_off),
                            contentDescription = null,
                            tint = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "WiFi is not enabled!",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isLoading) {
                val infiniteTransition = rememberInfiniteTransition(label = "")
                val bgColor by infiniteTransition.animateColor(
                    initialValue = MaterialTheme.colorScheme.primary,
                    targetValue = Color.Black,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "Background Animation"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredSize(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
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
                            painterResource(id = R.drawable.wifi_pending),
                            contentDescription = null,
                            tint = Color.White
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Connecting to camera...",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    DownloadButton(onClick = {
                        isLoading = true

                        onConnect()
                    })


                    Button(
                        onClick = onChangeWifiDetails,
                        colors = ButtonDefaults.filledTonalButtonColors(),
                    ) {
                        Icon(
                            painterResource(id = R.drawable.wifi_lock),
                            contentDescription = null,
                            modifier = Modifier.requiredSize(16.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = stringResource(R.string.change_wifi_details),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartViewPreview() {
    StartView(
        onConnect = {},
        onChangeWifiDetails = {},
        onCheckWifiDisabled = { true }
    )
}
