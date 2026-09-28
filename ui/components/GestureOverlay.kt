package com.powerfulmedia.player.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import com.powerfulmedia.player.PlayerViewModel

@Composable
fun GestureOverlay(
    viewModel: PlayerViewModel,
    systemServiceProvider: SystemServiceProvider,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { systemServiceProvider.getAudioManager() }

    // Helper to find Activity for brightness control
    val activity = remember(context) {
        findActivity(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
                detectVerticalSwipes(touchSlop) { offset ->
                    val width = size.width
                    val x = offset.x
                    val deltaY = offset.y

                    if (x < width / 2) {
                        // Left half: Volume Control
                        if (deltaY > 0) {
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_LOWER,
                                0
                            )
                        } else if (deltaY < 0) {
                            audioManager.adjustStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                AudioManager.ADJUST_RAISE,
                                0
                            )
                        }
                    } else {
                        // Right half: Brightness Control
                        activity?.window?.let { window ->
                            val layoutParams = window.attributes
                            val currentBrightness = layoutParams.screenBrightness
                            val brightnessDelta = deltaY / size.height.toFloat()
                            val newBrightness = (currentBrightness - brightnessDelta).coerceIn(0f, 1f)
                            layoutParams.screenBrightness = newBrightness
                            window.attributes = layoutParams
                        }
                    }
                }
            }
    ) {
        content()
    }
}

private fun findActivity(context: Context): Activity? {
    var currentContext = context
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}

private suspend fun PointerInputScope.detectVerticalSwipes(
    touchSlop: Float,
    onSwipe: (offset: Offset) -> Unit
) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitFirstDown()
            var lastOffset = event.position
            var totalDeltaY = 0f
            var hasExceededSlop = false
            var accumulatedDeltaSinceLastUpdate = 0f
            val updateThresholdDp = 8f // Update every ~8dp of movement
            val density = this@detectVerticalSwipes.density
            val updateThresholdPx = updateThresholdDp * density.density

            do {
                val eventChange = awaitPointerEvent()
                val currentOffset = eventChange.changes.first().position
                val delta = currentOffset - lastOffset

                if (!hasExceededSlop) {
                    totalDeltaY += delta.y
                    if (kotlin.math.abs(totalDeltaY) > touchSlop) {
                        hasExceededSlop = true
                        // Trigger initial movement once slop is breached
                        onSwipe(Offset(currentOffset.x, totalDeltaY))
                        accumulatedDeltaSinceLastUpdate = 0f
                    }
                } else {
                    if (kotlin.math.abs(delta.y) > kotlin.math.abs(delta.x)) {
                        accumulatedDeltaSinceLastUpdate += delta.y
                        if (kotlin.math.abs(accumulatedDeltaSinceLastUpdate) >= updateThresholdPx) {
                            onSwipe(Offset(currentOffset.x, accumulatedDeltaSinceLastUpdate))
                            accumulatedDeltaSinceLastUpdate = 0f
                        }
                    }
                }

                lastOffset = currentOffset
            } while (eventChange.changes.any { it.pressed })
        }
    }
}
