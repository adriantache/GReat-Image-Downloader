package com.example.greatimagedownloader.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.domain.ui.model.WifiDetails
import com.example.greatimagedownloader.ui.wifi.WifiUtil
import com.example.greatimagedownloader.ui.wifi.WifiUtilImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

        CircularProgressIndicator()
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
