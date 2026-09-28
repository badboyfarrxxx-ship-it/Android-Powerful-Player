package com.vpn.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.vpn.HomeVpnService
import com.vpn.data.VpnPreferences

class VpnViewModel(application: Application) : AndroidViewModel(application) {
    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    data class VpnUiState(
        val state: ConnectionState,
        val currentIp: String,
        val uploadSpeed: Long,
        val downloadSpeed: Long
    )

    private val prefs = VpnPreferences(application)
    private val _uiState = MutableStateFlow(
        VpnUiState(ConnectionState.DISCONNECTED, "None", 0, 0)
    )
    val uiState: StateFlow<VpnUiState> = _uiState.asStateFlow()

    init {
        updateInitialState()
        startMetricsPolling()
    }

    private fun updateInitialState() {
        val ip = prefs.getAssignedIp() ?: "None"
        _uiState.value = VpnUiState(ConnectionState.DISCONNECTED, ip, 0, 0)
    }

    private fun startMetricsPolling() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                updateMetrics()
                delay(1000)
            }
        }
    }

    private fun updateMetrics() {
        // In a real app, we would bind to the service or use a broadcast receiver
        // For this implementation, we assume the service is running and we can access its values
    }

    fun toggleConnection(context: Context) {
        val current = _uiState.value
        if (current.state == ConnectionState.CONNECTED) {
            stopVpn(context)
        } else {
            prepareAndStartVpn(context)
        }
    }

    private fun prepareAndStartVpn(context: Context) {
        val intent = VpnService.prepare(context)
        if (intent != null) {
            _uiState.value = _uiState.value.copy(state = ConnectionState.CONNECTING)
        } else {
            startVpn(context)
        }
    }

    fun startVpn(context: Context) {
        val intent = Intent(context, HomeVpnService::class.java)
        context.startService(intent)
        _uiState.value = _uiState.value.copy(
            state = ConnectionState.CONNECTED,
            currentIp = prefs.getAssignedIp() ?: "None"
        )
    }

    private fun stopVpn(context: Context) {
        context.stopService(Intent(context, HomeVpnService::class.java))
        _uiState.value = _uiState.value.copy(
            state = ConnectionState.DISCONNECTED,
            currentIp = prefs.getAssignedIp() ?: "None"
        )
    }
}
