package com.zelgius.greenhousesensor.common.ui.record_history

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.zelgius.greenhousesensor.common.usecases.GetRecordHistoryUseCase
import com.zelgius.greenhousesensor.common.model.SensorRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import kotlin.time.DurationUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


val timeFormat = DateTimeComponents.Format {
    hour(); char(':'); minute()
}


val dateFormat = DateTimeComponents.Format {
    dayOfMonth(); char('/'); monthNumber()
}

@SuppressLint("MissingPermission")
class RecordHistoryViewModel(
    private val getRecordHistoryUseCase: GetRecordHistoryUseCase,
) : ViewModel() {
    private val _modelProducer = MutableStateFlow<CartesianChartModelProducer?>(null)
    val modelProducer = _modelProducer.asStateFlow()
    private val _history = MutableStateFlow<List<SensorRecord>>(emptyList())
    private val xToDateMapKey = ExtraStore.Key<List<Long>>()

    init {
        viewModelScope.launch {
            _history.collect { history ->
                val modelProducer = _modelProducer.value ?: CartesianChartModelProducer()

                val series = history.splitSeries()
                if (series.isEmpty()) return@collect

                modelProducer.runTransaction {
                    val xToDates = history.map { it.timestamp }

                    lineSeries {
                        series.forEach {
                            series(
                                x = it.map { r ->
                                    ((history.first().timestamp - r.timestamp).seconds).toLong(
                                        DurationUnit.MINUTES
                                    )
                                },
                                y = it.map { r -> r.humidity })
                        }

                    }

                    lineSeries {
                        series.forEach {
                            series(
                                x = it.map { r ->
                                    ((history.first().timestamp - r.timestamp).seconds).toLong(
                                        DurationUnit.MINUTES
                                    )
                                },
                                y = it.map { r -> r.temperature })
                        }

                    }
                    extras { it[xToDateMapKey] = xToDates }
                }

                _modelProducer.value = modelProducer
            }
        }
    }

    val formatter = CartesianValueFormatter { context, x, _ ->
        try {
            val time =
                Instant.fromEpochSeconds((_history.value.first().timestamp.seconds - x.minutes).inWholeSeconds)
            "${time.format(dateFormat)}\n${time.format(timeFormat)}"

        } catch (e: Exception) {
            e.printStackTrace()
            println("$x")
            "?"
        }
    }


    val formatterCompact = CartesianValueFormatter { context, x, _ ->
        try {
            val time =
                Instant.fromEpochSeconds((_history.value.first().timestamp.seconds - x.minutes).inWholeSeconds)
            time.format(timeFormat)

        } catch (e: Exception) {
            e.printStackTrace()
            println("$x")
            "?"
        }
    }

    fun refresh() {
        viewModelScope.launch {
            getRecordHistoryUseCase.execute().collect {
                _history.value = it
            }
        }
    }

}


fun List<SensorRecord>.splitSeries(): List<List<SensorRecord>> {
    val series = mutableListOf<List<SensorRecord>>()
    var currentSerie = mutableListOf<SensorRecord>()

    var currentRecord: SensorRecord? = null

    forEach {
        if (currentRecord == null || currentRecord.date - it.date < 25.minutes) {
            currentSerie.add(it)
        } else {
            series.add(currentSerie)
            currentSerie = mutableListOf()
        }

        currentRecord = it
    }

    series.add(currentSerie)

    return series.filter { it.isNotEmpty() }
}
