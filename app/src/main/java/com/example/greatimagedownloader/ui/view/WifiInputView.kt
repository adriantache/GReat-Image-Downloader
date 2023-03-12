package com.example.greatimagedownloader.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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

        Image(
            modifier = Modifier.size(100.dp),
            painter = painterResource(id = R.drawable.wifi_lock),
            contentDescription = null
        )

        Text(
            modifier = Modifier.padding(horizontal = 64.dp),
            text = stringResource(R.string.wifi_details_hint),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = wifiName,
            onValueChange = { wifiName = it },
            label = { Text("SSID") }
        )

        TextField(
            value = wifiPass,
            onValueChange = { wifiPass = it },
            label = { Text("Password") }
        )

        Button(onClick = { onWifiCredentialsInput(WifiDetails(wifiName, wifiPass)) }) {
            Text(stringResource(R.string.save))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WifiInputViewPreview() {
    WifiInputView {}
}
