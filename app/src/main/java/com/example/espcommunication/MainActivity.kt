package com.example.espcommunication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.espcommunication.ui.AppUI
import com.example.espcommunication.ui.AppViewModel
import com.example.espcommunication.ui.theme.ESPCommunicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = AppViewModel()
        viewModel.setupConnectivityManager(this)

        setContent {
            ESPCommunicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppUI(viewModel)
                }
            }
        }
    }

    // When the activity is stopped, we will reconnect to the original wifi network
    override fun onStop() {
        super.onStop()
        viewModel.destroyConnectivityManager()
    }
}