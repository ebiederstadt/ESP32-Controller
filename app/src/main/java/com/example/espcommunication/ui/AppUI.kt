package com.example.espcommunication.ui

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.espcommunication.R
import com.example.espcommunication.ui.theme.ESPCommunicationTheme
import kotlinx.coroutines.launch

@Composable
fun AppUI(viewModel: AppViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    // Check to see if we saved a version of this file, but ensure that we only do it once :)
    LaunchedEffect(Unit) {
        viewModel.getPasswordFromAsset(context)
    }

    if (!uiState.passwordAssumedValid) {
        EnterPassword(
            onConfirmation = {
                viewModel.writePasswordToAsset(context)
            },
            updatePassword = { viewModel.updatePassword(it) },
            password = viewModel.password,
            detailMessage = if (uiState.failedToConnectToWifi) stringResource(R.string.wifi_connect_message_fail) else stringResource(
                R.string.wifi_connect_message_default
            )
        )
    }

    if (uiState.passwordAssumedValid) {
        LaunchedEffect(Unit) {
            viewModel.requestNetwork()
        }
    }

    if (uiState.currentlyConnectingToNetwork) {
        IndicateNetworkLoading()
    }

    if (!uiState.failedToConnectToWifi and !uiState.currentlyConnectingToNetwork) {
        OpenButton {
            // We are not allowed to make network requests on the main thread, so we have to use a
            // co-routine scope
            coroutineScope.launch {
                val result = viewModel.sendRequest()
                result?.let {
                    Log.i("WIFI Connection", it)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EnterPassword(
    onConfirmation: () -> Unit,
    updatePassword: (String) -> Unit,
    password: String,
    detailMessage: String
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val padding = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Enter Network Password",
                    textAlign = TextAlign.Center,
                    modifier = padding,
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = detailMessage,
                    modifier = padding,
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                TextField(
                    value = password,
                    onValueChange = updatePassword,
                    label = { Text("Password") },
                    modifier = padding.autofill(
                        autofillTypes = listOf(AutofillType.Password, AutofillType.NewPassword),
                        onFill = updatePassword
                    ),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        val description = if (passwordVisible) "Hide password" else "Show password"
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, description)
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxSize()) {
                    TextButton(
                        onClick = onConfirmation,
                        modifier = padding,
                    ) {
                        Text("Submit", color = MaterialTheme.colorScheme.onSecondary)
                    }
                }
            }
        }
    }

}

@Composable
fun IndicateNetworkLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Connecting...",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 10.dp)
        )
        CircularProgressIndicator(
            modifier = Modifier.width(64.dp),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun OpenButton(sendRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = sendRequest) {
            Text(
                text = "Open/Close",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
) = composed {
    val autofill = LocalAutofill.current
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
    LocalAutofillTree.current += autofillNode

    this
        .onGloballyPositioned {
            autofillNode.boundingBox = it.boundsInWindow()
        }
        .onFocusChanged { focusState ->
            autofill?.run {
                if (focusState.isFocused) {
                    requestAutofillForNode(autofillNode)
                } else {
                    cancelAutofillForNode(autofillNode)
                }
            }
        }
}

@Preview(showBackground = true)
@Composable
fun DialogPreview() {
    ESPCommunicationTheme {
        EnterPassword(
            onConfirmation = {
                Log.d("Password", "password entered")
            },
            updatePassword = { _: String -> Log.d("Password", "Password Updated") },
            password = "test",
            detailMessage = stringResource(R.string.wifi_connect_message_default)
        )
    }
}
