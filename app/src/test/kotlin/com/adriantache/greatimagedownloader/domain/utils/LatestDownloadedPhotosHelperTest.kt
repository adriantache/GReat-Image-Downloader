package com.adriantache.greatimagedownloader.domain.utils

import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LatestDownloadedPhotosHelperTest {
    @Test
    fun `does not advance marker past a failed photo in the same directory`() {
        val existing = listOf(PhotoFile("100RICOH", "R0000100.JPG"))
        val photosToDownload = listOf(
            PhotoFile("100RICOH", "R0000101.JPG"),
            PhotoFile("100RICOH", "R0000102.JPG"),
            PhotoFile("100RICOH", "R0000103.JPG"),
        )
        // Simulate R0000102.JPG failing while R0000101 and R0000103 completed
        val completed = setOf(
            PhotoFile("100RICOH", "R0000101.JPG"),
            PhotoFile("100RICOH", "R0000103.JPG"),
        )

        val result = computeUpdatedLatestDownloadedPhotos(existing, photosToDownload, completed)

        assertThat(result).containsExactly(PhotoFile("100RICOH", "R0000101.JPG"))
    }

    @Test
    fun `keeps existing marker when first photo in directory fails`() {
        val existing = listOf(PhotoFile("100RICOH", "R0000100.JPG"))
        val photosToDownload = listOf(
            PhotoFile("100RICOH", "R0000101.JPG"),
            PhotoFile("100RICOH", "R0000102.JPG"),
        )
        val completed = setOf(PhotoFile("100RICOH", "R0000102.JPG"))

        val result = computeUpdatedLatestDownloadedPhotos(existing, photosToDownload, completed)

        assertThat(result).containsExactly(PhotoFile("100RICOH", "R0000100.JPG"))
    }

    @Test
    fun `updates each directory independently`() {
        val existing = listOf(PhotoFile("100RICOH", "R0000100.JPG"))
        val photosToDownload = listOf(
            PhotoFile("100RICOH", "R0000101.JPG"),
            PhotoFile("101RICOH", "R0000201.JPG"),
            PhotoFile("101RICOH", "R0000202.JPG"),
        )
        val completed = setOf(
            PhotoFile("101RICOH", "R0000201.JPG"),
        )

        val result = computeUpdatedLatestDownloadedPhotos(existing, photosToDownload, completed)

        assertThat(result).containsExactlyInAnyOrder(
            PhotoFile("100RICOH", "R0000100.JPG"),
            PhotoFile("101RICOH", "R0000201.JPG"),
        )
    }
}
