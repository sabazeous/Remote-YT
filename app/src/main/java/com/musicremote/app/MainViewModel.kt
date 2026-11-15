package com.musicremote.app

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.musicremote.app.network.RelayConfig
import com.musicremote.app.network.RelayWebSocketClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("music_remote_prefs", Context.MODE_PRIVATE)
    private val socketClient = RelayWebSocketClient(viewModelScope)

    val connectionState = socketClient.connectionState
    val playbackState = socketClient.playbackState
    val sessionStatus = socketClient.sessionStatus
    val errorMessage = socketClient.errorMessage

    private val _relayUrl = MutableStateFlow(
        prefs.getString("relay_url", RelayConfig.DEFAULT_RELAY_URL) ?: RelayConfig.DEFAULT_RELAY_URL
    )
    val relayUrl: StateFlow<String> = _relayUrl

    private val _pairingCode = MutableStateFlow(
        prefs.getString("pairing_code", RelayConfig.DEFAULT_PAIRING_CODE) ?: RelayConfig.DEFAULT_PAIRING_CODE
    )
    val pairingCode: StateFlow<String> = _pairingCode

    private val _isPaired = MutableStateFlow(_pairingCode.value.isNotBlank() && _relayUrl.value.isNotBlank())
    val isPaired: StateFlow<Boolean> = _isPaired

    private val _interpolatedPositionSec = MutableStateFlow(0)
    val interpolatedPositionSec: StateFlow<Int> = _interpolatedPositionSec

    init {
        if (_isPaired.value) {
            socketClient.connect(_relayUrl.value, _pairingCode.value)
        }

        // Smooth 1-second progress interpolation synced to server playback updates
        viewModelScope.launch {
            playbackState.collect { state ->
                _interpolatedPositionSec.value = state.positionSec
            }
        }

        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val state = playbackState.value
                if (state.isPlaying && state.durationSec > 0) {
                    val nextPos = (_interpolatedPositionSec.value + 1).coerceAtMost(state.durationSec)
                    _interpolatedPositionSec.value = nextPos
                }
            }
        }
    }

    fun pair(url: String, code: String) {
        val cleanUrl = url.trim()
        val cleanCode = code.trim().uppercase()
        if (cleanUrl.isBlank() || cleanCode.isBlank()) return

        prefs.edit()
            .putString("relay_url", cleanUrl)
            .putString("pairing_code", cleanCode)
            .apply()

        _relayUrl.value = cleanUrl
        _pairingCode.value = cleanCode
        _isPaired.value = true
        socketClient.connect(cleanUrl, cleanCode)
    }

    fun unpair() {
        socketClient.disconnect()
        prefs.edit().remove("pairing_code").apply()
        _pairingCode.value = ""
        _isPaired.value = false
    }

    fun togglePlayPause() = socketClient.sendCommand("toggle")
    fun nextTrack() = socketClient.sendCommand("next")
    fun previousTrack() = socketClient.sendCommand("previous")
    fun seekTo(seconds: Int) = socketClient.sendCommand("seek", seconds.toDouble())
    fun setVolume(percent: Int) = socketClient.sendCommand("setVolume", percent.coerceIn(0, 100).toDouble())
    fun reconnect() = socketClient.connect(_relayUrl.value, _pairingCode.value)

    override fun onCleared() {
        super.onCleared()
        socketClient.disconnect()
    }
}
