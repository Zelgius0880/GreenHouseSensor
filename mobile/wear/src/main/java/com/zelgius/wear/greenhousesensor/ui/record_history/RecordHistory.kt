package com.zelgius.wear.greenhousesensor.ui.record_history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.tooling.preview.devices.WearDevices
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberEnd
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.point
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.Axis
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.component.TextComponent
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import com.patrykandpatrick.vico.core.common.shape.CorneredShape
import com.zelgius.greenhousesensor.common.ui.record_history.RecordHistoryViewModel
import com.zelgius.greenhousesensor.common.ui.record_history.dateFormat
import com.zelgius.greenhousesensor.common.ui.record_history.sample
import com.zelgius.greenhousesensor.common.ui.record_history.splitSeries
import com.zelgius.greenhousesensor.common.ui.record_history.timeFormat
import com.zelgius.wear.greenhousesensor.ui.theme.GreenHouseSensorTheme
import kotlinx.coroutines.runBlocking
import kotlin.time.Instant
import kotlinx.datetime.format
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime


@Composable
private fun RecordHistory(
    modelProducer: CartesianChartModelProducer,
    modifier: Modifier = Modifier,
    xFormatter: CartesianValueFormatter
) {
    val textComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.onBackground,
        lineCount = 1,
        minWidth = TextComponent.MinWidth.fixed(48f)
    )

    val yTextComponent = rememberTextComponent(
        color = MaterialTheme.colorScheme.onBackground,
        lineCount = 1,
    )

    CartesianChartHost(
        zoomState = rememberVicoZoomState(
            initialZoom =
                remember { Zoom.fixed(0.2f) }
        ),
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                pointSpacing = 2.dp,
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(
                        fill = LineCartesianLayer.LineFill.single(fill(Color.Blue)),
                        pointConnector = LineCartesianLayer.PointConnector.cubic(),
                        areaFill = LineCartesianLayer.AreaFill.single(fill(Color.Blue.copy(alpha = 0.5f)))
                    ),
                ),
                verticalAxisPosition = Axis.Position.Vertical.End
            ),
            rememberLineCartesianLayer(
                pointSpacing = 2.dp,
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.Line(
                        LineCartesianLayer.LineFill.single(fill(Color(0xffee2b2b))),
                        pointProvider =
                            LineCartesianLayer.PointProvider.single(
                                LineCartesianLayer.point(
                                    rememberShapeComponent(
                                        fill(Color(0xffee2b2b)),
                                        CorneredShape.Pill
                                    )
                                )
                            ),
                    )
                ),
                verticalAxisPosition = Axis.Position.Vertical.Start
            ),

            startAxis = VerticalAxis.rememberStart(label = yTextComponent),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = xFormatter,
                label = textComponent,
                //labelRotationDegrees = 45f
            ),
            endAxis = VerticalAxis.rememberEnd(label = yTextComponent),
        ),

        modelProducer = modelProducer,
        modifier = modifier,
    )
}

@Composable
fun RecordHistory(
    modifier: Modifier = Modifier,
    viewModel: RecordHistoryViewModel = koinViewModel()
) {

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    val modelProducer by viewModel.modelProducer.collectAsState()
    modelProducer?.let {
        RecordHistory(
            modelProducer = it,
            xFormatter = viewModel.formatterCompact,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
private fun RecordHistoryPreview() {
    GreenHouseSensorTheme {
        val modelProducer = remember { CartesianChartModelProducer() }
        val xToDateMapKey = ExtraStore.Key<List<Long>>()
        val xToDates = sample.map { it.timestamp }
        val formatter = CartesianValueFormatter { context, x, _ ->
            try {
                val time =
                    Instant.fromEpochSeconds((sample.first().timestamp.seconds + x.minutes).inWholeSeconds)
                time.format(timeFormat)
            } catch (e: Exception) {
                e.printStackTrace()
                "?"
            }
        }

        runBlocking {
            modelProducer.runTransaction {
                val series = sample.splitSeries()

                lineSeries {
                    series.forEach {
                        series(
                            x = it.map { r ->
                                ((sample.first().timestamp - r.timestamp).seconds).toLong(
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
                                ((sample.first().timestamp - r.timestamp).seconds).toLong(
                                    DurationUnit.MINUTES
                                )
                            },
                            y = it.map { r -> r.temperature })
                    }
                }
                extras { it[xToDateMapKey] = xToDates }
            }
        }
        Card(
            modifier = Modifier
                .padding(vertical = 16.dp),
            onClick = {},
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 4.dp)
        ) { RecordHistory(modelProducer = modelProducer, xFormatter = formatter) }
    }
}
