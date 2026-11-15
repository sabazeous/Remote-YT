package com.musicremote.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.LaptopWindows
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.musicremote.app.model.PlaybackState
import com.musicremote.app.model.RelayConnectionState
import com.musicremote.app.model.SessionStatus
import com.musicremote.app.ui.theme.CrimsonAccent
import com.musicremote.app.ui.theme.ObsidianBackground
import com.musicremote.app.ui.theme.ObsidianBorder
import com.musicremote.app.ui.theme.ObsidianSurface
import com.musicremote.app.ui.theme.TextPrimary
import com.musicremote.app.ui.theme.TextSecondary

@Composable
fun PlayerScreen(
    playbackState: PlaybackState,
    sessionStatus: SessionStatus,
    connectionState: RelayConnectionState,
    positionSec: Int,
    pairingCode: String,
    errorMessage: String?,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onReconnect: () -> Unit,
    onUnpair: () -> Unit
) {
    val context = LocalContext.current

    var isSeeking by remember { mutableStateOf(false) }
    var seekDraft by remember { mutableFloatStateOf(0f) }

    var isAdjustingVolume by remember { mutableStateOf(false) }
    var volumeDraft by remember { mutableFloatStateOf(playbackState.volume.toFloat()) }

    val activePosition = if (isSeeking) seekDraft.toInt() else positionSec
    val duration = playbackState.durationSec.coerceAtLeast(1)
    val activeVolume = if (isAdjustingVolume) volumeDraft.toInt() else playbackState.volume

    val statusLabel = when {
        connectionState == RelayConnectionState.CONNECTING -> "Connecting to relay..."
        connectionState == RelayConnectionState.DISCONNECTED -> "Relay offline · Reconnecting"
        !sessionStatus.companionOnline -> "Laptop offline · Code $pairingCode"
        !playbackState.hasActiveTab -> "${sessionStatus.companionName} · Open YouTube Music"
        playbackState.isPlaying -> "${sessionStatus.companionName} · Playing"
        else -> "${sessionStatus.companionName} · Paused"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF181016), ObsidianBackground, ObsidianBackground)
                )
            )
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top device status bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LaptopWindows,
                    contentDescription = "Connected laptop",
                    tint = if (sessionStatus.companionOnline) Color(0xFF34D399) else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusLabel,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(onClick = onReconnect) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Reconnect",
                        tint = TextSecondary
                    )
                }
                IconButton(onClick = onUnpair) {
                    Icon(
                        imageVector = Icons.Rounded.LinkOff,
                        contentDescription = "Unpair device",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Prominent centered 1:1 album artwork with smooth crossfade & Coil caching
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = playbackState.artworkUrl,
                animationSpec = tween(durationMillis = 420),
                label = "artwork_crossfade"
            ) { artworkUrl ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianSurface)
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (artworkUrl.isBlank()) {
                        MusicNotePlaceholder()
                    } else {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(artworkUrl)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .build(),
                            contentDescription = "${playbackState.title} album artwork",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            loading = { MusicNotePlaceholder() },
                            error = { MusicNotePlaceholder() }
                        )
                    }
                }
            }
        }

        // Track metadata + controls
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = playbackState.title.ifBlank { "Nothing playing" },
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            val subtitle = listOfNotNull(
                playbackState.artist.takeIf { it.isNotBlank() },
                playbackState.album.takeIf { it.isNotBlank() }
            ).joinToString(" · ")

            Text(
                text = subtitle.ifBlank { "Open music.youtube.com on your laptop" },
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = CrimsonAccent,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playback progress bar
            Slider(
                value = activePosition.coerceIn(0, duration).toFloat(),
                onValueChange = {
                    isSeeking = true
                    seekDraft = it
                },
                onValueChangeFinished = {
                    isSeeking = false
                    onSeek(seekDraft.toInt())
                },
                valueRange = 0f..duration.toFloat(),
                enabled = playbackState.durationSec > 0,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = CrimsonAccent,
                    inactiveTrackColor = Color(0xFF27272A)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(activePosition),
                    color = TextSecondary,
                    fontSize = 12.sp,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                Text(
                    text = if (playbackState.durationSec > 0) formatTime(playbackState.durationSec) else "--:--",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Transport controls (Previous / Play-Pause / Next)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous track",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                FilledIconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier.size(72.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = CrimsonAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next track",
                        tint = TextPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volume slider (0-100%)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onVolumeChange(if (activeVolume == 0) 80 else 0) },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = when {
                            activeVolume == 0 -> Icons.AutoMirrored.Rounded.VolumeOff
                            activeVolume < 50 -> Icons.AutoMirrored.Rounded.VolumeDown
                            else -> Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = "Volume",
                        tint = TextSecondary
                    )
                }

                Slider(
                    value = activeVolume.toFloat(),
                    onValueChange = {
                        isAdjustingVolume = true
                        volumeDraft = it
                    },
                    onValueChangeFinished = {
                        isAdjustingVolume = false
                        onVolumeChange(volumeDraft.toInt())
                    },
                    valueRange = 0f..100f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = TextPrimary,
                        inactiveTrackColor = Color(0xFF27272A)
                    )
                )

                Text(
                    text = "${activeVolume}%",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    style = TextStyle(fontFeatureSettings = "tnum"),
                    modifier = Modifier.width(38.dp)
                )
            }
        }
    }
}

@Composable
private fun MusicNotePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1C1C22), Color(0xFF121215))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = "Album artwork unavailable",
            tint = Color(0xFF52525B),
            modifier = Modifier.size(72.dp)
        )
    }
}

private fun formatTime(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%d:%02d".format(mins, secs)
}
