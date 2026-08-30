package com.adriantache.greatimagedownloader.ui.permissions

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// TODO: move this functionality to usecase and platform layers
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsRequester(
    onPermissionsGranted: () -> Unit,
    content: @Composable (
        isLocationPermissionGranted: Boolean,
        isPhotosPermissionGranted: Boolean,
        isNotificationsPermissionGranted: Boolean,
        onRequestPermissions: () -> Unit,
    ) -> Unit,
) {
    val photosPermissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            READ_MEDIA_IMAGES
        } else {
            READ_EXTERNAL_STORAGE
        }
    ) { }

    val locationPermissionState = rememberPermissionState(permission = ACCESS_FINE_LOCATION) { }

    val notificationsPermissionsState: PermissionState? = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        null
    } else {
        rememberPermissionState(permission = POST_NOTIFICATIONS) { }
    }

    val isNotificationsPermissionGranted by remember(notificationsPermissionsState) {
        derivedStateOf {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    notificationsPermissionsState?.status?.isGranted == true
        }
    }

    LaunchedEffect(locationPermissionState, photosPermissionState, isNotificationsPermissionGranted) {
        if (locationPermissionState.status.isGranted &&
            photosPermissionState.status.isGranted &&
            isNotificationsPermissionGranted
        ) {
            onPermissionsGranted()
        }
    }

    content(
        /* isLocationPermissionGranted */ locationPermissionState.status.isGranted,
        /* isPhotosPermissionGranted */ photosPermissionState.status.isGranted,
        /* isNotificationsPermissionGranted */ isNotificationsPermissionGranted,
    ) /* onRequestPermissions */ {
        onRequestPermissions(
            photosPermissionState,
            locationPermissionState,
            notificationsPermissionsState,
            onPermissionsGranted,
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun onRequestPermissions(
    photosPermissionState: PermissionState,
    locationPermissionState: PermissionState,
    notificationsPermissionState: PermissionState?,
    onPermissionsGranted: () -> Unit,
) {
    when {
        !locationPermissionState.status.isGranted -> locationPermissionState.launchPermissionRequest()
        !photosPermissionState.status.isGranted -> photosPermissionState.launchPermissionRequest()
        notificationsPermissionState != null && !notificationsPermissionState.status.isGranted -> notificationsPermissionState.launchPermissionRequest()
        else -> onPermissionsGranted()
    }
}
