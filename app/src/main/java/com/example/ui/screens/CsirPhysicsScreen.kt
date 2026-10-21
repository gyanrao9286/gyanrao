package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantSettings
import com.example.data.model.DailyThoughtPoster
import com.example.data.model.PhysicsQuizItem
import com.example.util.openGoogleSearch
import com.example.util.tr
import com.example.viewmodel.GroundedQueryState
import com.example.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CsirPhysicsScreen(
    physicsQuizzesState: UiState<List<PhysicsQuizItem>>,
    thoughtPostersState: UiState<List<DailyThoughtPoster>>,
    settings: AssistantSettings,
    groundedQueryState: GroundedQueryState,
    onGeneratePhysicsQuiz: (topic: String, partType: String) -> Unit,
    onSelectQuizOption: (PhysicsQuizItem, Int) -> Unit,
    onGenerateThoughtPoster: (theme: String, palette: String) -> Unit,
    onCyclePosterPalette: (DailyThoughtPoster) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onRunGoogleSearchGrounding: (String) -> Unit,
    onRunGoogleMapsGrounding: (String) -> Unit,
    onClearGroundedResult: () -> Unit,
    onSpeakText: (String) -> Unit,
    onStartVoiceSearch: ((String) -> Unit) -> Unit
) {
    val quizzes = (physicsQuizzesState as? UiState.Success)?.data.orEmpty()
    val posters = (thoughtPostersState as? UiState.Success)?.data.orEmpty()

    var selectedTopic by rememberSaveable { mutableStateOf("Quantum Mechanics") }
    var selectedPart by rememberSaveable { mutableStateOf("PART_C") }

    val csirTopics = listOf(
        "Quantum Mechanics",
        "Electromagnetic Theory",
        "Statistical Mechanics",
        "Classical Mechanics",
        "Solid State Physics",
        "Mathematical Physics",
        "Atomic & Molecular Physics",
        "Nuclear & Particle Physics",
        "Electronics & Experimental"
    )

    val attemptedCount = quizzes.count { it.selectedIndex >= 0 }
    val correctCount = quizzes.count { it.selectedIndex >= 0 && it.selectedIndex == it.correctIndex }
    val earnedMarks = quizzes.filter { it.selectedIndex >= 0 && it.selectedIndex == it.correctIndex }
        .sumOf { if (it.partType == "PART_C") 5.0 else 3.5 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("csir_physics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Instant Hindi / English / Bilingual Language Switcher
        item {
            LanguageSwitcherStrip(
                currentLanguage = settings.appLanguage,
                onSelectLanguage = onSelectLanguage
            )
        }

        // 2. Google Search & Maps Grounding Bar
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

        // 3. CSIR NET Physical Sciences Exam Hero & Generator
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
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.75f)
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
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Science,
                                        contentDescription = "CSIR NET Physics",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Column {
                                    Text(
                                        text = tr(
                                            settings.appLanguage,
                                            "CSIR NET Physical Sciences Quiz",
                                            "CSIR NET भौतिक विज्ञान (Physics) प्रश्नोत्तरी"
                                        ),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Solved: $correctCount/$attemptedCount • Score: +$earnedMarks Marks",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        // Part B vs Part C Selector
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr(settings.appLanguage, "Exam Section:", "परीक्षा भाग:"),
                                style = MaterialTheme.typography.labelLarge
                            )
                            FilterChip(
                                selected = selectedPart == "PART_B",
                                onClick = { selectedPart = "PART_B" },
                                label = { Text("Part B (3.5 Marks)") }
                            )
                            FilterChip(
                                selected = selectedPart == "PART_C",
                                onClick = { selectedPart = "PART_C" },
                                label = { Text("Part C (5.0 Marks)") }
                            )
                        }

                        Text(
                            text = tr(
                                settings.appLanguage,
                                "Select CSIR Physics Subject Topic:",
                                "CSIR भौतिकी विषय चुनें:"
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            csirTopics.forEach { topic ->
                                FilterChip(
                                    selected = selectedTopic == topic,
                                    onClick = { selectedTopic = topic },
                                    label = { Text(topic, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }

                        Button(
                            onClick = { onGeneratePhysicsQuiz(selectedTopic, selectedPart) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("generate_csir_physics_quiz_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr(
                                    settings.appLanguage,
                                    "Generate New CSIR Physics Question ($selectedPart)",
                                    "नया CSIR भौतिकी प्रश्न और हल बनाएं ($selectedPart)"
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. CSIR Physics Quiz Question Cards with Bilingual Questions, Options & Step-by-Step Answers
        if (physicsQuizzesState is UiState.Loading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else {
            items(quizzes, key = { it.id }) { quizItem ->
                CsirPhysicsQuestionCard(
                    item = quizItem,
                    appLanguage = settings.appLanguage,
                    onSelectOption = { optIndex -> onSelectQuizOption(quizItem, optIndex) },
                    onSpeakText = onSpeakText
                )
            }
        }

        // 5. Daily Thought / Quote of the Day Poster Section (Hindi + English)
        item {
            Text(
                text = tr(
                    settings.appLanguage,
                    "Daily Thought & Quote Poster (Hindi & English)",
                    "आज का सुविचार पोस्टर (हिंदी और अंग्रेजी)"
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        item {
            DailyThoughtPosterCard(
                poster = posters.firstOrNull(),
                appLanguage = settings.appLanguage,
                onGenerateNewPoster = {
                    onGenerateThoughtPoster(
                        "CSIR Exam Perseverance, Scientific Curiosity & Life Wisdom",
                        "SAFFRON_GOLD"
                    )
                },
                onCyclePalette = onCyclePosterPalette,
                onSpeakPoster = onSpeakText
            )
        }
    }
}

@Composable
private fun CsirPhysicsQuestionCard(
    item: PhysicsQuizItem,
    appLanguage: String,
    onSelectOption: (Int) -> Unit,
    onSpeakText: (String) -> Unit
) {
    val context = LocalContext.current
    val isAnswered = item.selectedIndex >= 0
    val isCorrect = isAnswered && item.selectedIndex == item.correctIndex
    var showAnswerOverride by remember(item.id) { mutableStateOf(false) }
    val showExplanation = isAnswered || showAnswerOverride

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("physics_quiz_card_${item.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Topic & Section Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "${item.partType.replace('_', ' ')} • ${if (item.partType == "PART_C") "5.0M" else "3.5M"}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    )
                    Text(
                        text = item.topic,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            val readText = when (appLanguage) {
                                "HINDI" -> "${item.questionHi}. ${item.explanationHi}"
                                else -> "${item.questionEn}. ${item.explanationEn}"
                            }
                            onSpeakText(readText)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Read Physics Question Aloud",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            openGoogleSearch(context, "CSIR NET Physics ${item.topic} ${item.questionEn.take(70)}")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search this Physics Problem on Google",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Question Stem (English / Hindi / Both)
            if (appLanguage == "ENGLISH" || appLanguage == "BILINGUAL") {
                Text(
                    text = "Q (EN): ${item.questionEn}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (appLanguage == "HINDI" || appLanguage == "BILINGUAL") {
                Text(
                    text = "प्रश्न (HI): ${item.questionHi}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (appLanguage == "HINDI") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 4 Interactive Option Rows (A, B, C, D)
            val optionLabels = listOf("A", "B", "C", "D")
            for (idx in 0 until 4) {
                val optEn = item.optionsEn.getOrNull(idx).orEmpty()
                val optHi = item.optionsHi.getOrNull(idx).orEmpty()
                val optText = when (appLanguage) {
                    "ENGLISH" -> optEn
                    "HINDI" -> optHi
                    else -> if (optEn == optHi) optEn else "$optEn  /  $optHi"
                }

                val isThisSelected = item.selectedIndex == idx.toLong()
                val isThisCorrect = item.correctIndex == idx.toLong()

                val borderColor = when {
                    showExplanation && isThisCorrect -> Color(0xFF10B981)
                    isAnswered && isThisSelected && !isThisCorrect -> MaterialTheme.colorScheme.error
                    isThisSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                }

                val bgColor = when {
                    showExplanation && isThisCorrect -> Color(0xFF10B981).copy(alpha = 0.16f)
                    isAnswered && isThisSelected && !isThisCorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(bgColor)
                        .border(1.5.dp, borderColor, MaterialTheme.shapes.medium)
                        .clickable { onSelectOption(idx) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("quiz_${item.id}_option_$idx"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(borderColor.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = optionLabels[idx],
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = optText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (showExplanation && isThisCorrect) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Correct Option",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (isAnswered && isThisSelected && !isThisCorrect) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Incorrect Option",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Toggle Solution Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showAnswerOverride = !showAnswerOverride },
                    modifier = Modifier.testTag("toggle_answer_${item.id}")
                ) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showExplanation) {
                            tr(appLanguage, "Hide Answer & Solution", "उत्तर और व्याख्या छुपाएं")
                        } else {
                            tr(appLanguage, "Show Answer & CSIR Solution", "उत्तर और विस्तृत हल देखें")
                        }
                    )
                }

                if (isAnswered) {
                    Text(
                        text = if (isCorrect) "✅ Correct / सही उत्तर" else "❌ Review Solution / हल देखें",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isCorrect) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Detailed Answer & Formula Derivation Box (English + Hindi)
            AnimatedVisibility(visible = showExplanation) {
                val correctOptionLetter = optionLabels.getOrElse(item.correctIndex.toInt()) { "A" }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                            MaterialTheme.shapes.medium
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "✔ Correct Answer / सही विकल्प: Option ($correctOptionLetter)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold
                    )
                    if (appLanguage == "ENGLISH" || appLanguage == "BILINGUAL") {
                        Text(
                            text = "Step-by-Step Solution (EN): ${item.explanationEn}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (appLanguage == "HINDI" || appLanguage == "BILINGUAL") {
                        Text(
                            text = "विस्तृत व्याख्या (HI): ${item.explanationHi}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
