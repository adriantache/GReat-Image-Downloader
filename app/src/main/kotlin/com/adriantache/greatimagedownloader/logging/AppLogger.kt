package com.adriantache.greatimagedownloader.logging

import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Centralized diagnostic logger that mirrors connection/transfer breadcrumbs and non-fatal errors
 * to both Logcat and Firebase Crashlytics (when Firebase is configured via google-services.json).
 */
object AppLogger {
    interface LogSink {
        fun log(level: Level, tag: String, message: String, throwable: Throwable? = null)
        fun setKey(key: String, value: String)
        fun recordException(throwable: Throwable)
    }

    enum class Level {
        DEBUG,
        WARN,
        ERROR,
    }

    class ConnectionDiagnosticException(
        message: String,
        cause: Throwable? = null,
    ) : Exception(message, cause)

    private val defaultSink = object : LogSink {
        private fun crashlytics(): FirebaseCrashlytics? =
            runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()

        override fun log(level: Level, tag: String, message: String, throwable: Throwable?) {
            val prefix = when (level) {
                Level.DEBUG -> "D"
                Level.WARN -> "W"
                Level.ERROR -> "E"
            }

            runCatching {
                when (level) {
                    Level.DEBUG -> Log.d(tag, message)
                    Level.WARN -> Log.w(tag, message)
                    Level.ERROR -> if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
                }
            }

            crashlytics()?.log("$prefix/$tag: $message")
        }

        override fun setKey(key: String, value: String) {
            crashlytics()?.setCustomKey(key, value)
        }

        override fun recordException(throwable: Throwable) {
            crashlytics()?.recordException(throwable)
        }
    }

    @Volatile
    private var sink: LogSink = defaultSink

    fun d(tag: String, message: String) {
        sink.log(Level.DEBUG, tag, message)
    }

    fun w(tag: String, message: String) {
        sink.log(Level.WARN, tag, message)
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        keys: Map<String, String> = emptyMap(),
    ) {
        keys.forEach { (k, v) -> sink.setKey(k, v) }
        sink.log(Level.ERROR, tag, message, throwable)
        sink.recordException(throwable ?: ConnectionDiagnosticException("$tag: $message"))
    }

    fun setKey(key: String, value: String) {
        sink.setKey(key, value)
    }

    fun setKey(key: String, value: Int) {
        sink.setKey(key, value.toString())
    }

    fun setKey(key: String, value: Boolean) {
        sink.setKey(key, value.toString())
    }

    @VisibleForTesting
    internal fun setSinkForTest(testSink: LogSink?) {
        sink = testSink ?: defaultSink
    }
}
