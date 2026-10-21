package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AssistantSettings
import com.example.data.model.DailyThoughtPoster
import com.example.data.model.EnglishTask
import com.example.data.model.InboxItem
import com.example.data.model.NewsBriefing
import com.example.data.model.PhysicsQuizItem
import com.example.data.repository.AuraRepository
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.generationConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AuraViewModel"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiActionState {
    data object Idle : AiActionState
    data class Working(val label: String) : AiActionState
    data class Success(val message: String, val spokenText: String? = null) : AiActionState
    data class Error(val message: String) : AiActionState
}

class AuraViewModel(
    private val repository: AuraRepository,
    private val currentUserId: String
) : ViewModel() {

    val liveAndGrounding = LiveAndGroundingController(viewModelScope)

    // --- Lazy Generative Models (gemini-3.8-flash) ---

    private val replyModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = AuraAiSchemas.replySchema
            }
        )
    }

    private val briefingModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = AuraAiSchemas.briefingSchema
            }
        )
    }

    private val englishGeneratorModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = AuraAiSchemas.englishTaskSchema
            }
        )
    }

    private val englishCoachModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = AuraAiSchemas.englishEvalSchema
            }
        )
    }

    private val physicsQuizModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = AuraAiSchemas.physicsQuizSchema
            }
        )
    }

    private val thoughtPosterModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = AuraAiSchemas.thoughtPosterSchema
            }
        )
    }

    private val voiceSettingsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = AuraAiSchemas.voiceSettingsSchema
            }
        )
    }

    // --- Realtime Firestore Streams ---

    val inboxState: StateFlow<UiState<List<InboxItem>>> = repository.observeInbox(currentUserId)
        .map<List<InboxItem>, UiState<List<InboxItem>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing inbox", error)
            emit(UiState.Error(error.message ?: "Failed to load inbox items"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val briefingsState: StateFlow<UiState<List<NewsBriefing>>> = repository.observeBriefings(currentUserId)
        .map<List<NewsBriefing>, UiState<List<NewsBriefing>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing briefings", error)
            emit(UiState.Error(error.message ?: "Failed to load news briefings"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val englishTasksState: StateFlow<UiState<List<EnglishTask>>> = repository.observeEnglishTasks(currentUserId)
        .map<List<EnglishTask>, UiState<List<EnglishTask>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing English tasks", error)
            emit(UiState.Error(error.message ?: "Failed to load English learning tasks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val physicsQuizzesState: StateFlow<UiState<List<PhysicsQuizItem>>> = repository.observePhysicsQuizzes(currentUserId)
        .map<List<PhysicsQuizItem>, UiState<List<PhysicsQuizItem>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing CSIR Physics quizzes", error)
            emit(UiState.Error(error.message ?: "Failed to load CSIR Physics quizzes"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val thoughtPostersState: StateFlow<UiState<List<DailyThoughtPoster>>> = repository.observeThoughtPosters(currentUserId)
        .map<List<DailyThoughtPoster>, UiState<List<DailyThoughtPoster>>> { UiState.Success(it) }
        .catch { error ->
            Log.w(TAG, "Error observing daily thought posters", error)
            emit(UiState.Error(error.message ?: "Failed to load daily thought posters"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val settingsState: StateFlow<AssistantSettings> = repository.observeSettings(currentUserId)
        .map { it ?: AssistantSettings(userId = currentUserId) }
        .catch { error ->
            Log.w(TAG, "Error observing settings", error)
            emit(AssistantSettings(userId = currentUserId))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AssistantSettings(userId = currentUserId)
        )

    private val _aiActionState = MutableStateFlow<AiActionState>(AiActionState.Idle)
    val aiActionState: StateFlow<AiActionState> = _aiActionState.asStateFlow()

    private val _isBackgroundAgentRunning = MutableStateFlow(true)
    val isBackgroundAgentRunning: StateFlow<Boolean> = _isBackgroundAgentRunning.asStateFlow()

    private val _minutesToNextHour = MutableStateFlow(60)
    val minutesToNextHour: StateFlow<Int> = _minutesToNextHour.asStateFlow()

    private var hasSeededWorkspace = false

    init {
        seedInitialWorkspaceIfNeeded()
        startBackgroundHourlyAndInboxMonitor()
    }

    fun clearAiActionState() {
        _aiActionState.value = AiActionState.Idle
    }

    fun toggleBackgroundAgent(enabled: Boolean) {
        _isBackgroundAgentRunning.value = enabled
    }

    private fun seedInitialWorkspaceIfNeeded() {
        viewModelScope.launch {
            if (hasSeededWorkspace) return@launch
            hasSeededWorkspace = true
            try {
                val existingSettings = repository.observeSettings(currentUserId).firstOrNull()
                if (existingSettings == null) {
                    repository.saveSettings(
                        AssistantSettings(
                            userId = currentUserId,
                            autoReplyMessages = true,
                            autoReplyEmails = true,
                            replyTone = "EXECUTIVE",
                            hourlyBriefingEnabled = true,
                            indiaFocusLevel = "HIGH",
                            englishDifficulty = "BUSINESS_FLUENT",
                            dailyGoalMinutes = 20L,
                            voiceReadbackEnabled = true,
                            appLanguage = "BILINGUAL",
                            lastVoiceCommand = "Default bilingual Hindi & English executive profile active"
                        )
                    )
                }

                val existingInbox = repository.observeInbox(currentUserId).firstOrNull().orEmpty()
                if (existingInbox.isEmpty()) {
                    repository.createInboxItem(
                        channel = "EMAIL",
                        sender = "Rohan Mehta • VP Operations (Bengaluru)",
                        subject = "Urgent: Q4 India Expansion Budget Review",
                        incomingBody = "Hi, we need your sign-off on the Bengaluru & Hyderabad cloud infrastructure allocation before 5 PM IST today. Should we proceed with Option A?",
                        aiReplyDraft = "Hi Rohan, I have reviewed the Q4 allocation. Please proceed with Option A for Bengaluru and Hyderabad, and share the final tracker by 4:30 PM IST.",
                        status = "AUTO_REPLIED",
                        priority = "HIGH"
                    )
                    repository.createInboxItem(
                        channel = "MESSAGE",
                        sender = "Neha Kapoor • Product Lead",
                        subject = "Client Demo Reschedule Request",
                        incomingBody = "Hey! Can we shift tomorrow's sprint demo from 11 AM to 2:30 PM IST? The design team is finalizing the dashboard polish.",
                        aiReplyDraft = "",
                        status = "PENDING",
                        priority = "NORMAL"
                    )
                }

                val existingBriefings = repository.observeBriefings(currentUserId).firstOrNull().orEmpty()
                if (existingBriefings.isEmpty()) {
                    val timeStr = SimpleDateFormat("hh:00 a 'IST'", Locale.getDefault()).format(Date())
                    repository.createBriefing(
                        hourLabel = "$timeStr • Hourly Pulse",
                        indiaHeadline = "India's National Quantum Mission & CSIR Research Labs Expand Domestic Compute Corridors",
                        indiaSummary = "CSIR laboratories, IISc Bengaluru, and sovereign AI clusters announced new photonic & quantum sensor milestones this hour, while UPI cross-border payments hit a fresh record.",
                        worldHeadline = "Global Clean-Tech Supply Chains & Next-Gen Fusion Diagnostics Accelerate",
                        worldSummary = "International physics consortia and semiconductor foundries reported high-efficiency superconducting magnet breakthroughs alongside steady global tech indices.",
                        keyTakeaway = "Align daily research & CSIR preparation with emerging quantum and condensed-matter milestones."
                    )
                }

                val existingTasks = repository.observeEnglishTasks(currentUserId).firstOrNull().orEmpty()
                if (existingTasks.isEmpty()) {
                    repository.createEnglishTask(
                        dayTopic = "Executive Alignment & Scientific Clarity",
                        category = "SPEAKING",
                        targetPhrase = "To move the needle on key deliverables",
                        meaningAndNuance = "Used in leadership and research meetings to describe actions that create a measurable, high-impact positive result (महत्वपूर्ण और मापने योग्य प्रगति करना).",
                        practicePrompt = "Speak or write 1-2 sentences explaining how focused study or a new project will 'move the needle' this week."
                    )
                }

                val existingQuizzes = repository.observePhysicsQuizzes(currentUserId).firstOrNull().orEmpty()
                if (existingQuizzes.isEmpty()) {
                    repository.createPhysicsQuizItem(
                        topic = "Quantum Mechanics",
                        partType = "PART_C",
                        questionEn = "A particle of mass m is confined in a 1D infinite potential well of width L (0 ≤ x ≤ L). If the particle is in the state ψ(x) = √(2/L) sin(3πx/L), what is the expectation value of momentum ⟨p_x⟩ and kinetic energy ⟨T⟩?",
                        questionHi = "द्रव्यमान m का एक कण चौड़ाई L (0 ≤ x ≤ L) के एकविमीय अनंत विभव कूप (1D box) में अवस्था ψ(x) = √(2/L) sin(3πx/L) में है। संवेग का प्रत्याशा मान ⟨p_x⟩ तथा गतिज ऊर्जा ⟨T⟩ क्या होगी?",
                        optionsEn = listOf(
                            "⟨p_x⟩ = 0,  ⟨T⟩ = 9π²ħ² / (2mL²)",
                            "⟨p_x⟩ = 3πħ / L,  ⟨T⟩ = 9π²ħ² / (2mL²)",
                            "⟨p_x⟩ = 0,  ⟨T⟩ = 3π²ħ² / (2mL²)",
                            "⟨p_x⟩ = -3πħ / L,  ⟨T⟩ = π²ħ² / (2mL²)"
                        ),
                        optionsHi = listOf(
                            "⟨p_x⟩ = 0,  ⟨T⟩ = 9π²ħ² / (2mL²)",
                            "⟨p_x⟩ = 3πħ / L,  ⟨T⟩ = 9π²ħ² / (2mL²)",
                            "⟨p_x⟩ = 0,  ⟨T⟩ = 3π²ħ² / (2mL²)",
                            "⟨p_x⟩ = -3πħ / L,  ⟨T⟩ = π²ħ² / (2mL²)"
                        ),
                        correctIndex = 0,
                        explanationEn = "For any stationary bound state ψ_n(x) with real wavefunction in a 1D infinite well, ⟨p_x⟩ = 0 because the state is an equal superposition of +k and -k momentum eigenstates. Here n = 3, so ⟨T⟩ = E_3 = n²π²ħ²/(2mL²) = 9π²ħ²/(2mL²).",
                        explanationHi = "1D अनंत विभव कूप की किसी भी वास्तविक बद्ध अवस्था (stationary bound state) के लिए संवेग का प्रत्याशा मान ⟨p_x⟩ = 0 होता है क्योंकि कण के दाएँ व बाएँ जाने की प्रायिकता समान होती है। यहाँ n = 3 है, अतः गतिज ऊर्जा ⟨T⟩ = E_3 = n²π²ħ²/(2mL²) = 9π²ħ²/(2mL²)।"
                    )
                    repository.createPhysicsQuizItem(
                        topic = "Statistical Mechanics",
                        partType = "PART_B",
                        questionEn = "For a two-level non-degenerate quantum system with energies 0 and ε in thermal equilibrium at temperature T, what is the internal energy U of N non-interacting particles?",
                        questionHi = "ऊर्जा 0 तथा ε वाली द्वि-स्तरीय (two-level) अपभ्रष्ट-रहित प्रणाली में तापमान T पर N स्वतंत्र कणों की आंतरिक ऊर्जा U क्या होगी?",
                        optionsEn = listOf(
                            "U = Nε / (1 + exp(ε / k_B T))",
                            "U = Nε / (1 - exp(-ε / k_B T))",
                            "U = Nε exp(ε / k_B T)",
                            "U = (1/2) N k_B T"
                        ),
                        optionsHi = listOf(
                            "U = Nε / (1 + exp(ε / k_B T))",
                            "U = Nε / (1 - exp(-ε / k_B T))",
                            "U = Nε exp(ε / k_B T)",
                            "U = (1/2) N k_B T"
                        ),
                        correctIndex = 0,
                        explanationEn = "Single-particle partition function Z_1 = 1 + exp(-βε) where β = 1/(k_B T). Internal energy U = -N ∂(ln Z_1)/∂β = Nε exp(-βε)/(1 + exp(-βε)) = Nε / (1 + exp(ε / k_B T)).",
                        explanationHi = "एकल-कण विभाजन फलन (Partition Function) Z_1 = 1 + exp(-βε) है, जहाँ β = 1/(k_B T)। आंतरिक ऊर्जा U = -N ∂(ln Z_1)/∂β = Nε exp(-βε)/(1 + exp(-βε)) = Nε / (1 + exp(ε / k_B T))।"
                    )
                    repository.createPhysicsQuizItem(
                        topic = "Electromagnetic Theory",
                        partType = "PART_B",
                        questionEn = "In an electromagnetic wave propagating in free space with electric field amplitude E_0, the time-averaged Poynting vector magnitude ⟨|S|⟩ is given by:",
                        questionHi = "मुक्त आकाश (free space) में विद्युत क्षेत्र आयाम E_0 के साथ संचरित होने वाली विद्युतचुंबकीय तरंग के समय-औसत पॉयंटिंग सदिश ⟨|S|⟩ का परिमाण क्या होता है?",
                        optionsEn = listOf(
                            "(1/2) c ε_0 E_0²",
                            "c ε_0 E_0²",
                            "(1/4) c ε_0 E_0²",
                            "E_0² / (2 c μ_0)"
                        ),
                        optionsHi = listOf(
                            "(1/2) c ε_0 E_0²",
                            "c ε_0 E_0²",
                            "(1/4) c ε_0 E_0²",
                            "E_0² / (2 c μ_0)"
                        ),
                        correctIndex = 0,
                        explanationEn = "The instantaneous Poynting vector is S = (1/μ_0)(E × B). Since B_0 = E_0/c and ⟨cos²(kz - ωt)⟩ = 1/2, the average intensity is ⟨|S|⟩ = E_0² / (2 μ_0 c) = (1/2) c ε_0 E_0².",
                        explanationHi = "पॉयंटिंग सदिश S = (1/μ_0)(E × B) होता है। चूँकि B_0 = E_0/c तथा ⟨cos²(kz - ωt)⟩ = 1/2 है, अतः औसत तीव्रता ⟨|S|⟩ = (1/2) c ε_0 E_0² होती है।"
                    )
                }

                val existingPosters = repository.observeThoughtPosters(currentUserId).firstOrNull().orEmpty()
                if (existingPosters.isEmpty()) {
                    repository.createThoughtPoster(
                        themeTitle = "Thought of the Day • आज का सुविचार",
                        quoteEnglish = "Dream is not that which you see while sleeping, it is something that does not let you sleep.",
                        quoteHindi = "सपने वो नहीं हैं जो आप नींद में देखते हैं, सपने वो हैं जो आपको नींद ही नहीं आने दें।",
                        authorOrSource = "Dr. A.P.J. Abdul Kalam • डॉ. ए.पी.जे. अब्दुल कलाम",
                        reflectionNote = "Dedicate focused deep-work blocks today to CSIR Physics mastery and confident communication. • आज अपने लक्ष्य और अध्ययन पर पूर्ण एकाग्रता रखें।",
                        paletteStyle = "SAFFRON_GOLD"
                    )
                    repository.createThoughtPoster(
                        themeTitle = "Scientific Spirit • वैज्ञानिक चेतना",
                        quoteEnglish = "Ask the right questions, and nature will open the doors to her secrets.",
                        quoteHindi = "सही प्रश्न पूछिए, और प्रकृति अपने रहस्यों के द्वार आपके लिए खोल देगी।",
                        authorOrSource = "Sir C.V. Raman • सर सी.वी. रमन (Nobel Laureate)",
                        reflectionNote = "Approach every CSIR NET problem from first principles rather than rote memorization. • प्रत्येक भौतिकी प्रश्न को मूल सिद्धांतों से समझें।",
                        paletteStyle = "ROYAL_INDIGO"
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Initial workspace check encountered non-fatal error", e)
            }
        }
    }

    private fun startBackgroundHourlyAndInboxMonitor() {
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val millisIntoHour = now % 3_600_000L
                val remainingMinutes = ((3_600_000L - millisIntoHour) / 60_000L).toInt().coerceIn(1, 60)
                _minutesToNextHour.value = remainingMinutes
                delay(30_000L)
            }
        }
    }

    // --- LANGUAGE SWITCHER (HINDI / ENGLISH / BILINGUAL) ---

    fun setAppLanguage(language: String) {
        val safeLang = if (language in listOf("ENGLISH", "HINDI", "BILINGUAL")) language else "BILINGUAL"
        viewModelScope.launch {
            try {
                val current = settingsState.value
                repository.saveSettings(
                    current.copy(
                        userId = currentUserId,
                        appLanguage = safeLang
                    )
                ).getOrThrow()
                val label = when (safeLang) {
                    "HINDI" -> "भाषा हिंदी में बदल दी गई है (Language set to Hindi)"
                    "ENGLISH" -> "Language set to English"
                    else -> "Bilingual Mode: Hindi + English active (द्विभाषी मोड सक्रिय)"
                }
                _aiActionState.value = AiActionState.Success(message = label)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to switch language")
            }
        }
    }

    // --- 1. BACKGROUND MESSAGE & EMAIL CHECK + AI REPLY ---

    fun generateAndApplyReplyForInboxItem(item: InboxItem, markAutoReplied: Boolean) {
        viewModelScope.launch {
            _aiActionState.value = AiActionState.Working("Drafting ${settingsState.value.replyTone.lowercase()} reply for ${item.sender}...")
            try {
                val currentSettings = settingsState.value
                val prompt = """
                    You are an executive AI assistant handling background ${item.channel.lowercase()}s.
                    Sender: ${item.sender}
                    Subject: ${item.subject}
                    Incoming Content: ${item.incomingBody}
                    Desired Reply Tone: ${currentSettings.replyTone}
                    
                    Generate a concise, high-clarity reply draft matching the ${currentSettings.replyTone} tone and assess priority (HIGH, NORMAL, LOW).
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    replyModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
                val json = JSONObject(jsonText)
                val draft = json.getString("replyDraft")
                val priority = json.optString("suggestedPriority", item.priority)
                val newStatus = if (markAutoReplied) "AUTO_REPLIED" else "DRAFT_READY"

                repository.updateInboxItem(
                    item.copy(
                        aiReplyDraft = draft,
                        priority = if (priority in listOf("HIGH", "NORMAL", "LOW")) priority else item.priority,
                        status = newStatus
                    )
                ).getOrThrow()

                val actionWord = if (markAutoReplied) "Auto-replied to" else "Prepared smart draft for"
                _aiActionState.value = AiActionState.Success(
                    message = "$actionWord ${item.sender}",
                    spokenText = if (currentSettings.voiceReadbackEnabled) "$actionWord ${item.sender}." else null
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI reply generation timed out", e)
                _aiActionState.value = AiActionState.Error("Reply generation timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI reply generation failed", e)
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to generate reply.")
            }
        }
    }

    fun processAllPendingInboxInBackground() {
        viewModelScope.launch {
            val currentList = (inboxState.value as? UiState.Success)?.data.orEmpty()
            val pendingItems = currentList.filter { it.status == "PENDING" || it.aiReplyDraft.isBlank() }
            if (pendingItems.isEmpty()) {
                checkAndAutoReplyNewIncoming(
                    channel = "EMAIL",
                    sender = "Vikramaditya Rao • Partner, India Ventures",
                    subject = "Follow-up: AI Automation Pilot Metrics",
                    incomingBody = "Hi, could you share the latest weekly productivity numbers from the pilot team before our 6 PM call?"
                )
                return@launch
            }

            _aiActionState.value = AiActionState.Working("Background agent checking ${pendingItems.size} pending item(s)...")
            val currentSettings = settingsState.value
            try {
                for (item in pendingItems) {
                    val shouldAutoSend = (item.channel == "EMAIL" && currentSettings.autoReplyEmails) ||
                        (item.channel == "MESSAGE" && currentSettings.autoReplyMessages)
                    val prompt = """
                        You are an executive AI assistant replying on behalf of the user.
                        Channel: ${item.channel}
                        Sender: ${item.sender}
                        Subject: ${item.subject}
                        Message: ${item.incomingBody}
                        Required Tone: ${currentSettings.replyTone}
                    """.trimIndent()

                    val response = withTimeout(60_000L) {
                        replyModel.generateContent(prompt)
                    }
                    val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
                    val json = JSONObject(jsonText)
                    val draft = json.getString("replyDraft")
                    val priority = json.optString("suggestedPriority", item.priority)

                    repository.updateInboxItem(
                        item.copy(
                            aiReplyDraft = draft,
                            priority = if (priority in listOf("HIGH", "NORMAL", "LOW")) priority else item.priority,
                            status = if (shouldAutoSend) "AUTO_REPLIED" else "DRAFT_READY"
                        )
                    ).getOrThrow()
                }
                _aiActionState.value = AiActionState.Success(
                    message = "Processed ${pendingItems.size} background inbox item(s).",
                    spokenText = if (currentSettings.voiceReadbackEnabled) {
                        "Background inbox check complete. Processed ${pendingItems.size} items."
                    } else null
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Background inbox processing timed out", e)
                _aiActionState.value = AiActionState.Error("Inbox check timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Background inbox processing failed", e)
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Background inbox check failed.")
            }
        }
    }

    fun checkAndAutoReplyNewIncoming(
        channel: String,
        sender: String,
        subject: String,
        incomingBody: String
    ) {
        viewModelScope.launch {
            val currentSettings = settingsState.value
            val shouldAutoSend = (channel == "EMAIL" && currentSettings.autoReplyEmails) ||
                (channel == "MESSAGE" && currentSettings.autoReplyMessages)

            _aiActionState.value = AiActionState.Working("Checking incoming ${channel.lowercase()} from $sender & crafting ${currentSettings.replyTone.lowercase()} reply...")
            try {
                val prompt = """
                    You are an executive AI assistant monitoring incoming ${channel.lowercase()}s in the background.
                    Sender: $sender
                    Subject: $subject
                    Incoming Text: $incomingBody
                    User's Configured Reply Tone: ${currentSettings.replyTone}
                    
                    Write a clear, natural, professional reply draft and classify urgency (HIGH, NORMAL, LOW).
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    replyModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
                val json = JSONObject(jsonText)
                val draft = json.getString("replyDraft")
                val priority = json.optString("suggestedPriority", "NORMAL")
                val status = if (shouldAutoSend) "AUTO_REPLIED" else "DRAFT_READY"

                repository.createInboxItem(
                    channel = channel,
                    sender = sender,
                    subject = subject,
                    incomingBody = incomingBody,
                    aiReplyDraft = draft,
                    status = status,
                    priority = if (priority in listOf("HIGH", "NORMAL", "LOW")) priority else "NORMAL"
                ).getOrThrow()

                val statusLabel = if (shouldAutoSend) "Auto-replied in background" else "Saved AI draft"
                _aiActionState.value = AiActionState.Success(
                    message = "$statusLabel to $sender",
                    spokenText = if (currentSettings.voiceReadbackEnabled) "$statusLabel to $sender." else null
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Incoming auto-reply timed out", e)
                _aiActionState.value = AiActionState.Error("Auto-reply timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Incoming auto-reply failed", e)
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to process incoming message.")
            }
        }
    }

    fun approveAndSendDraft(item: InboxItem, editedDraft: String = item.aiReplyDraft) {
        viewModelScope.launch {
            try {
                repository.updateInboxItem(
                    item.copy(
                        aiReplyDraft = editedDraft,
                        status = "AUTO_REPLIED"
                    )
                ).getOrThrow()
                _aiActionState.value = AiActionState.Success("Reply sent to ${item.sender}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to update reply status")
            }
        }
    }

    fun deleteInboxItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteInboxItem(itemId)
        }
    }

    // --- 2. HOURLY WORLD & INDIA NEWS BRIEFING ---

    fun generateHourlyBriefingNow(customTopicHint: String = "") {
        viewModelScope.launch {
            val currentSettings = settingsState.value
            val timeStr = SimpleDateFormat("hh:mm a 'IST'", Locale.getDefault()).format(Date())
            _aiActionState.value = AiActionState.Working("Synthesizing hourly India & World briefing ($timeStr)...")
            try {
                val langInstruction = when (currentSettings.appLanguage) {
                    "HINDI" -> "Write headlines, summaries, and keyTakeaway in clear Hindi (Devanagari) with key English terms in parentheses."
                    "BILINGUAL" -> "Write headlines in English + Hindi (Bilingual) and make summaries clear and high-signal."
                    else -> "Write in crisp executive English."
                }
                val topicClause = if (customTopicHint.isNotBlank()) "Include special attention to: $customTopicHint." else ""
                val prompt = """
                    Generate an executive hourly briefing for right now ($timeStr) covering:
                    1) What is new in India in brief (technology, CSIR/science, economy, infrastructure, policy).
                    2) What is new in the World in brief (global tech, physics/science, macroeconomy, diplomacy, AI).
                    3) One actionable executive takeaway.
                    India Focus Preference: ${currentSettings.indiaFocusLevel}.
                    Language Instruction: $langInstruction
                    $topicClause
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    briefingModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
                val json = JSONObject(jsonText)

                val hourLabel = "$timeStr • Hourly Pulse"
                val indiaHeadline = json.getString("indiaHeadline")
                val indiaSummary = json.getString("indiaSummary")
                val worldHeadline = json.getString("worldHeadline")
                val worldSummary = json.getString("worldSummary")
                val keyTakeaway = json.getString("keyTakeaway")

                repository.createBriefing(
                    hourLabel = hourLabel,
                    indiaHeadline = indiaHeadline,
                    indiaSummary = indiaSummary,
                    worldHeadline = worldHeadline,
                    worldSummary = worldSummary,
                    keyTakeaway = keyTakeaway
                ).getOrThrow()

                _aiActionState.value = AiActionState.Success(
                    message = "Hourly India & World briefing updated ($timeStr)",
                    spokenText = if (currentSettings.voiceReadbackEnabled) {
                        "Hourly briefing ready. In India: $indiaHeadline. In the World: $worldHeadline."
                    } else null
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Hourly briefing generation timed out", e)
                _aiActionState.value = AiActionState.Error("Hourly briefing timed out after 60 seconds. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Hourly briefing generation failed", e)
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to generate hourly briefing.")
            }
        }
    }

    fun markBriefingAsRead(briefing: NewsBriefing) {
        viewModelScope.launch {
            repository.updateBriefing(briefing.copy(isRead = true))
        }
    }

    // --- 3. DAILY ENGLISH LEARNING COACH ---

    fun generateDailyEnglishTask(selectedCategory: String = "") {
        viewModelScope.launch {
            val currentSettings = settingsState.value
            _aiActionState.value = AiActionState.Working("Creating ${currentSettings.englishDifficulty.lowercase().replace('_', ' ')} English task...")
            try {
                val existingTopics = (englishTasksState.value as? UiState.Success)?.data
                    ?.take(5)
                    ?.joinToString(", ") { it.targetPhrase }
                    .orEmpty()

                val categoryHint = if (selectedCategory.isNotBlank()) "Target skill category: $selectedCategory." else "Pick the most impactful skill category (VOCABULARY, SPEAKING, GRAMMAR, or WRITING)."
                val prompt = """
                    Create a new daily English learning task for an ambitious learner in India.
                    Difficulty Level: ${currentSettings.englishDifficulty}
                    Daily Goal: ${currentSettings.dailyGoalMinutes} minutes
                    $categoryHint
                    Avoid repeating these recent phrases: [$existingTopics].
                    In meaningAndNuance, include both clear English explanation and the Hindi meaning (हिंदी अर्थ) so Hindi-English learners grasp it immediately.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    englishGeneratorModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
                val json = JSONObject(jsonText)

                val dayTopic = json.getString("dayTopic")
                val category = json.getString("category")
                val targetPhrase = json.getString("targetPhrase")
                val meaningAndNuance = json.getString("meaningAndNuance")
                val practicePrompt = json.getString("practicePrompt")

                repository.createEnglishTask(
                    dayTopic = dayTopic,
                    category = category,
                    targetPhrase = targetPhrase,
                    meaningAndNuance = meaningAndNuance,
                    practicePrompt = practicePrompt
                ).getOrThrow()

                _aiActionState.value = AiActionState.Success(
                    message = "New English Mission: $targetPhrase",
                    spokenText = if (currentSettings.voiceReadbackEnabled) {
                        "New English mission ready: $dayTopic. Your target phrase is: $targetPhrase."
                    } else null
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "English task generation timed out", e)
                _aiActionState.value = AiActionState.Error("English task generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "English task generation failed", e)
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to generate English task.")
            }
        }
    }

    fun submitEnglishPracticeResponse(task: EnglishTask, userAttempt: String) {
        if (userAttempt.isBlank()) {
            _aiActionState.value = AiActionState.Error("Please speak or type your practice sentence first.")
            return
        }
        viewModelScope.launch {
            val currentSettings = settingsState.value
            _aiActionState.value = AiActionState.Working("Evaluating your English fluency & phrasing...")
            try {
                val prompt = """
                    You are an encouraging bilingual (English + Hindi) communication coach.
                    Target Topic: ${task.dayTopic}
                    Target Phrase/Concept: ${task.targetPhrase}
                    Practice Prompt: ${task.practicePrompt}
                    User's Spoken/Written Attempt: "$userAttempt"
                    
                    Score the attempt from 0 to 100 on clarity, grammar, and natural usage of the target concept.
                    Give concise, constructive coaching feedback in English (with helpful Hindi tip if useful) and show 1 polished alternative sentence.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    englishCoachModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
                val json = JSONObject(jsonText)

                val score = json.optInt("masteryScore", 85).toLong().coerceIn(0L, 100L)
                val feedback = json.getString("aiFeedback")

                repository.updateEnglishTask(
                    task.copy(
                        userResponse = userAttempt,
                        aiFeedback = feedback,
                        masteryScore = score,
                        completed = true
                    )
                ).getOrThrow()

                _aiActionState.value = AiActionState.Success(
                    message = "Task Completed! Fluency Score: $score/100",
                    spokenText = if (currentSettings.voiceReadbackEnabled) {
                        "Great job! You scored $score out of 100. $feedback"
                    } else null
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "English evaluation timed out", e)
                _aiActionState.value = AiActionState.Error("Fluency evaluation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "English evaluation failed", e)
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to evaluate English practice.")
            }
        }
    }

    // --- 4. CSIR NET PHYSICAL SCIENCES QUIZ & ANSWERS (BILINGUAL HINDI + ENGLISH) ---

    fun generateCsirPhysicsQuiz(topic: String = "Quantum Mechanics", partType: String = "PART_C") {
        viewModelScope.launch {
            val currentSettings = settingsState.value
            _aiActionState.value = AiActionState.Working("Generating CSIR NET Physics ($partType) problem on $topic...")
            try {
                val recentQuestions = (physicsQuizzesState.value as? UiState.Success)?.data
                    ?.take(5)
                    ?.joinToString(" | ") { it.questionEn.take(80) }
                    .orEmpty()

                val prompt = """
                    You are a senior CSIR NET Physical Sciences (Physics) exam setter.
                    Generate a rigorous, exam-authentic CSIR NET Physics MCQ for:
                    - Subject Topic: $topic
                    - Section: $partType (PART_B = 3.5 marks core physics problem, PART_C = 5 marks analytical/derivation problem)
                    - Do NOT repeat these recent questions: [$recentQuestions]
                    
                    Provide:
                    1. questionEn (in English with clean Unicode math symbols like ħ, ω, π, ε_0, ψ, ⟨x⟩, ∇, etc.)
                    2. questionHi (accurate Hindi translation in Devanagari script with the same math symbols)
                    3. Four distinct options in English (optionA_En..optionD_En) and Hindi (optionA_Hi..optionD_Hi)
                    4. correctIndex (0, 1, 2, or 3)
                    5. Detailed step-by-step formula derivation and physical insight in both English (explanationEn) and Hindi (explanationHi).
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    physicsQuizModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
                val json = JSONObject(jsonText)

                val optionsEn = listOf(
                    json.getString("optionA_En"),
                    json.getString("optionB_En"),
                    json.getString("optionC_En"),
                    json.getString("optionD_En")
                )
                val optionsHi = listOf(
                    json.getString("optionA_Hi"),
                    json.getString("optionB_Hi"),
                    json.getString("optionC_Hi"),
                    json.getString("optionD_Hi")
                )
                val correctIndex = json.optInt("correctIndex", 0).coerceIn(0, 3)

                repository.createPhysicsQuizItem(
                    topic = json.optString("topic", topic),
                    partType = json.optString("partType", partType),
                    questionEn = json.getString("questionEn"),
                    questionHi = json.getString("questionHi"),
                    optionsEn = optionsEn,
                    optionsHi = optionsHi,
                    correctIndex = correctIndex,
                    explanationEn = json.getString("explanationEn"),
                    explanationHi = json.getString("explanationHi")
                ).getOrThrow()

                _aiActionState.value = AiActionState.Success(
                    message = "New CSIR NET Physics ($topic) Question Ready!",
                    spokenText = if (currentSettings.voiceReadbackEnabled) {
                        "New CSIR Physics question on $topic is ready."
                    } else null
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "CSIR Physics quiz generation timed out", e)
                _aiActionState.value = AiActionState.Error("CSIR Physics question generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "CSIR Physics quiz generation failed", e)
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to generate CSIR Physics question.")
            }
        }
    }

    fun selectPhysicsQuizAnswer(item: PhysicsQuizItem, selectedOption: Int) {
        viewModelScope.launch {
            try {
                repository.updatePhysicsQuizSelection(item, selectedOption).getOrThrow()
                val isCorrect = selectedOption.toLong() == item.correctIndex
                val currentSettings = settingsState.value
                val msg = if (isCorrect) {
                    "Correct Answer! (+${if (item.partType == "PART_C") "5.0" else "3.5"} CSIR Marks) • सही उत्तर!"
                } else {
                    "Answer Recorded — Review the step-by-step CSIR solution below • व्याख्या देखें"
                }
                _aiActionState.value = AiActionState.Success(
                    message = msg,
                    spokenText = if (currentSettings.voiceReadbackEnabled) {
                        if (isCorrect) "Correct answer! ${item.explanationEn}" else "Not quite right. ${item.explanationEn}"
                    } else null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to save quiz answer")
            }
        }
    }

    // --- 5. DAILY THOUGHT / QUOTE POSTER (HINDI & ENGLISH) ---

    fun generateDailyThoughtPoster(customTheme: String = "", preferredPalette: String = "SAFFRON_GOLD") {
        viewModelScope.launch {
            val currentSettings = settingsState.value
            _aiActionState.value = AiActionState.Working("Designing bilingual Hindi & English Thought of the Day Poster...")
            try {
                val themeHint = if (customTheme.isNotBlank()) "Focus on theme: $customTheme." else "Theme: Scientific excellence, perseverance, self-mastery, and wisdom."
                val prompt = """
                    Create an inspiring 'Thought of the Day / Quote Poster' in BOTH English and Hindi (Devanagari script).
                    $themeHint
                    Preferred visual palette: $preferredPalette (Allowed: SAFFRON_GOLD, ROYAL_INDIGO, EMERALD_DAWN, CRIMSON_LOTUS).
                    Make both the English quote and the Hindi quote poetic, powerful, and memorable, with a clear daily reflection note.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    thoughtPosterModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
                val json = JSONObject(jsonText)

                val themeTitle = json.getString("themeTitle")
                val quoteEn = json.getString("quoteEnglish")
                val quoteHi = json.getString("quoteHindi")
                val author = json.getString("authorOrSource")
                val reflection = json.getString("reflectionNote")
                val palette = json.optString("paletteStyle", preferredPalette)

                repository.createThoughtPoster(
                    themeTitle = themeTitle,
                    quoteEnglish = quoteEn,
                    quoteHindi = quoteHi,
                    authorOrSource = author,
                    reflectionNote = reflection,
                    paletteStyle = palette
                ).getOrThrow()

                _aiActionState.value = AiActionState.Success(
                    message = "New Bilingual Thought Poster Created!",
                    spokenText = if (currentSettings.voiceReadbackEnabled) {
                        "Thought of the day: $quoteEn"
                    } else null
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Thought poster generation timed out", e)
                _aiActionState.value = AiActionState.Error("Poster generation timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Thought poster generation failed", e)
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to generate Thought Poster.")
            }
        }
    }

    fun cycleThoughtPosterPalette(poster: DailyThoughtPoster) {
        val palettes = listOf("SAFFRON_GOLD", "ROYAL_INDIGO", "EMERALD_DAWN", "CRIMSON_LOTUS")
        val nextIndex = (palettes.indexOf(poster.paletteStyle) + 1) % palettes.size
        val nextPalette = palettes[nextIndex]
        viewModelScope.launch {
            repository.updateThoughtPosterPalette(poster, nextPalette)
        }
    }

    // --- 6. VOICE-COMMANDED SETTINGS CUSTOMIZATION ---

    fun applyVoiceCommandToSettings(spokenCommand: String) {
        if (spokenCommand.isBlank()) return
        viewModelScope.launch {
            val current = settingsState.value
            _aiActionState.value = AiActionState.Working("Applying voice command: \"$spokenCommand\"...")
            try {
                val prompt = """
                    You are the voice settings controller for the Aura Flow assistant.
                    Current Settings JSON:
                    - autoReplyMessages: ${current.autoReplyMessages}
                    - autoReplyEmails: ${current.autoReplyEmails}
                    - replyTone: ${current.replyTone} (Allowed: EXECUTIVE, FRIENDLY, CONCISE, FORMAL)
                    - hourlyBriefingEnabled: ${current.hourlyBriefingEnabled}
                    - indiaFocusLevel: ${current.indiaFocusLevel} (Allowed: HIGH, BALANCED, GLOBAL_FIRST)
                    - englishDifficulty: ${current.englishDifficulty} (Allowed: BEGINNER, INTERMEDIATE, ADVANCED, BUSINESS_FLUENT)
                    - dailyGoalMinutes: ${current.dailyGoalMinutes} (Allowed integer: 5 to 180)
                    - voiceReadbackEnabled: ${current.voiceReadbackEnabled}
                    - appLanguage: ${current.appLanguage} (Allowed: ENGLISH, HINDI, BILINGUAL)
                    
                    User's Spoken Voice Command: "$spokenCommand"
                    
                    Update ONLY the fields mentioned or implied by the user's voice command, keeping all other fields at their current values.
                    If the user asks for Hindi, set appLanguage to HINDI. If English, set to ENGLISH. If both Hindi and English, set to BILINGUAL.
                    Return the complete updated settings object and a short friendly confirmationSpeech explaining the change.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    voiceSettingsModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Model returned empty response.")
                val json = JSONObject(jsonText)

                val updated = current.copy(
                    userId = currentUserId,
                    autoReplyMessages = json.optBoolean("autoReplyMessages", current.autoReplyMessages),
                    autoReplyEmails = json.optBoolean("autoReplyEmails", current.autoReplyEmails),
                    replyTone = json.optString("replyTone", current.replyTone).let {
                        if (it in listOf("EXECUTIVE", "FRIENDLY", "CONCISE", "FORMAL")) it else current.replyTone
                    },
                    hourlyBriefingEnabled = json.optBoolean("hourlyBriefingEnabled", current.hourlyBriefingEnabled),
                    indiaFocusLevel = json.optString("indiaFocusLevel", current.indiaFocusLevel).let {
                        if (it in listOf("HIGH", "BALANCED", "GLOBAL_FIRST")) it else current.indiaFocusLevel
                    },
                    englishDifficulty = json.optString("englishDifficulty", current.englishDifficulty).let {
                        if (it in listOf("BEGINNER", "INTERMEDIATE", "ADVANCED", "BUSINESS_FLUENT")) it else current.englishDifficulty
                    },
                    dailyGoalMinutes = json.optInt("dailyGoalMinutes", current.dailyGoalMinutes.toInt()).toLong().coerceIn(5L, 180L),
                    voiceReadbackEnabled = json.optBoolean("voiceReadbackEnabled", current.voiceReadbackEnabled),
                    appLanguage = json.optString("appLanguage", current.appLanguage).let {
                        if (it in listOf("ENGLISH", "HINDI", "BILINGUAL")) it else current.appLanguage
                    },
                    lastVoiceCommand = spokenCommand.take(300)
                )

                val confirmationSpeech = json.optString(
                    "confirmationSpeech",
                    "Settings updated from your voice command."
                )

                repository.saveSettings(updated).getOrThrow()

                _aiActionState.value = AiActionState.Success(
                    message = confirmationSpeech,
                    spokenText = if (updated.voiceReadbackEnabled) confirmationSpeech else null
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Voice command parsing timed out", e)
                _aiActionState.value = AiActionState.Error("Voice command processing timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Voice command parsing failed", e)
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Could not apply voice command.")
            }
        }
    }

    fun updateSettingsDirectly(updated: AssistantSettings) {
        viewModelScope.launch {
            try {
                repository.saveSettings(updated.copy(userId = currentUserId)).getOrThrow()
                _aiActionState.value = AiActionState.Success("Assistant settings saved")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _aiActionState.value = AiActionState.Error(e.localizedMessage ?: "Failed to save settings")
            }
        }
    }
}
