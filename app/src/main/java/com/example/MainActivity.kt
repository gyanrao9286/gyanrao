package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.AuraRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.CsirPhysicsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EnglishCoachScreen
import com.example.ui.screens.HourlyBriefingScreen
import com.example.ui.screens.InboxReplyScreen
import com.example.ui.screens.VoiceSettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.AudioRecorderHelper
import com.example.util.configureAppCheck
import com.example.viewmodel.AiActionState
import com.example.viewmodel.AudioTranscribeState
import com.example.viewmodel.AuraViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        AuraMainContent(currentUserId = user.uid)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AuraMainContent(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val audioRecorder = remember { AudioRecorderHelper(context) }

    val viewModel: AuraViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                AuraViewModel(AuraRepository(db), currentUserId)
            }
        }
    )

    val inboxState by viewModel.inboxState.collectAsStateWithLifecycle()
    val briefingsState by viewModel.briefingsState.collectAsStateWithLifecycle()
    val englishTasksState by viewModel.englishTasksState.collectAsStateWithLifecycle()
    val physicsQuizzesState by viewModel.physicsQuizzesState.collectAsStateWithLifecycle()
    val thoughtPostersState by viewModel.thoughtPostersState.collectAsStateWithLifecycle()
    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val aiActionState by viewModel.aiActionState.collectAsStateWithLifecycle()
    val isBackgroundRunning by viewModel.isBackgroundAgentRunning.collectAsStateWithLifecycle()
    val minutesToNextHour by viewModel.minutesToNextHour.collectAsStateWithLifecycle()

    val liveVoiceState by viewModel.liveAndGrounding.liveVoiceState.collectAsStateWithLifecycle()
    val groundedQueryState by viewModel.liveAndGrounding.groundedQueryState.collectAsStateWithLifecycle()
    val transcribeState by viewModel.liveAndGrounding.transcribeState.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showVoiceCommandSheet by remember { mutableStateOf(false) }
    var pendingSpeechCallback by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var pendingAudioAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // BackHandler for secondary tabs / screens
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    // Text-to-Speech Engine for Briefings, Pronunciation, Physics & Posters
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.ENGLISH
            }
        }
        ttsEngine = instance
        onDispose {
            audioRecorder.stopRecordingCleanup()
            instance?.stop()
            instance?.shutdown()
        }
    }

    val speakAloud: (String) -> Unit = { text ->
        ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aura_tts_${System.currentTimeMillis()}")
    }

    // Speech Recognizer Activity Result Launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.firstOrNull().orEmpty()
            if (spokenText.isNotBlank()) {
                pendingSpeechCallback?.invoke(spokenText)
            }
        }
        pendingSpeechCallback = null
    }

    val launchSpeechIntent: () -> Unit = {
        try {
            val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command, Google search, or English practice…")
            }
            speechRecognizerLauncher.launch(speechIntent)
        } catch (e: Exception) {
            showVoiceCommandSheet = true
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val customAction = pendingAudioAction
            pendingAudioAction = null
            if (customAction != null) {
                customAction()
            } else {
                launchSpeechIntent()
            }
        } else {
            pendingAudioAction = null
            showVoiceCommandSheet = true
        }
    }

    val runWithAudioPermission: (() -> Unit) -> Unit = { action ->
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            action()
        } else {
            pendingAudioAction = action
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val triggerSpeechInput: ((String) -> Unit) -> Unit = { onResult ->
        pendingSpeechCallback = onResult
        runWithAudioPermission { launchSpeechIntent() }
    }

    val handleToggleAudioTranscription: () -> Unit = {
        if (transcribeState is AudioTranscribeState.Recording) {
            val bytesResult = audioRecorder.stopAndReadBytes()
            bytesResult.onSuccess { bytes ->
                viewModel.liveAndGrounding.transcribeRecordedAudio(bytes, "audio/mp4")
            }.onFailure {
                viewModel.liveAndGrounding.clearTranscriptionState()
            }
        } else {
            runWithAudioPermission {
                audioRecorder.startRecording().onSuccess {
                    viewModel.liveAndGrounding.markRecordingStarted()
                }
            }
        }
    }

    // Observe AI Action State for Snackbars and TTS Readback
    LaunchedEffect(aiActionState) {
        when (val state = aiActionState) {
            is AiActionState.Success -> {
                if (!state.spokenText.isNullOrBlank() && settings.voiceReadbackEnabled) {
                    speakAloud(state.spokenText)
                }
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearAiActionState()
            }
            is AiActionState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.clearAiActionState()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "CSIR Physics • ${settings.appLanguage} • Auto-Reply",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // 1-Tap Hindi / English / Bilingual Cycle Button in Top Bar
                    val nextLang = when (settings.appLanguage) {
                        "BILINGUAL" -> "HINDI"
                        "HINDI" -> "ENGLISH"
                        else -> "BILINGUAL"
                    }
                    val langBadge = when (settings.appLanguage) {
                        "HINDI" -> "हिंदी"
                        "ENGLISH" -> "EN"
                        else -> "हिं+EN"
                    }
                    AssistChip(
                        onClick = { viewModel.setAppLanguage(nextLang) },
                        label = { Text(langBadge, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Switch Hindi/English Language",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.testTag("top_bar_language_button")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = { selectedTab = 5 },
                        modifier = Modifier.testTag("top_bar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsVoice,
                            contentDescription = "Open Voice & Language Settings",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (selectedTab != 5) {
                FloatingActionButton(
                    onClick = { showVoiceCommandSheet = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_voice_command")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Command Settings"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                val navItems = listOf(
                    Triple(0, stringResource(R.string.nav_dashboard), Icons.Default.Dashboard to Icons.Outlined.Dashboard),
                    Triple(1, stringResource(R.string.nav_inbox), Icons.Default.MarkEmailRead to Icons.Outlined.MarkEmailRead),
                    Triple(2, stringResource(R.string.nav_briefings), Icons.Default.Public to Icons.Outlined.Public),
                    Triple(3, stringResource(R.string.nav_physics), Icons.Default.Science to Icons.Outlined.Science),
                    Triple(4, stringResource(R.string.nav_english), Icons.Default.RecordVoiceOver to Icons.Outlined.RecordVoiceOver)
                )
                navItems.forEach { (index, label, icons) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) icons.first else icons.second,
                                contentDescription = label
                            )
                        },
                        label = { Text(label) },
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
            ) {
                // Live AI Working Banner
                AnimatedVisibility(visible = aiActionState is AiActionState.Working) {
                    val workingLabel = (aiActionState as? AiActionState.Working)?.label.orEmpty()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = workingLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                when (selectedTab) {
                    0 -> DashboardScreen(
                        inboxState = inboxState,
                        briefingsState = briefingsState,
                        englishTasksState = englishTasksState,
                        physicsQuizzesState = physicsQuizzesState,
                        thoughtPostersState = thoughtPostersState,
                        settings = settings,
                        isBackgroundRunning = isBackgroundRunning,
                        minutesToNextHour = minutesToNextHour,
                        liveVoiceState = liveVoiceState,
                        groundedQueryState = groundedQueryState,
                        transcribeState = transcribeState,
                        onToggleBackgroundAgent = viewModel::toggleBackgroundAgent,
                        onScanInboxNow = viewModel::processAllPendingInboxInBackground,
                        onGenerateBriefingNow = { viewModel.generateHourlyBriefingNow() },
                        onGenerateNewPoster = { viewModel.generateDailyThoughtPoster() },
                        onCyclePosterPalette = viewModel::cycleThoughtPosterPalette,
                        onSelectLanguage = viewModel::setAppLanguage,
                        onStartLiveVoice = {
                            runWithAudioPermission {
                                viewModel.liveAndGrounding.startLiveVoiceConversation()
                            }
                        },
                        onStopLiveVoice = viewModel.liveAndGrounding::stopLiveVoiceConversation,
                        onSendLiveText = viewModel.liveAndGrounding::sendTextPromptToLiveSession,
                        onToggleAudioTranscription = handleToggleAudioTranscription,
                        onApplyTranscribedText = { text ->
                            viewModel.applyVoiceCommandToSettings(text)
                            viewModel.liveAndGrounding.clearTranscriptionState()
                        },
                        onRunGoogleSearchGrounding = { query ->
                            viewModel.liveAndGrounding.runGoogleSearchGroundedQuery(query, settings.appLanguage)
                        },
                        onRunGoogleMapsGrounding = { query ->
                            viewModel.liveAndGrounding.runGoogleMapsGroundedQuery(query, settings.appLanguage)
                        },
                        onClearGroundedResult = viewModel.liveAndGrounding::clearGroundedQuery,
                        onSpeakText = speakAloud,
                        onStartVoiceSearch = triggerSpeechInput,
                        onOpenVoiceCommandModal = { showVoiceCommandSheet = true },
                        onNavigateToTab = { selectedTab = it }
                    )
                    1 -> InboxReplyScreen(
                        inboxState = inboxState,
                        settings = settings,
                        onProcessPendingQueue = viewModel::processAllPendingInboxInBackground,
                        onCheckAndAutoReplyIncoming = viewModel::checkAndAutoReplyNewIncoming,
                        onGenerateAiReplyForItem = viewModel::generateAndApplyReplyForInboxItem,
                        onApproveDraft = viewModel::approveAndSendDraft,
                        onDeleteItem = viewModel::deleteInboxItem
                    )
                    2 -> HourlyBriefingScreen(
                        briefingsState = briefingsState,
                        settings = settings,
                        minutesToNextHour = minutesToNextHour,
                        onGenerateBriefing = viewModel::generateHourlyBriefingNow,
                        onMarkRead = viewModel::markBriefingAsRead,
                        onSpeakBriefing = speakAloud
                    )
                    3 -> CsirPhysicsScreen(
                        physicsQuizzesState = physicsQuizzesState,
                        thoughtPostersState = thoughtPostersState,
                        settings = settings,
                        groundedQueryState = groundedQueryState,
                        onGeneratePhysicsQuiz = viewModel::generateCsirPhysicsQuiz,
                        onSelectQuizOption = viewModel::selectPhysicsQuizAnswer,
                        onGenerateThoughtPoster = viewModel::generateDailyThoughtPoster,
                        onCyclePosterPalette = viewModel::cycleThoughtPosterPalette,
                        onSelectLanguage = viewModel::setAppLanguage,
                        onRunGoogleSearchGrounding = { query ->
                            viewModel.liveAndGrounding.runGoogleSearchGroundedQuery(query, settings.appLanguage)
                        },
                        onRunGoogleMapsGrounding = { query ->
                            viewModel.liveAndGrounding.runGoogleMapsGroundedQuery(query, settings.appLanguage)
                        },
                        onClearGroundedResult = viewModel.liveAndGrounding::clearGroundedQuery,
                        onSpeakText = speakAloud,
                        onStartVoiceSearch = triggerSpeechInput
                    )
                    4 -> EnglishCoachScreen(
                        englishTasksState = englishTasksState,
                        settings = settings,
                        onGenerateNewTask = viewModel::generateDailyEnglishTask,
                        onSubmitPractice = viewModel::submitEnglishPracticeResponse,
                        onSpeakText = speakAloud,
                        onStartSpeechRecognition = triggerSpeechInput
                    )
                    5 -> VoiceSettingsScreen(
                        settings = settings,
                        onApplyVoiceCommand = viewModel::applyVoiceCommandToSettings,
                        onStartVoiceRecognition = triggerSpeechInput,
                        onUpdateSettingsDirectly = viewModel::updateSettingsDirectly,
                        onSignOut = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = {},
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    }

    if (showVoiceCommandSheet) {
        QuickVoiceCommandDialog(
            onDismiss = { showVoiceCommandSheet = false },
            onStartSystemMic = {
                showVoiceCommandSheet = false
                triggerSpeechInput { spoken ->
                    viewModel.applyVoiceCommandToSettings(spoken)
                }
            },
            onApplyCommand = { cmd ->
                showVoiceCommandSheet = false
                viewModel.applyVoiceCommandToSettings(cmd)
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickVoiceCommandDialog(
    onDismiss: () -> Unit,
    onStartSystemMic: () -> Unit,
    onApplyCommand: (String) -> Unit
) {
    var commandText by remember { mutableStateOf("") }
    val sampleCommands = listOf(
        "Change language to Hindi and English bilingual",
        "Switch language to Hindi (भाषा हिंदी करें)",
        "Set reply tone to Executive and turn on email auto-reply",
        "Focus more on India news and read briefings aloud"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Voice Command & Language Assistant")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Speak or select a command to switch language (Hindi/English), customize background replies, or adjust study goals:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onStartSystemMic,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Use Device Microphone")
                }

                OutlinedTextField(
                    value = commandText,
                    onValueChange = { commandText = it },
                    label = { Text("Spoken command") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_voice_command_input")
                )

                Text(
                    text = "Or tap a voice command:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sampleCommands.forEach { sample ->
                        AssistChip(
                            onClick = { onApplyCommand(sample) },
                            label = { Text("\"$sample\"") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (commandText.isNotBlank()) {
                        onApplyCommand(commandText)
                    }
                },
                enabled = commandText.isNotBlank()
            ) {
                Text("Apply Voice Command")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
