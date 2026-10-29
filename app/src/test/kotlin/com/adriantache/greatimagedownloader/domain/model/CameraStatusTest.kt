package com.adriantache.greatimagedownloader.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CameraStatusTest {

    @Test
    fun `formattedBattery returns appropriate bar tiers`() {
        assertThat(CameraStatus(batteryLevel = 100).formattedBattery).isEqualTo("Full (3/3 bars)")
        assertThat(CameraStatus(batteryLevel = 80).formattedBattery).isEqualTo("Full (3/3 bars)")
        assertThat(CameraStatus(batteryLevel = 79).formattedBattery).isEqualTo("Medium (2/3 bars)")
        assertThat(CameraStatus(batteryLevel = 50).formattedBattery).isEqualTo("Medium (2/3 bars)")
        assertThat(CameraStatus(batteryLevel = 49).formattedBattery).isEqualTo("Low (1/3 bar)")
        assertThat(CameraStatus(batteryLevel = 21).formattedBattery).isEqualTo("Low (1/3 bar)")
        assertThat(CameraStatus(batteryLevel = 20).formattedBattery).isEqualTo("Critical (20%)")
        assertThat(CameraStatus(batteryLevel = 5).formattedBattery).isEqualTo("Critical (5%)")
        assertThat(CameraStatus(batteryLevel = null).formattedBattery).isNull()
    }
}
