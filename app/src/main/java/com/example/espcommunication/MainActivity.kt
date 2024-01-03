package com.example.espcommunication

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.espcommunication.ui.theme.ESPCommunicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Scanner

class MainActivity : ComponentActivity() {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: NetworkCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        setContent {
            ESPCommunicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    networkCallback = ConnectToNetwork(connectivityManager)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}

@Composable
fun ConnectToNetwork(
    connectivityManager: ConnectivityManager,
): NetworkCallback {
    var foundNetwork by remember { mutableStateOf<Network?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val specifier = WifiNetworkSpecifier.Builder()
        .setSsid("ESP32")
//        .setBssid(MacAddress.fromString("08:F9:E0:20:45:0C"))
        .build()

    val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .setNetworkSpecifier(specifier)
        .build()

    val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("WIFI connection", "Network was found!")
            foundNetwork = network
        }

        override fun onUnavailable() {
            Log.e("WIFI connection", "Not available")
        }
    }

    // This will display a loading bar + error message with option to retry
    connectivityManager.requestNetwork(request, networkCallback)

    OpenButton {
        foundNetwork?.let { network ->
            // We are not allowed to make network requests on the main thread, so we have to use a co-routine scope
            coroutineScope.launch {
                val result = sendRequest(network)
                Log.i("WIFI Connection", result)
            }
        }
    }

    return networkCallback
}

suspend fun sendRequest(network: Network): String {
    return withContext(Dispatchers.IO) {
        val connection = network.openConnection(URL("http://192.168.4.1/blink"))
        val response = connection.getInputStream()
        val scanner = Scanner(response)
        return@withContext scanner.useDelimiter("\\A").next()
    }
}

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun OpenButton(sendRequest: () -> Unit) {
    val snackBarHostState = remember {
        SnackbarHostState()
    }
    Scaffold(content = {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = sendRequest) {
                Text(
                    text = "Open/Close",
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }, snackbarHost = { SnackbarHost(hostState = snackBarHostState) })
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ESPCommunicationTheme {
        OpenButton {}
    }
}
