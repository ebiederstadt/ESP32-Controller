package com.example.espcommunication.ui

import android.content.Context
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

    var password by mutableStateOf("")
        private set

    val uiState = _uiState.asStateFlow()

    fun updatePassword(newPassword: String) {
        password = newPassword
    }

    fun getPasswordFromAsset(context: Context) {
        val passwordString: String
        try {
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

    private fun updateAppState() {
        _uiState.update { currentState ->
            currentState.copy(
                passwordAssumedValid = true
            )
        }
    }
}