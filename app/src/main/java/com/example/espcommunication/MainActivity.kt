package com.example.espcommunication

import android.annotation.SuppressLint
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.espcommunication.ui.theme.ESPCommunicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ESPCommunicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OpenButton()
                }
            }
        }
    }
}

@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun OpenButton() {
    val context = LocalContext.current

    val snackBarHostState = remember {
        SnackbarHostState()
    }
    val coroutineScope = rememberCoroutineScope()
    Scaffold(content = {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                // Send the request
                val queue = Volley.newRequestQueue(context)
                val url = "http://192.168.4.1/blink"

                val stringRequest = StringRequest(Request.Method.GET, url,
                    { response ->
                        // Display the first 500 characters of the response string.
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(
                                message = "Response is: ${response.substring(0, 500)}",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    {error ->
                        Log.e("Volley Error", error.toString())
                        coroutineScope.launch {
                            snackBarHostState.showSnackbar(
                                message = "That didn't work!",
                                duration = SnackbarDuration.Short
                            )
                        }
                    })

                queue.add(stringRequest)
            }) {
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
        OpenButton()
    }
}
