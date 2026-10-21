package com.example.data.model

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject

@IgnoreExtraProperties
data class InboxItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val channel: String = "EMAIL", // "EMAIL" or "MESSAGE"
    val sender: String = "",
    val subject: String = "",
    val incomingBody: String = "",
    val aiReplyDraft: String = "",
    val status: String = "PENDING", // "PENDING", "DRAFT_READY", "AUTO_REPLIED", "ARCHIVED"
    val priority: String = "HIGH", // "HIGH", "NORMAL", "LOW"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NewsBriefing(
    @DocumentId val id: String = "",
    val userId: String = "",
    val hourLabel: String = "",
    val indiaHeadline: String = "",
    val indiaSummary: String = "",
    val worldHeadline: String = "",
    val worldSummary: String = "",
    val keyTakeaway: String = "",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class EnglishTask(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dayTopic: String = "",
    val category: String = "SPEAKING", // "VOCABULARY", "SPEAKING", "GRAMMAR", "WRITING"
    val targetPhrase: String = "",
    val meaningAndNuance: String = "",
    val practicePrompt: String = "",
    val userResponse: String = "",
    val aiFeedback: String = "",
    val masteryScore: Long = 0L,
    val completed: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PhysicsQuizItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val topic: String = "Quantum Mechanics",
    val partType: String = "PART_C", // "PART_B" or "PART_C"
    val questionEn: String = "",
    val questionHi: String = "",
    val optionsEn: List<String> = emptyList(),
    val optionsHi: List<String> = emptyList(),
    val correctIndex: Long = 0L,
    val explanationEn: String = "",
    val explanationHi: String = "",
    val selectedIndex: Long = -1L, // -1 if unanswered, 0..3 if answered
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DailyThoughtPoster(
    @DocumentId val id: String = "",
    val userId: String = "",
    val themeTitle: String = "",
    val quoteEnglish: String = "",
    val quoteHindi: String = "",
    val authorOrSource: String = "",
    val reflectionNote: String = "",
    val paletteStyle: String = "SAFFRON_GOLD", // "SAFFRON_GOLD", "ROYAL_INDIGO", "EMERALD_DAWN", "CRIMSON_LOTUS"
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AssistantSettings(
    @DocumentId val id: String = "primary",
    val userId: String = "",
    val autoReplyMessages: Boolean = true,
    val autoReplyEmails: Boolean = true,
    val replyTone: String = "EXECUTIVE", // "EXECUTIVE", "FRIENDLY", "CONCISE", "FORMAL"
    val hourlyBriefingEnabled: Boolean = true,
    val indiaFocusLevel: String = "HIGH", // "HIGH", "BALANCED", "GLOBAL_FIRST"
    val englishDifficulty: String = "BUSINESS_FLUENT", // "BEGINNER", "INTERMEDIATE", "ADVANCED", "BUSINESS_FLUENT"
    val dailyGoalMinutes: Long = 20L,
    val voiceReadbackEnabled: Boolean = true,
    val appLanguage: String = "BILINGUAL", // "ENGLISH", "HINDI", "BILINGUAL"
    val lastVoiceCommand: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}
