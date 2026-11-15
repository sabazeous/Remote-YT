package com.musicremote.app.model

data class PlaybackState(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val artworkUrl: String = "",
    val isPlaying: Boolean = false,
    val positionSec: Int = 0,
    val durationSec: Int = 0,
    val volume: Int = 80,
    val isMuted: Boolean = false,
    val hasActiveTab: Boolean = false,
    val tabTitle: String = "",
    val updatedAt: Long = 0L
)

data class SessionStatus(
    val pairingCode: String = "",
    val companionOnline: Boolean = false,
    val companionName: String = "Windows Laptop",
    val chromeConnected: Boolean = false,
    val remoteCount: Int = 0
)

enum class RelayConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}
