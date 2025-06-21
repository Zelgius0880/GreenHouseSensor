package com.zelgius.greenhousesensor.ui

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zelgius.greenhousesensor.common.canScan
import com.zelgius.greenhousesensor.common.service.BleService
import com.zelgius.greenhousesensor.common.service.BleState
import com.zelgius.greenhousesensor.common.ui.home.HomeUiState
import com.zelgius.greenhousesensor.common.ui.home.HomeViewModel
import com.zelgius.greenhousesensor.ui.current_record.CurrentRecord
import com.zelgius.greenhousesensor.ui.record_history.RecordHistory
import com.zelgius.greenhousesensor.ui.theme.GreenHouseSensorTheme
import com.zelgius.greenhousesensor.common.usecases.ConnectDeviceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    GreenHouseSensorTheme {
        Home(HomeUiState(BleState.Connecting))
    }
}

@Composable
fun Home(viewModel: HomeViewModel = koinViewModel()) {
    var showFindDeviceDialog by remember { mutableStateOf(false) }
    val scanDeviceRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            showFindDeviceDialog = true
        }

    val canScan = canScan

    val uiState by viewModel.uiState.collectAsState()
    Home(uiState) {
        if (canScan) {
            showFindDeviceDialog = true
        } else {
            scanDeviceRequest.launch(Manifest.permission.BLUETOOTH_SCAN)
        }
    }

    if (showFindDeviceDialog) {
        FindDeviceDialog(onDismissRequest = {
            showFindDeviceDialog = false
        })
    }
}


@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
private fun Home(uiState: HomeUiState = HomeUiState(), onSettingsClicked: () -> Unit = {}) {
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    AnimatedContent(uiState.status) {
                        when (it) {
                            BleState.Disconnected -> Icon(Icons.Default.BluetoothDisabled, contentDescription = "Disconnected")
                            BleState.Connecting -> CircularWavyProgressIndicator(
                                modifier = Modifier.size(
                                    32.dp
                                )
                            )

                            BleState.Connected -> Icon(Icons.Default.BluetoothConnected, contentDescription = "Connected")
                        }
                    }

                    val rotation = remember { Animatable(0f, 0f) }
                    IconButton(onClick = {
                        scope.launch {
                            rotation.animateTo(
                                180f,
                                animationSpec = spring(stiffness = Spring.StiffnessLow)
                            )
                        }
                        onSettingsClicked()
                    }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.rotate(rotation.value)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            CurrentRecord()
            RecordHistory(Modifier.fillMaxWidth().aspectRatio(4f / 3f))
        }
    }

}
