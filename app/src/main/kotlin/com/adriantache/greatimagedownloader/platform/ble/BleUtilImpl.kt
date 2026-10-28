@file:SuppressLint("MissingPermission")

package com.adriantache.greatimagedownloader.platform.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.adriantache.greatimagedownloader.domain.ble.BleUtil
import com.adriantache.greatimagedownloader.domain.ui.model.WifiDetails
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds

/**
 * Implements Ricoh GR III / GR IIIx Bluetooth Low Energy (BLE) wake-up and WLAN AP control.
 *
 * GATT UUIDs based on community reverse-engineering (`dm-zharov/ricoh-gr-bluetooth-api`):
 * - Camera Service: 4B445988-CAA0-4DD3-941D-37B4F52ACA86
 *   - Camera Power: B58CE84C-0666-4DE9-BEC8-2D27B27B3211 (sint8: 0 = Off, 1 = On, 2 = Sleep)
 * - WLAN Control Service: F37F568F-9071-445D-A938-5441F2E82399
 *   - Network Type: 9111CDD0-9F01-45C4-A2D4-E09E8FB0424D (sint8: 0 = OFF, 1 = AP mode)
 *   - SSID: 90638E5A-E77D-409D-B550-78F7E1CA5AB4 (utf8s)
 *   - Passphrase: 0F38279C-FE9E-461B-8596-81287E8C9A81 (utf8s)
 */
class BleUtilImpl(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BleUtil {
    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    override suspend fun wakeAndEnableCameraWifi(): WifiDetails? = withContext(dispatcher) {
        if (!hasBluetoothConnectPermission()) return@withContext null

        val adapter: BluetoothAdapter = bluetoothManager?.adapter ?: return@withContext null
        if (!adapter.isEnabled) return@withContext null

        val cameraDevice = runCatching {
            adapter.bondedDevices?.firstOrNull { isLikelyRicohBleDevice(it.name) }
        }.getOrNull() ?: return@withContext null

        withTimeoutOrNull(6.seconds) {
            connectAndEnableAp(cameraDevice)
        }
    }

    internal fun isLikelyRicohBleDevice(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        return name.startsWith("GR_", ignoreCase = true) ||
            name.startsWith("RICOH", ignoreCase = true) ||
            name.startsWith("PENTAX", ignoreCase = true) ||
            name.contains("ricoh", ignoreCase = true)
    }

    internal fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun connectAndEnableAp(device: BluetoothDevice): WifiDetails? =
        suspendCancellableCoroutine { continuation ->
            var gattRef: BluetoothGatt? = null
            var discoveredSsid: String? = null
            var discoveredPassphrase: String? = null

            val operationQueue = ArrayDeque<GattAction>()

            fun buildWifiDetails(): WifiDetails? {
                val ssid = discoveredSsid
                val pass = discoveredPassphrase
                return if (!ssid.isNullOrBlank() && !pass.isNullOrBlank()) {
                    WifiDetails(ssid = ssid, password = pass, bssid = null)
                } else {
                    null
                }
            }

            fun processNext(gatt: BluetoothGatt) {
                val next = operationQueue.removeFirstOrNull()
                if (next == null) {
                    runCatching { gatt.disconnect() }
                    runCatching { gatt.close() }
                    if (continuation.isActive) {
                        continuation.resume(buildWifiDetails())
                    }
                    return
                }

                val started = when (next) {
                    is GattAction.Read -> gatt.readCharacteristic(next.characteristic)
                    is GattAction.Write -> {
                        next.characteristic.value = next.payload
                        gatt.writeCharacteristic(next.characteristic)
                    }
                }
                if (!started) {
                    processNext(gatt)
                }
            }

            val callback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        runCatching { gatt.close() }
                        if (continuation.isActive) {
                            continuation.resume(buildWifiDetails())
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        processNext(gatt)
                        return
                    }

                    val cameraService = gatt.getService(CAMERA_SERVICE_UUID)
                    val powerChar = cameraService?.getCharacteristic(CAMERA_POWER_CHAR_UUID)
                    if (powerChar != null) {
                        operationQueue.add(GattAction.Write(powerChar, byteArrayOf(POWER_ON)))
                    }

                    val wlanService = gatt.getService(WLAN_SERVICE_UUID)
                    val ssidChar = wlanService?.getCharacteristic(WLAN_SSID_CHAR_UUID)
                    val passChar = wlanService?.getCharacteristic(WLAN_PASSPHRASE_CHAR_UUID)
                    val netTypeChar = wlanService?.getCharacteristic(WLAN_NETWORK_TYPE_CHAR_UUID)

                    if (ssidChar != null) operationQueue.add(GattAction.Read(ssidChar))
                    if (passChar != null) operationQueue.add(GattAction.Read(passChar))
                    if (netTypeChar != null) {
                        operationQueue.add(GattAction.Write(netTypeChar, byteArrayOf(NETWORK_TYPE_AP)))
                    }

                    processNext(gatt)
                }

                @Deprecated("Deprecated in Java")
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int,
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        val value = characteristic.value?.toString(Charsets.UTF_8)?.trimEnd('\u0000')?.trim()
                        when (characteristic.uuid) {
                            WLAN_SSID_CHAR_UUID -> discoveredSsid = value
                            WLAN_PASSPHRASE_CHAR_UUID -> discoveredPassphrase = value
                        }
                    }
                    processNext(gatt)
                }

                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int,
                ) {
                    processNext(gatt)
                }
            }

            gattRef = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)

            continuation.invokeOnCancellation {
                runCatching { gattRef?.disconnect() }
                runCatching { gattRef?.close() }
            }
        }

    private sealed interface GattAction {
        data class Read(val characteristic: BluetoothGattCharacteristic) : GattAction
        data class Write(val characteristic: BluetoothGattCharacteristic, val payload: ByteArray) : GattAction
    }

    companion object {
        val CAMERA_SERVICE_UUID: UUID = UUID.fromString("4B445988-CAA0-4DD3-941D-37B4F52ACA86")
        val CAMERA_POWER_CHAR_UUID: UUID = UUID.fromString("B58CE84C-0666-4DE9-BEC8-2D27B27B3211")

        val WLAN_SERVICE_UUID: UUID = UUID.fromString("F37F568F-9071-445D-A938-5441F2E82399")
        val WLAN_NETWORK_TYPE_CHAR_UUID: UUID = UUID.fromString("9111CDD0-9F01-45C4-A2D4-E09E8FB0424D")
        val WLAN_SSID_CHAR_UUID: UUID = UUID.fromString("90638E5A-E77D-409D-B550-78F7E1CA5AB4")
        val WLAN_PASSPHRASE_CHAR_UUID: UUID = UUID.fromString("0F38279C-FE9E-461B-8596-81287E8C9A81")

        private const val POWER_ON: Byte = 1
        private const val NETWORK_TYPE_AP: Byte = 1
    }
}
