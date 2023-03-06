package com.example.greatimagedownloader.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.domain.ui.model.WifiDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiInputView(
    onWifiCredentialsInput: (WifiDetails) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var wifiName by remember { mutableStateOf("") }
        var wifiPass by remember { mutableStateOf("") }

        Text(
            modifier = Modifier.padding(horizontal = 64.dp),
            text = stringResource(R.string.wifi_details_hint),
            textAlign = TextAlign.Center,
            style = typography.labelLarge,
        )

        TextField(value = wifiName, onValueChange = { wifiName = it })

        TextField(value = wifiPass, onValueChange = { wifiPass = it })

        Button(onClick = { onWifiCredentialsInput(WifiDetails(wifiName, wifiPass)) }) {
            Text(stringResource(R.string.connect))
        }
    }
}
