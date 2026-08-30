package com.adriantache.greatimagedownloader.service

import com.adriantache.greatimagedownloader.domain.data.model.PhotoDownloadInfo
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import com.adriantache.greatimagedownloader.domain.utils.model.Kbps
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DataTransferToolTest {
    @Test
    fun `reset clears all flows`() {
        val tool = DataTransferTool()
        tool.imageFlow.value = PhotoDownloadInfo("name", "uri", 50, Kbps(100.0))
        tool.errorFlow.value = Event(DomainError.NetworkError)
        tool.downloadFinishedFlow.value = Event(Unit)
        
        tool.reset()
        
        assertThat(tool.imageFlow.value).isNull()
        assertThat(tool.errorFlow.value).isNull()
        assertThat(tool.downloadFinishedFlow.value).isNull()
    }
}
