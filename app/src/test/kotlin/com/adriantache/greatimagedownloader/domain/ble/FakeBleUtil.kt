package com.adriantache.greatimagedownloader.domain.ble

import com.adriantache.greatimagedownloader.domain.ui.model.WifiDetails

class FakeBleUtil : BleUtil {
    var wakeAndEnableResult: WifiDetails? = null
    var wakeCalledCount = 0
    var disconnectCalledCount = 0

    override suspend fun wakeAndEnableCameraWifi(): WifiDetails? {
        wakeCalledCount++
        return wakeAndEnableResult
    }

    override fun disconnect() {
        disconnectCalledCount++
    }
}
