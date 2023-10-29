package com.example.greatimagedownloader.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.domain.model.Events
import com.example.greatimagedownloader.domain.model.Events.CannotDownloadPhotos
import com.example.greatimagedownloader.domain.model.Events.InvalidWifiInput
import com.example.greatimagedownloader.domain.model.Events.SuccessfulDownload
import com.example.greatimagedownloader.domain.model.States
import com.example.greatimagedownloader.domain.model.States.ChangeSettings
import com.example.greatimagedownloader.domain.model.States.ConnectWifi
import com.example.greatimagedownloader.domain.model.States.DownloadPhotos
import com.example.greatimagedownloader.domain.model.States.GetPhotos
import com.example.greatimagedownloader.domain.model.States.Init
import com.example.greatimagedownloader.domain.model.States.RequestPermissions
import com.example.greatimagedownloader.domain.model.States.RequestWifiCredentials
import com.example.greatimagedownloader.ui.permissions.PermissionsRequester
import com.example.greatimagedownloader.ui.view.ChangeSettingsScreen
import com.example.greatimagedownloader.ui.view.DownloadingView
import com.example.greatimagedownloader.ui.view.PermissionsView
import com.example.greatimagedownloader.ui.view.SelectFoldersView
import com.example.greatimagedownloader.ui.view.StartView
import com.example.greatimagedownloader.ui.view.SyncView
import com.example.greatimagedownloader.ui.view.WifiInputView
import org.koin.androidx.compose.getViewModel

@Composable
fun MainScreenStateMachine(
    viewModel: MainScreenViewModel = getViewModel(),
) {
    val stateValue by viewModel.downloadPhotosState.collectAsState()
    val event by viewModel.downloadPhotosEvents.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(stateValue) {
        when (val state = stateValue) {
            is Init -> state.onInit()
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        event?.value?.let {
            HandleEvent(it, snackbarHostState)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(stateValue, label = "stateMachineAnimation") { state ->
                when (state) {
                    is Init -> Unit

                    is RequestPermissions -> PermissionsRequester(
                        onPermissionsGranted = state.onPermissionsGranted
                    ) { isLocationPermissionGranted, isPhotosPermissionGranted, isNotificationsPermissionGranted, onRequestPermissions ->
                        PermissionsView(
                            isLocationPermissionGranted = isLocationPermissionGranted,
                            isPhotosPermissionGranted = isPhotosPermissionGranted,
                            isNotificationsPermissionGranted = isNotificationsPermissionGranted,
                            onRequestPermissions = onRequestPermissions,
                        )
                    }

                    is RequestWifiCredentials -> WifiInputView(
                        onWifiCredentialsInput = state.onWifiCredentialsInput,
                        onSuggestWifiName = state.onSuggestWifiName,
                    )

                    is ConnectWifi -> StartView(
                        onCheckWifiDisabled = state.onCheckWifiDisabled,
                        onConnect = state.onConnect,
                        onChangeWifiDetails = state.onChangeWifiDetails,
                        onAdjustSettings = state.onAdjustSettings,
                    )

                    GetPhotos -> SyncView()

                    is States.SelectFolders -> SelectFoldersView(
                        folderInfo = state.folderInfo,
                        onFoldersSelect = state.onFoldersSelect,
                    )

                    is DownloadPhotos -> DownloadingView(
                        currentPhoto = state.currentPhotoNum,
                        totalPhotos = state.totalPhotos,
                        photoDownloadInfo = state.downloadedPhotos,
                        downloadSpeed = state.downloadSpeed,
                        isStopping = state.isStopping,
                        onClose = state.onStopDownloading,
                    )

                    is ChangeSettings -> ChangeSettingsScreen(state)
                }
            }
        }
    }
}

@Composable
private fun HandleEvent(
    event: Events,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(event) {
        when (event) {
            is SuccessfulDownload -> snackbarHostState.showSnackbar("Downloaded ${event.numDownloadedPhotos} photos.")

            CannotDownloadPhotos -> Unit
            InvalidWifiInput -> Unit
            is Events.ConfirmDeleteAllPhotos -> Unit
        }
    }

    when (event) {
        InvalidWifiInput -> Text(stringResource(R.string.error_invalid_wifi_input))
        CannotDownloadPhotos -> Text(stringResource(R.string.error_cannot_get_photos))
        is Events.ConfirmDeleteAllPhotos -> AlertDialog(
            onDismissRequest = event.onDismiss,
            title = { Text("Are you sure you want to delete this?") },
            text = { Text("This action cannot be undone") },
            confirmButton = {
                TextButton(onClick = event.onConfirm) {
                    Text(
                        text = "Delete it".uppercase(),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = event.onDismiss) {
                    Text("Cancel".uppercase())
                }
            },
        )

        is SuccessfulDownload -> Unit
    }
}
