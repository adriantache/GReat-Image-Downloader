package com.adriantache.greatimagedownloader.wear.model

import kotlinx.serialization.Serializable

@Serializable
data class WearDownloadStatus(
    val state: String = "IDLE", // IDLE, CONNECTING, CHECKING, DOWNLOADING, STOPPING, DONE, ERROR
    val stageText: String = "",
    val connectionStep: Int = 0,
    val totalConnectionSteps: Int = 5,
    val currentPhoto: Int = 0,
    val totalPhotos: Int = 0,
    val photoProgressPercent: Int = 0,
    val downloadSpeedKbps: Double = 0.0,
    val currentFileName: String = "",
    val downloadCount: Int = 0,
    val errorMessage: String? = null,
) {
    val overallProgress: Float
        get() {
            if (totalPhotos <= 0) return 0f
            val completedPhotos = (currentPhoto - 1).coerceIn(0, totalPhotos)
            val currentFileFraction = photoProgressPercent.coerceIn(0, 100) / 100f
            return ((completedPhotos + currentFileFraction) / totalPhotos).coerceIn(0f, 1f)
        }

    val resolvedConnectionStep: Int
        get() {
            if (connectionStep > 0) return connectionStep.coerceIn(1, totalConnectionSteps.coerceAtLeast(1))
            return when {
                state == "CHECKING" -> 5
                stageText.contains("Joining", ignoreCase = true) -> 4
                stageText.contains("Starting", ignoreCase = true) -> 3
                stageText.contains("Waking", ignoreCase = true) -> 2
                state == "CONNECTING" -> 1
                else -> 0
            }
        }
}
