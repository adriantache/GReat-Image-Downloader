package com.example.greatimagedownloader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.domain.model.Events
import com.example.greatimagedownloader.domain.model.States.ChangeSettings
import com.example.greatimagedownloader.domain.model.States.ConnectWifi
import com.example.greatimagedownloader.domain.model.States.Disconnected
import com.example.greatimagedownloader.domain.model.States.DownloadPhotos
import com.example.greatimagedownloader.domain.model.States.GetPhotos
import com.example.greatimagedownloader.domain.model.States.Init
import com.example.greatimagedownloader.domain.model.States.RequestPermissions
import com.example.greatimagedownloader.domain.model.States.RequestWifiCredentials
import com.example.greatimagedownloader.ui.permissions.PermissionsRequester
import com.example.greatimagedownloader.ui.view.DownloadingView
import com.example.greatimagedownloader.ui.view.PermissionsView
import com.example.greatimagedownloader.ui.view.StartView
import com.example.greatimagedownloader.ui.view.SyncView
import com.example.greatimagedownloader.ui.view.WifiInputView
import org.koin.androidx.compose.getViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenStateMachine(
    viewModel: MainScreenViewModel = getViewModel(),
) {
    val stateValue by viewModel.downloadPhotosState.collectAsState()
    val eventValue by viewModel.downloadPhotosEvents.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            eventValue?.value?.let {
                HandleEvent(it)
            }

            when (val state = stateValue) {
                is Init -> state.onInit()

                is RequestPermissions -> PermissionsRequester(
                    onPermissionsGranted = state.onPermissionsGranted
                ) { isLocationPermissionGranted, isPhotosPermissionGranted, onRequestPermissions ->
                    PermissionsView(
                        isLocationPermissionGranted = isLocationPermissionGranted,
                        isPhotosPermissionGranted = isPhotosPermissionGranted,
                        onRequestPermissions = onRequestPermissions,
                    )
                }

                is RequestWifiCredentials -> WifiInputView(onWifiCredentialsInput = state.onWifiCredentialsInput)
                is ConnectWifi -> StartView(
                    onCheckWifiDisabled = state.onCheckWifiDisabled,
                    onConnect = state.onConnect,
                    onChangeWifiDetails = state.onChangeWifiDetails,
                    onAdjustSettings = state.onAdjustSettings,
                )

                GetPhotos -> SyncView()

                is DownloadPhotos -> {
                    DownloadingView(
                        currentPhoto = state.currentPhotoNum,
                        totalPhotos = state.totalPhotos,
                        photoDownloadInfo = state.downloadedPhotos,
                        downloadSpeed = state.downloadSpeed,
                    )
                }

                // TODO: reset after delay in use case?
                is Disconnected ->
                    // TODO: make proper UI
                    Column {
                        Text("Downloaded ${state.numDownloadedPhotos} photos.")

                        Spacer(Modifier.height(8.dp))

                        Button(onClick = state.onRestart) {
                            Text("Restart process")
                        }
                    }

                ChangeSettings -> Text("Settings not implemented...")
            }
        }
    }
}

@Composable
private fun HandleEvent(event: Events) {
    when (event) {
        Events.InvalidWifiInput -> Text(stringResource(R.string.error_invalid_wifi_input))
        Events.CannotDownloadPhotos -> Text(stringResource(R.string.error_cannot_get_photos))
    }
}
