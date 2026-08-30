package com.adriantache.greatimagedownloader.domain.model

import com.adriantache.greatimagedownloader.domain.data.model.PhotoFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class FolderInfoTest {
    @Test
    fun `constructor correctly maps media list to folders`() {
        val media = listOf(
            PhotoFile("folder1", "photo1.jpg"),
            PhotoFile("folder1", "photo2.jpg"),
            PhotoFile("folder2", "photo3.jpg")
        )
        
        val folderInfo = FolderInfo(media)
        
        assertThat(folderInfo.folders).hasSize(2)
        assertThat(folderInfo.folders["folder1"]).isEqualTo(2)
        assertThat(folderInfo.folders["folder2"]).isEqualTo(1)
        assertThat(folderInfo.hasMultipleFolders).isTrue
    }

    @Test
    fun `hasMultipleFolders returns false for single folder`() {
        val media = listOf(PhotoFile("folder1", "photo1.jpg"))
        val folderInfo = FolderInfo(media)
        assertThat(folderInfo.hasMultipleFolders).isFalse
    }
}
