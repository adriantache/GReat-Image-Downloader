package com.adriantache.greatimagedownloader.platform.ble

import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.adriantache.greatimagedownloader.domain.ble.ConnectionStage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
@OptIn(ExperimentalCoroutinesApi::class)
class BleUtilImplTest {
    private lateinit var context: Context
    private lateinit var bleUtil: BleUtilImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        bleUtil = BleUtilImpl(context)
    }

    @Test
    fun `isLikelyRicohBleDevice matches GR and RICOH device names`() {
        assertThat(bleUtil.isLikelyRicohBleDevice("GR_2119E5")).isTrue
        assertThat(bleUtil.isLikelyRicohBleDevice("RICOH GR III")).isTrue
        assertThat(bleUtil.isLikelyRicohBleDevice("PENTAX K-3")).isTrue
        assertThat(bleUtil.isLikelyRicohBleDevice("Pixel Watch")).isFalse
        assertThat(bleUtil.isLikelyRicohBleDevice(null)).isFalse
    }

    @Test
    fun `wakeAndEnableCameraWifi returns null when bluetooth adapter has no bonded camera`() = runTest {
        val result = bleUtil.wakeAndEnableCameraWifi()
        assertThat(result).isNull()
    }

    @Test
    fun `wakeAndEnableCameraWifi emits CONNECTING_BLE stage to onProgress`() = runTest {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        shadowOf(bluetoothManager.adapter).setEnabled(true)

        val stages = mutableListOf<ConnectionStage>()
        bleUtil.wakeAndEnableCameraWifi { stage ->
            stages.add(stage)
        }
        assertThat(stages).contains(ConnectionStage.CONNECTING_BLE)
    }
}
