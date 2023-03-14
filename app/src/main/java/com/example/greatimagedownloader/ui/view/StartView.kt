package com.example.greatimagedownloader.ui.view

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.domain.ui.model.WifiDetails
import com.example.greatimagedownloader.ui.wifi.WifiUtil
import com.example.greatimagedownloader.ui.wifi.WifiUtilImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// TODO: rename this file and split it into a loading view and a start view
// TODO: make some nice animations between the two states
// TODO: add preview
@Composable
fun StartView(
    wifiDetails: WifiDetails,
    onConnectionSuccess: () -> Unit,
    onConnectionLost: () -> Unit,
    onChangeWifiDetails: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val wifiUtil: WifiUtil by remember { mutableStateOf(WifiUtilImpl(context)) }

    var isLoading by remember { mutableStateOf(false) }

    // TODO: move this logic to the use case and add a new WifiConnectionPending state for it
    //  and send an event if we time out to show a snackbar or something
    if (isLoading) {
        LaunchedEffect(Unit) {
            delay(7000)

            isLoading = false
        }

        val infiniteTransition = rememberInfiniteTransition()
        val bgColor by infiniteTransition.animateColor(
            initialValue = MaterialTheme.colorScheme.primary,
            targetValue = MaterialTheme.colorScheme.tertiary,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            )
        )

        Box(
            modifier = Modifier.requiredSize(200.dp),
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DownloadButton(onClick = {
                coroutineScope.launch {
                    isLoading = true

                    connectToWifi(
                        wifiUtil = wifiUtil,
                        wifiDetails = wifiDetails,
                        onConnectionSuccess = {
                            isLoading = false
                            onConnectionSuccess()
                        },
                        onConnectionLost = {
                            isLoading = false
                            onConnectionLost()
                        }
                    )
                }
            })

            TextButton(onClick = onChangeWifiDetails) {
                Text(stringResource(R.string.change_wifi_details))
            }
        }
    }
}

// TODO: implement timeout, retry mechanism
private suspend fun connectToWifi(
    wifiUtil: WifiUtil,
    wifiDetails: WifiDetails,
    onConnectionSuccess: () -> Unit,
    onConnectionLost: () -> Unit
) {
    wifiUtil.connectToWifi(
        wifiDetails = wifiDetails,
        onConnectionSuccess = onConnectionSuccess,
        onConnectionLost = onConnectionLost
    )
}
