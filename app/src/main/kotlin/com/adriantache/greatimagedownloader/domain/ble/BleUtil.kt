package com.adriantache.greatimagedownloader.domain.ble

import com.adriantache.greatimagedownloader.domain.ui.model.WifiDetails

interface BleUtil {
    /**
     * Attempts to connect to a paired Ricoh GR camera over BLE, wake it if needed,
     * enable its Wi-Fi Access Point mode, and read its Wi-Fi SSID and passphrase.
     */
    suspend fun wakeAndEnableCameraWifi(): WifiDetails?
}
