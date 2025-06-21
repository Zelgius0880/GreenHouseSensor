@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.zelgius.greenhousesensor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelgius.greenhousesensor.common.service.BleDevice
import com.zelgius.greenhousesensor.common.service.BleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.zelgius.greenhousesensor.common.R
import com.zelgius.greenhousesensor.ui.theme.GreenHouseSensorTheme
import com.zelgius.greenhousesensor.common.usecases.ConnectDeviceUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FindDeviceDialog(viewModel: FindDeviceViewModel = koinViewModel(), onDismissRequest: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    FindDeviceDialog(uiState, onDismissRequest) {
        viewModel.connect(it)
        onDismissRequest()
    }

    LaunchedEffect(Unit) {
        viewModel.startScanning()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopScanning()
        }
    }
}

@Composable
private fun FindDeviceDialog(
    uiState: FindDeviceUiState,
    onDismissRequest: () -> Unit = {},
    onDeviceClicked: (BleDevice) -> Unit = {}
) {
    AlertDialog(
        icon = {
            Icon(
                Icons.AutoMirrored.Default.BluetoothSearching,
                contentDescription = null
            )
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(stringResource(R.string.bluetooth_scanning_title))
                AnimatedVisibility(uiState.scanning) {
                    CircularWavyProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .heightIn(max = 250.dp)
                    .width(250.dp)
            ) {
                items(uiState.foundDevices.toList(), key = { it }) {
                    Column(
                        modifier = Modifier
                            .animateItem()
                            .clickable { onDeviceClicked(it) }) {
                        Text(it.name)
                        Text(it.address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text(stringResource(R.string.comon_cancel))
            }
        }
    )
}

@Preview
@Composable
private fun FindDeviceDialogPreview() {
    GreenHouseSensorTheme {
        FindDeviceDialog(
            uiState = FindDeviceUiState(
                foundDevices = List(5) {
                    BleDevice(
                        name = "Device ${it + 1}",
                        address = "00:00:00:00:${it + 1}",
                        device = null
                    )
                }.toSet(),
                scanning = true
            )
        )
    }
}

@SuppressWarnings("MissingPermission")
class FindDeviceViewModel(private val bleService: BleService, private val connectDeviceUseCase: ConnectDeviceUseCase) : ViewModel() {

    private val _uiState = MutableStateFlow(FindDeviceUiState())
    val uiState = _uiState.asStateFlow()
        .combine(bleService.scanning) { uiState, scanning ->
            uiState.copy(scanning = scanning)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FindDeviceUiState())

    fun startScanning() = viewModelScope.launch {
        _uiState.value = _uiState.value.copy(scanning = true)
        bleService.stopScan()
        bleService.startScan().collect {
            _uiState.value = _uiState.value.copy(foundDevices = _uiState.value.foundDevices + it)
        }
    }

    fun stopScanning() {
        bleService.stopScan()
    }

    fun connect(device: BleDevice) = viewModelScope.launch {
        connectDeviceUseCase.execute(device.device)
    }
}

data class FindDeviceUiState(
    val scanning: Boolean = false,
    val foundDevices: Set<BleDevice> = emptySet(),
)