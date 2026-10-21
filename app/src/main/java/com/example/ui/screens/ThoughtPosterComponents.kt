package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyThoughtPoster
import com.example.util.openGoogleSearch
import com.example.util.shareThoughtPoster
import com.example.util.tr
import com.example.viewmodel.GroundedQueryState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageSwitcherStrip(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Language Switcher",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Language / भाषा:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val modes = listOf(
                "BILINGUAL" to "हिंदी + EN",
                "HINDI" to "हिंदी",
                "ENGLISH" to "English"
            )
            modes.forEach { (code, label) ->
                FilterChip(
                    selected = currentLanguage == code,
                    onClick = { onSelectLanguage(code) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.testTag("lang_chip_$code")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GoogleSearchBarCard(
    appLanguage: String,
    groundedQueryState: GroundedQueryState = GroundedQueryState.Idle,
    onRunGoogleSearchGrounding: ((String) -> Unit)? = null,
    onRunGoogleMapsGrounding: ((String) -> Unit)? = null,
    onClearGroundedResult: (() -> Unit)? = null,
    onStartVoiceSearch: (((String) -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val quickQueries = listOf(
        "CSIR NET Physical Sciences Cut-off & Syllabus",
        "Top CSIR Physics Research Institutes & Exam Centers in India",
        "India Science & Technology News Today",
        "Quantum Mechanics Commutator Relations"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("google_search_bar_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Google Search & Maps Grounding",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = tr(
                            appLanguage,
                            "Google Search & Maps Grounding Hub",
                            "गूगल सर्च और मैप्स ग्राउंडिंग बार"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = tr(
                            appLanguage,
                            "Powered by gemini-3.8-flash (Google Search + Google Maps Grounding)",
                            "CSIR भौतिकी, समाचार और स्थान (Google Search व Maps) खोजें"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        tr(
                            appLanguage,
                            "Ask Google Search or Maps (e.g. CSIR NPL Delhi, Quantum formula)…",
                            "Google Search या Maps पर कुछ भी खोजें…"
                        )
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (onStartVoiceSearch != null) {
                        IconButton(
                            onClick = {
                                onStartVoiceSearch { spoken ->
                                    searchQuery = spoken
                                    if (onRunGoogleSearchGrounding != null) {
                                        onRunGoogleSearchGrounding(spoken)
                                    } else {
                                        openGoogleSearch(context, spoken)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("google_voice_search_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice Google Search",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (onRunGoogleSearchGrounding != null && searchQuery.isNotBlank()) {
                            onRunGoogleSearchGrounding(searchQuery)
                        } else {
                            openGoogleSearch(context, searchQuery)
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("google_search_input")
            )

            // Action Buttons: AI Google Search Grounding, AI Google Maps Grounding, and Web Browser Search
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val q = searchQuery.ifBlank { "Latest CSIR NET Physical Sciences updates & India science news" }
                        searchQuery = q
                        if (onRunGoogleSearchGrounding != null) {
                            onRunGoogleSearchGrounding(q)
                        } else {
                            openGoogleSearch(context, q)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("google_search_grounding_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr(appLanguage, "AI Search", "AI सर्च"), maxLines = 1)
                }

                FilledTonalButton(
                    onClick = {
                        val q = searchQuery.ifBlank { "Top CSIR laboratories, IITs, and science libraries in India" }
                        searchQuery = q
                        onRunGoogleMapsGrounding?.invoke(q)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("google_maps_grounding_button")
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr(appLanguage, "Maps Data", "मैप्स खोज"), maxLines = 1)
                }

                OutlinedButton(
                    onClick = {
                        openGoogleSearch(context, searchQuery.ifBlank { "CSIR NET Physics" })
                    },
                    modifier = Modifier.testTag("google_search_submit_button")
                ) {
                    Icon(Icons.Default.Public, contentDescription = "Open Google Web", modifier = Modifier.size(16.dp))
                }
            }

            // Grounded Search / Maps Result Box
            AnimatedVisibility(visible = groundedQueryState !is GroundedQueryState.Idle) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (groundedQueryState) {
                        is GroundedQueryState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "${groundedQueryState.modeLabel}: \"${groundedQueryState.query}\"…",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        is GroundedQueryState.Success -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✔ ${groundedQueryState.modeLabel}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                if (onClearGroundedResult != null) {
                                    IconButton(
                                        onClick = onClearGroundedResult,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear result",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = groundedQueryState.responseText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        is GroundedQueryState.Error -> {
                            Text(
                                text = groundedQueryState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        GroundedQueryState.Idle -> {}
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickQueries.forEach { sample ->
                    AssistChip(
                        onClick = {
                            searchQuery = sample
                            if (onRunGoogleSearchGrounding != null) {
                                onRunGoogleSearchGrounding(sample)
                            } else {
                                openGoogleSearch(context, sample)
                            }
                        },
                        label = {
                            Text(
                                text = sample,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DailyThoughtPosterCard(
    poster: DailyThoughtPoster?,
    appLanguage: String,
    onGenerateNewPoster: () -> Unit,
    onCyclePalette: (DailyThoughtPoster) -> Unit,
    onSpeakPoster: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val gradientColors = when (poster?.paletteStyle) {
        "ROYAL_INDIGO" -> listOf(
            Color(0xFF1E1B4B),
            Color(0xFF312E81),
            Color(0xFF0F172A)
        )
        "EMERALD_DAWN" -> listOf(
            Color(0xFF064E3B),
            Color(0xFF0F766E),
            Color(0xFF0F172A)
        )
        "CRIMSON_LOTUS" -> listOf(
            Color(0xFF7F1D1D),
            Color(0xFF9D174D),
            Color(0xFF1E1B4B)
        )
        else -> listOf(
            Color(0xFF78350F),
            Color(0xFFB45309),
            Color(0xFF1E1B4B)
        )
    }

    val goldAccent = Color(0xFFFBBF24)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_thought_poster_card"),
        shape = MaterialTheme.shapes.large
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .border(
                    width = 1.5.dp,
                    color = goldAccent.copy(alpha = 0.55f),
                    shape = MaterialTheme.shapes.large
                )
                .padding(20.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = onGenerateNewPoster,
                        label = {
                            Text(
                                text = poster?.themeTitle ?: "Thought of the Day • आज का सुविचार",
                                fontWeight = FontWeight.Bold,
                                color = goldAccent
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.Black.copy(alpha = 0.35f)
                        )
                    )

                    Row {
                        if (poster != null) {
                            IconButton(
                                onClick = { onCyclePalette(poster) },
                                modifier = Modifier.testTag("poster_palette_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Change Poster Theme",
                                    tint = goldAccent
                                )
                            }
                            IconButton(
                                onClick = {
                                    onSpeakPoster("${poster.quoteEnglish}. ${poster.quoteHindi}. By ${poster.authorOrSource}")
                                },
                                modifier = Modifier.testTag("poster_listen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Listen to Quote Poster",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { shareThoughtPoster(context, poster) },
                                modifier = Modifier.testTag("poster_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share Quote Poster",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                if (poster != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .background(Color.Black.copy(alpha = 0.28f))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🇮🇳 आज का सुविचार (Hindi Quote)",
                            style = MaterialTheme.typography.labelMedium,
                            color = goldAccent,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "“${poster.quoteHindi}”",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )

                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth(0.4f)
                                .height(1.dp)
                                .background(goldAccent.copy(alpha = 0.4f))
                        )

                        Text(
                            text = "🌐 Thought of the Day (English Quote)",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "“${poster.quoteEnglish}”",
                            style = MaterialTheme.typography.titleMedium,
                            fontStyle = FontStyle.Italic,
                            color = Color.White.copy(alpha = 0.95f),
                            textAlign = TextAlign.Center
                        )
                    }

                    Text(
                        text = "— ${poster.authorOrSource}",
                        style = MaterialTheme.typography.labelLarge,
                        color = goldAccent,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "💡 ${poster.reflectionNote}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.88f),
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onGenerateNewPoster,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("generate_new_poster_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = goldAccent,
                            contentColor = Color(0xFF0F172A)
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tr(appLanguage, "New Quote Poster", "नया सुविचार पोस्टर"),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (poster != null) {
                        OutlinedButton(
                            onClick = { shareThoughtPoster(context, poster) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tr(appLanguage, "Share Poster", "पोस्टर साझा करें"),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
