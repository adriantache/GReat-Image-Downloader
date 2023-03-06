package com.example.greatimagedownloader.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.greatimagedownloader.domain.model.States.ConnectWifi
import com.example.greatimagedownloader.domain.model.States.Disconnect
import com.example.greatimagedownloader.domain.model.States.Disconnected
import com.example.greatimagedownloader.domain.model.States.DownloadPhotos
import com.example.greatimagedownloader.domain.model.States.GetPhotos
import com.example.greatimagedownloader.domain.model.States.Init
import com.example.greatimagedownloader.domain.model.States.RequestPermissions
import com.example.greatimagedownloader.domain.model.States.RequestWifiCredentials
import com.example.greatimagedownloader.ui.permissions.PermissionsRequester
import com.example.greatimagedownloader.ui.view.PermissionsView
import com.example.greatimagedownloader.ui.view.WifiInputView
import org.koin.core.context.GlobalContext.get

@Composable
fun MainScreenStateMachine(
    viewModel: MainScreenViewModel = get().get(),
) {
    val stateValue by viewModel.downloadPhotosState.collectAsState()

    when (val state = stateValue) {
        is Init -> state.onInit()

        is RequestPermissions -> PermissionsRequester(onPermissionsGranted = state.onPermissionsGranted) {
            PermissionsView()
        }

        is RequestWifiCredentials -> WifiInputView(state.onWifiCredentialsInput)
        is ConnectWifi -> TODO()

        GetPhotos -> TODO()
        is DownloadPhotos -> TODO()
        Disconnect -> TODO()

        Disconnected -> TODO()
    }
}
