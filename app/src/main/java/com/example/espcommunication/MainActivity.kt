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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.espcommunication.ui.theme.ESPCommunicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
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
                    passwordHandler()
//                    networkCallback = connectToNetwork(connectivityManager)
                }
            }
        }
    }

    // When the activity is stopped, we will reconnect to the original wifi network
    override fun onStop() {
        super.onStop()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    // If the user re-navigates to the network, then we can re-register the network callback
    override fun onRestart() {
        super.onRestart()
        requestNetwork(connectivityManager, networkCallback)
    }
}

fun requestNetwork(connectivityManager: ConnectivityManager, networkCallback: NetworkCallback) {
    val specifier = WifiNetworkSpecifier.Builder()
        .setSsid("ESP32")
        .setIsHiddenSsid(true)
        .setWpa2Passphrase("example_pass")
        .build()

    val request = NetworkRequest.Builder()
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .setNetworkSpecifier(specifier)
        .build()
    connectivityManager.requestNetwork(request, networkCallback)
}

@Composable
fun connectToNetwork(
    connectivityManager: ConnectivityManager,
): NetworkCallback {
    var foundNetwork by remember { mutableStateOf<Network?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val networkCallback = object : NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d("WIFI connection", "Network was found!")
            foundNetwork = network
        }

        override fun onUnavailable() {
            // TODO: Need to show something to the user in this case, because it will happen if the password is incorrect
            Log.e("WIFI connection", "Not available")
        }
    }

    requestNetwork(connectivityManager, networkCallback)

    openButton {
        foundNetwork?.let { network ->
            // We are not allowed to make network requests on the main thread, so we have to use a
            // co-routine scope
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
fun openButton(sendRequest: () -> Unit) {
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

fun getPasswordFromAsset(context: Context): String? {
    val passwordString: String
    try {
        passwordString = context.openFileInput("password_storage.txt").bufferedReader().use {
            it.readText()
        }
    } catch (exp: FileNotFoundException) {
        return null
    }
    if (passwordString == "") {
        return null
    }
    return passwordString
}

fun writePasswordToAsset(context: Context, password: String) {
    context.openFileOutput("password_storage.txt", Context.MODE_PRIVATE).use {
        it.write(password.toByteArray())
    }
}

@Composable
fun passwordHandler() {
    val openAlertDialog = remember { mutableStateOf(true) }
    val password = remember { mutableStateOf("") }

    val context = LocalContext.current
    // Check to see if we saved a version of this file, but ensure that we only do it once :)
    LaunchedEffect(Unit) {
        getPasswordFromAsset(context)?.let {
            password.value = it
            Log.d("Password", "Found password in file")
            openAlertDialog.value = false
        }
    }

    if (openAlertDialog.value) {
        EnterPassword(
            onConfirmation = {
                openAlertDialog.value = false
                writePasswordToAsset(context, password.value)
                Log.d("Password", "password saved: ${password.value}")
            },
            updatePassword = { newPassword: String -> password.value = newPassword },
            password = password.value
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EnterPassword(
    onConfirmation: () -> Unit,
    updatePassword: (String) -> Unit,
    password: String
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
                    text = "Enter the password used by the ESP32 to create the network.",
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

@Preview(showBackground = true)
@Composable
fun DialogPreview() {
    ESPCommunicationTheme {
        EnterPassword(
            onConfirmation = {
                Log.d("Password", "password entered")
            },
            updatePassword = { _: String -> Log.d("Password", "Password Updated") },
            password = "test"
        )
    }
}
