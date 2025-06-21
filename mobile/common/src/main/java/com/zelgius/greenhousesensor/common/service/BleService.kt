package com.zelgius.greenhousesensor.common.service

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Parcelable
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.getSystemService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.parcelize.Parcelize
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalStdlibApi::class)
class BleService(private val context: Context) {
    companion object {
        const val SCAN_PERIOD = 10000L
        const val DELAY_BETWEEN_REQUEST = 10L
    }

    lateinit var gattConfig: GattConfig

    private val adapter: BluetoothAdapter? = context.getSystemService<BluetoothManager>()?.adapter
    private val scanner = adapter?.bluetoothLeScanner

    private val _scanning = MutableStateFlow(false)
    val scanning = _scanning.asStateFlow()

    private var gatt: BluetoothGatt? = null

    private val callback = GattClientCallback()

    private var device: BluetoothDevice? = null

    private val _status = MutableStateFlow(BleState.Disconnected)
    val status = _status.asStateFlow()

    private val characteristics = mutableMapOf<String, BluetoothGattCharacteristic>()

    // Flow for characteristic changes (notifications/indications)
    private val _characteristicChangedFlow = MutableSharedFlow<GattInfo>(replay = 72)

    // Flow for characteristic read results
    private val _characteristicReadFlow = MutableSharedFlow<GattInfo>(replay = 0)

    // Flow for characteristic write results
    private val _characteristicWriteFlow = MutableSharedFlow<GattInfo>(replay = 0)

    // We'll use a suspend function for reads primarily, but a flow could be an option too
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScan() = callbackFlow {
        val scanCallback = object : ScanCallback() {
            @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val record = result.scanRecord
                trySend(
                    BleDevice(
                        address = result.device.address,
                        device = result.device,
                        name = record?.deviceName
                            ?: "<Unknown>" // should require Bluetooth connect permissions,
                    )
                )
            }
        }

        _scanning.value = true
        scanner?.startScan(scanCallback)

        delay(SCAN_PERIOD)

        _scanning.value = false
        scanner?.stopScan(scanCallback)
        awaitClose {
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScan() {
        _scanning.value = false
        scanner?.stopScan(object : ScanCallback() {})
    }


    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice, gattConfig: GattConfig) {
        this.gattConfig = gattConfig
        this.device = device
        _status.value = BleState.Connecting
        device.connectGatt(context, true, callback)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(address: String, gattConfig: GattConfig) {
        adapter?.getRemoteDevice(address)?.let {
            connect(it, gattConfig)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() {
        gatt?.disconnect()
    }

    /**
     * Reads the value of a specific characteristic.
     * This is a suspending function that returns the value directly.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun readCharacteristic(characteristicUuid: String): ByteArray  = delayedRequests{
        val gattInstance = gatt
        val characteristic = characteristics[characteristicUuid]

        if (gattInstance == null || characteristic == null) {
            throw IllegalStateException("GATT not connected or characteristic not found.")
        }

        if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_READ) == 0) {
            throw IllegalStateException("Characteristic $characteristicUuid is not readable.")
        }

        if (!gattInstance.readCharacteristic(characteristic)) {
            throw IllegalStateException("Failed to initiate characteristic read for $characteristicUuid.")
        }

         _characteristicReadFlow.collectCharacteristic(uid = characteristicUuid)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun writeCharacteristic(characteristicUuid: String, value: ByteArray): Boolean =
        delayedRequests {
            val characteristic = characteristics[characteristicUuid]
            characteristic?.let {
                gatt?.writeCharacteristic(it, value, WRITE_TYPE_NO_RESPONSE)
                _characteristicWriteFlow.collectWriteStatus()
            } ?: false
        }

    private inner class GattClientCallback : BluetoothGattCallback() {

        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val isSuccess = status == BluetoothGatt.GATT_SUCCESS
            val isConnected = newState == BluetoothProfile.STATE_CONNECTED
            // try to send a message to the other device as a test
            if (isSuccess && isConnected) {
                // discover services
                gatt.discoverServices()
            } else {
                _status.value = BleState.Disconnected
            }
        }


        override fun onServicesDiscovered(discoveredGatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt = discoveredGatt
                val service = discoveredGatt.getService(UUID.fromString(gattConfig.serviceUid))

                gattConfig.characteristicUids.forEach {
                    characteristics[it] = service.getCharacteristic(UUID.fromString(it))
                }
                _status.value = BleState.Connected
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {

            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(
                    "BleService",
                    "Characteristic write failed for ${characteristic?.uuid}, status: $status"
                )
                scope.launch {
                    _characteristicWriteFlow.emit(GattInfo.DataWriteFailed)
                }
            } else {
                Log.d("BleService", "Characteristic write success for ${characteristic?.uuid}")
                scope.launch {
                    _characteristicWriteFlow.emit(GattInfo.DataWriteSuccess)
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            _characteristicChangedFlow.tryEmit(
                GattInfo.CharacteristicData(
                    characteristic.uuid.toString(),
                    value
                )
            )
        }

        // For older versions, the onCharacteristicRead(gatt, characteristic, status) is used.
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                scope.launch {
                    _characteristicReadFlow.emit(
                        GattInfo.CharacteristicData(
                            characteristic.uuid.toString(),
                            value
                        )
                    )
                }
            } else {
                Log.e(
                    "BleService",
                    "Characteristic read failed for ${characteristic.uuid}, status: $status, value: ${value.decodeToString()}"
                )
            }
        }
    }


    private var lastRequestTime = 0L

    private val mutex = Mutex()
    internal suspend fun <T> delayedRequests(block: suspend () -> T): T = mutex.withLock {
        val delay = (System.currentTimeMillis() - lastRequestTime)
        if (delay < DELAY_BETWEEN_REQUEST) {
            delay(DELAY_BETWEEN_REQUEST - delay)
        }
        lastRequestTime = System.currentTimeMillis()

        return block()
    }

}

internal sealed interface GattInfo {
    class CharacteristicData(
        val uuid: String,
        val value: ByteArray,
    ) : GattInfo

    data object DataWriteFailed : GattInfo
    data object DataWriteSuccess : GattInfo
}

data class GattConfig(
    val serviceUid: String,
    val characteristicUids: List<String>,
)

enum class BleState {
    Disconnected, Connecting, Connected
}

@Parcelize
data class BleDevice(
    val address: String,
    val name: String,
    val device: BluetoothDevice? // need to be nullable because it not possible to create it
) : Parcelable

internal suspend fun Flow<GattInfo>.collectCharacteristic(uid: String): ByteArray =
    mapNotNull { it as? GattInfo.CharacteristicData }
        .filter { it.uuid == uid }
        .first()
        .value

internal suspend fun Flow<GattInfo>.collectWriteStatus(): Boolean = withTimeout(1.seconds) {
    filter { it is GattInfo.DataWriteSuccess || it is GattInfo.DataWriteFailed }
        .first() is GattInfo.DataWriteSuccess
}