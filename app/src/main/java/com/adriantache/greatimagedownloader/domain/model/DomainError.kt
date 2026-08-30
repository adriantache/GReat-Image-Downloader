package com.adriantache.greatimagedownloader.domain.model

sealed interface DomainError {
    data object CameraDisconnected : DomainError
    data object StorageFull : DomainError
    data object NetworkError : DomainError
    data class Unknown(val message: String?) : DomainError
}

class DomainException(val domainError: DomainError) : Exception()

