package com.adriantache.greatimagedownloader.data.mapper

import com.adriantache.greatimagedownloader.data.api.error.CameraException
import com.adriantache.greatimagedownloader.data.storage.error.FilesStorageException
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.model.DomainException

fun Throwable.toDomainException(): DomainException {
    val domainError = when (this) {
        is CameraException.CameraDisconnected -> DomainError.CameraDisconnected
        is CameraException.NetworkError -> DomainError.NetworkError
        is FilesStorageException.StorageFull -> DomainError.StorageFull
        is FilesStorageException.FileCreationError -> DomainError.NetworkError // Or maybe a new DomainError?
        is DomainException -> return this
        else -> DomainError.Unknown(message)
    }

    return DomainException(domainError)
}

/**
 * Maps the failure exception from a [Result] to a [DomainException].
 */
fun <T> Result<T>.mapDomainError(): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = { Result.failure(it.toDomainException()) }
)
