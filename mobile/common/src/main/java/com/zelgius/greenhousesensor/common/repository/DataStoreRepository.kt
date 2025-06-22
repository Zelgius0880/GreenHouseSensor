package com.zelgius.greenhousesensor.common.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

interface DataStoreRepository {
    suspend fun saveMacAddress(macAddress: String)
    fun getMacAddress(): Flow<String?>
}

class DataStoreRepositoryImpl(private val context: Context): DataStoreRepository {

    // Create a DataStore instance. The name "bluetooth_prefs" is arbitrary.
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bluetooth_prefs")

    // Define a key for storing the Bluetooth device MAC address
    private val bluetoothMacAddressKey = stringPreferencesKey("bluetooth_mac_address")

    /**
     * Saves the given Bluetooth device MAC address to DataStore.
     *
     * @param macAddress The MAC address string to save.
     */
    override suspend fun saveMacAddress(macAddress: String) {
        context.dataStore.edit { preferences ->
            preferences[bluetoothMacAddressKey] = macAddress
        }
    }

    /**
     * Reads the saved Bluetooth device MAC address from DataStore.
     *
     * @return A Flow that emits the saved MAC address string, or null if no address is saved.
     */
    override fun getMacAddress(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[bluetoothMacAddressKey]
        }
    }

}

class MockDataStoreRepository: DataStoreRepository {
    override suspend fun saveMacAddress(macAddress: String) {

    }

    override fun getMacAddress(): Flow<String?> = flow {
        emit("kshdflshgklfh")
    }

}