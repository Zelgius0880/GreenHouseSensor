package com.zelgius.wear.greenhousesensor.ui.home

import android.Manifest
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.tooling.preview.devices.WearDevices
import com.zelgius.greenhousesensor.common.R
import com.zelgius.greenhousesensor.common.canScan
import com.zelgius.greenhousesensor.common.service.BleState
import com.zelgius.greenhousesensor.common.ui.current_record.CurrentRecordViewModel
import com.zelgius.greenhousesensor.common.ui.home.HomeUiState
import com.zelgius.greenhousesensor.common.ui.home.HomeViewModel
import com.zelgius.wear.greenhousesensor.ui.FindDeviceDialog
import com.zelgius.wear.greenhousesensor.ui.current_record.currentRecordItems
import com.zelgius.wear.greenhousesensor.ui.record_history.RecordHistory
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import kotlin.concurrent.timer

@Composable
fun Home(viewModel: HomeViewModel) {
    var showFindDeviceDialog by remember { mutableStateOf(false) }

    if (showFindDeviceDialog) {
        FindDeviceDialog(true, onDismissRequest = {
            showFindDeviceDialog = false
        })
    }
    val scanDeviceRequest =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            showFindDeviceDialog = true
        }

    val canScan = LocalContext.current.canScan

    val uiState by viewModel.uiState.collectAsState()
    Home(uiState) {
        if (canScan) {
            showFindDeviceDialog = true
        } else {
            scanDeviceRequest.launch(Manifest.permission.BLUETOOTH_SCAN)
        }
    }
}


@Composable
private fun Home(uiState: HomeUiState = HomeUiState(), onSettingsClicked: () -> Unit = {}) {
    val scope = rememberCoroutineScope()

    val scrollState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollState = scrollState,
        modifier = Modifier.fillMaxSize(),
        timeText = {
            AnimatedContent(uiState.status) {
                when (it) {
                    BleState.Disconnected -> Icon(
                        Icons.Default.BluetoothDisabled,
                        contentDescription = "Disconnected",
                        tint = MaterialTheme.colorScheme.onBackground
                    )

                    BleState.Connecting -> CircularProgressIndicator(
                        modifier = Modifier.size(
                            32.dp
                        )
                    )

                    BleState.Connected -> Icon(
                        Icons.Default.BluetoothConnected,
                        contentDescription = "Connected",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        },
        edgeButton = {
            val rotation = remember { Animatable(0f, 0f) }
            EdgeButton(onClick = {
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

                Text(stringResource(R.string.connect_device))
            }
        }
    ) { paddingValues ->
        val currentRecordViewModel = koinViewModel<CurrentRecordViewModel>()
        val currentRecordUiState by currentRecordViewModel.uiState.collectAsState()
        ScalingLazyColumn(state = scrollState, modifier = Modifier.padding(paddingValues)) {

            if (currentRecordUiState.loading) {
                item {
                    CircularProgressIndicator()
                }
            } else {
                currentRecordItems(currentRecordUiState)
            }

            item {
                Card(onClick = {}) {
                    RecordHistory()
                }
            }
        }
    }

}


@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun HomePreview() {
    ScreenScaffold {
        Home(uiState = HomeUiState(BleState.Disconnected)) {
            println("Clicked")
        }
    }
}