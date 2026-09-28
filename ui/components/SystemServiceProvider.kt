package com.powerfulmedia.player.ui.components

import android.content.Context
import android.media.AudioManager
import android.view.WindowManager

interface SystemServiceProvider {
    fun getAudioManager(): AudioManager
    fun getWindowManager(): WindowManager
}

class SystemServiceProviderImpl(private val context: Context) : SystemServiceProvider {
    override fun getAudioManager(): AudioManager {
        return context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun getWindowManager(): WindowManager {
        return context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
}
