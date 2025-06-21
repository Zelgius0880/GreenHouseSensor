package com.zelgius.greenhousesensor.common.ui.home

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelgius.greenhousesensor.common.service.BleService
import com.zelgius.greenhousesensor.common.service.BleState
import com.zelgius.greenhousesensor.common.usecases.ConnectDeviceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val bleService: BleService,
    private val connectDeviceUseCase: ConnectDeviceUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()
        .combine(bleService.status) { state, bleState ->
            state.copy(status = bleState)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())


    @SuppressLint("MissingPermission")
    fun connect() = viewModelScope.launch {
        connectDeviceUseCase.execute()
    }

    @SuppressLint("MissingPermission")
    fun disconnect() = viewModelScope.launch {
        bleService.disconnect()
    }
}

data class HomeUiState(
    val status: BleState = BleState.Disconnected
)