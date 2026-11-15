package com.musicremote.app.network

import android.os.Build
import com.musicremote.app.model.PlaybackState
import com.musicremote.app.model.RelayConnectionState
import com.musicremote.app.model.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow

class RelayWebSocketClient(
    private val scope: CoroutineScope
) {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var relayUrl: String = ""
    private var pairingCode: String = ""
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    private var manuallyClosed = false

    private val _connectionState = MutableStateFlow(RelayConnectionState.DISCONNECTED)
    val connectionState: StateFlow<RelayConnectionState> = _connectionState

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _sessionStatus = MutableStateFlow(SessionStatus())
    val sessionStatus: StateFlow<SessionStatus> = _sessionStatus

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun connect(url: String, code: String) {
        manuallyClosed = false
        relayUrl = url.trim()
        pairingCode = code.trim().uppercase()
        reconnectAttempts = 0
        reconnectJob?.cancel()
        openSocket()
    }

    fun disconnect() {
        manuallyClosed = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        _connectionState.value = RelayConnectionState.DISCONNECTED
    }

    private fun openSocket() {
        if (relayUrl.isBlank() || pairingCode.isBlank()) return
        webSocket?.cancel()
        _connectionState.value = RelayConnectionState.CONNECTING
        _errorMessage.value = null

        val request = Request.Builder().url(relayUrl).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                _connectionState.value = RelayConnectionState.CONNECTED

                val registerJson = JSONObject().apply {
                    put("type", "register_remote")
                    put("pairingCode", pairingCode)
                    put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}")
                    put("clientType", "android")
                }
                webSocket.send(registerJson.toString())
                sendCommand("requestSync")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _connectionState.value = RelayConnectionState.DISCONNECTED
                if (!manuallyClosed) scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.value = RelayConnectionState.DISCONNECTED
                _errorMessage.value = t.message ?: "Connection interrupted"
                if (!manuallyClosed) scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectAttempts += 1
        val delayMs = min(15_000L, (1000.0 * 1.5.pow(min(reconnectAttempts, 6))).toLong())
        reconnectJob = scope.launch(Dispatchers.IO) {
            delay(delayMs)
            if (!manuallyClosed) openSocket()
        }
    }

    private fun handleMessage(raw: String) {
        try {
            val root = JSONObject(raw)
            when (root.optString("type")) {
                "ping" -> {
                    val pong = JSONObject().apply {
                        put("type", "pong")
                        put("timestamp", System.currentTimeMillis())
                    }
                    webSocket?.send(pong.toString())
                }

                "playback_state" -> {
                    val s = root.optJSONObject("state") ?: return
                    _playbackState.value = parsePlaybackState(s)
                }

                "session_status" -> {
                    val st = root.optJSONObject("status") ?: return
                    _sessionStatus.value = SessionStatus(
                        pairingCode = st.optString("pairingCode", pairingCode),
                        companionOnline = st.optBoolean("companionOnline", false),
                        companionName = st.optString("companionName", "Windows Laptop"),
                        chromeConnected = st.optBoolean("chromeConnected", false),
                        remoteCount = st.optInt("remoteCount", 1)
                    )
                }

                "command_ack" -> {
                    val ack = root.optJSONObject("ack") ?: return
                    if (!ack.optBoolean("success", true)) {
                        _errorMessage.value = ack.optString("error", "Command failed on Windows")
                    } else {
                        _errorMessage.value = null
                    }
                    ack.optJSONObject("state")?.let {
                        _playbackState.value = parsePlaybackState(it)
                    }
                }

                "error" -> {
                    _errorMessage.value = root.optString("message", "Relay error")
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun parsePlaybackState(s: JSONObject): PlaybackState {
        return PlaybackState(
            title = s.optString("title", ""),
            artist = s.optString("artist", ""),
            album = s.optString("album", ""),
            artworkUrl = s.optString("artworkUrl", ""),
            isPlaying = s.optBoolean("isPlaying", false),
            positionSec = s.optInt("positionSec", 0),
            durationSec = s.optInt("durationSec", 0),
            volume = s.optInt("volume", 80),
            isMuted = s.optBoolean("isMuted", false),
            hasActiveTab = s.optBoolean("hasActiveTab", false),
            tabTitle = s.optString("tabTitle", ""),
            updatedAt = s.optLong("updatedAt", System.currentTimeMillis())
        )
    }

    fun sendCommand(type: String, value: Double? = null) {
        val cmd = JSONObject().apply {
            put("commandId", UUID.randomUUID().toString())
            put("type", type)
            put("issuedAt", System.currentTimeMillis())
            if (value != null) put("value", value)
        }
        val envelope = JSONObject().apply {
            put("type", "command")
            put("pairingCode", pairingCode)
            put("command", cmd)
        }
        webSocket?.send(envelope.toString())
    }
}
