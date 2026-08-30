package com.adriantache.greatimagedownloader.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import com.adriantache.greatimagedownloader.domain.data.model.WifiDetails as WifiDetailsData

class WifiDetailsEntityTest {
    @Test
    fun `isValid returns false when ssid is null`() {
        val ssid: String? = null
        val password = "password"
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        assertThat(wifiDetailsEntity.isValid).isFalse
    }

    @Test
    fun `isValid returns false when password is null`() {
        val ssid = "ssid"
        val password: String? = null
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        assertThat(wifiDetailsEntity.isValid).isFalse
    }

    @Test
    fun `isValid returns false when ssid is empty`() {
        val ssid = ""
        val password = "password"
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        assertThat(wifiDetailsEntity.isValid).isFalse
    }

    @Test
    fun `isValid returns false when password is empty`() {
        val ssid = "ssid"
        val password = ""
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        assertThat(wifiDetailsEntity.isValid).isFalse
    }

    @Test
    fun `isValid returns false when ssid is blank`() {
        val ssid = " "
        val password = "password"
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        assertThat(wifiDetailsEntity.isValid).isFalse
    }

    @Test
    fun `isValid returns false when password is blank`() {
        val ssid = "ssid"
        val password = " "
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        assertThat(wifiDetailsEntity.isValid).isFalse
    }

    @Test
    fun `isValid returns false when password is too short`() {
        val ssid = "ssid"
        val password = "short"
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        assertThat(wifiDetailsEntity.isValid).isFalse
    }

    @Test
    fun `isValid returns true when ssid and password are not null or blank and password is long enough`() {
        val ssid = "ssid"
        val password = "longpassword"
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        assertThat(wifiDetailsEntity.isValid).isTrue
    }

    @Test
    fun `isValid returns false when ssid is blank and password is valid`() {
        val ssid = " "
        val password = "longpassword"
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        assertThat(wifiDetailsEntity.isValid).isFalse
    }

    @Test
    fun `isValid returns false when password is blank and ssid is valid`() {
        val ssid = "ssid"
        val password = " "
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        assertThat(wifiDetailsEntity.isValid).isFalse
    }

    @Test
    fun `toData returns a valid WifiDetailsData object when wifi details entity is valid`() {
        val ssid = "ssid"
        val password = "password"
        val wifiDetailsEntity = WifiDetailsEntity(ssid, password, null)
        val expectedWifiDetailsData = WifiDetailsData(ssid = ssid, password = password, bssid = null)
        assertThat(wifiDetailsEntity.toData()).isEqualTo(expectedWifiDetailsData)
    }
}
