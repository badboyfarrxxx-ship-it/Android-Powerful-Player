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
import androidx.compose.ui.viewinterop.AndroidView;
import com.vpn.viewmodel.VpnViewModel;
import androidx.lifecycle.viewmodel.compose.viewModel;

@Composable
fun VpnScreen(vpnViewModel: VpnViewModel = viewModel()) {
    val state by vpnViewModel.uiState.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Home VPN",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Metrics Dashboard
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                MetricRow("Status", state.state.name, state.state == VpnViewModel.ConnectionState.CONNECTED)
                MetricRow("VPN IP", state.currentIp, true)
                MetricRow("Upload", "${state.uploadSpeed} KB/s", true)
                MetricRow("Download", "${state.downloadSpeed} KB/s", true)
            }
        }

        // Connection Toggle Button
        Button(
            onClick = { vpnViewModel.toggleConnection(context) },
            modifier = Modifier
                .size(200.dp, 60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (state.state == VpnViewModel.ConnectionState.CONNECTED)
                    Color(0xFFE57373) else Color(0xFF81C784)
            )
        ) {
            Text(
                text = if (state.state == VpnViewModel.ConnectionState.CONNECTED)
                    "DISCONNECT" else "CONNECT",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
fun MetricRow(label: String, value: String, isHighlight: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
