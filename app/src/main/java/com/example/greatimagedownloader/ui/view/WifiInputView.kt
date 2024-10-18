package com.example.greatimagedownloader.ui.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.domain.ui.model.WifiDetails

@Composable
fun WifiInputView(
    onWifiCredentialsInput: (WifiDetails) -> Unit,
    onSuggestWifiName: suspend () -> String,
) {
    val focusManager = LocalFocusManager.current

    var triggerWifiSuggestion by remember { mutableStateOf(false) }

    var wifiName by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }

    LaunchedEffect(triggerWifiSuggestion) {
        if (!triggerWifiSuggestion) return@LaunchedEffect

        wifiName = onSuggestWifiName()
        triggerWifiSuggestion = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(100.dp),
            painter = painterResource(id = R.drawable.wifi_lock),
            contentDescription = null
        )

        Text(
            modifier = Modifier.padding(horizontal = 64.dp),
            text = stringResource(R.string.wifi_details_hint),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall,
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = wifiName,
            onValueChange = { wifiName = it },
            singleLine = true,
            label = { Text("SSID") },
            trailingIcon = {
                val iconSize = 28.dp

                if (triggerWifiSuggestion) {
                    CircularProgressIndicator(modifier = Modifier.requiredSize(iconSize))
                } else {
                    IconButton(onClick = {
                        triggerWifiSuggestion = true
                        focusManager.clearFocus()
                    }) {
                        Icon(
                            painterResource(id = R.drawable.wand),
                            contentDescription = "Auto scan",
                            modifier = Modifier.requiredSize(iconSize),
                        )
                    }
                }
            }
        )

        TextField(
            value = wifiPass,
            singleLine = true,
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
    WifiInputView({}, { "" })
}
