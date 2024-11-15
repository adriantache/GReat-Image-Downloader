package com.adriantache.greatimagedownloader.domain.utils.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

internal class EventTest {
    @Test
    fun `event value, not consumed`() {
        val event = Event(true)

        assertThat(event.value).isTrue
    }

    @Test
    fun `event value, consumed`() {
        val event = Event(true)

        // Consume the event.
        event.value

        assertThat(event.value).isNull()
    }
}
