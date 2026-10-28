@file:SuppressLint("MissingPermission")

package com.adriantache.greatimagedownloader.platform.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.adriantache.greatimagedownloader.domain.ble.BleUtil
import com.adriantache.greatimagedownloader.domain.ui.model.WifiDetails
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Implements Ricoh GR III / GR IIIx Bluetooth Low Energy (BLE) wake-up and WLAN AP control.
 *
 * GATT UUIDs based on reverse-engineered Ricoh protocol (`def_ble.decrypted.yaml` & hardware verification):
 * - Bluetooth Control Service: 0F291746-0C80-4726-87A7-3C501FD3B4B6
 *   - BLE Enable Condition: D8676C92-DC4E-4D9E-ACCE-B9E251DDCC0C (sint8: 0 = Disable, 1 = Enable anytime, 2 = Enable power on)
 *   - Paired Device Name: FE3A32F8-A189-42DE-A391-BC81AE4DAA76 (utf8s, Write)
 * - Camera Service: 4B445988-CAA0-4DD3-941D-37B4F52ACA86
 *   - Camera Service Notification: FAA0AEAF-1654-4842-A139-F4E1C1E722AC (Read, Notify)
 *   - Camera Power: B58CE84C-0666-4DE9-BEC8-2D27B27B3211 (sint8: 0 = Off, 1 = On, 2 = Sleep)
 *   - Operation Mode: 1452335A-EC7F-4877-B8AB-0F72E18BB295 (sint8: 0 = Capture, 1 = Playback, 2 = BLE Startup, 3 = Other, 4 = PowerOffTransfer)
 * - WLAN Control Service: F37F568F-9071-445D-A938-5441F2E82399
 *   - Network Type: 9111CDD0-9F01-45C4-A2D4-E09E8FB0424D (sint8: 0 = OFF, 1 = AP mode)
 *   - SSID: 90638E5A-E77D-409D-B550-78F7E1CA5AB4 (utf8s)
 *   - Passphrase: 0F38279C-FE9E-461B-8596-81287E8C9A81 (utf8s)
 *
 * Hardware verification note: Writing Network Type = 1 (AP) on Ricoh GR III is rejected with 0x80 while the
 * camera is in BLE_STARTUP (0x02) or POWER_OFF_TRANSFER (0x04). Once Operation Mode transitions to CAPTURE (0x00),
 * PLAYBACK (0x01), or OTHER (0x03), writing Network Type = 1 succeeds and raises the camera's Wi-Fi AP.
 */
class BleUtilImpl(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BleUtil {
    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var activeGatt: BluetoothGatt? = null

    override suspend fun wakeAndEnableCameraWifi(): WifiDetails? = withContext(dispatcher) {
        if (!hasBluetoothConnectPermission()) return@withContext null

        val adapter: BluetoothAdapter = bluetoothManager?.adapter ?: return@withContext null
        if (!adapter.isEnabled) return@withContext null

        disconnect()

        val bondedCandidates = runCatching {
            adapter.bondedDevices?.filter { isLikelyRicohBleDevice(it.name) }.orEmpty()
        }.getOrDefault(emptyList())

        var details: WifiDetails? = null

        for (bonded in bondedCandidates) {
            Log.d(TAG, "Attempting connection to bonded camera: ${bonded.name} (${bonded.address})")
            details = withTimeoutOrNull(12.seconds) {
                connectAndEnableAp(bonded)
            }
            if (details != null || activeGatt != null) break
        }

        if (details == null && activeGatt == null) {
            Log.d(TAG, "No bonded connection established, starting active BLE scan...")
            val scannedDevice = scanForRicohBleDevice(
                adapter = adapter,
                bondedAddresses = bondedCandidates.mapNotNull { it.address }.toSet(),
            )
            if (scannedDevice != null) {
                Log.d(TAG, "Scanned Ricoh camera found: ${scannedDevice.name} (${scannedDevice.address})")
                details = withTimeoutOrNull(12.seconds) {
                    connectAndEnableAp(scannedDevice)
                }
            }
        }

        // Give the camera Wi-Fi AP 2.5 seconds to boot and broadcast beacons
        // while holding the BLE GATT link open so the camera does not abort Wi-Fi startup.
        if (details != null || activeGatt != null) {
            Log.d(TAG, "Waiting 2500ms for camera Wi-Fi AP beacons to start broadcasting...")
            delay(2500.milliseconds)
        }

        details
    }

    override fun disconnect() {
        val gatt = activeGatt
        activeGatt = null
        if (gatt != null) {
            Log.d(TAG, "Closing active BLE GATT session")
            runCatching { gatt.disconnect() }
            runCatching { gatt.close() }
        }
    }

    private suspend fun scanForRicohBleDevice(
        adapter: BluetoothAdapter,
        bondedAddresses: Set<String> = emptySet(),
    ): BluetoothDevice? {
        if (!hasBluetoothScanPermission()) return null
        val scanner = runCatching { adapter.bluetoothLeScanner }.getOrNull() ?: return null

        return withTimeoutOrNull(3000.milliseconds) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        val device = result?.device ?: return
                        val scanRecord = result.scanRecord
                        val name = scanRecord?.deviceName ?: device.name
                        val serviceUuids = scanRecord?.serviceUuids?.map { it.uuid }.orEmpty()
                        val matchesService = serviceUuids.any { it in RICOH_SERVICE_UUIDS }
                        val matchesManufacturer = scanRecord?.getManufacturerSpecificData(RICOH_COMPANY_ID) != null
                        val matchesBondedAddress = device.address != null && device.address in bondedAddresses

                        if (matchesService || matchesManufacturer || matchesBondedAddress || isLikelyRicohBleDevice(name)) {
                            runCatching { scanner.stopScan(this) }
                            if (continuation.isActive) {
                                continuation.resume(device)
                            }
                        }
                    }

                    override fun onScanFailed(errorCode: Int) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }

                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()

                runCatching { scanner.startScan(null, settings, callback) }
                    .onFailure {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }

                continuation.invokeOnCancellation {
                    runCatching { scanner.stopScan(callback) }
                }
            }
        }
    }

    internal fun isLikelyRicohBleDevice(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        return name.equals("GR", ignoreCase = true) ||
            name.startsWith("GR_", ignoreCase = true) ||
            name.startsWith("GR ", ignoreCase = true) ||
            name.startsWith("GRIII", ignoreCase = true) ||
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

    internal fun hasBluetoothScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
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
            var opModePollRetries = 0
            var netTypeWriteRetries = 0

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
                    activeGatt = gatt
                    Log.d(TAG, "All GATT setup operations finished. Keeping BLE connection alive for Wi-Fi handoff.")
                    if (continuation.isActive) {
                        continuation.resume(buildWifiDetails())
                    }
                    return
                }

                when (next) {
                    is GattAction.Delay -> {
                        mainHandler.postDelayed({ processNext(gatt) }, next.delayMs)
                    }

                    is GattAction.EnableNotification -> {
                        gatt.setCharacteristicNotification(next.characteristic, true)
                        val cccd = next.characteristic.getDescriptor(CCCD_UUID)
                        val started = if (cccd != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.writeDescriptor(
                                    cccd,
                                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE,
                                ) == BluetoothGatt.GATT_SUCCESS
                            } else {
                                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(cccd)
                            }
                        } else {
                            false
                        }
                        if (!started) {
                            processNext(gatt)
                        }
                    }

                    is GattAction.Read -> {
                        val started = gatt.readCharacteristic(next.characteristic)
                        if (!started) {
                            processNext(gatt)
                        }
                    }

                    is GattAction.Write -> {
                        val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            gatt.writeCharacteristic(
                                next.characteristic,
                                next.payload,
                                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                            ) == BluetoothGatt.GATT_SUCCESS
                        } else {
                            next.characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                            next.characteristic.value = next.payload
                            gatt.writeCharacteristic(next.characteristic)
                        }
                        if (!started) {
                            processNext(gatt)
                        }
                    }
                }
            }

            val callback = object : BluetoothGattCallback() {
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    Log.d(TAG, "GATT onConnectionStateChange status=$status newState=$newState")
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(TAG, "Connected to GATT server, discovering services...")
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(TAG, "Disconnected from GATT server")
                        if (activeGatt === gatt) {
                            activeGatt = null
                        }
                        runCatching { gatt.close() }
                        if (continuation.isActive) {
                            continuation.resume(buildWifiDetails())
                        }
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    Log.d(TAG, "onServicesDiscovered status=$status")
                    if (status != BluetoothGatt.GATT_SUCCESS) {
                        runCatching { gatt.disconnect() }
                        runCatching { gatt.close() }
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                        return
                    }

                    // 1. Bluetooth Control Service: register paired device name & enable BLE anytime
                    val btControlService = gatt.getService(BLUETOOTH_CONTROL_SERVICE_UUID)
                    val pairedNameChar = btControlService?.getCharacteristic(PAIRED_DEVICE_NAME_CHAR_UUID)
                    if (pairedNameChar != null) {
                        val deviceNameBytes = Build.MODEL.orEmpty().ifBlank { "Android" }
                            .take(20)
                            .toByteArray(Charsets.UTF_8)
                        operationQueue.add(GattAction.Write(pairedNameChar, deviceNameBytes))
                    }
                    val bleEnableChar = btControlService?.getCharacteristic(BLE_ENABLE_CONDITION_CHAR_UUID)
                    if (bleEnableChar != null) {
                        operationQueue.add(GattAction.Write(bleEnableChar, byteArrayOf(BLE_ENABLE_ANYTIME)))
                    }

                    // 2. Camera Service: wake camera by writing Power = 1
                    val cameraService = gatt.getService(CAMERA_SERVICE_UUID)
                    val powerChar = cameraService?.getCharacteristic(CAMERA_POWER_CHAR_UUID)
                    if (powerChar != null) {
                        operationQueue.add(GattAction.Write(powerChar, byteArrayOf(POWER_ON)))
                    }

                    // 3. WLAN Service: read SSID and Passphrase
                    val wlanService = gatt.getService(WLAN_SERVICE_UUID)
                    val ssidChar = wlanService?.getCharacteristic(WLAN_SSID_CHAR_UUID)
                    val passChar = wlanService?.getCharacteristic(WLAN_PASSPHRASE_CHAR_UUID)
                    if (ssidChar != null) operationQueue.add(GattAction.Read(ssidChar))
                    if (passChar != null) operationQueue.add(GattAction.Read(passChar))

                    // 4. Poll Operation Mode until camera leaves BLE_STARTUP / POWER_OFF_TRANSFER
                    val opModeChar = cameraService?.getCharacteristic(OPERATION_MODE_CHAR_UUID)
                    if (opModeChar != null) {
                        operationQueue.add(GattAction.Delay(500L))
                        operationQueue.add(GattAction.Read(opModeChar))
                    } else {
                        // Fallback if Operation Mode characteristic not exposed: wait 3s then write Network Type
                        val netTypeChar = wlanService?.getCharacteristic(WLAN_NETWORK_TYPE_CHAR_UUID)
                        if (netTypeChar != null) {
                            operationQueue.add(GattAction.Delay(3000L))
                            operationQueue.add(GattAction.Write(netTypeChar, byteArrayOf(NETWORK_TYPE_AP)))
                        }
                    }

                    processNext(gatt)
                }

                override fun onDescriptorWrite(
                    gatt: BluetoothGatt,
                    descriptor: BluetoothGattDescriptor,
                    status: Int,
                ) {
                    processNext(gatt)
                }

                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    value: ByteArray,
                    status: Int,
                ) {
                    handleCharacteristicRead(gatt, characteristic.uuid, value, status)
                }

                @Deprecated("Deprecated in Java")
                override fun onCharacteristicRead(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int,
                ) {
                    val value = characteristic.value ?: ByteArray(0)
                    handleCharacteristicRead(gatt, characteristic.uuid, value, status)
                }

                private fun handleCharacteristicRead(
                    gatt: BluetoothGatt,
                    uuid: UUID,
                    value: ByteArray,
                    status: Int,
                ) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        when (uuid) {
                            WLAN_SSID_CHAR_UUID -> {
                                discoveredSsid = value.toString(Charsets.UTF_8).trimEnd('\u0000').trim()
                                Log.d(TAG, "Read WLAN SSID from camera BLE: '$discoveredSsid'")
                            }

                            WLAN_PASSPHRASE_CHAR_UUID -> {
                                discoveredPassphrase = value.toString(Charsets.UTF_8).trimEnd('\u0000').trim()
                                Log.d(TAG, "Read WLAN Passphrase from camera BLE")
                            }

                            OPERATION_MODE_CHAR_UUID -> {
                                val mode = if (value.isNotEmpty()) value[0].toInt() else -1
                                Log.d(TAG, "Read Operation Mode from camera BLE: 0x%02X".format(mode))

                                val isStandby = mode == OPERATION_MODE_BLE_STARTUP.toInt() ||
                                    mode == OPERATION_MODE_POWER_OFF_TRANSFER.toInt()

                                if (isStandby && opModePollRetries < 10) {
                                    opModePollRetries++
                                    Log.d(TAG, "Camera is still in standby/boot mode (0x%02X). Waiting and retrying poll $opModePollRetries/10...".format(mode))
                                    operationQueue.addFirst(GattAction.Read(gatt.getService(CAMERA_SERVICE_UUID)!!.getCharacteristic(OPERATION_MODE_CHAR_UUID)))
                                    operationQueue.addFirst(GattAction.Delay(500L))
                                } else {
                                    Log.d(TAG, "Camera is ready to raise Wi-Fi AP (Operation Mode: 0x%02X). Queueing Network Type = 1 (AP mode).".format(mode))
                                    val wlanService = gatt.getService(WLAN_SERVICE_UUID)
                                    val netTypeChar = wlanService?.getCharacteristic(WLAN_NETWORK_TYPE_CHAR_UUID)
                                    if (netTypeChar != null) {
                                        operationQueue.addFirst(GattAction.Write(netTypeChar, byteArrayOf(NETWORK_TYPE_AP)))
                                    }
                                }
                            }
                        }
                    }
                    processNext(gatt)
                }

                override fun onCharacteristicWrite(
                    gatt: BluetoothGatt,
                    characteristic: BluetoothGattCharacteristic,
                    status: Int,
                ) {
                    Log.d(TAG, "onCharacteristicWrite uuid=${characteristic.uuid} status=$status")
                    if (characteristic.uuid == WLAN_NETWORK_TYPE_CHAR_UUID) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            Log.d(TAG, "BLE WLAN activation: Camera accepted Network Type = 1 (AP mode)! Wi-Fi is spinning up.")
                        } else if (netTypeWriteRetries < 3) {
                            netTypeWriteRetries++
                            Log.d(TAG, "Network Type write rejected with status=$status; retrying write $netTypeWriteRetries/3 after 600ms delay...")
                            operationQueue.addFirst(GattAction.Write(characteristic, byteArrayOf(NETWORK_TYPE_AP)))
                            operationQueue.addFirst(GattAction.Delay(600L))
                        }
                    }
                    processNext(gatt)
                }
            }

            gattRef = device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)

            continuation.invokeOnCancellation {
                if (activeGatt !== gattRef) {
                    runCatching { gattRef?.disconnect() }
                    runCatching { gattRef?.close() }
                }
            }
        }

    private sealed interface GattAction {
        data class Delay(val delayMs: Long) : GattAction
        data class EnableNotification(val characteristic: BluetoothGattCharacteristic) : GattAction
        data class Read(val characteristic: BluetoothGattCharacteristic) : GattAction
        data class Write(val characteristic: BluetoothGattCharacteristic, val payload: ByteArray) : GattAction
    }

    companion object {
        private const val TAG = "BleUtil"
        const val RICOH_COMPANY_ID: Int = 0x065F

        val INFO_SERVICE_UUID: UUID = UUID.fromString("9A5ED1C5-74CC-4C50-B5B6-66A48E7CCFF1")
        val SHOOTING_SERVICE_UUID: UUID = UUID.fromString("9F00F387-8345-4BBC-8B92-B87B52E3091A")

        val BLUETOOTH_CONTROL_SERVICE_UUID: UUID = UUID.fromString("0F291746-0C80-4726-87A7-3C501FD3B4B6")
        val BLE_ENABLE_CONDITION_CHAR_UUID: UUID = UUID.fromString("D8676C92-DC4E-4D9E-ACCE-B9E251DDCC0C")
        val PAIRED_DEVICE_NAME_CHAR_UUID: UUID = UUID.fromString("FE3A32F8-A189-42DE-A391-BC81AE4DAA76")

        val CAMERA_SERVICE_UUID: UUID = UUID.fromString("4B445988-CAA0-4DD3-941D-37B4F52ACA86")
        val CAMERA_SERVICE_NOTIFICATION_CHAR_UUID: UUID = UUID.fromString("FAA0AEAF-1654-4842-A139-F4E1C1E722AC")
        val CAMERA_POWER_CHAR_UUID: UUID = UUID.fromString("B58CE84C-0666-4DE9-BEC8-2D27B27B3211")
        val OPERATION_MODE_CHAR_UUID: UUID = UUID.fromString("1452335A-EC7F-4877-B8AB-0F72E18BB295")

        val WLAN_SERVICE_UUID: UUID = UUID.fromString("F37F568F-9071-445D-A938-5441F2E82399")
        val WLAN_NETWORK_TYPE_CHAR_UUID: UUID = UUID.fromString("9111CDD0-9F01-45C4-A2D4-E09E8FB0424D")
        val WLAN_SSID_CHAR_UUID: UUID = UUID.fromString("90638E5A-E77D-409D-B550-78F7E1CA5AB4")
        val WLAN_PASSPHRASE_CHAR_UUID: UUID = UUID.fromString("0F38279C-FE9E-461B-8596-81287E8C9A81")

        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        val RICOH_SERVICE_UUIDS: Set<UUID> = setOf(
            INFO_SERVICE_UUID,
            CAMERA_SERVICE_UUID,
            SHOOTING_SERVICE_UUID,
            BLUETOOTH_CONTROL_SERVICE_UUID,
            WLAN_SERVICE_UUID,
        )

        private const val BLE_ENABLE_ANYTIME: Byte = 1
        private const val POWER_ON: Byte = 1
        private const val OPERATION_MODE_BLE_STARTUP: Byte = 2
        private const val OPERATION_MODE_POWER_OFF_TRANSFER: Byte = 4
        private const val NETWORK_TYPE_AP: Byte = 1
    }
}
