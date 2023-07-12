package com.example.greatimagedownloader.data.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import kotlin.random.Random

class ModeCalculatorKtTest {
    @Test
    fun mode() {
        val list = listOf(1, 2, 3, 3, 4, 5, 5, 5, 6, 7, 8, 9, 10)

        assertThat(list.mode()).isEqualTo(5)
    }

    @Test
    fun `mode, large dataset`() {
        val list = mutableListOf<Int>()

        repeat(10000) {
            list.add(Random.nextInt())
        }

        assertThat(list.mode()).isEqualTo(list.findMode())
    }

    private fun List<Int>.findMode(): Int {
        var maxValue = this[0]
        var maxCount = 0

        for (i in this.indices) {
            var count = 0

            for (j in this.indices) {
                if (this[j] == this[i]) count++
            }

            if (count > maxCount) {
                maxCount = count
                maxValue = this[i]
            }
        }

        return maxValue
    }
}
