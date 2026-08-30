package com.adriantache.greatimagedownloader.data.storage.error

sealed class FilesStorageException : Exception() {
    data object StorageFull : FilesStorageException()
    data object FileCreationError : FilesStorageException()
    data class Unknown(override val cause: Throwable) : FilesStorageException()
}

sealed class WifiStorageException : Exception() {
    data class Unknown(override val cause: Throwable) : WifiStorageException()
}

sealed class PreferencesStorageException : Exception() {
    data class SerializationError(override val cause: Throwable) : PreferencesStorageException()
    data class Unknown(override val cause: Throwable) : PreferencesStorageException()
}
