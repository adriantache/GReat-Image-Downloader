package com.example.greatimagedownloader.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.domain.ui.model.WifiDetails
import com.example.greatimagedownloader.ui.wifi.WifiUtil
import com.example.greatimagedownloader.ui.wifi.WifiUtilImpl

@Composable
fun StartView(
    wifiDetails: WifiDetails,
    onConnectionSuccess: () -> Unit,
    onConnectionLost: () -> Unit,
    onChangeWifiDetails: () -> Unit,
) {
    val context = LocalContext.current

    val wifiUtil: WifiUtil by remember { mutableStateOf(WifiUtilImpl(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = {
            connectToWifi(
                wifiUtil = wifiUtil,
                wifiDetails = wifiDetails,
                onConnectionSuccess = onConnectionSuccess,
                onConnectionLost = onConnectionLost
            )
        }) {
            Text("Connect and start download")
        }

        Button(onClick = onChangeWifiDetails) {
            Text("Change wifi details")
        }
    }
}

// TODO: implement timeout, retry mechanism
private fun connectToWifi(
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
