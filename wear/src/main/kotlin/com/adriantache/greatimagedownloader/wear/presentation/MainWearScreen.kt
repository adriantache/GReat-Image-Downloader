package com.adriantache.greatimagedownloader.wear.presentation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.adriantache.greatimagedownloader.wear.R
import com.adriantache.greatimagedownloader.wear.data.WearCommunicationClient
import com.adriantache.greatimagedownloader.wear.model.WearDownloadStatus
import com.adriantache.greatimagedownloader.wear.theme.ErrorRed
import com.adriantache.greatimagedownloader.wear.theme.RicohYellow
import com.adriantache.greatimagedownloader.wear.theme.RicohYellowContainer
import com.adriantache.greatimagedownloader.wear.theme.SuccessGreen

@Composable
fun MainWearScreen(
    client: WearCommunicationClient,
) {
    val status by client.statusFlow.collectAsState()
    val isPhoneConnected by client.isPhoneConnectedFlow.collectAsState()

    Scaffold(
        timeText = { TimeText() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (!isPhoneConnected) {
                PhoneDisconnectedContent(onRetry = { client.checkPhoneConnection() })
            } else {
                when (status.state) {
                    "CONNECTING" -> ConnectingContent(status = status, onCancel = { client.sendStopSync() })
                    "CHECKING" -> CheckingContent(onCancel = { client.sendStopSync() })
                    "DOWNLOADING" -> DownloadingContent(status = status, onStop = { client.sendStopSync() })
                    "STOPPING" -> StoppingContent()
                    "DONE" -> DoneContent(status = status, onDismiss = { client.resetToIdle() })
                    "ERROR" -> ErrorContent(status = status, onRetry = { client.sendStartSync() })
                    else -> IdleContent(onStart = { client.sendStartSync() })
                }
            }
        }
    }
}

@Composable
private fun IdleContent(
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(RicohYellowContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "GR",
                color = RicohYellow,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onStart,
            colors = ButtonDefaults.primaryButtonColors(),
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.sync_photos),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.ready_to_sync),
            color = Color.Gray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConnectingContent(
    status: WearDownloadStatus,
    onCancel: () -> Unit,
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            indicatorColor = RicohYellow,
            trackColor = Color(0x33FFCA28),
            modifier = Modifier.fillMaxSize(0.88f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Connecting…",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = RicohYellow,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = status.stageText.ifBlank { "Connecting to camera…" },
                fontSize = 10.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.alpha(alpha),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.secondaryButtonColors(),
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(R.string.stop),
                    tint = ErrorRed,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CheckingContent(
    onCancel: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            indicatorColor = RicohYellow,
            trackColor = Color(0x33FFCA28),
            modifier = Modifier.fillMaxSize(0.88f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.checking_camera),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = RicohYellow,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onCancel,
                colors = ButtonDefaults.secondaryButtonColors(),
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(R.string.stop),
                    tint = ErrorRed,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun DownloadingContent(
    status: WearDownloadStatus,
    onStop: () -> Unit,
) {
    val total = status.totalPhotos.coerceAtLeast(1)
    val progress = (status.currentPhoto.toFloat() / total).coerceIn(0f, 1f)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = progress,
            indicatorColor = RicohYellow,
            trackColor = Color(0x33FFCA28),
            modifier = Modifier.fillMaxSize(0.88f),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${status.currentPhoto} / ${status.totalPhotos}",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color.White,
            )

            if (status.photoProgressPercent > 0) {
                Text(
                    text = "${status.photoProgressPercent}%",
                    fontSize = 11.sp,
                    color = RicohYellow,
                )
            } else {
                Text(
                    text = stringResource(R.string.downloading),
                    fontSize = 10.sp,
                    color = Color.Gray,
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onStop,
                colors = ButtonDefaults.secondaryButtonColors(),
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(R.string.stop),
                    tint = ErrorRed,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun StoppingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            indicatorColor = ErrorRed,
            modifier = Modifier.size(32.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.stopping),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DoneContent(
    status: WearDownloadStatus,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(36.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.done),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White,
        )

        Text(
            text = if (status.downloadCount == 0) {
                stringResource(R.string.no_new_photos)
            } else {
                stringResource(R.string.photos_downloaded, status.downloadCount)
            },
            fontSize = 10.sp,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onDismiss,
            colors = ButtonDefaults.primaryButtonColors(),
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(36.dp),
        ) {
            Text(
                text = stringResource(R.string.ok),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ErrorContent(
    status: WearDownloadStatus,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = ErrorRed,
            modifier = Modifier.size(32.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = status.errorMessage ?: "Failed",
            fontSize = 10.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.primaryButtonColors(),
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(36.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.retry),
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun PhoneDisconnectedContent(
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.PhoneAndroid,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(32.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.phone_not_connected),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.secondaryButtonColors(),
            modifier = Modifier
                .fillMaxWidth(0.65f)
                .height(36.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.retry),
                    color = Color.White,
                    fontSize = 11.sp,
                )
            }
        }
    }
}
