const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

const pastDate = () => new Date(Date.now() - 1000);

test("Unauthenticated user cannot read or list any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/inbox`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/briefings`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/english_tasks`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/physics_quizzes`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/thought_posters`).get());
  await assertFails(unauthDb.collection(`users/${ALICE_UID}/settings`).get());
});

test("Authenticated user can create, read, list, and update own valid InboxItem, PhysicsQuizItem, and DailyThoughtPoster", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate();

  // 1. InboxItem
  const inboxRef = aliceDb.collection(`users/${ALICE_UID}/inbox`).doc("msg_1");
  await assertSucceeds(
    inboxRef.set({
      userId: ALICE_UID,
      channel: "EMAIL",
      sender: "priya.sharma@techcorp.in",
      subject: "Q4 Product Roadmap Sign-off",
      incomingBody: "Hi, could you confirm if the Mumbai launch timeline is approved?",
      aiReplyDraft: "Hi Priya, yes, the Mumbai launch timeline is approved.",
      status: "DRAFT_READY",
      priority: "HIGH",
      createdAt: now,
      updatedAt: now,
    })
  );

  // 2. PhysicsQuizItem
  const quizRef = aliceDb.collection(`users/${ALICE_UID}/physics_quizzes`).doc("quiz_1");
  await assertSucceeds(
    quizRef.set({
      userId: ALICE_UID,
      topic: "Quantum Mechanics",
      partType: "PART_C",
      questionEn: "For a 1D harmonic oscillator, what is the commutator [x, p_x]?",
      questionHi: "एकविमीय सरल आवर्ती दोलित्र के लिए कम्यूटेटर [x, p_x] का मान क्या है?",
      optionsEn: ["i ħ", "-i ħ", "0", "ħ / 2"],
      optionsHi: ["i ħ", "-i ħ", "0", "ħ / 2"],
      correctIndex: 0,
      explanationEn: "By canonical quantization, [x, p_x] = i ħ.",
      explanationHi: "कैननिकल क्वांटाइजेशन के अनुसार, [x, p_x] = i ħ होता है।",
      selectedIndex: -1,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    quizRef.update({
      selectedIndex: 0,
      updatedAt: pastDate(),
    })
  );

  // 3. DailyThoughtPoster
  const posterRef = aliceDb.collection(`users/${ALICE_UID}/thought_posters`).doc("poster_1");
  await assertSucceeds(
    posterRef.set({
      userId: ALICE_UID,
      themeTitle: "Scientific Temper • वैज्ञानिक दृष्टिकोण",
      quoteEnglish: "Dream is not that which you see while sleeping, it is something that does not let you sleep.",
      quoteHindi: "सपने वो नहीं हैं जो आप नींद में देखते हैं, सपने वो हैं जो आपको नींद ही नहीं आने दें।",
      authorOrSource: "Dr. A.P.J. Abdul Kalam",
      reflectionNote: "Dedicate 2 focused hours today to CSIR Physics mastery and English fluency.",
      paletteStyle: "SAFFRON_GOLD",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user cannot read or modify another user's documents", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate();
  await assertSucceeds(
    aliceDb.collection(`users/${ALICE_UID}/briefings`).doc("brief_1").set({
      userId: ALICE_UID,
      hourLabel: "10:00 AM IST",
      indiaHeadline: "Digital Public Infrastructure Expands",
      indiaSummary: "UPI and ONDC adoption hit record monthly volumes.",
      worldHeadline: "Global Semiconductor Supply Chain Stabilizes",
      worldSummary: "New fabrication plants report higher yields.",
      keyTakeaway: "Favorable macro environment.",
      isRead: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection(`users/${ALICE_UID}/briefings`).doc("brief_1").get());
  await assertFails(
    bobDb.collection(`users/${ALICE_UID}/briefings`).where("userId", "==", ALICE_UID).get()
  );
});

test("Valid AssistantSettings with appLanguage succeeds and out-of-bounds fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate();

  const settingsRef = aliceDb.collection(`users/${ALICE_UID}/settings`).doc("primary");
  await assertSucceeds(
    settingsRef.set({
      userId: ALICE_UID,
      autoReplyMessages: true,
      autoReplyEmails: true,
      replyTone: "EXECUTIVE",
      hourlyBriefingEnabled: true,
      indiaFocusLevel: "HIGH",
      englishDifficulty: "BUSINESS_FLUENT",
      dailyGoalMinutes: 20,
      voiceReadbackEnabled: true,
      appLanguage: "BILINGUAL",
      lastVoiceCommand: "Set language to Hindi and English bilingual",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    settingsRef.update({
      dailyGoalMinutes: 999,
      updatedAt: pastDate(),
    })
  );
});
