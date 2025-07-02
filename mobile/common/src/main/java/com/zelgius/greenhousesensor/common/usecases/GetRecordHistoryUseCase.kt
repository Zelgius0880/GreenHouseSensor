package com.zelgius.greenhousesensor.common.usecases

import com.zelgius.greenhousesensor.common.model.SensorRecord
import com.zelgius.greenhousesensor.common.repository.RecordRepository
import com.zelgius.greenhousesensor.common.service.BleState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@SuppressWarnings("MissingPermission")
class GetRecordHistoryUseCase(
    private val recordRepository: RecordRepository,
    private val connectDeviceUseCase: ConnectDeviceUseCase
) {

    @OptIn(ExperimentalTime::class)
    suspend fun execute(): Flow<List<SensorRecord>> {
        connectDeviceUseCase.execute().first { it == BleState.Connected }

        return recordRepository.requestRecords().map {
            it.filter { r ->
                r.date > (Clock.System.now() - 30.days)
            }
        }
    }
}