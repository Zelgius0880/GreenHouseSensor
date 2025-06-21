package com.zelgius.greenhousesensor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import com.zelgius.greenhousesensor.common.canConnect
import com.zelgius.greenhousesensor.ui.Home
import com.zelgius.greenhousesensor.common.ui.home.HomeViewModel
import com.zelgius.greenhousesensor.ui.theme.GreenHouseSensorTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    val viewModel: HomeViewModel by viewModel()

    val connectPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.connect()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreenHouseSensorTheme {
                Home(viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()

        if(canConnect) {
            viewModel.connect()
        } else {
            connectPermissionRequest.launch(android.Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.disconnect()
    }
}