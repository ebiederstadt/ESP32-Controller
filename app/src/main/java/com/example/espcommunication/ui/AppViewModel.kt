package com.example.espcommunication.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.FileNotFoundException

class AppViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UIState())
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    var password by mutableStateOf("")
        private set
    var foundNetwork by mutableStateOf<Network?>(null)
        private set

    val uiState = _uiState.asStateFlow()

    fun setupConnectivityManager(activity: ComponentActivity) {
        connectivityManager =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun destroyConnectivityManager() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun getPasswordFromAsset(context: Context) {
        val passwordString: String
        try {
            Log.d("File Path", context.filesDir.absolutePath)
            passwordString = context.openFileInput("password_storage.txt").bufferedReader().use {
                it.readText()
            }
        } catch (exp: FileNotFoundException) {
            return
        }
        if (passwordString != "") {
            updatePassword(passwordString)
            updateAppState()
        }
    }

    fun writePasswordToAsset(context: Context) {
        context.openFileOutput("password_storage.txt", Context.MODE_PRIVATE).use {
            it.write(password.toByteArray())
        }
        updateAppState()
    }

    fun requestNetwork() {
        if (foundNetwork != null)
            return

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                foundNetwork = network
                updateAppState()
                Log.d("WIFI connection", "Network was found!")
            }

            override fun onUnavailable() {
                updateAppState(passwordPotentiallyValid = false, failedToConnect = true)
                Log.e("WIFI connection", "Not available")
            }
        }

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid("ESP32")
            .setIsHiddenSsid(true)
            .setWpa2Passphrase(password)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()
        connectivityManager.requestNetwork(request, networkCallback)
    }

    private fun updateAppState(
        passwordPotentiallyValid: Boolean = true,
        failedToConnect: Boolean = false
    ) {
        _uiState.update { currentState ->
            currentState.copy(
                passwordAssumedValid = passwordPotentiallyValid,
                failedToConnectToWifi = failedToConnect
            )
        }
    }
}