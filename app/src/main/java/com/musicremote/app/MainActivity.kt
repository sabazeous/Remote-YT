package com.musicremote.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.musicremote.app.ui.PairingScreen
import com.musicremote.app.ui.PlayerScreen
import com.musicremote.app.ui.theme.MusicRemoteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicRemoteTheme {
                val vm: MainViewModel = viewModel()
                val isPaired by vm.isPaired.collectAsState()
                val relayUrl by vm.relayUrl.collectAsState()
                val pairingCode by vm.pairingCode.collectAsState()
                val playbackState by vm.playbackState.collectAsState()
                val sessionStatus by vm.sessionStatus.collectAsState()
                val connectionState by vm.connectionState.collectAsState()
                val positionSec by vm.interpolatedPositionSec.collectAsState()
                val errorMessage by vm.errorMessage.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                ) {
                    if (!isPaired) {
                        PairingScreen(
                            initialRelayUrl = relayUrl,
                            initialPairingCode = pairingCode,
                            onPair = { url, code -> vm.pair(url, code) }
                        )
                    } else {
                        PlayerScreen(
                            playbackState = playbackState,
                            sessionStatus = sessionStatus,
                            connectionState = connectionState,
                            positionSec = positionSec,
                            pairingCode = pairingCode,
                            errorMessage = errorMessage,
                            onTogglePlayPause = vm::togglePlayPause,
                            onNext = vm::nextTrack,
                            onPrevious = vm::previousTrack,
                            onSeek = vm::seekTo,
                            onVolumeChange = vm::setVolume,
                            onReconnect = vm::reconnect,
                            onUnpair = vm::unpair
                        )
                    }
                }
            }
        }
    }
}
