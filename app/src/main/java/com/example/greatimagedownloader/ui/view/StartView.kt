package com.example.greatimagedownloader.ui.view

import android.content.Intent
import android.provider.Settings.ACTION_WIFI_SETTINGS
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.ui.util.KeepScreenOn

// TODO: rename this file and split it into a loading view and a start view
// TODO: make some nice animations between the two states
// TODO: add preview
@Composable
fun StartView(
    onCheckWifiDisabled: () -> Boolean,
    onConnect: () -> Unit,
    onChangeWifiDetails: () -> Unit,
    onAdjustSettings: () -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onAdjustSettings,
                    colors = ButtonDefaults.filledTonalButtonColors(),
                ) {
                    Icon(
                        painterResource(id = R.drawable.baseline_settings_24),
                        contentDescription = null,
                        modifier = Modifier.requiredSize(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(200.dp))

            if (isWifiDisabled) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    ActionButton(
                        bgColor = Color.Red,
                        iconPainter = painterResource(id = R.drawable.wifi_off),
                        text = stringResource(R.string.wifi_is_not_enabled),
                        onClick = { context.startActivity(Intent(ACTION_WIFI_SETTINGS)) }
                    )
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

                KeepScreenOn()

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    ActionButton(
                        bgColor = bgColor,
                        iconPainter = painterResource(id = R.drawable.wifi_pending),
                        text = stringResource(R.string.connecting_to_camera),
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ActionButton(
                        iconPainter = painterResource(id = R.drawable.tap_and_play),
                        text = stringResource(R.string.connect_and_start_download),
                        onClick = {
                            isLoading = true

                            onConnect()
                        }
                    )

                    // TODO: move to settings
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
        onCheckWifiDisabled = { true },
        onConnect = {},
        onChangeWifiDetails = {},
        onAdjustSettings = {},
    )
}
