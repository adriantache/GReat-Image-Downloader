package com.example.greatimagedownloader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.domain.model.Events
import com.example.greatimagedownloader.domain.model.States.ConnectWifi
import com.example.greatimagedownloader.domain.model.States.Disconnect
import com.example.greatimagedownloader.domain.model.States.Disconnected
import com.example.greatimagedownloader.domain.model.States.DownloadPhotos
import com.example.greatimagedownloader.domain.model.States.GetPhotos
import com.example.greatimagedownloader.domain.model.States.Init
import com.example.greatimagedownloader.domain.model.States.RequestPermissions
import com.example.greatimagedownloader.domain.model.States.RequestWifiCredentials
import com.example.greatimagedownloader.ui.permissions.PermissionsRequester
import com.example.greatimagedownloader.ui.util.KeepScreenOn
import com.example.greatimagedownloader.ui.view.PermissionsView
import com.example.greatimagedownloader.ui.view.StartView
import com.example.greatimagedownloader.ui.view.WifiInputView
import org.koin.androidx.compose.getViewModel

@Composable
fun MainScreenStateMachine(
    viewModel: MainScreenViewModel = getViewModel(),
) {
    val stateValue by viewModel.downloadPhotosState.collectAsState()
    val eventValue by viewModel.downloadPhotosEvents.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        eventValue?.value?.let {
            HandleEvent(it)
        }

        when (val state = stateValue) {
            is Init -> state.onInit()

            is RequestPermissions -> PermissionsRequester(onPermissionsGranted = state.onPermissionsGranted) {
                PermissionsView()
            }

            is RequestWifiCredentials -> WifiInputView(state.onWifiCredentialsInput)
            is ConnectWifi -> StartView(
                wifiDetails = state.wifiDetails,
                onConnectionSuccess = state.onConnectionSuccess,
                onConnectionLost = state.onConnectionLost,
                onChangeWifiDetails = state.onChangeWifiDetails,
            )

            GetPhotos -> Text("Getting existing photos")
            is DownloadPhotos -> {
                KeepScreenOn()
                Text("Downloading photos: ${state.currentPhotoNum}/${state.totalPhotos}")
            }

            Disconnect -> Text("Disconnecting")

            // TODO: reset after delay in use case?
            is Disconnected -> Button(onClick = state.onRestart) {
                Text("Restart process")
            }
        }
    }
}

@Composable
private fun HandleEvent(event: Events) {
    when (event) {
        Events.InvalidWifiInput -> Text(stringResource(R.string.error_invalid_wifi_input))
        Events.CannotDownloadPhotos -> Text("Cannot download photo list from camera!")
    }
}
