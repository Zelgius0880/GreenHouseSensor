package com.zelgius.greenhousesensor.common.repository

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import com.zelgius.greenhousesensor.common.service.BleService
import com.zelgius.greenhousesensor.common.service.GattConfig
import java.nio.ByteBuffer
import com.zelgius.greenhousesensor.common.model.SensorRecord
import com.zelgius.greenhousesensor.common.toByteArray
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteOrder

class RecordRepository(private val bleService: BleService) {
    companion object {
        const val RECORD_LIST_SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
        const val RECORD_REQUEST_CHARACTERISTIC_UUID = "00000001-1fb5-459e-8fcc-c5c9c331914b"
        const val RECORD_DATA_CHARACTERISTIC_UUID = "00000002-1fb5-459e-8fcc-c5c9c331914b"

        const val NUMBER_OF_RECORDS_IN_A_DAY = 72 // a record each 20 min

        val recordGattConfig = GattConfig(
            serviceUid = RECORD_LIST_SERVICE_UUID, characteristicUids = listOf(
                RECORD_REQUEST_CHARACTERISTIC_UUID,
                RECORD_DATA_CHARACTERISTIC_UUID,
            )
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(address: String) = bleService.connect(address, recordGattConfig)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connect(device: BluetoothDevice) = bleService.connect(device, recordGattConfig)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disconnect() = bleService.disconnect()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun getCurrentRecord(): SensorRecord? {
        return readRecord(0xffFF) // Current record is always present
    }

    private val mutex = Mutex()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    suspend fun requestRecords(count: Int = NUMBER_OF_RECORDS_IN_A_DAY) = mutex.withLock {
        flow {
            val records = mutableListOf<SensorRecord>()

            for (i in 0 until count) {
                val record = readRecord(i)

                record?.let {
                    println("Record $i: $it")
                    records.add(it)
                }

                emit(records.toList())
            }
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private suspend fun readRecord(id: Int): SensorRecord? = mutex.withLock {
        bleService.writeCharacteristic(
            RECORD_REQUEST_CHARACTERISTIC_UUID, id.toUInt().toShort().toByteArray()
        )

        var record = bleService.readCharacteristic(RECORD_DATA_CHARACTERISTIC_UUID).toRecord()
        var tries = 0
        while (record != null && record.offset != id && tries < 5) {
            delay(10)
            bleService.writeCharacteristic(
                RECORD_REQUEST_CHARACTERISTIC_UUID, id.toShort().toByteArray()
            )
            record = bleService.readCharacteristic(RECORD_DATA_CHARACTERISTIC_UUID).toRecord()
            ++tries
        }

        return if (tries == 5) null else record
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun ByteArray.toRecord() =
        if (isEmpty()) null else with(ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)) {
            SensorRecord(
                this.short.toUShort().toInt(), this.float, this.float, this.int.toLong()
            )
        }.also {
            println(this.toHexString())
        }
}