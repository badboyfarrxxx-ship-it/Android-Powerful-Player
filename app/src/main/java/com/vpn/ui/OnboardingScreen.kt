package com.vpn.ui;

import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.unit.dp;
import androidx.compose.ui.unit.sp;
import androidx.compose.ui.graphics.Color;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.platform.LocalContext;
import com.vpn.data.VpnPreferences;
import com.vpn.network.VpnNetworkClient;
import kotlinx.coroutines.launch;

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var serverDomain by remember { mutableStateOf("") }
    var deviceName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "VPN Setup",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Configure your connection to the Raspberry Pi gateway",
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        OutlinedTextField(
            value = serverDomain,
            onValueChange = { serverDomain = it },
            label = { Text("Server Domain") },
            placeholder = { Text("example.duckdns.org") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            enabled = !isLoading
        )

        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text("Device Name") },
            placeholder = { Text("My Pixel 8") },
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            enabled = !isLoading
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = {
                if (serverDomain.isBlank() || deviceName.isBlank()) {
                    errorMessage = "Please fill in all fields"
                    return@Button
                }

                isLoading = true
                errorMessage = null

                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        val client = VpnNetworkClient()
                        val response = client.registerDevice(serverDomain, deviceName)

                        val prefs = VpnPreferences(context)
                        prefs.saveOnboardingData(serverDomain, deviceName)
                        prefs.saveServerConfig(response.publicKey, response.assignedIp)

                        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            isLoading = false
                            onComplete()
                        }
                    } catch (e: Exception) {
                        scope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            isLoading = false
                            errorMessage = "Registration failed: ${e.message}"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Register Device", fontSize = 18.sp)
            }
        }
    }
}
