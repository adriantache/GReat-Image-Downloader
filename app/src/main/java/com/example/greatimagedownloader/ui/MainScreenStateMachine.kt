package com.example.greatimagedownloader.ui

import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.greatimagedownloader.R
import com.example.greatimagedownloader.domain.data.model.PhotoFile
import com.example.greatimagedownloader.domain.model.Events
import com.example.greatimagedownloader.domain.model.Events.CannotDownloadPhotos
import com.example.greatimagedownloader.domain.model.Events.ConfirmDeleteAllPhotos
import com.example.greatimagedownloader.domain.model.Events.DownloadPhotosWithService
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
import com.example.greatimagedownloader.domain.utils.model.Event
import com.example.greatimagedownloader.service.DataTransferTool
import com.example.greatimagedownloader.service.PhotoDownloadService
import com.example.greatimagedownloader.service.PhotoDownloadService.Actions
import com.example.greatimagedownloader.service.PhotoDownloadService.Companion.PHOTOS_LIST_EXTRA
import com.example.greatimagedownloader.service.model.PhotoFileItem
import com.example.greatimagedownloader.ui.permissions.PermissionsRequester
import com.example.greatimagedownloader.ui.view.ChangeSettingsScreen
import com.example.greatimagedownloader.ui.view.DownloadingView
import com.example.greatimagedownloader.ui.view.PermissionsView
import com.example.greatimagedownloader.ui.view.SelectFoldersView
import com.example.greatimagedownloader.ui.view.StartView
import com.example.greatimagedownloader.ui.view.SyncView
import com.example.greatimagedownloader.ui.view.WifiInputView
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject


@Composable
fun MainScreenStateMachine(
    viewModel: MainScreenViewModel = koinViewModel(),
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

    // TODO: add a BackHandler and implement behaviour

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
            HandleEvent(event, snackbarHostState)

            when (val state = stateValue) {
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
                    isSoftWifiTimeout = state.isSoftTimeout,
                    onSoftWifiTimeoutRetry = state.onSoftTimeoutRetry,
                    isHardWifiTimeout = state.isHardTimeout,
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

@Composable
private fun HandleEvent(
    events: Event<Events>?,
    snackbarHostState: SnackbarHostState,
    dataTransferTool: DataTransferTool = koinInject(),
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            val serviceIntent = Intent(context, PhotoDownloadService::class.java)
            serviceIntent.action = Actions.STOP.name
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    val event = events?.value
    LaunchedEffect(event) {
        if (event == null) return@LaunchedEffect

        when (event) {
            is SuccessfulDownload -> snackbarHostState.showSnackbar("Downloaded ${event.numDownloadedPhotos} photos.")

            CannotDownloadPhotos -> Unit
            InvalidWifiInput -> Unit
            is ConfirmDeleteAllPhotos -> Unit
            is DownloadPhotosWithService -> {
                launch {
                    dataTransferTool.imageFlow.collect { photoDownloadInfo ->
                        println("Received: $photoDownloadInfo")

                        if (photoDownloadInfo != null) {
                            event.onDownloadInfo(photoDownloadInfo)
                        }
                    }
                }

                launch {
                    dataTransferTool.downloadFinishedFlow.collect {
                        if (it?.value != null) event.onDownloadFinished()
                    }
                }

                downloadPhotos(context, event.photosToDownload)
            }
        }
    }

    when (event) {
        InvalidWifiInput -> Text(stringResource(R.string.error_invalid_wifi_input))
        CannotDownloadPhotos -> Text(stringResource(R.string.error_cannot_get_photos))
        is ConfirmDeleteAllPhotos -> AlertDialog(
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
        is DownloadPhotosWithService -> Unit
        null -> Unit
    }
}

fun downloadPhotos(
    context: Context,
    photosToDownload: List<PhotoFile>,
) {
    val photos = photosToDownload.map {
        PhotoFileItem(
            directory = it.directory,
            name = it.name,
        )
    }

    val serviceIntent = Intent(context, PhotoDownloadService::class.java)
    serviceIntent.putParcelableArrayListExtra(PHOTOS_LIST_EXTRA, ArrayList(photos))
    serviceIntent.action = Actions.START.name

    ContextCompat.startForegroundService(context, serviceIntent)
}
