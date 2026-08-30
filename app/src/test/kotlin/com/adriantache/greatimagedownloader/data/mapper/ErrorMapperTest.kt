package com.adriantache.greatimagedownloader.data.mapper

import com.adriantache.greatimagedownloader.data.api.error.CameraException
import com.adriantache.greatimagedownloader.data.storage.error.FilesStorageException
import com.adriantache.greatimagedownloader.data.storage.error.PreferencesStorageException
import com.adriantache.greatimagedownloader.data.storage.error.WifiStorageException
import com.adriantache.greatimagedownloader.domain.model.DomainError
import com.adriantache.greatimagedownloader.domain.model.DomainException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ErrorMapperTest {

    @Test
    fun `toDomainException maps CameraDisconnected correctly`() {
        val exception = CameraException.CameraDisconnected
        val domainException = exception.toDomainException()
        assertThat(domainException.domainError).isEqualTo(DomainError.CameraDisconnected)
    }

    @Test
    fun `toDomainException maps Camera NetworkError correctly`() {
        val cause = Exception("network")
        val exception = CameraException.NetworkError(cause)
        val domainException = exception.toDomainException()
        assertThat(domainException.domainError).isEqualTo(DomainError.NetworkError)
    }

    @Test
    fun `toDomainException maps Camera Unknown correctly`() {
        val exception = CameraException.Unknown("cam error")
        val domainException = exception.toDomainException()
        assertThat(domainException.domainError).isEqualTo(DomainError.Unknown("cam error"))
    }

    @Test
    fun `toDomainException maps StorageFull correctly`() {
        val exception = FilesStorageException.StorageFull
        val domainException = exception.toDomainException()
        assertThat(domainException.domainError).isEqualTo(DomainError.StorageFull)
    }

    @Test
    fun `toDomainException maps FileCreationError to NetworkError`() {
        val exception = FilesStorageException.FileCreationError
        val domainException = exception.toDomainException()
        assertThat(domainException.domainError).isEqualTo(DomainError.NetworkError)
    }

    @Test
    fun `toDomainException maps FilesStorageException Unknown correctly`() {
        val exception = FilesStorageException.Unknown(Exception("files error"))
        val domainException = exception.toDomainException()
        assertThat(domainException.domainError).isEqualTo(DomainError.Unknown("files error"))
    }

    @Test
    fun `toDomainException maps WifiStorageException Unknown correctly`() {
        val exception = WifiStorageException.Unknown(Exception("wifi error"))
        val domainException = exception.toDomainException()
        assertThat(domainException.domainError).isEqualTo(DomainError.Unknown("wifi error"))
    }

    @Test
    fun `toDomainException maps PreferencesStorageException SerializationError correctly`() {
        val exception = PreferencesStorageException.SerializationError(Exception("json error"))
        val domainException = exception.toDomainException()
        assertThat(domainException.domainError).isEqualTo(DomainError.Unknown("Serialization error: json error"))
    }

    @Test
    fun `toDomainException maps PreferencesStorageException Unknown correctly`() {
        val exception = PreferencesStorageException.Unknown(Exception("prefs error"))
        val domainException = exception.toDomainException()
        assertThat(domainException.domainError).isEqualTo(DomainError.Unknown("prefs error"))
    }

    @Test
    fun `toDomainException returns same exception if already DomainException`() {
        val exception = DomainException(DomainError.CameraDisconnected)
        val domainException = exception.toDomainException()
        assertThat(domainException).isSameAs(exception)
    }

    @Test
    fun `toDomainException maps unknown exception to Unknown`() {
        val exception = Exception("something went wrong")
        val domainException = exception.toDomainException()
        assertThat(domainException.domainError).isEqualTo(DomainError.Unknown("something went wrong"))
    }

    @Test
    fun `mapDomainError maps success correctly`() {
        val result = Result.success("success")
        val mappedResult = result.mapDomainError()
        assertThat(mappedResult.isSuccess).isTrue
        assertThat(mappedResult.getOrNull()).isEqualTo("success")
    }

    @Test
    fun `mapDomainError maps failure correctly`() {
        val result = Result.failure<String>(CameraException.CameraDisconnected)
        val mappedResult = result.mapDomainError()
        assertThat(mappedResult.isFailure).isTrue
        val exception = mappedResult.exceptionOrNull() as DomainException
        assertThat(exception.domainError).isEqualTo(DomainError.CameraDisconnected)
    }
}
