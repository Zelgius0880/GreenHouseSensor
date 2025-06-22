package com.zelgius.greenhousesensor.common.repository

import android.Manifest
import android.bluetooth.BluetoothDevice
import androidx.annotation.RequiresPermission
import com.zelgius.greenhousesensor.common.service.BleService
import com.zelgius.greenhousesensor.common.service.GattConfig
import java.nio.ByteBuffer
import com.zelgius.greenhousesensor.common.model.SensorRecord
import com.zelgius.greenhousesensor.common.repository.RecordRepository.Companion.NUMBER_OF_RECORDS_IN_A_DAY
import com.zelgius.greenhousesensor.common.repository.RecordRepository.Companion.RECORD_DATA_CHARACTERISTIC_UUID
import com.zelgius.greenhousesensor.common.repository.RecordRepository.Companion.RECORD_REQUEST_CHARACTERISTIC_UUID
import com.zelgius.greenhousesensor.common.toByteArray
import com.zelgius.greenhousesensor.common.ui.record_history.sample
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteOrder

interface RecordRepository {
    companion object {
        const val RECORD_LIST_SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
        const val RECORD_REQUEST_CHARACTERISTIC_UUID = "00000001-1fb5-459e-8fcc-c5c9c331914b"
        const val RECORD_DATA_CHARACTERISTIC_UUID = "00000002-1fb5-459e-8fcc-c5c9c331914b"

        const val NUMBER_OF_RECORDS_IN_A_DAY = 72 // a record each 20 min

        val RECORD_GATT_CONFIG = GattConfig(
            serviceUid = RECORD_LIST_SERVICE_UUID, characteristicUids = listOf(
                RECORD_REQUEST_CHARACTERISTIC_UUID,
                RECORD_DATA_CHARACTERISTIC_UUID,
            )
        )

    }
    suspend fun requestRecords(count: Int = NUMBER_OF_RECORDS_IN_A_DAY): Flow<List<SensorRecord>>
    fun connect(address: String)
    fun connect(device: BluetoothDevice)
    fun disconnect()
    suspend fun getCurrentRecord(): SensorRecord?
}

class RecordRepositoryImpl(private val bleService: BleService) : RecordRepository{

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(address: String) = bleService.connect(address, RecordRepository.RECORD_GATT_CONFIG)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun connect(device: BluetoothDevice) = bleService.connect(device, RecordRepository.RECORD_GATT_CONFIG)

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun disconnect() = bleService.disconnect()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun getCurrentRecord(): SensorRecord? {
        return readRecord(0xffFF) // Current record is always present
    }

    private val mutex = Mutex()

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override suspend fun requestRecords(count: Int) = mutex.withLock {
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

class MockRecordRepository() : RecordRepository {
    override suspend fun requestRecords(count: Int): Flow<List<SensorRecord>> {
        return flow {
            val records: MutableList<SensorRecord> = mutableListOf()
            sample.slice(0 .. count.coerceAtMost(sample.size)).forEach {
                records.add(it)
                delay(10)
                emit(records)
            }
        }
    }

    override fun connect(address: String) {
    }

    override fun connect(device: BluetoothDevice) {
    }

    override fun disconnect() {
    }

    override suspend fun getCurrentRecord(): SensorRecord? {
        return sample.first()
    }

}