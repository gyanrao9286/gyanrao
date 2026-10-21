package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.util.tr
import com.example.viewmodel.AudioTranscribeState
import com.example.viewmodel.LiveVoiceState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveVoiceConversationCard(
    liveVoiceState: LiveVoiceState,
    transcribeState: AudioTranscribeState,
    appLanguage: String,
    onStartLiveVoice: () -> Unit,
    onStopLiveVoice: () -> Unit,
    onSendLiveText: (String) -> Unit,
    onToggleAudioTranscription: () -> Unit,
    onApplyTranscribedText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var livePromptInput by rememberSaveable { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_voice_conversation_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (liveVoiceState is LiveVoiceState.Active) {
                                        Color(0xFF10B981)
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Live Voice API",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = tr(
                                    appLanguage,
                                    "Live Voice Conversation & Audio Transcribe",
                                    "लाइव वॉइस बातचीत और ऑडियो ट्रांसक्रिप्शन"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "gemini-3.8-live (Real-time Voice) • gemini-3.5-transcribe",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Live Conversation Controls (gemini-3.8-live)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (liveVoiceState is LiveVoiceState.Active || liveVoiceState is LiveVoiceState.Connecting) {
                        Button(
                            onClick = onStopLiveVoice,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stop_live_voice_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(appLanguage, "End Live Voice", "लाइव बातचीत बंद करें"))
                        }
                    } else {
                        Button(
                            onClick = onStartLiveVoice,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("start_live_voice_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tr(appLanguage, "Start Live Voice", "लाइव बातचीत शुरू करें"))
                        }
                    }

                    // Audio Transcription Button (gemini-3.5-transcribe)
                    val isRec = transcribeState is AudioTranscribeState.Recording
                    OutlinedButton(
                        onClick = onToggleAudioTranscription,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transcribe_audio_button")
                    ) {
                        Icon(
                            imageVector = if (isRec) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isRec) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRec) {
                                tr(appLanguage, "Stop & Transcribe", "रोकें और लिखें")
                            } else {
                                tr(appLanguage, "Transcribe Mic", "ऑडियो ट्रांसक्राइब")
                            }
                        )
                    }
                }

                // Live Session Status & Real-Time Transcripts
                when (liveVoiceState) {
                    is LiveVoiceState.Connecting -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text(
                                text = "Connecting to gemini-3.8-live real-time audio session…",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is LiveVoiceState.Active -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "🟢 ${liveVoiceState.statusText}",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                            if (liveVoiceState.userTranscript.isNotBlank()) {
                                Text(
                                    text = "You: ${liveVoiceState.userTranscript}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (liveVoiceState.modelTranscript.isNotBlank()) {
                                Text(
                                    text = "Aura Live: ${liveVoiceState.modelTranscript}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = livePromptInput,
                                    onValueChange = { livePromptInput = it },
                                    placeholder = { Text("Ask Live AI anything (CSIR Physics, English, News)…") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        if (livePromptInput.isNotBlank()) {
                                            onSendLiveText(livePromptInput)
                                            livePromptInput = ""
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send to Live Session",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    is LiveVoiceState.Error -> {
                        Text(
                            text = liveVoiceState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    LiveVoiceState.Idle -> {}
                }

                // Audio Transcription Status & Output (gemini-3.5-transcribe)
                AnimatedVisibility(visible = transcribeState !is AudioTranscribeState.Idle) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (transcribeState) {
                            is AudioTranscribeState.Recording -> {
                                Text(
                                    text = "🔴 Recording microphone audio… Tap 'Stop & Transcribe' when finished.",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            is AudioTranscribeState.Transcribing -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = "Transcribing audio with gemini-3.5-transcribe…",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            is AudioTranscribeState.Success -> {
                                Text(
                                    text = "Transcribed Audio (gemini-3.5-transcribe):",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "\"${transcribeState.text}\"",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    AssistChip(
                                        onClick = { onApplyTranscribedText(transcribeState.text) },
                                        label = { Text("Apply as Voice Command / Search") }
                                    )
                                }
                            }
                            is AudioTranscribeState.Error -> {
                                Text(
                                    text = transcribeState.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            AudioTranscribeState.Idle -> {}
                        }
                    }
                }
            }
        }
    }
}
