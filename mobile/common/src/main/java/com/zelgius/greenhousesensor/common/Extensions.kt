package com.zelgius.greenhousesensor.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

val Context.canConnect: Boolean
    get() = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED


val canScan: Boolean
    @Composable
    get() = LocalContext.current.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED


fun Number.toByteArray(): ByteArray {
    return when (this) {
        is Int -> ByteBuffer.allocate(Int.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(this).array()
        is Long -> ByteBuffer.allocate(Long.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putLong(this).array()
        is Float -> ByteBuffer.allocate(Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putFloat(this).array()
        is Double -> ByteBuffer.allocate(Double.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putDouble(this).array()
        is Short -> ByteBuffer.allocate(Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN).putShort(this).array()
        is Byte -> byteArrayOf(this)
        else -> throw IllegalArgumentException("Unsupported Number type: ${this::class.java.name}")
    }
}