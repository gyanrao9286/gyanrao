package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantSettings
import com.example.data.model.DailyThoughtPoster
import com.example.data.model.EnglishTask
import com.example.data.model.InboxItem
import com.example.data.model.NewsBriefing
import com.example.data.model.PhysicsQuizItem
import com.example.util.tr
import com.example.viewmodel.AudioTranscribeState
import com.example.viewmodel.GroundedQueryState
import com.example.viewmodel.LiveVoiceState
import com.example.viewmodel.UiState

@Composable
fun DashboardScreen(
    inboxState: UiState<List<InboxItem>>,
    briefingsState: UiState<List<NewsBriefing>>,
    englishTasksState: UiState<List<EnglishTask>>,
    physicsQuizzesState: UiState<List<PhysicsQuizItem>>,
    thoughtPostersState: UiState<List<DailyThoughtPoster>>,
    settings: AssistantSettings,
    isBackgroundRunning: Boolean,
    minutesToNextHour: Int,
    liveVoiceState: LiveVoiceState,
    groundedQueryState: GroundedQueryState,
    transcribeState: AudioTranscribeState,
    onToggleBackgroundAgent: (Boolean) -> Unit,
    onScanInboxNow: () -> Unit,
    onGenerateBriefingNow: () -> Unit,
    onGenerateNewPoster: () -> Unit,
    onCyclePosterPalette: (DailyThoughtPoster) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onStartLiveVoice: () -> Unit,
    onStopLiveVoice: () -> Unit,
    onSendLiveText: (String) -> Unit,
    onToggleAudioTranscription: () -> Unit,
    onApplyTranscribedText: (String) -> Unit,
    onRunGoogleSearchGrounding: (String) -> Unit,
    onRunGoogleMapsGrounding: (String) -> Unit,
    onClearGroundedResult: () -> Unit,
    onSpeakText: (String) -> Unit,
    onStartVoiceSearch: ((String) -> Unit) -> Unit,
    onOpenVoiceCommandModal: () -> Unit,
    onNavigateToTab: (Int) -> Unit
) {
    val inboxItems = (inboxState as? UiState.Success)?.data.orEmpty()
    val briefings = (briefingsState as? UiState.Success)?.data.orEmpty()
    val englishTasks = (englishTasksState as? UiState.Success)?.data.orEmpty()
    val physicsQuizzes = (physicsQuizzesState as? UiState.Success)?.data.orEmpty()
    val thoughtPosters = (thoughtPostersState as? UiState.Success)?.data.orEmpty()

    val autoRepliedCount = inboxItems.count { it.status == "AUTO_REPLIED" }
    val pendingInboxCount = inboxItems.count { it.status == "PENDING" || it.status == "DRAFT_READY" }
    val completedEnglishCount = englishTasks.count { it.completed }
    val solvedPhysicsCount = physicsQuizzes.count { it.selectedIndex >= 0 && it.selectedIndex == it.correctIndex }

    val productivityProgress = if (inboxItems.isEmpty() && englishTasks.isEmpty() && physicsQuizzes.isEmpty()) {
        0.5f
    } else {
        val totalItems = (inboxItems.size + englishTasks.size + physicsQuizzes.size).coerceAtLeast(1)
        val doneItems = autoRepliedCount + completedEnglishCount + solvedPhysicsCount
        (doneItems.toFloat() / totalItems.toFloat()).coerceIn(0.2f, 1f)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Bilingual Hindi / English Language Switcher Strip
        item {
            LanguageSwitcherStrip(
                currentLanguage = settings.appLanguage,
                onSelectLanguage = onSelectLanguage
            )
        }

        // 2. Live Voice Conversation (gemini-3.8-live) & Audio Transcription (gemini-3.5-transcribe)
        item {
            LiveVoiceConversationCard(
                liveVoiceState = liveVoiceState,
                transcribeState = transcribeState,
                appLanguage = settings.appLanguage,
                onStartLiveVoice = onStartLiveVoice,
                onStopLiveVoice = onStopLiveVoice,
                onSendLiveText = onSendLiveText,
                onToggleAudioTranscription = onToggleAudioTranscription,
                onApplyTranscribedText = onApplyTranscribedText
            )
        }

        // 3. Google Search & Maps Grounding Bar (gemini-3.8-flash + Tool.googleSearch / Tool.googleMaps)
        item {
            GoogleSearchBarCard(
                appLanguage = settings.appLanguage,
                groundedQueryState = groundedQueryState,
                onRunGoogleSearchGrounding = onRunGoogleSearchGrounding,
                onRunGoogleMapsGrounding = onRunGoogleMapsGrounding,
                onClearGroundedResult = onClearGroundedResult,
                onStartVoiceSearch = onStartVoiceSearch
            )
        }

        // 3. Daily Thought / Quote of the Day Poster (Both Hindi & English)
        item {
            DailyThoughtPosterCard(
                poster = thoughtPosters.firstOrNull(),
                appLanguage = settings.appLanguage,
                onGenerateNewPoster = onGenerateNewPoster,
                onCyclePalette = onCyclePosterPalette,
                onSpeakPoster = onSpeakText
            )
        }

        // 4. CSIR NET Physical Sciences Quiz Snapshot Card
        item {
            val latestQuiz = physicsQuizzes.firstOrNull()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Science,
                                contentDescription = "CSIR Physics Quiz",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    text = tr(
                                        settings.appLanguage,
                                        "CSIR NET Physics Quiz & Answers",
                                        "CSIR NET भौतिकी प्रश्नोत्तरी और हल"
                                    ),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Part B & Part C • Bilingual Hindi & English Solutions",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (latestQuiz != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${latestQuiz.partType.replace('_', ' ')} • ${latestQuiz.topic}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (settings.appLanguage == "HINDI") latestQuiz.questionHi else latestQuiz.questionEn,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = { onNavigateToTab(3) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_open_physics_quiz_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text(
                            tr(
                                settings.appLanguage,
                                "Open CSIR Physics Quiz & Step-by-Step Answers (${physicsQuizzes.size})",
                                "CSIR भौतिकी प्रश्न और उत्तर खोलें (${physicsQuizzes.size})"
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 5. Hero Executive Command Banner
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
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f),
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Aura Executive Agent",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = tr(
                                            settings.appLanguage,
                                            "Executive & Study Pulse",
                                            "दैनिक कार्य और अध्ययन डैशबोर्ड"
                                        ),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isBackgroundRunning) {
                                            "Background Agent Active • Next briefing in ${minutesToNextHour}m"
                                        } else {
                                            "Background Agent Paused"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            Switch(
                                checked = isBackgroundRunning,
                                onCheckedChange = onToggleBackgroundAgent,
                                modifier = Modifier.testTag("background_agent_switch")
                            )
                        }

                        // Daily Productivity & Learning Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = tr(
                                        settings.appLanguage,
                                        "Daily Productivity, Physics & English Progress",
                                        "दैनिक उत्पादकता, भौतिकी और अंग्रेजी प्रगति"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${(productivityProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { productivityProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                            )
                        }

                        // 3 KPI Summary Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.MarkEmailRead,
                                value = "$autoRepliedCount",
                                label = "Auto-Replied",
                                subLabel = "$pendingInboxCount in queue",
                                onClick = { onNavigateToTab(1) }
                            )
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Public,
                                value = "${briefings.size}",
                                label = "India/World",
                                subLabel = "Focus: ${settings.indiaFocusLevel}",
                                onClick = { onNavigateToTab(2) }
                            )
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Science,
                                value = "$solvedPhysicsCount/${physicsQuizzes.size}",
                                label = "CSIR Physics",
                                subLabel = "Part B & C",
                                onClick = { onNavigateToTab(3) }
                            )
                        }

                        // Quick Voice Command Bar inside Hero
                        FilledTonalButton(
                            onClick = onOpenVoiceCommandModal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_voice_command_button"),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Command",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    settings.appLanguage,
                                    "Voice Command Settings (Tone: ${settings.replyTone})",
                                    "वॉइस कमांड सेटिंग्स (भाषा: ${settings.appLanguage})"
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 6. Hourly India & World News Briefing Snapshot
        item {
            val latestBriefing = briefings.firstOrNull()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = "Hourly Briefing",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Column {
                                Text(
                                    text = tr(
                                        settings.appLanguage,
                                        "Hourly India & World Brief",
                                        "हर घंटे भारत और विश्व समाचार संक्षेप"
                                    ),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = latestBriefing?.hourLabel ?: "Updated hourly • India & Global",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row {
                            if (latestBriefing != null) {
                                IconButton(
                                    onClick = {
                                        onSpeakText(
                                            "India update: ${latestBriefing.indiaHeadline}. ${latestBriefing.indiaSummary}. World update: ${latestBriefing.worldHeadline}. ${latestBriefing.worldSummary}."
                                        )
                                    },
                                    modifier = Modifier.testTag("dashboard_listen_briefing_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Read briefing aloud",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(
                                onClick = onGenerateBriefingNow,
                                modifier = Modifier.testTag("dashboard_refresh_briefing_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh hourly briefing",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }

                    if (latestBriefing != null) {
                        BriefingHighlightBlock(
                            badge = "INDIA PULSE • भारत",
                            headline = latestBriefing.indiaHeadline,
                            summary = latestBriefing.indiaSummary,
                            isIndia = true
                        )
                        BriefingHighlightBlock(
                            badge = "WORLD PULSE • विश्व",
                            headline = latestBriefing.worldHeadline,
                            summary = latestBriefing.worldSummary,
                            isIndia = false
                        )
                        Text(
                            text = "Takeaway: ${latestBriefing.keyTakeaway}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedButton(
                        onClick = { onNavigateToTab(2) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(tr(settings.appLanguage, "Open Full Hourly Briefing Feed", "सभी समाचार संक्षेप देखें"))
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 7. Background Message & Email Auto-Reply Center Snapshot
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MarkEmailRead,
                            contentDescription = "Background Replies",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = tr(
                                    settings.appLanguage,
                                    "Background Message & Email Replies",
                                    "बैकग्राउंड मैसेज और ईमेल ऑटो-रिप्लाई"
                                ),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                text = "Emails: ${if (settings.autoReplyEmails) "Auto-Reply ON" else "Draft Only"} • Messages: ${if (settings.autoReplyMessages) "Auto-Reply ON" else "Draft Only"}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    inboxItems.take(2).forEach { item ->
                        MiniInboxRow(item = item, onClick = { onNavigateToTab(1) })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onScanInboxNow,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dashboard_scan_inbox_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check & Auto-Reply")
                        }
                        OutlinedButton(
                            onClick = { onNavigateToTab(1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Queue (${inboxItems.size})")
                        }
                    }
                }
            }
        }

        // 8. Daily English Learning Mission Snapshot
        item {
            val todayTask = englishTasks.firstOrNull { !it.completed } ?: englishTasks.firstOrNull()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "English Learning",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column {
                                Text(
                                    text = tr(
                                        settings.appLanguage,
                                        "Daily English Mastery Task",
                                        "दैनिक अंग्रेजी सीखने का अभ्यास"
                                    ),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Level: ${settings.englishDifficulty.replace('_', ' ')} • Goal: ${settings.dailyGoalMinutes} min/day",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (todayTask?.completed == true) {
                            AssistChip(
                                onClick = { onNavigateToTab(4) },
                                label = { Text("Done • ${todayTask.masteryScore}%") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    if (todayTask != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${todayTask.category} • ${todayTask.dayTopic}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "\"${todayTask.targetPhrase}\"",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = todayTask.meaningAndNuance,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = { onNavigateToTab(4) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_open_english_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        Text(
                            tr(
                                settings.appLanguage,
                                "Open Daily English Coach",
                                "अंग्रेजी अभ्यास कोच खोलें"
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    subLabel: String,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.55f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BriefingHighlightBlock(
    badge: String,
    headline: String,
    summary: String,
    isIndia: Boolean
) {
    val accent = if (isIndia) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = badge,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = headline,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MiniInboxRow(
    item: InboxItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.channel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "• ${item.sender}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = item.subject,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item.aiReplyDraft.isNotBlank()) {
                Text(
                    text = "AI Reply: ${item.aiReplyDraft}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        AssistChip(
            onClick = onClick,
            label = {
                Text(
                    text = item.status.replace('_', ' '),
                    style = MaterialTheme.typography.labelMedium
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (item.status == "AUTO_REPLIED") {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                }
            )
        )
    }
}
