package com.example.greatimagedownloader.ui.permissions

import android.Manifest.permission.*
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsRequester(
    onPermissionsGranted: () -> Unit,
    content: @Composable (
        isLocationPermissionGranted: Boolean,
        isPhotosPermissionGranted: Boolean,
        onRequestPermissions: () -> Unit,
    ) -> Unit,
) {
    val photosPermissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            READ_MEDIA_IMAGES
        } else {
            READ_EXTERNAL_STORAGE
        }
    ) {
        // Handled in LaunchedEffect
    }

    val locationPermissionState = rememberPermissionState(
        permission = ACCESS_FINE_LOCATION
    ) {
        // Handled in LaunchedEffect
    }


    LaunchedEffect(photosPermissionState, locationPermissionState) {
        onRequestPermissions(photosPermissionState, locationPermissionState, onPermissionsGranted)
    }

    if (!photosPermissionState.status.isGranted || !locationPermissionState.status.isGranted) {
        content(
            isLocationPermissionGranted = locationPermissionState.status.isGranted,
            isPhotosPermissionGranted = photosPermissionState.status.isGranted,
            onRequestPermissions = {
                onRequestPermissions(
                    photosPermissionState,
                    locationPermissionState,
                    onPermissionsGranted
                )
            }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun onRequestPermissions(
    photosPermissionState: PermissionState,
    locationPermissionState: PermissionState,
    onPermissionsGranted: () -> Unit
) {
    when {
        !locationPermissionState.status.isGranted -> locationPermissionState.launchPermissionRequest()
        !photosPermissionState.status.isGranted -> photosPermissionState.launchPermissionRequest()
        else -> onPermissionsGranted()
    }
}
