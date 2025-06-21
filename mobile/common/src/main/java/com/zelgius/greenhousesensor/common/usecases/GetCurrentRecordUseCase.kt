package com.zelgius.greenhousesensor.common.usecases

import com.zelgius.greenhousesensor.common.model.SensorRecord
import com.zelgius.greenhousesensor.common.repository.RecordRepository
import com.zelgius.greenhousesensor.common.service.BleState
import kotlinx.coroutines.flow.first

@SuppressWarnings("MissingPermission")
class GetCurrentRecordUseCase(
    private val recordRepository: RecordRepository,
    private val connectDeviceUseCase: ConnectDeviceUseCase
) {

    suspend fun execute(): SensorRecord? = try {
        connectDeviceUseCase.execute().first { it == BleState.Connected }
        recordRepository.getCurrentRecord()
    }catch (e: Exception) {
        e.printStackTrace()
        null
    }
}