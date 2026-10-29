package com.adriantache.greatimagedownloader.wear

import com.adriantache.greatimagedownloader.wear.model.WearDownloadStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
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
            currentPhoto = 4,
            totalPhotos = 12,
            photoProgressPercent = 85,
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
        assertThat(decoded.errorMessage).isNull()
    }
}
