package com.adriantache.greatimagedownloader.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test

class AppLoggerTest {
    private val loggedMessages = mutableListOf<Triple<AppLogger.Level, String, String>>()
    private val recordedKeys = mutableMapOf<String, String>()
    private val recordedExceptions = mutableListOf<Throwable>()

    private val fakeSink = object : AppLogger.LogSink {
        override fun log(level: AppLogger.Level, tag: String, message: String, throwable: Throwable?) {
            loggedMessages.add(Triple(level, tag, message))
        }

        override fun setKey(key: String, value: String) {
            recordedKeys[key] = value
        }

        override fun recordException(throwable: Throwable) {
            recordedExceptions.add(throwable)
        }
    }

    @After
    fun tearDown() {
        AppLogger.setSinkForTest(null)
    }

    @Test
    fun `debug and warn log breadcrumbs without recording exceptions`() {
        AppLogger.setSinkForTest(fakeSink)

        AppLogger.d("Widget", "tapped")
        AppLogger.w("Wifi", "lost")

        assertThat(loggedMessages).containsExactly(
            Triple(AppLogger.Level.DEBUG, "Widget", "tapped"),
            Triple(AppLogger.Level.WARN, "Wifi", "lost"),
        )
        assertThat(recordedExceptions).isEmpty()
    }

    @Test
    fun `error records provided throwable and custom keys`() {
        AppLogger.setSinkForTest(fakeSink)
        val cause = IllegalStateException("socket closed")

        AppLogger.e(
            tag = "Service",
            message = "download failed",
            throwable = cause,
            keys = mapOf("attempt" to "3", "flow_source" to "widget"),
        )

        assertThat(recordedKeys).containsEntry("attempt", "3")
        assertThat(recordedKeys).containsEntry("flow_source", "widget")
        assertThat(loggedMessages).containsExactly(
            Triple(AppLogger.Level.ERROR, "Service", "download failed"),
        )
        assertThat(recordedExceptions).containsExactly(cause)
    }

    @Test
    fun `error creates ConnectionDiagnosticException when throwable is null`() {
        AppLogger.setSinkForTest(fakeSink)

        AppLogger.e(tag = "UseCase", message = "hard timeout")

        assertThat(recordedExceptions).hasSize(1)
        assertThat(recordedExceptions.first())
            .isInstanceOf(AppLogger.ConnectionDiagnosticException::class.java)
            .hasMessage("UseCase: hard timeout")
    }

    @Test
    fun `setKey overloads convert primitives to strings`() {
        AppLogger.setSinkForTest(fakeSink)

        AppLogger.setKey("stage", "JOINING_WIFI")
        AppLogger.setKey("attempt", 2)
        AppLogger.setKey("is_background", true)

        assertThat(recordedKeys).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "stage" to "JOINING_WIFI",
                "attempt" to "2",
                "is_background" to "true",
            )
        )
    }

    @Test
    fun `default sink safely no-ops when Firebase is not initialized in unit tests`() {
        AppLogger.setSinkForTest(null)

        AppLogger.d("Test", "breadcrumb")
        AppLogger.w("Test", "warning")
        AppLogger.setKey("key", "value")
        AppLogger.e("Test", "non-fatal")
    }
}
