package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AssistantSettings
import com.example.data.model.DailyThoughtPoster
import com.example.data.model.EnglishTask
import com.example.data.model.InboxItem
import com.example.data.model.NewsBriefing
import com.example.data.model.OperationType
import com.example.data.model.PhysicsQuizItem
import com.example.data.model.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AuraRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth get() = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // --- INBOX ITEMS (Background Message & Email Replies) ---

    fun observeInbox(userId: String = requireUserId()): Flow<List<InboxItem>> = flow {
        val path = "users/$userId/inbox"
        emitAll(
            db.collection("users").document(userId).collection("inbox")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(InboxItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createInboxItem(
        channel: String,
        sender: String,
        subject: String,
        incomingBody: String,
        aiReplyDraft: String,
        status: String,
        priority: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val itemId = "inbox_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("inbox").document(itemId)
        val payload = mapOf(
            "userId" to uid,
            "channel" to channel.take(20),
            "sender" to sender.take(120).ifBlank { "Unknown Sender" },
            "subject" to subject.take(200).ifBlank { "No Subject" },
            "incomingBody" to incomingBody.take(2000).ifBlank { "Empty message" },
            "aiReplyDraft" to aiReplyDraft.take(2000),
            "status" to status,
            "priority" to priority,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            itemId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getInboxItemById(targetUserId: String, itemId: String): Result<InboxItem?> = runCatching {
        val docRef = db.collection("users").document(targetUserId).collection("inbox").document(itemId)
        try {
            val snapshot = docRef.get().await()
            snapshot.toObject(InboxItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun updateInboxItem(item: InboxItem): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(item.id.isNotBlank()) { "InboxItem id must not be blank" }
        requireNotNull(item.createdAt) { "InboxItem createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("inbox").document(item.id)
        try {
            docRef.set(
                item.copy(
                    userId = uid,
                    sender = item.sender.take(120).ifBlank { "Sender" },
                    subject = item.subject.take(200).ifBlank { "Subject" },
                    incomingBody = item.incomingBody.take(2000).ifBlank { "Message" },
                    aiReplyDraft = item.aiReplyDraft.take(2000),
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteInboxItem(itemId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("inbox").document(itemId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // --- HOURLY INDIA & WORLD BRIEFINGS ---

    fun observeBriefings(userId: String = requireUserId()): Flow<List<NewsBriefing>> = flow {
        val path = "users/$userId/briefings"
        emitAll(
            db.collection("users").document(userId).collection("briefings")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(NewsBriefing::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createBriefing(
        hourLabel: String,
        indiaHeadline: String,
        indiaSummary: String,
        worldHeadline: String,
        worldSummary: String,
        keyTakeaway: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val briefingId = "brief_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("briefings").document(briefingId)
        val payload = mapOf(
            "userId" to uid,
            "hourLabel" to hourLabel.take(60).ifBlank { "Hourly Briefing" },
            "indiaHeadline" to indiaHeadline.take(300).ifBlank { "India Update" },
            "indiaSummary" to indiaSummary.take(1500).ifBlank { "Summary pending" },
            "worldHeadline" to worldHeadline.take(300).ifBlank { "World Update" },
            "worldSummary" to worldSummary.take(1500).ifBlank { "Summary pending" },
            "keyTakeaway" to keyTakeaway.take(500).ifBlank { "Key takeaway" },
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            briefingId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateBriefing(briefing: NewsBriefing): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(briefing.id.isNotBlank()) { "NewsBriefing id must not be blank" }
        requireNotNull(briefing.createdAt) { "NewsBriefing createdAt must not be null" }
        val docRef = db.collection("users").document(uid).collection("briefings").document(briefing.id)
        try {
            docRef.set(briefing.copy(userId = uid, updatedAt = null)).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- DAILY ENGLISH LEARNING TASKS ---

    fun observeEnglishTasks(userId: String = requireUserId()): Flow<List<EnglishTask>> = flow {
        val path = "users/$userId/english_tasks"
        emitAll(
            db.collection("users").document(userId).collection("english_tasks")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(EnglishTask::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createEnglishTask(
        dayTopic: String,
        category: String,
        targetPhrase: String,
        meaningAndNuance: String,
        practicePrompt: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val taskId = "eng_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("english_tasks").document(taskId)
        val safeCategory = if (category in listOf("VOCABULARY", "SPEAKING", "GRAMMAR", "WRITING")) category else "SPEAKING"
        val payload = mapOf(
            "userId" to uid,
            "dayTopic" to dayTopic.take(150).ifBlank { "Executive English Challenge" },
            "category" to safeCategory,
            "targetPhrase" to targetPhrase.take(300).ifBlank { "Key Phrase" },
            "meaningAndNuance" to meaningAndNuance.take(1000).ifBlank { "Meaning & nuance" },
            "practicePrompt" to practicePrompt.take(600).ifBlank { "Use this phrase in a sentence." },
            "userResponse" to "",
            "aiFeedback" to "",
            "masteryScore" to 0L,
            "completed" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            taskId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateEnglishTask(task: EnglishTask): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "EnglishTask id must not be blank" }
        requireNotNull(task.createdAt) { "EnglishTask createdAt must not be null" }
        val docRef = db.collection("users").document(uid).collection("english_tasks").document(task.id)
        try {
            docRef.set(
                task.copy(
                    userId = uid,
                    userResponse = task.userResponse.take(1500),
                    aiFeedback = task.aiFeedback.take(1500),
                    masteryScore = task.masteryScore.coerceIn(0L, 100L),
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- CSIR NET PHYSICAL SCIENCES QUIZ & ANSWERS ---

    fun observePhysicsQuizzes(userId: String = requireUserId()): Flow<List<PhysicsQuizItem>> = flow {
        val path = "users/$userId/physics_quizzes"
        emitAll(
            db.collection("users").document(userId).collection("physics_quizzes")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PhysicsQuizItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createPhysicsQuizItem(
        topic: String,
        partType: String,
        questionEn: String,
        questionHi: String,
        optionsEn: List<String>,
        optionsHi: List<String>,
        correctIndex: Int,
        explanationEn: String,
        explanationHi: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val quizId = "quiz_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("physics_quizzes").document(quizId)
        val safePart = if (partType in listOf("PART_B", "PART_C")) partType else "PART_C"
        val paddedOptionsEn = (optionsEn.map { it.take(300).ifBlank { "Option" } } + listOf("A", "B", "C", "D")).take(4)
        val paddedOptionsHi = (optionsHi.map { it.take(300).ifBlank { "विकल्प" } } + paddedOptionsEn).take(4)
        val payload = mapOf(
            "userId" to uid,
            "topic" to topic.take(120).ifBlank { "Quantum Mechanics" },
            "partType" to safePart,
            "questionEn" to questionEn.take(1500).ifBlank { "CSIR Physics Problem" },
            "questionHi" to questionHi.take(1500).ifBlank { questionEn.take(1500).ifBlank { "भौतिकी प्रश्न" } },
            "optionsEn" to paddedOptionsEn,
            "optionsHi" to paddedOptionsHi,
            "correctIndex" to correctIndex.coerceIn(0, 3).toLong(),
            "explanationEn" to explanationEn.take(2500).ifBlank { "Detailed derivation." },
            "explanationHi" to explanationHi.take(2500).ifBlank { explanationEn.take(2500).ifBlank { "विस्तृत हल।" } },
            "selectedIndex" to -1L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            quizId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePhysicsQuizSelection(item: PhysicsQuizItem, selectedIndex: Int): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(item.id.isNotBlank()) { "PhysicsQuizItem id must not be blank" }
        requireNotNull(item.createdAt) { "PhysicsQuizItem createdAt must not be null" }
        val docRef = db.collection("users").document(uid).collection("physics_quizzes").document(item.id)
        try {
            docRef.set(
                item.copy(
                    userId = uid,
                    selectedIndex = selectedIndex.coerceIn(-1, 3).toLong(),
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- DAILY THOUGHT / QUOTE POSTERS (HINDI & ENGLISH) ---

    fun observeThoughtPosters(userId: String = requireUserId()): Flow<List<DailyThoughtPoster>> = flow {
        val path = "users/$userId/thought_posters"
        emitAll(
            db.collection("users").document(userId).collection("thought_posters")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(DailyThoughtPoster::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createThoughtPoster(
        themeTitle: String,
        quoteEnglish: String,
        quoteHindi: String,
        authorOrSource: String,
        reflectionNote: String,
        paletteStyle: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val posterId = "poster_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("users").document(uid).collection("thought_posters").document(posterId)
        val safePalette = if (paletteStyle in listOf("SAFFRON_GOLD", "ROYAL_INDIGO", "EMERALD_DAWN", "CRIMSON_LOTUS")) {
            paletteStyle
        } else {
            "SAFFRON_GOLD"
        }
        val payload = mapOf(
            "userId" to uid,
            "themeTitle" to themeTitle.take(120).ifBlank { "Thought of the Day • आज का सुविचार" },
            "quoteEnglish" to quoteEnglish.take(600).ifBlank { "Excellence is a continuous process and not an accident." },
            "quoteHindi" to quoteHindi.take(600).ifBlank { "उत्कृष्टता एक सतत प्रक्रिया है, कोई संयोग नहीं।" },
            "authorOrSource" to authorOrSource.take(120).ifBlank { "Dr. A.P.J. Abdul Kalam" },
            "reflectionNote" to reflectionNote.take(800).ifBlank { "Stay focused on your daily learning and scientific inquiry." },
            "paletteStyle" to safePalette,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            posterId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateThoughtPosterPalette(poster: DailyThoughtPoster, paletteStyle: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        require(poster.id.isNotBlank()) { "DailyThoughtPoster id must not be blank" }
        requireNotNull(poster.createdAt) { "DailyThoughtPoster createdAt must not be null" }
        val safePalette = if (paletteStyle in listOf("SAFFRON_GOLD", "ROYAL_INDIGO", "EMERALD_DAWN", "CRIMSON_LOTUS")) {
            paletteStyle
        } else {
            poster.paletteStyle
        }
        val docRef = db.collection("users").document(uid).collection("thought_posters").document(poster.id)
        try {
            docRef.set(
                poster.copy(
                    userId = uid,
                    paletteStyle = safePalette,
                    updatedAt = null
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- VOICE-CUSTOMIZABLE SETTINGS & LANGUAGE ---

    fun observeSettings(userId: String = requireUserId()): Flow<AssistantSettings?> = flow {
        val path = "users/$userId/settings/primary"
        emitAll(
            db.collection("users").document(userId).collection("settings").document("primary")
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(AssistantSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun saveSettings(settings: AssistantSettings): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("settings").document("primary")
        val safeLanguage = if (settings.appLanguage in listOf("ENGLISH", "HINDI", "BILINGUAL")) {
            settings.appLanguage
        } else {
            "BILINGUAL"
        }
        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "autoReplyMessages" to settings.autoReplyMessages,
                    "autoReplyEmails" to settings.autoReplyEmails,
                    "replyTone" to settings.replyTone,
                    "hourlyBriefingEnabled" to settings.hourlyBriefingEnabled,
                    "indiaFocusLevel" to settings.indiaFocusLevel,
                    "englishDifficulty" to settings.englishDifficulty,
                    "dailyGoalMinutes" to settings.dailyGoalMinutes.coerceIn(5L, 180L),
                    "voiceReadbackEnabled" to settings.voiceReadbackEnabled,
                    "appLanguage" to safeLanguage,
                    "lastVoiceCommand" to settings.lastVoiceCommand.take(300),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val existingCreatedAt = existingSnap.getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: Timestamp.now()
                docRef.set(
                    settings.copy(
                        id = "primary",
                        userId = uid,
                        dailyGoalMinutes = settings.dailyGoalMinutes.coerceIn(5L, 180L),
                        appLanguage = safeLanguage,
                        lastVoiceCommand = settings.lastVoiceCommand.take(300),
                        createdAt = existingCreatedAt,
                        updatedAt = null
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }
}
