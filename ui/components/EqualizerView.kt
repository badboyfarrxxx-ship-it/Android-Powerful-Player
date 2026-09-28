package com.powerfulmedia.player.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@Composable
fun EqualizerView(
    bands: List<EqualizerBand>,
    onBandGainChanged: (bandIndex: Int, newGain: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Equalizer",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bands.forEachIndexed { index, band ->
                EqualizerBandView(
                    band = band,
                    onGainChanged = { newGain -> onBandGainChanged(index, newGain) }
                )
            }
        }
    }
}

@Composable
fun EqualizerBandView(
    band: EqualizerBand,
    onGainChanged: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${band.gain}dB",
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Slider(
            value = band.gain.toFloat(),
            onValueChange = { newValue ->
                onGainChanged(newValue.toInt())
            },
            valueRange = -15f..15f,
            modifier = Modifier
                .width(40.dp)
                .height(200.dp),
            steps = 30 // 1dB increments
        )

        Text(
            text = band.label,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

data class EqualizerBand(
    val label: String,
    val gain: Int
)
