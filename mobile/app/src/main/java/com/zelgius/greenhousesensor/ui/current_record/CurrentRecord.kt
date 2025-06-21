package com.zelgius.greenhousesensor.ui.current_record

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zelgius.greenhousesensor.common.R
import com.zelgius.greenhousesensor.common.model.SensorRecord
import com.zelgius.greenhousesensor.common.ui.current_record.CurrentRecordUiState
import com.zelgius.greenhousesensor.common.ui.current_record.CurrentRecordViewModel
import com.zelgius.greenhousesensor.ui.theme.GreenHouseSensorTheme
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import org.koin.compose.viewmodel.koinViewModel
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal val format = DateTimeComponents.Format {
    hour(); char(':'); minute();char(' ');dayOfMonth();char('/');monthNumber();char('/');year()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CurrentRecord(viewModel: CurrentRecordViewModel = koinViewModel()) {
    LaunchedEffect(Unit) {
        viewModel.fetch()
    }

    val state by viewModel.uiState.collectAsState()
    AnimatedContent(state.loading, modifier = Modifier.fillMaxWidth()) {
        if (!it) {
            CurrentRecord(state) {
                viewModel.fetch()
            }
        } else {
            Box {
                CircularWavyProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }

}

@OptIn(ExperimentalTime::class)
@Composable
fun CurrentRecord(state: CurrentRecordUiState, onRefresh: () -> Unit) {
    val record = state.record?: return
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)){
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(record.date.format(format), modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MeasureBox(
                measure = String.format(Locale.getDefault(),"%.1f°C", record.temperature),
                label = stringResource(R.string.temperature_label),
                color = Color(0xffDC2626),
                modifier = Modifier.weight(1f)
            )
            MeasureBox(
                measure = String.format(Locale.getDefault(),"%.0f%%", record.humidity),
                label = stringResource(R.string.humidity_label),
                color = Color(0xff2563EB),
                modifier = Modifier.weight(1f)
            )
        }
    }



}

@Composable
fun MeasureBox(measure: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .border(1.dp, color = color, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.08f))
    ) {
        Column(
            Modifier
                .padding(16.dp)
                .align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(measure, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Preview
@Composable
fun MeasureBoxPreview() {
    Column {
        GreenHouseSensorTheme {
            Surface {
                MeasureBox(
                    measure = "10.5°C",
                    label = "Temperature",
                    color = Color(0xffDC2626),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        GreenHouseSensorTheme(darkTheme = true) {
            Surface {
                MeasureBox(
                    measure = "10.5°C",
                    label = "Temperature",
                    color = Color(0xffDC2626),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
@Preview
fun CurrentRecordPreview() = GreenHouseSensorTheme {
    Surface {
        CurrentRecord(
            CurrentRecordUiState(
                record = SensorRecord(
                    0,
                    10f,
                    20f,
                    Clock.System.now().epochSeconds
                )
            )
        ) {}
    }
}