package com.zelgius.wear.greenhousesensor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material3.AlertDialogContent
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Dialog
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.zelgius.greenhousesensor.common.R
import com.zelgius.greenhousesensor.common.service.BleDevice
import com.zelgius.greenhousesensor.common.service.BleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.zelgius.greenhousesensor.common.usecases.ConnectDeviceUseCase
import com.zelgius.wear.greenhousesensor.ui.theme.GreenHouseSensorTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.seconds

@Composable
fun FindDeviceDialog(
    showDialog: Boolean,
    viewModel: FindDeviceViewModel = koinViewModel(), onDismissRequest: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    FindDeviceDialog(showDialog, uiState, onDismissRequest) {
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
    showDialog: Boolean,
    uiState: FindDeviceUiState,
    onDismissRequest: () -> Unit = {},
    onDeviceClicked: (BleDevice) -> Unit = {}
) {
    Dialog(
        visible = showDialog,
        onDismissRequest = onDismissRequest,
        content = {
            AlertDialogContent(
                modifier = Modifier.fillMaxSize(),
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
                    }
                },
                content = {
                    item {
                        AnimatedVisibility(uiState.scanning) {
                            CircularProgressIndicator()
                        }
                    }
                    items(uiState.foundDevices.toList(), key = { it }) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CircleShape)
                                .clickable { onDeviceClicked(it) }
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text(it.name)
                            Text(it.address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                edgeButton = {
                    EdgeButton(
                        onClick = {
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(R.string.comon_cancel))
                    }
                },
            )
        }
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun FindDeviceDialogPreview() {
    var scanning by remember { mutableStateOf(true) }

    LaunchedEffect(scanning) {
        delay(5.seconds)
        scanning = !scanning
    }
    GreenHouseSensorTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            FindDeviceDialog(
                true,
                uiState = FindDeviceUiState(
                    foundDevices = List(5) {
                        BleDevice(
                            name = "Device ${it + 1}",
                            address = "00:00:00:00:${it + 1}",
                            device = null
                        )
                    }.toSet(),
                    scanning = scanning
                )
            )
        }
    }
}

@SuppressWarnings("MissingPermission")
class FindDeviceViewModel(
    private val bleService: BleService,
    private val connectDeviceUseCase: ConnectDeviceUseCase
) : ViewModel() {

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