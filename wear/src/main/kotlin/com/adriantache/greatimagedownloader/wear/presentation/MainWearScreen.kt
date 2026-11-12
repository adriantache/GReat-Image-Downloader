package com.adriantache.greatimagedownloader.wear.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.adriantache.greatimagedownloader.wear.R
import com.adriantache.greatimagedownloader.wear.data.WearCommunicationClient
import com.adriantache.greatimagedownloader.wear.model.WearDownloadStatus
import com.adriantache.greatimagedownloader.wear.theme.DarkCard
import com.adriantache.greatimagedownloader.wear.theme.ErrorRed
import com.adriantache.greatimagedownloader.wear.theme.RicohYellow
import com.adriantache.greatimagedownloader.wear.theme.RicohYellowContainer
import com.adriantache.greatimagedownloader.wear.theme.SuccessGreen
import java.util.Locale

// Open arc angles that leave the top 52° (10:30 to 1:30) free for TimeText per Wear OS guidelines
private const val ARC_START_ANGLE = 296f
private const val ARC_END_ANGLE = 244f
private val BEZEL_STROKE_WIDTH = 8.dp

@Composable
fun MainWearScreen(
    client: WearCommunicationClient,
) {
    val status by client.statusFlow.collectAsState()
    val isPhoneConnected by client.isPhoneConnectedFlow.collectAsState()
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(status.state) {
        when (status.state) {
            "DONE", "ERROR" -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (!isPhoneConnected) {
                PhoneDisconnectedContent(onRetry = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    client.checkPhoneConnection()
                })
            } else {
                when (status.state) {
                    "CONNECTING" -> ConnectingContent(status = status, onCancel = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        client.sendStopSync()
                    })
                    "CHECKING" -> CheckingContent(onCancel = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        client.sendStopSync()
                    })
                    "DOWNLOADING" -> DownloadingContent(status = status, onStop = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        client.sendStopSync()
                    })
                    "STOPPING" -> StoppingContent()
                    "DONE" -> DoneContent(status = status, onDismiss = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        client.resetToIdle()
                    })
                    "ERROR" -> ErrorContent(status = status, onRetry = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        client.sendStartSync()
                    })
                    else -> IdleContent(onStart = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        client.sendStartSync()
                    })
                }
            }
        }
    }
}

/**
 * Curved stadium bottom button spanning the curvature of the smartwatch screen.
 * Replaces small circular buttons with an ergonomic thumb target that hugs the bottom bezel.
 */
@Composable
private fun CurvedBottomButton(
    onClick: () -> Unit,
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.primaryButtonColors(
            backgroundColor = containerColor,
            contentColor = contentColor,
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth(0.86f)
            .height(46.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(19.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun IdleContent(
    onStart: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(RicohYellowContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "GR",
                    color = RicohYellow,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.app_name),
                color = Color.LightGray,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }

        Chip(
            onClick = onStart,
            colors = ChipDefaults.primaryChipColors(
                backgroundColor = RicohYellow,
                contentColor = Color.Black,
                secondaryContentColor = Color(0xCC000000),
                iconColor = Color.Black,
            ),
            icon = {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.sync_photos),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                )
            },
            secondaryLabel = {
                Text(
                    text = stringResource(R.string.ready_to_sync),
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            },
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.92f),
        )
    }
}

@Composable
private fun ConnectingContent(
    status: WearDownloadStatus,
    onCancel: () -> Unit,
) {
    val step = status.resolvedConnectionStep
    val totalSteps = status.totalConnectionSteps.coerceAtLeast(1)
    val targetFraction = (step.toFloat() / totalSteps).coerceIn(0.15f, 1f)

    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "connProgress",
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = animatedProgress,
            startAngle = ARC_START_ANGLE,
            endAngle = ARC_END_ANGLE,
            indicatorColor = RicohYellow,
            trackColor = Color(0x33FFCA28),
            strokeWidth = BEZEL_STROKE_WIDTH,
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, top = 26.dp, end = 14.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(RicohYellowContainer)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = stringResource(R.string.step_progress, step, totalSteps),
                    color = RicohYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    text = status.stageText.ifBlank { "Connecting to camera…" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            CurvedBottomButton(
                onClick = onCancel,
                icon = Icons.Default.Close,
                text = stringResource(R.string.cancel),
                containerColor = DarkCard,
                contentColor = ErrorRed,
            )
        }
    }
}

@Composable
private fun CheckingContent(
    onCancel: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "checkingProgress",
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = animatedProgress,
            startAngle = ARC_START_ANGLE,
            endAngle = ARC_END_ANGLE,
            indicatorColor = RicohYellow,
            trackColor = Color(0x33FFCA28),
            strokeWidth = BEZEL_STROKE_WIDTH,
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, top = 26.dp, end = 14.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(RicohYellowContainer)
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text(
                    text = stringResource(R.string.step_progress, 5, 5),
                    color = RicohYellow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }

            Text(
                text = stringResource(R.string.checking_camera),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )

            CurvedBottomButton(
                onClick = onCancel,
                icon = Icons.Default.Close,
                text = stringResource(R.string.cancel),
                containerColor = DarkCard,
                contentColor = ErrorRed,
            )
        }
    }
}

@Composable
private fun DownloadingContent(
    status: WearDownloadStatus,
    onStop: () -> Unit,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = status.overallProgress,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "overallProgress",
    )

    val speedText = remember(status.downloadSpeedKbps) {
        when {
            status.downloadSpeedKbps >= 1024.0 -> String.format(Locale.US, "%.1f MB/s", status.downloadSpeedKbps / 1024.0)
            status.downloadSpeedKbps > 0 -> String.format(Locale.US, "%.0f KB/s", status.downloadSpeedKbps)
            else -> null
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            progress = animatedProgress,
            startAngle = ARC_START_ANGLE,
            endAngle = ARC_END_ANGLE,
            indicatorColor = RicohYellow,
            trackColor = Color(0x33FFCA28),
            strokeWidth = BEZEL_STROKE_WIDTH,
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 14.dp, top = 26.dp, end = 14.dp, bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top: Photo count & percentage with clear clearance below TimeText
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "${status.currentPhoto} / ${status.totalPhotos}",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color.White,
                )

                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RicohYellow,
                )
            }

            // Middle: Live speed & filename
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = speedText ?: stringResource(R.string.downloading),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )

                if (status.currentFileName.isNotBlank()) {
                    Text(
                        text = status.currentFileName,
                        fontSize = 11.sp,
                        color = Color(0xFFAAAAAA),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Bottom: Curved Stop action button
            CurvedBottomButton(
                onClick = onStop,
                icon = Icons.Default.Stop,
                text = stringResource(R.string.stop),
                containerColor = DarkCard,
                contentColor = ErrorRed,
            )
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
            strokeWidth = 4.dp,
            modifier = Modifier.size(40.dp),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.stopping),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
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
            .padding(start = 14.dp, top = 22.dp, end = 14.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(SuccessGreen.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.done),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = if (status.downloadCount == 0) {
                    stringResource(R.string.no_new_photos)
                } else {
                    stringResource(R.string.photos_downloaded, status.downloadCount)
                },
                fontSize = 13.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }

        // Curved bottom confirmation button
        CurvedBottomButton(
            onClick = onDismiss,
            icon = Icons.Default.Check,
            text = stringResource(R.string.ok),
            containerColor = RicohYellow,
            contentColor = Color.Black,
        )
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
            .padding(start = 14.dp, top = 22.dp, end = 14.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ErrorRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = status.errorMessage ?: status.stageText.ifBlank { "Sync failed" },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Curved bottom retry button
        CurvedBottomButton(
            onClick = onRetry,
            icon = Icons.Default.Refresh,
            text = stringResource(R.string.retry),
            containerColor = RicohYellow,
            contentColor = Color.Black,
        )
    }
}

@Composable
private fun PhoneDisconnectedContent(
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 14.dp, top = 22.dp, end = 14.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.phone_not_connected),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = stringResource(R.string.check_phone),
                fontSize = 12.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }

        // Curved bottom retry button
        CurvedBottomButton(
            onClick = onRetry,
            icon = Icons.Default.Refresh,
            text = stringResource(R.string.retry),
            containerColor = DarkCard,
            contentColor = Color.White,
        )
    }
}
