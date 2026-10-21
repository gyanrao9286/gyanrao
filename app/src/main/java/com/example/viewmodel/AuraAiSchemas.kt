package com.example.viewmodel

import com.google.firebase.ai.type.Schema

object AuraAiSchemas {

    val replySchema = Schema.obj(
        properties = mapOf(
            "replyDraft" to Schema.string(description = "Polished reply message or email body ready to send"),
            "suggestedPriority" to Schema.enumeration(
                values = listOf("HIGH", "NORMAL", "LOW"),
                description = "Urgency priority of the incoming message"
            )
        )
    )

    val briefingSchema = Schema.obj(
        properties = mapOf(
            "hourLabel" to Schema.string(description = "Short hourly time label, e.g. 'Hourly Pulse • IST'"),
            "indiaHeadline" to Schema.string(description = "Compelling headline on what is new in India right now"),
            "indiaSummary" to Schema.string(description = "2-3 concise sentences summarizing key developments across India (economy, tech, policy, science)"),
            "worldHeadline" to Schema.string(description = "Compelling headline on what is new in the World right now"),
            "worldSummary" to Schema.string(description = "2-3 concise sentences summarizing key global developments (markets, geopolitics, science, AI)"),
            "keyTakeaway" to Schema.string(description = "One sharp executive takeaway for daily planning")
        )
    )

    val englishTaskSchema = Schema.obj(
        properties = mapOf(
            "dayTopic" to Schema.string(description = "Engaging title for today's English learning mission"),
            "category" to Schema.enumeration(
                values = listOf("VOCABULARY", "SPEAKING", "GRAMMAR", "WRITING"),
                description = "Skill category"
            ),
            "targetPhrase" to Schema.string(description = "Target English idiom, executive phrase, or grammar pattern"),
            "meaningAndNuance" to Schema.string(description = "Clear explanation of meaning, tone, and Hindi meaning/context"),
            "practicePrompt" to Schema.string(description = "Interactive prompt asking the user to write or speak a sentence using the target phrase")
        )
    )

    val englishEvalSchema = Schema.obj(
        properties = mapOf(
            "masteryScore" to Schema.integer(description = "Fluency and accuracy score from 0 to 100"),
            "aiFeedback" to Schema.string(description = "Encouraging, specific coaching feedback with a polished native-level version of the user's sentence")
        )
    )

    val physicsQuizSchema = Schema.obj(
        properties = mapOf(
            "topic" to Schema.string(description = "CSIR NET Physical Sciences topic name"),
            "partType" to Schema.enumeration(
                values = listOf("PART_B", "PART_C"),
                description = "CSIR exam section: PART_B (3.5 marks core concept) or PART_C (5 marks analytical)"
            ),
            "questionEn" to Schema.string(description = "Rigorous CSIR NET Physics multiple-choice question in English with mathematical notation"),
            "questionHi" to Schema.string(description = "Accurate Hindi (Devanagari) translation of the same CSIR NET Physics question"),
            "optionA_En" to Schema.string(description = "Option A in English"),
            "optionB_En" to Schema.string(description = "Option B in English"),
            "optionC_En" to Schema.string(description = "Option C in English"),
            "optionD_En" to Schema.string(description = "Option D in English"),
            "optionA_Hi" to Schema.string(description = "Option A in Hindi"),
            "optionB_Hi" to Schema.string(description = "Option B in Hindi"),
            "optionC_Hi" to Schema.string(description = "Option C in Hindi"),
            "optionD_Hi" to Schema.string(description = "Option D in Hindi"),
            "correctIndex" to Schema.integer(description = "Index of the correct option: 0 for A, 1 for B, 2 for C, 3 for D"),
            "explanationEn" to Schema.string(description = "Step-by-step formula derivation and answer explanation in English"),
            "explanationHi" to Schema.string(description = "Step-by-step formula derivation and answer explanation in Hindi (Devanagari)")
        )
    )

    val thoughtPosterSchema = Schema.obj(
        properties = mapOf(
            "themeTitle" to Schema.string(description = "Bilingual poster header, e.g. 'Scientific Temper • वैज्ञानिक दृष्टिकोण'"),
            "quoteEnglish" to Schema.string(description = "Inspiring thought of the day quote in English"),
            "quoteHindi" to Schema.string(description = "Inspiring thought of the day quote in Hindi (Devanagari script)"),
            "authorOrSource" to Schema.string(description = "Author, scientist, philosopher, or classic text source"),
            "reflectionNote" to Schema.string(description = "Bilingual English & Hindi actionable daily mindset note for students and professionals"),
            "paletteStyle" to Schema.enumeration(
                values = listOf("SAFFRON_GOLD", "ROYAL_INDIGO", "EMERALD_DAWN", "CRIMSON_LOTUS"),
                description = "Visual poster color theme"
            )
        )
    )

    val voiceSettingsSchema = Schema.obj(
        properties = mapOf(
            "autoReplyMessages" to Schema.boolean(description = "Whether background message auto-reply is enabled"),
            "autoReplyEmails" to Schema.boolean(description = "Whether background email check and auto-reply is enabled"),
            "replyTone" to Schema.enumeration(
                values = listOf("EXECUTIVE", "FRIENDLY", "CONCISE", "FORMAL"),
                description = "Selected reply tone"
            ),
            "hourlyBriefingEnabled" to Schema.boolean(description = "Whether hourly India and World briefings are enabled"),
            "indiaFocusLevel" to Schema.enumeration(
                values = listOf("HIGH", "BALANCED", "GLOBAL_FIRST"),
                description = "News emphasis level for India vs Global"
            ),
            "englishDifficulty" to Schema.enumeration(
                values = listOf("BEGINNER", "INTERMEDIATE", "ADVANCED", "BUSINESS_FLUENT"),
                description = "English learning difficulty level"
            ),
            "dailyGoalMinutes" to Schema.integer(description = "Daily English learning goal in minutes between 5 and 180"),
            "voiceReadbackEnabled" to Schema.boolean(description = "Whether voice readback is enabled"),
            "appLanguage" to Schema.enumeration(
                values = listOf("ENGLISH", "HINDI", "BILINGUAL"),
                description = "App display language: ENGLISH, HINDI, or BILINGUAL (both Hindi and English)"
            ),
            "confirmationSpeech" to Schema.string(description = "Short friendly 1-sentence confirmation of what settings were updated")
        )
    )
}
