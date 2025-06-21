package com.zelgius.greenhousesensor.common.ui.current_record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelgius.greenhousesensor.common.model.SensorRecord
import com.zelgius.greenhousesensor.common.usecases.GetCurrentRecordUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class CurrentRecordViewModel(private val getCurrentRecordUseCase: GetCurrentRecordUseCase) :
    ViewModel() {
    private val _uiState = MutableStateFlow(CurrentRecordUiState())
    val uiState = _uiState.asStateFlow()

    fun fetch() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(loading = true)
            }

            _uiState.value =
                CurrentRecordUiState(record = getCurrentRecordUseCase.execute(), loading = false)
        }
    }
}

data class CurrentRecordUiState(
    val loading: Boolean = false,
    val record: SensorRecord? = null
)
