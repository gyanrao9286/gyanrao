package com.example.viewmodel

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.liveGenerationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "LiveGroundingCtrl"

sealed interface LiveVoiceState {
    data object Idle : LiveVoiceState
    data object Connecting : LiveVoiceState
    data class Active(
        val statusText: String,
        val userTranscript: String = "",
        val modelTranscript: String = ""
    ) : LiveVoiceState
    data class Error(val message: String) : LiveVoiceState
}

sealed interface GroundedQueryState {
    data object Idle : GroundedQueryState
    data class Loading(val modeLabel: String, val query: String) : GroundedQueryState
    data class Success(
        val modeLabel: String,
        val query: String,
        val responseText: String
    ) : GroundedQueryState
    data class Error(val message: String) : GroundedQueryState
}

sealed interface AudioTranscribeState {
    data object Idle : AudioTranscribeState
    data object Recording : AudioTranscribeState
    data object Transcribing : AudioTranscribeState
    data class Success(val text: String) : AudioTranscribeState
    data class Error(val message: String) : AudioTranscribeState
}

@OptIn(PublicPreviewAPI::class)
class LiveAndGroundingController(private val scope: CoroutineScope) {

    // 1. Live Voice Conversation Model (gemini-3.8-live)
    private val liveModel by lazy {
        Firebase.ai.liveModel(
            modelName = "gemini-3.8-live",
            generationConfig = liveGenerationConfig {
                responseModality = ResponseModality.AUDIO
                inputAudioTranscription = AudioTranscriptionConfig()
                outputAudioTranscription = AudioTranscriptionConfig()
            },
            systemInstruction = content {
                text(
                    "You are Aura Flow, a friendly, concise bilingual (English and Hindi) AI executive assistant and CSIR NET Physical Sciences tutor. " +
                        "Respond naturally in English, Hindi, or both according to the user's preference."
                )
            }
        )
    }

    // 2. Google Search Grounded Model (gemini-3.8-flash + Tool.googleSearch())
    private val googleSearchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleSearch())
        )
    }

    // 3. Google Maps Grounded Model (gemini-3.8-flash + Tool.googleMaps())
    private val googleMapsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            tools = listOf(Tool.googleMaps())
        )
    }

    // 4. Audio Transcription Model (gemini-3.5-transcribe)
    private val transcribeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-transcribe"
        )
    }

    private var activeLiveSession: LiveSession? = null

    private val _liveVoiceState = MutableStateFlow<LiveVoiceState>(LiveVoiceState.Idle)
    val liveVoiceState: StateFlow<LiveVoiceState> = _liveVoiceState.asStateFlow()

    private val _groundedQueryState = MutableStateFlow<GroundedQueryState>(GroundedQueryState.Idle)
    val groundedQueryState: StateFlow<GroundedQueryState> = _groundedQueryState.asStateFlow()

    private val _transcribeState = MutableStateFlow<AudioTranscribeState>(AudioTranscribeState.Idle)
    val transcribeState: StateFlow<AudioTranscribeState> = _transcribeState.asStateFlow()

    // --- LIVE API (gemini-3.8-live) ---

    fun startLiveVoiceConversation() {
        if (_liveVoiceState.value is LiveVoiceState.Connecting || _liveVoiceState.value is LiveVoiceState.Active) {
            return
        }
        scope.launch {
            _liveVoiceState.value = LiveVoiceState.Connecting
            try {
                val session = withTimeout(35_000L) {
                    liveModel.connect()
                }
                activeLiveSession = session
                _liveVoiceState.value = LiveVoiceState.Active(
                    statusText = "Connected to gemini-3.8-live • Speak freely in Hindi or English!"
                )
                session.startAudioConversation(
                    functionCallHandler = { null },
                    transcriptHandler = { inputTranscript, outputTranscript ->
                        val current = _liveVoiceState.value as? LiveVoiceState.Active
                        val userStr = inputTranscript?.text ?: current?.userTranscript.orEmpty()
                        val modelStr = outputTranscript?.text ?: current?.modelTranscript.orEmpty()
                        _liveVoiceState.value = LiveVoiceState.Active(
                            statusText = "Live Audio Active (gemini-3.8-live)",
                            userTranscript = userStr,
                            modelTranscript = modelStr
                        )
                    },
                    enableInterruptions = true
                )
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Live API connection timed out", e)
                _liveVoiceState.value = LiveVoiceState.Error("Live voice connection timed out. Please try again.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Live API error", e)
                _liveVoiceState.value = LiveVoiceState.Error(
                    e.localizedMessage ?: "Failed to connect to gemini-3.8-live."
                )
            }
        }
    }

    fun sendTextPromptToLiveSession(prompt: String) {
        val session = activeLiveSession ?: return
        if (prompt.isBlank()) return
        scope.launch {
            try {
                session.send(prompt)
                val current = _liveVoiceState.value as? LiveVoiceState.Active
                if (current != null) {
                    _liveVoiceState.value = current.copy(userTranscript = prompt)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed sending prompt to live session", e)
            }
        }
    }

    fun stopLiveVoiceConversation() {
        scope.launch {
            try {
                activeLiveSession?.stopAudioConversation()
                activeLiveSession?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Closing live session warning", e)
            } finally {
                activeLiveSession = null
                _liveVoiceState.value = LiveVoiceState.Idle
            }
        }
    }

    // --- GOOGLE SEARCH GROUNDING (gemini-3.8-flash + Tool.googleSearch()) ---

    fun runGoogleSearchGroundedQuery(query: String, appLanguage: String) {
        if (query.isBlank()) return
        scope.launch {
            _groundedQueryState.value = GroundedQueryState.Loading("Google Search Grounding", query)
            try {
                val langHint = when (appLanguage) {
                    "HINDI" -> "Respond in clear Hindi (Devanagari) with key terms in English."
                    "BILINGUAL" -> "Respond concisely in both English and Hindi (Bilingual)."
                    else -> "Respond in crisp, well-structured English."
                }
                val prompt = """
                    Use Google Search grounding to provide an accurate, up-to-date answer for:
                    "$query"
                    $langHint
                    Include key facts, formulas (if Physics/CSIR related), or latest developments.
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    googleSearchModel.generateContent(prompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    _groundedQueryState.value = GroundedQueryState.Success(
                        modeLabel = "Google Search Grounded (gemini-3.8-flash)",
                        query = query,
                        responseText = text
                    )
                } else {
                    _groundedQueryState.value = GroundedQueryState.Error("Google Search returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Google Search grounding timed out", e)
                _groundedQueryState.value = GroundedQueryState.Error("Google Search grounding timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Search grounding failed", e)
                _groundedQueryState.value = GroundedQueryState.Error(
                    e.localizedMessage ?: "Google Search grounding failed."
                )
            }
        }
    }

    // --- GOOGLE MAPS GROUNDING (gemini-3.8-flash + Tool.googleMaps()) ---

    fun runGoogleMapsGroundedQuery(placeQuery: String, appLanguage: String) {
        if (placeQuery.isBlank()) return
        scope.launch {
            _groundedQueryState.value = GroundedQueryState.Loading("Google Maps Grounding", placeQuery)
            try {
                val langHint = when (appLanguage) {
                    "HINDI" -> "Respond in clear Hindi (Devanagari) and English place names."
                    "BILINGUAL" -> "Respond in both English and Hindi (Bilingual)."
                    else -> "Respond in clear English."
                }
                val prompt = """
                    Use Google Maps grounding to find accurate place details, locations, campuses, or directions context for:
                    "$placeQuery"
                    $langHint
                """.trimIndent()

                val response = withTimeout(60_000L) {
                    googleMapsModel.generateContent(prompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    _groundedQueryState.value = GroundedQueryState.Success(
                        modeLabel = "Google Maps Grounded (gemini-3.8-flash)",
                        query = placeQuery,
                        responseText = text
                    )
                } else {
                    _groundedQueryState.value = GroundedQueryState.Error("Google Maps grounding returned an empty response.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Google Maps grounding timed out", e)
                _groundedQueryState.value = GroundedQueryState.Error("Google Maps grounding timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Maps grounding failed", e)
                _groundedQueryState.value = GroundedQueryState.Error(
                    e.localizedMessage ?: "Google Maps grounding failed."
                )
            }
        }
    }

    fun clearGroundedQuery() {
        _groundedQueryState.value = GroundedQueryState.Idle
    }

    // --- AUDIO TRANSCRIPTION (gemini-3.5-transcribe) ---

    fun markRecordingStarted() {
        _transcribeState.value = AudioTranscribeState.Recording
    }

    fun transcribeRecordedAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp4",
        onTranscribed: (String) -> Unit = {}
    ) {
        scope.launch {
            _transcribeState.value = AudioTranscribeState.Transcribing
            try {
                val requestContent = content {
                    inlineData(audioBytes, mimeType)
                    text("Transcribe the spoken audio accurately. Preserve Hindi or English words as spoken.")
                }
                val response = withTimeout(60_000L) {
                    transcribeModel.generateContent(requestContent)
                }
                val transcript = response.text?.trim().orEmpty()
                if (transcript.isNotBlank()) {
                    _transcribeState.value = AudioTranscribeState.Success(transcript)
                    onTranscribed(transcript)
                } else {
                    _transcribeState.value = AudioTranscribeState.Error("No speech detected in audio.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Audio transcription timed out", e)
                _transcribeState.value = AudioTranscribeState.Error("Audio transcription timed out after 60s.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Audio transcription failed", e)
                _transcribeState.value = AudioTranscribeState.Error(
                    e.localizedMessage ?: "Audio transcription failed."
                )
            }
        }
    }

    fun clearTranscriptionState() {
        _transcribeState.value = AudioTranscribeState.Idle
    }
}
