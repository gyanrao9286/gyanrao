package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantSettings
import com.example.util.tr

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSettingsScreen(
    settings: AssistantSettings,
    onApplyVoiceCommand: (String) -> Unit,
    onStartVoiceRecognition: ((String) -> Unit) -> Unit,
    onUpdateSettingsDirectly: (AssistantSettings) -> Unit,
    onSignOut: () -> Unit
) {
    var voiceInputText by remember { mutableStateOf("") }

    val presetVoiceCommands = listOf(
        "Change language to Hindi and English bilingual",
        "Change app language to Hindi (हिंदी भाषा चुनें)",
        "Turn on auto reply for emails and set tone to Concise",
        "Focus more on India news and read briefings aloud",
        "Set English difficulty to Advanced and daily goal to 30 minutes"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("voice_settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Language Switcher Card (Hindi / English / Bilingual)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = tr(
                            settings.appLanguage,
                            "App Language • भाषा बदलें (Hindi / English)",
                            "ऐप की भाषा बदलें (हिंदी / अंग्रेजी)"
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    LanguageSwitcherStrip(
                        currentLanguage = settings.appLanguage,
                        onSelectLanguage = { newLang ->
                            onUpdateSettingsDirectly(settings.copy(appLanguage = newLang))
                        }
                    )
                }
            }
        }

        // 1. Voice Command Customization Hero Console
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = "Voice Customizer",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Text(
                            text = tr(
                                settings.appLanguage,
                                "Voice-Commanded Settings",
                                "वॉइस कमांड सेटिंग्स"
                            ),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tr(
                                settings.appLanguage,
                                "Speak naturally to change language (Hindi/English), customize auto-replies, hourly news focus, or English goals.",
                                "बोलकर भाषा (हिंदी/अंग्रेजी), ऑटो-रिप्लाई, हर घंटे के समाचार या अंग्रेजी लक्ष्य आसानी से बदलें।"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Button(
                            onClick = {
                                onStartVoiceRecognition { spoken ->
                                    voiceInputText = spoken
                                    onApplyVoiceCommand(spoken)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("voice_command_mic_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(settings.appLanguage, "Tap & Speak Settings Command", "बोलकर सेटिंग्स बदलें"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = voiceInputText,
                            onValueChange = { voiceInputText = it },
                            label = { Text("Or type / edit voice command here…") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("voice_command_text_input"),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (voiceInputText.isNotBlank()) {
                                    onApplyVoiceCommand(voiceInputText)
                                }
                            },
                            enabled = voiceInputText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("apply_voice_command_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Command with AI")
                        }

                        Text(
                            text = "Quick Voice Command Presets (1-Tap):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            presetVoiceCommands.forEach { cmd ->
                                AssistChip(
                                    onClick = {
                                        voiceInputText = cmd
                                        onApplyVoiceCommand(cmd)
                                    },
                                    label = { Text("\"$cmd\"") }
                                )
                            }
                        }

                        if (settings.lastVoiceCommand.isNotBlank()) {
                            Text(
                                text = "Last Voice Command: \"${settings.lastVoiceCommand}\"",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.align(Alignment.Start)
                            )
                        }
                    }
                }
            }
        }

        // 2. Manual & Live Synced Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Background Message & Email Replies",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SettingToggleRow(
                        title = "Auto-Reply to Background Messages",
                        subtitle = "Automatically draft and send smart replies for incoming messages",
                        checked = settings.autoReplyMessages,
                        onCheckedChange = {
                            onUpdateSettingsDirectly(settings.copy(autoReplyMessages = it))
                        }
                    )

                    SettingToggleRow(
                        title = "Auto-Check & Reply to Emails",
                        subtitle = "Monitor incoming emails and send AI replies matching your tone",
                        checked = settings.autoReplyEmails,
                        onCheckedChange = {
                            onUpdateSettingsDirectly(settings.copy(autoReplyEmails = it))
                        }
                    )

                    Text(
                        text = "AI Reply Tone",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("EXECUTIVE", "FRIENDLY", "CONCISE", "FORMAL").forEach { tone ->
                            FilterChip(
                                selected = settings.replyTone == tone,
                                onClick = {
                                    onUpdateSettingsDirectly(settings.copy(replyTone = tone))
                                },
                                label = { Text(tone) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Hourly World & India News Briefings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    SettingToggleRow(
                        title = "Hourly Briefing Updates",
                        subtitle = "Receive a concise summary of India & World news every hour",
                        checked = settings.hourlyBriefingEnabled,
                        onCheckedChange = {
                            onUpdateSettingsDirectly(settings.copy(hourlyBriefingEnabled = it))
                        }
                    )

                    Text(
                        text = "India vs. World Coverage Balance",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HIGH", "BALANCED", "GLOBAL_FIRST").forEach { focus ->
                            FilterChip(
                                selected = settings.indiaFocusLevel == focus,
                                onClick = {
                                    onUpdateSettingsDirectly(settings.copy(indiaFocusLevel = focus))
                                },
                                label = { Text(focus.replace('_', ' ')) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Daily English Learning & Voice Readback",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Text(
                        text = "English Coaching Difficulty",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BEGINNER", "INTERMEDIATE", "ADVANCED", "BUSINESS_FLUENT").forEach { level ->
                            FilterChip(
                                selected = settings.englishDifficulty == level,
                                onClick = {
                                    onUpdateSettingsDirectly(settings.copy(englishDifficulty = level))
                                },
                                label = { Text(level.replace('_', ' ')) }
                            )
                        }
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Daily English Learning Goal", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${settings.dailyGoalMinutes} mins/day",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.dailyGoalMinutes.toFloat(),
                            onValueChange = { newVal ->
                                onUpdateSettingsDirectly(
                                    settings.copy(dailyGoalMinutes = newVal.toLong().coerceIn(5L, 180L))
                                )
                            },
                            valueRange = 5f..90f,
                            steps = 16
                        )
                    }

                    SettingToggleRow(
                        title = "Voice Readback (TTS)",
                        subtitle = "Speak confirmations, briefings, and physics solutions aloud",
                        checked = settings.voiceReadbackEnabled,
                        onCheckedChange = {
                            onUpdateSettingsDirectly(settings.copy(voiceReadbackEnabled = it))
                        }
                    )
                }
            }
        }

        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sign_out_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Google Account")
            }
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
