package com.example.espcommunication.ui

data class UIState(
    // We don't actually know if the password is valid until we connect to the network
    val passwordAssumedValid: Boolean = false,
    val currentlyConnectingToNetwork: Boolean = false,
    val failedToConnectToWifi: Boolean = false
)
