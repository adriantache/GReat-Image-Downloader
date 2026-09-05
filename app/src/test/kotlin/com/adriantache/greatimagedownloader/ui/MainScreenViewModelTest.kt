package com.adriantache.greatimagedownloader.ui

import com.adriantache.greatimagedownloader.domain.DownloadPhotosUseCase
import com.adriantache.greatimagedownloader.domain.model.Events
import com.adriantache.greatimagedownloader.domain.model.States
import com.adriantache.greatimagedownloader.domain.utils.model.Event
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MainScreenViewModelTest {

    @Test
    fun `viewModel correctly exposes state and event flows from useCase`() {
        // Arrange
        val mockState = MutableStateFlow<States>(States.Init {})
        val mockEvent = MutableStateFlow<Event<Events>?>(null)

        val useCase = mockk<DownloadPhotosUseCase> {
            every { state } returns mockState
            every { event } returns mockEvent
        }

        // Act
        val viewModel = MainScreenViewModel(useCase)

        // Assert
        assertThat(viewModel.downloadPhotosState).isEqualTo(mockState)
        assertThat(viewModel.downloadPhotosEvents).isEqualTo(mockEvent)
    }
}
