package com.zelgius.greenhousesensor.common.model

import android.os.Parcelable
import kotlinx.datetime.Instant
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.time.ExperimentalTime

@Parcelize
data class SensorRecord(
    val offset: Int,
    val temperature: Float,
    val humidity: Float,
    val timestamp: Long
): Parcelable {
    @IgnoredOnParcel
    @OptIn(ExperimentalTime::class)
    val date = Instant.fromEpochSeconds(timestamp)
}