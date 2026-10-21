package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AssistantSettings
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AuraRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createAndObserveInbox_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = AuraRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createInboxItem(
                channel = "EMAIL",
                sender = "ananya.verma@finindia.in",
                subject = "Board Deck Review",
                incomingBody = "Please share your thoughts on the Q3 metrics slide.",
                aiReplyDraft = "Hi Ananya, the Q3 metrics look solid. Ready for tomorrow's presentation.",
                status = "DRAFT_READY",
                priority = "HIGH"
            )
        }
        assertTrue(createResult.isSuccess)
        val itemId = createResult.getOrThrow()

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeInbox(aliceUid).first { list -> list.any { it.id == itemId } }
        }
        assertTrue(emitted.any { it.id == itemId && it.sender == "ananya.verma@finindia.in" })
    }

    @Test
    fun createAndObservePhysicsQuizAndPoster_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = AuraRepository(firestore)

        val quizResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createPhysicsQuizItem(
                topic = "Quantum Mechanics",
                partType = "PART_C",
                questionEn = "What is the ground state energy of a 1D harmonic oscillator?",
                questionHi = "एकविमीय सरल आवर्ती दोलित्र की मूल अवस्था ऊर्जा क्या है?",
                optionsEn = listOf("ħω / 2", "ħω", "3ħω / 2", "0"),
                optionsHi = listOf("ħω / 2", "ħω", "3ħω / 2", "0"),
                correctIndex = 0,
                explanationEn = "E_0 = (0 + 1/2)ħω = ħω/2 due to zero-point quantum fluctuations.",
                explanationHi = "शून्य-बिंदु ऊर्जा के कारण मूल अवस्था ऊर्जा E_0 = ħω/2 होती है।"
            )
        }
        assertTrue("createPhysicsQuizItem failed: ${quizResult.exceptionOrNull()}", quizResult.isSuccess)
        val quizId = quizResult.getOrThrow()

        val quizzes = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observePhysicsQuizzes(aliceUid).first { list -> list.any { it.id == quizId } }
        }
        val createdQuiz = quizzes.first { it.id == quizId }
        assertEquals("Quantum Mechanics", createdQuiz.topic)

        val selectResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.updatePhysicsQuizSelection(createdQuiz, 0)
        }
        assertTrue("updatePhysicsQuizSelection failed: ${selectResult.exceptionOrNull()}", selectResult.isSuccess)

        val posterResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createThoughtPoster(
                themeTitle = "Perseverance • दृढ़ संकल्प",
                quoteEnglish = "Dream is not that which you see while sleeping, it is something that does not let you sleep.",
                quoteHindi = "सपने वो नहीं हैं जो आप नींद में देखते हैं, सपने वो हैं जो आपको नींद ही नहीं आने दें।",
                authorOrSource = "Dr. A.P.J. Abdul Kalam",
                reflectionNote = "Stay consistent with daily CSIR Physics problem solving.",
                paletteStyle = "SAFFRON_GOLD"
            )
        }
        assertTrue("createThoughtPoster failed: ${posterResult.exceptionOrNull()}", posterResult.isSuccess)
    }

    @Test
    fun getInboxItemById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = AuraRepository(firestore)
        val itemId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createInboxItem(
                channel = "MESSAGE",
                sender = "+91 98201 11223",
                subject = "Quick Sync",
                incomingBody = "Are you free for a 5-min call?",
                aiReplyDraft = "In a meeting right now, will call you at 5 PM.",
                status = "AUTO_REPLIED",
                priority = "NORMAL"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = AuraRepository(firestore)
        val crossResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getInboxItemById(aliceUid, itemId)
        }
        assertTrue(crossResult.isFailure)
        val ex = crossResult.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull(ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
    }

    @Test
    fun observeInbox_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = AuraRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeInbox("unauth_user_123").first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun saveAndObserveSettings_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = AuraRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveSettings(
                AssistantSettings(
                    userId = aliceUid,
                    autoReplyMessages = true,
                    autoReplyEmails = false,
                    replyTone = "CONCISE",
                    hourlyBriefingEnabled = true,
                    indiaFocusLevel = "HIGH",
                    englishDifficulty = "ADVANCED",
                    dailyGoalMinutes = 30L,
                    voiceReadbackEnabled = true,
                    appLanguage = "HINDI",
                    lastVoiceCommand = "Change language to Hindi and set daily goal to 30 minutes"
                )
            )
        }
        assertTrue("saveSettings failed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val loaded = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeSettings(aliceUid).first { it != null && it.replyTone == "CONCISE" }
        }
        assertNotNull(loaded)
        assertEquals(30L, loaded?.dailyGoalMinutes)
        assertEquals("HINDI", loaded?.appLanguage)
    }

    private companion object {
        const val ALICE_EMAIL = "alice@auraflow.test"
        const val BOB_EMAIL = "bob@auraflow.test"
        const val DEFAULT_TIMEOUT_MS = 15000L
        const val FLOW_TIMEOUT_MS = 10000L
    }
}
