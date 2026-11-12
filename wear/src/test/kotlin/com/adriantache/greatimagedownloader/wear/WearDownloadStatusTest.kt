package com.adriantache.greatimagedownloader.wear

import com.adriantache.greatimagedownloader.wear.model.WearDownloadStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset
import org.junit.Test

class WearDownloadStatusTest {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `serialize and deserialize WearDownloadStatus preserving all fields`() {
        val original = WearDownloadStatus(
            state = "DOWNLOADING",
            stageText = "Downloading photos…",
            connectionStep = 3,
            totalConnectionSteps = 5,
            currentPhoto = 4,
            totalPhotos = 12,
            photoProgressPercent = 85,
            downloadSpeedKbps = 4500.0,
            currentFileName = "R0001042.JPG",
            downloadCount = 3,
            errorMessage = null,
        )

        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<WearDownloadStatus>(encoded)

        assertThat(decoded).isEqualTo(original)
    }

    @Test
    fun `deserialize handles missing optional fields gracefully`() {
        val partialJson = """{"state":"CONNECTING","stageText":"Connecting…"}"""
        val decoded = json.decodeFromString<WearDownloadStatus>(partialJson)

        assertThat(decoded.state).isEqualTo("CONNECTING")
        assertThat(decoded.stageText).isEqualTo("Connecting…")
        assertThat(decoded.currentPhoto).isEqualTo(0)
        assertThat(decoded.totalPhotos).isEqualTo(0)
        assertThat(decoded.downloadSpeedKbps).isEqualTo(0.0)
        assertThat(decoded.currentFileName).isEmpty()
        assertThat(decoded.errorMessage).isNull()
    }

    @Test
    fun `overallProgress computes smooth progress combining photo index and in-flight percent`() {
        val status = WearDownloadStatus(
            state = "DOWNLOADING",
            currentPhoto = 3,
            totalPhotos = 4,
            photoProgressPercent = 50,
        )

        // 2 finished photos + 0.5 of photo 3 = 2.5 / 4 = 0.625f
        assertThat(status.overallProgress).isCloseTo(0.625f, Offset.offset(0.001f))
    }

    @Test
    fun `resolvedConnectionStep infers step from stageText when explicit step not provided`() {
        val wakingStatus = WearDownloadStatus(state = "CONNECTING", stageText = "Camera connected! Waking…")
        assertThat(wakingStatus.resolvedConnectionStep).isEqualTo(2)

        val wifiStatus = WearDownloadStatus(state = "CONNECTING", stageText = "Starting camera Wi-Fi…")
        assertThat(wifiStatus.resolvedConnectionStep).isEqualTo(3)

        val joiningStatus = WearDownloadStatus(state = "CONNECTING", stageText = "Joining camera Wi-Fi…")
        assertThat(joiningStatus.resolvedConnectionStep).isEqualTo(4)

        val checkingStatus = WearDownloadStatus(state = "CHECKING", stageText = "Checking camera…")
        assertThat(checkingStatus.resolvedConnectionStep).isEqualTo(5)
    }
}
