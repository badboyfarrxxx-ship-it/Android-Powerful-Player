package com.powerfulmedia.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powerfulmedia.player.DebugMetrics

@Composable
fun DebugOverlay(metrics: DebugMetrics) {
    Column(
        modifier = Modifier
            .padding(12.dp)
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(8.dp)
    ) {
        Text(
            text = "ENGINE ROOM",
            color = Color.Green,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "FPS: %.2f".format(metrics.fps),
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "BITRATE: %d kbps".format(metrics.bitrateKbps),
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "SYNC: %d ms".format(metrics.syncDriftUs / 1000),
            color = if (metrics.syncDriftUs > 40000) Color.Red else Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "DECODE: %.2f ms".format(metrics.decodeSpeedMs),
            color = Color.White,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
