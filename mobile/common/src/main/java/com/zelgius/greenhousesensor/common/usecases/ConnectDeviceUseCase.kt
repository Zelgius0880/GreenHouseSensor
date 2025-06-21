package com.zelgius.greenhousesensor.common.usecases

import android.bluetooth.BluetoothDevice
import com.zelgius.greenhousesensor.common.repository.DataStoreRepository
import com.zelgius.greenhousesensor.common.repository.RecordRepository
import com.zelgius.greenhousesensor.common.service.BleService
import com.zelgius.greenhousesensor.common.service.BleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
@SuppressWarnings("MissingPermission")
class ConnectDeviceUseCase(
    private val bleService: BleService,
    private val recordRepository: RecordRepository,
    private val dataStoreRepository: DataStoreRepository
) {
    suspend fun execute(device: BluetoothDevice? = null): Flow<BleState> {
        if(bleService.status.first() == BleState.Disconnected) {
            if (device != null) {
                dataStoreRepository.saveMacAddress(device.address)
                recordRepository.connect(device)
            } else {
                dataStoreRepository.getMacAddress().first()?.let {
                    recordRepository.connect(it)
                }
            }
        }

        return bleService.status
    }
}