package com.example.greatimagedownloader.data.utils

import com.example.greatimagedownloader.data.utils.speedCalculator.SpeedCalculatorImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class SpeedCalculatorImplTest {
    private val speedCalculator = SpeedCalculatorImpl()

    @Before
    fun setUp() {
        speedCalculator.clearData()
    }

    @Test
    fun getAverageSpeed() {
        repeat(5) {
            speedCalculator.registerData(1000, it * 1000L)
        }

        val result = speedCalculator.getAverageSpeedKbps(5000L)

        assertThat(result).isEqualTo(0.9765625)
    }

    @Test
    fun `getAverageSpeed, big dataset`() {
        repeat(20000) {
            speedCalculator.registerData(1024 * 1000, it * 100L)
        }

        val result = speedCalculator.getAverageSpeedKbps(5000L)

        assertThat(result).isEqualTo(10000.0)
    }
}
