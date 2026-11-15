package com.musicremote.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicremote.app.ui.theme.CrimsonAccent
import com.musicremote.app.ui.theme.ObsidianBackground
import com.musicremote.app.ui.theme.ObsidianSurface
import com.musicremote.app.ui.theme.TextPrimary
import com.musicremote.app.ui.theme.TextSecondary

@Composable
fun PairingScreen(
    initialRelayUrl: String,
    initialPairingCode: String,
    onPair: (relayUrl: String, pairingCode: String) -> Unit
) {
    var relayUrl by remember { mutableStateOf(initialRelayUrl) }
    var pairingCode by remember { mutableStateOf(initialPairingCode) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Music Remote",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Pair with your Windows laptop using the 6-character code shown in Music Remote Companion. Your Google password is never needed.",
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        OutlinedTextField(
            value = pairingCode,
            onValueChange = { pairingCode = it.uppercase().take(8) },
            label = { Text("Pairing Code") },
            placeholder = { Text("e.g. A7K9P2") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonAccent,
                unfocusedBorderColor = Color(0xFF27272A),
                focusedContainerColor = ObsidianSurface,
                unfocusedContainerColor = ObsidianSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = relayUrl,
            onValueChange = { relayUrl = it },
            label = { Text("Cloud Relay WSS URL") },
            placeholder = { Text("wss://your-relay.run.app/ws") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonAccent,
                unfocusedBorderColor = Color(0xFF27272A),
                focusedContainerColor = ObsidianSurface,
                unfocusedContainerColor = ObsidianSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onPair(relayUrl, pairingCode) },
            enabled = pairingCode.isNotBlank() && relayUrl.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent)
        ) {
            Text(
                text = "Connect to Windows Laptop",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
