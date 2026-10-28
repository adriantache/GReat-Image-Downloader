package com.adriantache.greatimagedownloader.domain.ble

import com.adriantache.greatimagedownloader.domain.ui.model.WifiDetails

enum class ConnectionStage {
    CONNECTING_BLE,
    CAMERA_CONNECTED,
    STARTING_WIFI,
    JOINING_WIFI,
}

interface BleUtil {
    /**
     * Attempts to connect to a paired Ricoh GR camera over BLE, wake it if needed,
     * enable its Wi-Fi Access Point mode, and read its Wi-Fi SSID and passphrase.
     */
    suspend fun wakeAndEnableCameraWifi(
        onProgress: (ConnectionStage) -> Unit = {},
    ): WifiDetails?

    /**
     * Closes any active BLE GATT connection held during Wi-Fi handoff/transfer.
     */
    fun disconnect() {}
}
