package com.zelgius.wear.greenhousesensor.ui.current_record

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.tooling.preview.devices.WearDevices
import com.zelgius.greenhousesensor.common.R
import com.zelgius.greenhousesensor.common.model.SensorRecord
import com.zelgius.greenhousesensor.common.ui.current_record.CurrentRecordUiState
import com.zelgius.wear.greenhousesensor.ui.theme.GreenHouseSensorTheme
import java.util.Locale


fun ScalingLazyListScope.currentRecordItems(uiState: CurrentRecordUiState) {
    val record = uiState.record ?: return
    item {
        TitleCard(
            onClick = {},
            title = { Text(text = stringResource(R.string.temperature_label)) },
            colors = CardDefaults.cardColors()
                .copy(containerColor = Color(0xffDC2626).copy(alpha = 0.2f))
        ) {
            Text(String.format(Locale.getDefault(), "%.1f°C", record.temperature))
        }
    }
    item {
        TitleCard(
            onClick = {},
            title = { Text(text = stringResource(R.string.humidity_label)) },
            colors = CardDefaults.cardColors()
                .copy(containerColor = Color(0xff2563EB).copy(alpha = 0.2f))
        ) {
            Text(String.format(Locale.getDefault(), "%.0f%%", record.humidity))
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun CurrentRecordPreview() {
    val scrollState = rememberScalingLazyListState()
    GreenHouseSensorTheme {
        ScreenScaffold(
            scrollState = scrollState,
        ) {
            ScalingLazyColumn (modifier = Modifier.fillMaxSize().padding(it), state = scrollState){
                currentRecordItems(
                    uiState = CurrentRecordUiState(
                        loading = false,
                        record = SensorRecord(
                            offset = 0,
                            humidity = 50f,
                            temperature = 20.5f,
                            timestamp = 0
                        )
                    )
                )
            }
        }
    }
}