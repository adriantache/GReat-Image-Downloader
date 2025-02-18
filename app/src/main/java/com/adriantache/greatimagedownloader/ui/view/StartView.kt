package com.adriantache.greatimagedownloader.ui.view

import android.content.Intent
import android.provider.Settings.ACTION_WIFI_SETTINGS
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.adriantache.greatimagedownloader.R
import com.adriantache.greatimagedownloader.domain.utils.model.delay
import com.adriantache.greatimagedownloader.ui.util.KeepScreenOn
import kotlinx.coroutines.launch

// TODO: rename this file and split it into a loading view and a start view
// TODO: make some nice animations between the two states
// TODO: add preview
@Composable
fun StartView(
    isSoftWifiTimeout: Boolean,
    onSoftWifiTimeoutRetry: () -> Unit,
    isHardWifiTimeout: Boolean,
    onCheckWifiDisabled: () -> Boolean,
    onConnect: () -> Unit,
    onChangeWifiDetails: () -> Unit,
    onAdjustSettings: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(false) }
    var isWifiDisabled by remember { mutableStateOf(false) }

    LifecycleResumeEffect(onCheckWifiDisabled) {
        isWifiDisabled = onCheckWifiDisabled()

        onPauseOrDispose {}
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
                        modifier = Modifier.requiredSize(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
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
                    WifiDecorator {
                        ActionButton(
                            bgColor = bgColor,
                            iconPainter = painterResource(id = R.drawable.wifi_pending),
                            text = stringResource(R.string.connecting_to_camera),
                        )
                    }
                }

                AnimatedVisibility(isSoftWifiTimeout) {
                    var waitingProgress by remember { mutableIntStateOf(0) }

                    LaunchedEffect(Unit) {
                        val delayDurationMs = 5000

                        launch {
                            val delayInterval = delayDurationMs / 100

                            while (true) {
                                delay(delayInterval)
                                waitingProgress = (++waitingProgress).coerceAtMost(100)
                            }
                        }

                        delay(delayDurationMs) // Give people time to read the message.

                        onSoftWifiTimeoutRetry()
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.wifi_soft_timeout),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = { waitingProgress.toFloat() / 100 },
                            drawStopIndicator = { Unit }
                        )
                    }
                }

                AnimatedVisibility(isHardWifiTimeout) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.wifi_hard_timeout),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
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
        isSoftWifiTimeout = true,
        isHardWifiTimeout = true,
        onSoftWifiTimeoutRetry = {},
    )
}
