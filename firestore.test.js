const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const ADMIN_UID = "admin_uid_003";
const GROUP_ID = "grp_tre4_main";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msAgo = 5000) {
  return new Date(Date.now() - msAgo);
}

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

async function seedGroupAndMembers() {
  const created = pastDate(20000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("groups").doc(GROUP_ID).set({
      name: "BPSC TRE 4.0 Aspirants",
      description: "Official preparation group",
      inviteCode: "TRE4BIHAR",
      createdByUid: ALICE_UID,
      createdByName: "Rahul Kumar",
      memberCount: 2,
      createdAt: created,
      updatedAt: created,
    });
    await db.collection("groups").doc(GROUP_ID).collection("members").doc(ALICE_UID).set({
      groupId: GROUP_ID,
      userId: ALICE_UID,
      displayName: "Rahul Kumar",
      role: "GROUP_ADMIN",
      gsTestsAttempted: 0,
      gsTotalScore: 0,
      gsAverageScore: 0,
      langStatus: "NOT_ATTEMPTED",
      publicSharing: true,
      joinedAt: created,
      updatedAt: created,
    });
    await db.collection("groups").doc(GROUP_ID).collection("members").doc(BOB_UID).set({
      groupId: GROUP_ID,
      userId: BOB_UID,
      displayName: "Amit Kumar",
      role: "ASPIRANT",
      gsTestsAttempted: 0,
      gsTotalScore: 0,
      gsAverageScore: 0,
      langStatus: "NOT_ATTEMPTED",
      publicSharing: true,
      joinedAt: created,
      updatedAt: created,
    });
  });
}

test("Unauthenticated users cannot read or write any collection", async () => {
  await seedGroupAndMembers();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("groups").doc(GROUP_ID).get());
  await assertFails(
    unauthDb.collection("groups").doc(GROUP_ID).collection("tests").where("groupId", "==", GROUP_ID).get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacker",
      role: "ASPIRANT",
      languagePref: "BILINGUAL",
      publicSharing: true,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("User profile PII isolation, valid Kotlin create/update, and role-escalation defense", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);

  // Valid create (ASPIRANT role, no @DocumentId 'id' key)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Rahul Kumar",
      role: "ASPIRANT",
      languagePref: "BILINGUAL",
      publicSharing: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject self-assigned SUPER_ADMIN on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Amit Kumar",
      role: "SUPER_ADMIN",
      languagePref: "BILINGUAL",
      publicSharing: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Reject cross-user profile read (PII blanket test)
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Valid partial update preserving createdAt and role
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Rahul K.",
      languagePref: "HINDI",
      updatedAt: pastDate(1000),
    })
  );

  // Reject role escalation on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "SUPER_ADMIN",
      updatedAt: pastDate(1000),
    })
  );

  // Shadow update test: reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: pastDate(1000),
    })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Future Rahul",
      updatedAt: new Date(Date.now() + 60000),
    })
  );
});

test("Email spoofing defense: unverified admin email is rejected, verified bootstrap email succeeds", async () => {
  await seedGroupAndMembers();
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "gopal.guptas27@gmail.com",
      email_verified: false,
    })
    .firestore();
  await assertFails(spoofDb.collection("users").doc(ALICE_UID).get());

  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "gopal.guptas27@gmail.com",
      email_verified: true,
    })
    .firestore();
  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      uid: ADMIN_UID,
      displayName: "Gopal Gupta",
      createdAt: pastDate(),
    })
  );
});

test("Query alignment: filtered group queries succeed while unfiltered list queries fail", async () => {
  await seedGroupAndMembers();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("members").where("groupId", "==", GROUP_ID).get()
  );
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("members").get()
  );
  await assertSucceeds(
    bobDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .where("visibleToUids", "array-contains-any", ["ALL", BOB_UID])
      .get()
  );
  // Non-admin member cannot list unapproved/pending tests or query without visibleToUids filter
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("tests").where("groupId", "==", GROUP_ID).get()
  );
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("tests").get()
  );
});

test("Test creation, approval, locked attempt creation, and duplicate submission prevention", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);

  const sampleQuestion = {
    number: 1,
    subject: "Mathematics",
    topic: "Number System",
    source: "NCERT Class 8",
    text: "What is the square root of 144?",
    optionA: "10",
    optionB: "11",
    optionC: "12",
    optionD: "14",
    optionE: "Don't Know / पता नहीं",
    correctOption: "C",
    explanation: "12 x 12 = 144.",
  };

  // Aspirant (Bob) cannot directly publish PUBLISHED test, must submit PENDING
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("tests").doc("test_illegal").set({
      groupId: GROUP_ID,
      title: "Unauthorized Published Set",
      paperType: "COMMON_GS",
      difficulty: "MODERATE",
      status: "PUBLISHED",
      creatorUid: BOB_UID,
      creatorName: "Amit Kumar",
      durationMinutes: 40,
      questionCount: 1,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      questions: [sampleQuestion],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Group Admin (Alice) publishes official test
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("test_01").set({
      groupId: GROUP_ID,
      title: "TRE 4.0 Common Mock Test 01",
      paperType: "COMMON_GS",
      difficulty: "MODERATE",
      status: "PUBLISHED",
      creatorUid: ALICE_UID,
      creatorName: "Rahul Kumar",
      durationMinutes: 40,
      questionCount: 1,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      questions: [sampleQuestion],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob submits attempt with deterministic ID test_01_bob_uid_002
  const attemptId = `test_01_${BOB_UID}`;
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(attemptId).set({
      groupId: GROUP_ID,
      testId: "test_01",
      testTitle: "TRE 4.0 Common Mock Test 01",
      paperType: "COMMON_GS",
      userId: BOB_UID,
      aspirantName: "Amit Kumar",
      answers: { "0": "C" },
      correctCount: 1,
      incorrectCount: 0,
      dontKnowCount: 0,
      notAttemptedCount: 0,
      totalScore: 1.0,
      maxMarks: 1.0,
      percentage: 100.0,
      negativeDeducted: 0.0,
      subjectScores: { Mathematics: 1.0 },
      timeSpentSeconds: 35,
      submittedAt: pastDate(2000),
    })
  );

  // Bob cannot tamper with locked attempt after submission
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(attemptId).update({
      totalScore: 40.0,
    })
  );

  // Bob cannot submit attempt with mismatched attemptId to bypass duplicate check
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("attempts").doc("random_dup_id").set({
      groupId: GROUP_ID,
      testId: "test_01",
      testTitle: "TRE 4.0 Common Mock Test 01",
      paperType: "COMMON_GS",
      userId: BOB_UID,
      aspirantName: "Amit Kumar",
      answers: { "0": "C" },
      correctCount: 1,
      incorrectCount: 0,
      dontKnowCount: 0,
      notAttemptedCount: 0,
      totalScore: 1.0,
      maxMarks: 1.0,
      percentage: 100.0,
      negativeDeducted: 0.0,
      subjectScores: { Mathematics: 1.0 },
      timeSpentSeconds: 10,
      submittedAt: pastDate(1000),
    })
  );
});

test("Interactive photo poll: hidden private answer key, one-vote-per-user, creator finalization", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);

  // Bob creates poll with hidden correctOptionIndex == -1
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("polls").doc("poll_01").set({
      groupId: GROUP_ID,
      creatorUid: BOB_UID,
      creatorName: "Amit Kumar",
      questionTitle: "Which river is known as Sorrow of Bihar?",
      description: "NCERT Geography Class 9",
      options: ["Ganga", "Kosi", "Son", "Gandak", "Don't Know / पता नहीं"],
      status: "OPEN",
      correctOptionIndex: -1,
      voteCounts: [0, 0, 0, 0, 0],
      totalVotes: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob stores hidden private key
  await assertSucceeds(
    bobDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("polls")
      .doc("poll_01")
      .collection("privateKey")
      .doc("answer")
      .set({
        groupId: GROUP_ID,
        pollId: "poll_01",
        creatorUid: BOB_UID,
        correctOptionIndex: 1,
        createdAt: now,
        updatedAt: now,
      })
  );

  // Another aspirant (non-creator, non-admin) cannot read privateKey — here let's test Charlie
  const charlieDb = testEnv.authenticatedContext("charlie_uid").firestore();
  await assertFails(
    charlieDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("polls")
      .doc("poll_01")
      .collection("privateKey")
      .doc("answer")
      .get()
  );

  // Alice votes once
  await assertSucceeds(
    aliceDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("polls")
      .doc("poll_01")
      .collection("votes")
      .doc(ALICE_UID)
      .set({
        groupId: GROUP_ID,
        pollId: "poll_01",
        userId: ALICE_UID,
        voterName: "Rahul Kumar",
        selectedOptionIndex: 1,
        createdAt: pastDate(2000),
      })
  );

  // Alice updates poll voteCounts (+1)
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("polls").doc("poll_01").update({
      voteCounts: [0, 1, 0, 0, 0],
      totalVotes: 1,
      updatedAt: pastDate(1500),
    })
  );

  // Bob (creator) finalizes answer & explanation
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("polls").doc("poll_01").update({
      status: "FINALIZED",
      correctOptionIndex: 1,
      finalExplanation: "Kosi river shifts course frequently causing floods in North Bihar.",
      updatedAt: pastDate(1000),
    })
  );
});

test("Cross-group isolation: non-members cannot read Group A members, tests, attempts, polls, or discussions", async () => {
  await seedGroupAndMembers();
  const outsiderDb = testEnv.authenticatedContext("outsider_uid_999").firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);

  // Seed a COMBINED paperType test (45 min) by Group Admin Alice
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("combined_01").set({
      groupId: GROUP_ID,
      title: "TRE 4.0 Combined GS + Language Paper",
      paperType: "COMBINED",
      difficulty: "MODERATE",
      status: "PUBLISHED",
      creatorUid: ALICE_UID,
      creatorName: "Rahul Kumar",
      durationMinutes: 45,
      questionCount: 1,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      questions: [{ number: 1, subject: "Mathematics", text: "2+2=?", optionA: "3", optionB: "4", optionC: "5", optionD: "6", optionE: "Don't Know", correctOption: "B", explanation: "2+2=4" }],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Outsider cannot read members list, tests list, attempts list, or specific test doc
  await assertFails(
    outsiderDb.collection("groups").doc(GROUP_ID).collection("members").where("groupId", "==", GROUP_ID).get()
  );
  await assertFails(
    outsiderDb.collection("groups").doc(GROUP_ID).collection("tests").where("groupId", "==", GROUP_ID).get()
  );
  await assertFails(
    outsiderDb.collection("groups").doc(GROUP_ID).collection("tests").doc("combined_01").get()
  );
  await assertFails(
    outsiderDb.collection("groups").doc(GROUP_ID).collection("attempts").where("groupId", "==", GROUP_ID).get()
  );
  await assertFails(
    outsiderDb.collection("groups").doc(GROUP_ID).collection("discussions").where("groupId", "==", GROUP_ID).get()
  );

  // Group member Bob CAN post a question error report with targetType='TEST' and SOLUTION commentType
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("rep_01").set({
      groupId: GROUP_ID,
      targetType: "TEST",
      targetId: "combined_01_Q1",
      authorUid: BOB_UID,
      authorName: "Amit Kumar",
      message: "Q1: Please verify NCERT reference",
      commentType: "REPORT_QUESTION",
      createdAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("sol_01").set({
      groupId: GROUP_ID,
      targetType: "POLL",
      targetId: "poll_01",
      authorUid: BOB_UID,
      authorName: "Amit Kumar",
      message: "Step-by-step solution",
      commentType: "SOLUTION",
      createdAt: pastDate(1500),
    })
  );

  // Member cannot tamper gsAverageScore above valid ceiling (150) or assign self GROUP_ADMIN
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("members").doc(BOB_UID).update({
      gsTestsAttempted: 1,
      gsAverageScore: 999.0,
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("members").doc(BOB_UID).update({
      role: "GROUP_ADMIN",
      updatedAt: pastDate(1000),
    })
  );
});

test("Admin daily release workflow: pending sets hidden from invited members until approved, 150-Q types, admin-only banner, and doubt photo upload", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);

  const qSample = {
    number: 1,
    subject: "Mathematics",
    text: "Find x if 2x + 4 = 10",
    optionA: "2",
    optionB: "3",
    optionC: "4",
    optionD: "5",
    optionE: "Not Attempted / नॉट अटेम्प्ट",
    correctOption: "B",
    explanation: "2x = 6 => x = 3",
    section: "SUBJECT",
  };

  // Group Admin creates a PENDING Class 6-8 Math & Science set (120 min) & Class 9-10 Math set (135 min)
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("set_6_8_1").set({
      groupId: GROUP_ID,
      title: "Class 6 to 8 Mathematics and Science Set 1",
      paperType: "CLASS_6_8_MATH_SCIENCE",
      difficulty: "MODERATE",
      status: "PENDING",
      creatorUid: ALICE_UID,
      creatorName: "Rahul Kumar",
      durationMinutes: 120,
      questionCount: 1,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      questions: [qSample],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("set_9_10_1").set({
      groupId: GROUP_ID,
      title: "Class 9-10 Mathematics Set 1",
      paperType: "CLASS_9_10_MATH",
      difficulty: "HARD",
      status: "PENDING",
      creatorUid: ALICE_UID,
      creatorName: "Rahul Kumar",
      durationMinutes: 135,
      questionCount: 1,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      questions: [qSample],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin can list all tests including PENDING
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").where("groupId", "==", GROUP_ID).get()
  );

  // Regular invited member (Bob) CANNOT read Admin's PENDING set before approval
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("tests").doc("set_6_8_1").get()
  );

  // Admin approves set_6_8_1 for today -> Bob CAN now read it!
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("set_6_8_1").update({
      status: "PUBLISHED",
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("tests").doc("set_6_8_1").get()
  );

  // Admin can delete old sets; Bob cannot delete Admin's set
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("tests").doc("set_6_8_1").delete()
  );
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("set_9_10_1").delete()
  );

  // Banner update: Group Admin succeeds, regular aspirant fails
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("currentAffairs").doc("banner_1").set({
      groupId: GROUP_ID,
      title: "PIB Daily Update",
      summary: "Important facts",
      dateLabel: "21 Nov 2026",
      source: "PIB",
      category: "National",
      publishedByUid: ALICE_UID,
      publishedByName: "Rahul Kumar",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("currentAffairs").doc("banner_1").update({
      title: "PIB Updated Banner",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("currentAffairs").doc("banner_1").update({
      title: "Hacked Banner",
      updatedAt: pastDate(1000),
    })
  );

  // Aspirant (Bob) asks a doubt with screenshot photo & another member posts photo solution
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("doubt_img_1").set({
      groupId: GROUP_ID,
      targetType: "GENERAL",
      targetId: "group",
      authorUid: BOB_UID,
      authorName: "Amit Kumar",
      message: "Please solve this question from screenshot",
      commentType: "COMMENT",
      solutionImageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB",
      createdAt: pastDate(1500),
    })
  );
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("doubt_sol_1").set({
      groupId: GROUP_ID,
      targetType: "GENERAL",
      targetId: "group",
      authorUid: ALICE_UID,
      authorName: "Rahul Kumar",
      message: "Here is the handwritten solution photo",
      commentType: "SOLUTION",
      parentCommentId: "doubt_img_1",
      solutionImageBase64: "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB",
      createdAt: pastDate(1000),
    })
  );
});

test("Per-member set approval access control and Group Admin Fresh Launch trial wipe", async () => {
  await seedGroupAndMembers();
  const CHARLIE_UID = "charlie_uid_004";
  const created = pastDate(20000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("groups").doc(GROUP_ID).collection("members").doc(CHARLIE_UID).set({
      groupId: GROUP_ID,
      userId: CHARLIE_UID,
      displayName: "Charlie Kumar",
      role: "ASPIRANT",
      gsTestsAttempted: 2,
      gsTotalScore: 55.0,
      gsAverageScore: 27.5,
      langStatus: "PASS",
      langScore: 18.0,
      publicSharing: true,
      joinedAt: created,
      updatedAt: created,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const now = pastDate(8000);
  const qSample = {
    number: 1,
    subject: "Mathematics",
    text: "2 + 2 = ?",
    optionA: "3",
    optionB: "4",
    optionC: "5",
    optionD: "6",
    optionE: "Not Attempted / नॉट अटेम्प्ट",
    correctOption: "B",
    explanation: "4",
  };

  // PENDING set with non-empty visibleToUids MUST be rejected
  await assertFails(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("bad_pending").set({
      groupId: GROUP_ID,
      title: "Invalid Pending Set",
      paperType: "COMMON_GS",
      difficulty: "MODERATE",
      status: "PENDING",
      creatorUid: ALICE_UID,
      creatorName: "Rahul Kumar",
      durationMinutes: 30,
      questionCount: 1,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      questions: [qSample],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      approvedForAll: false,
      approvedMemberIds: [BOB_UID],
      visibleToUids: [BOB_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin creates PENDING set (visibleToUids = [])
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("selective_set").set({
      groupId: GROUP_ID,
      title: "Common Paper Set 1",
      paperType: "COMMON_GS",
      difficulty: "MODERATE",
      status: "PENDING",
      creatorUid: ALICE_UID,
      creatorName: "Rahul Kumar",
      durationMinutes: 30,
      questionCount: 1,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      questions: [qSample],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      approvedForAll: false,
      approvedMemberIds: [],
      visibleToUids: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Admin approves selective_set ONLY for BOB_UID (NOT Charlie!)
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("selective_set").update({
      status: "PUBLISHED",
      approvedForAll: false,
      approvedMemberIds: [BOB_UID],
      visibleToUids: [BOB_UID],
      updatedAt: pastDate(3000),
    })
  );

  // Bob CAN get & list selective_set
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("tests").doc("selective_set").get()
  );
  const bobList = await assertSucceeds(
    bobDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .where("visibleToUids", "array-contains-any", ["ALL", BOB_UID])
      .get()
  );
  if (bobList.size !== 1) throw new Error(`Expected Bob to see 1 set, saw ${bobList.size}`);

  // Charlie CANNOT get selective_set and sees 0 sets in his list query
  await assertFails(
    charlieDb.collection("groups").doc(GROUP_ID).collection("tests").doc("selective_set").get()
  );
  const charlieList = await assertSucceeds(
    charlieDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .where("visibleToUids", "array-contains-any", ["ALL", CHARLIE_UID])
      .get()
  );
  if (charlieList.size !== 0) throw new Error(`Expected Charlie to see 0 sets, saw ${charlieList.size}`);

  // Admin updates selective_set to ALL members -> Charlie can now read & list it
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("selective_set").update({
      approvedForAll: true,
      approvedMemberIds: [],
      visibleToUids: ["ALL"],
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    charlieDb.collection("groups").doc(GROUP_ID).collection("tests").doc("selective_set").get()
  );

  // Bob submits a trial attempt; Bob cannot delete attempts, Group Admin Alice CAN delete attempts & reset stats
  const attId = `selective_set_${BOB_UID}`;
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(attId).set({
      groupId: GROUP_ID,
      testId: "selective_set",
      testTitle: "Common Paper Set 1",
      paperType: "COMMON_GS",
      userId: BOB_UID,
      aspirantName: "Amit Kumar",
      answers: { "0": "B" },
      correctCount: 1,
      incorrectCount: 0,
      dontKnowCount: 0,
      notAttemptedCount: 0,
      totalScore: 1.0,
      maxMarks: 1.0,
      percentage: 100.0,
      negativeDeducted: 0.0,
      subjectScores: { Mathematics: 1.0 },
      timeSpentSeconds: 20,
      submittedAt: pastDate(1500),
    })
  );
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(attId).delete()
  );
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(attId).delete()
  );

  // Fresh Launch stats reset: Bob cannot reset Charlie's stats; Group Admin Alice CAN reset Charlie's stats to 0
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("members").doc(CHARLIE_UID).update({
      gsTestsAttempted: 0,
      gsTotalScore: 0,
      gsAverageScore: 0,
      langStatus: "NOT_ATTEMPTED",
      updatedAt: pastDate(1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("members").doc(CHARLIE_UID).update({
      gsTestsAttempted: 0,
      gsTotalScore: 0,
      gsAverageScore: 0,
      gsHighestScore: 0,
      gsLowestScore: 0,
      latestGsScore: 0,
      latestGsTestTitle: "",
      weeklyGsTotal: 0,
      weeklyGsCount: 0,
      monthlyGsTotal: 0,
      monthlyGsCount: 0,
      langScore: 0,
      langHindiScore: 0,
      langEnglishScore: 0,
      langStatus: "NOT_ATTEMPTED",
      previousRank: 0,
      updatedAt: pastDate(1000),
    })
  );
});

test("Strict RBAC: USER cannot create even PENDING tests or modify set icons; ADMIN can create new subject paper types and set icons", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);
  const sampleQ = {
    number: 1,
    subject: "Mathematics",
    text: "5 + 5 = ?",
    optionA: "8",
    optionB: "9",
    optionC: "10",
    optionD: "11",
    optionE: "प्रयास नहीं किया गया / Not Attempted",
    correctOption: "C",
    explanation: "5 + 5 = 10",
  };

  // USER (Bob) CANNOT create even a PENDING test set
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("tests").doc("bob_pending_test").set({
      groupId: GROUP_ID,
      title: "Unauthorized Pending Set",
      paperType: "CLASS_6_8_MATH",
      difficulty: "MODERATE",
      status: "PENDING",
      creatorUid: BOB_UID,
      creatorName: "Amit Kumar",
      durationMinutes: 40,
      questionCount: 1,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      questions: [sampleQ],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      createdAt: now,
      updatedAt: now,
    })
  );

  // ADMIN (Alice) CAN create separate subject sets (CLASS_6_8_MATH, CLASS_6_8_SCIENCE, CLASS_9_10_MATH_80) with Set Icon
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("math_6_8_set1").set({
      groupId: GROUP_ID,
      title: "Class 6-8 Mathematics Set 1",
      paperType: "CLASS_6_8_MATH",
      difficulty: "MODERATE",
      status: "PUBLISHED",
      creatorUid: ALICE_UID,
      creatorName: "Rahul Kumar",
      durationMinutes: 40,
      questionCount: 1,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      questions: [sampleQ],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      approvedForAll: true,
      approvedMemberIds: [],
      visibleToUids: ["ALL"],
      iconKey: "CALCULATOR",
      createdAt: now,
      updatedAt: now,
    })
  );

  // USER (Bob) CANNOT update Set Icon
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("tests").doc("math_6_8_set1").update({
      iconKey: "TROPHY",
      updatedAt: pastDate(1000),
    })
  );

  // ADMIN (Alice) CAN update Set Icon
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("math_6_8_set1").update({
      iconKey: "SCIENCE_FLASK",
      updatedAt: pastDate(1000),
    })
  );
});

test("Strict Profile & WhatsApp Privacy: User-to-User blocked, Admin can view group Users, Users can read Official Admin Contact only", async () => {
  await seedGroupAndMembers();
  const CHARLIE_UID = "charlie_uid_005";
  const now = pastDate(8000);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("groups").doc(GROUP_ID).collection("members").doc(CHARLIE_UID).set({
      groupId: GROUP_ID,
      userId: CHARLIE_UID,
      displayName: "Charlie Kumar",
      role: "ASPIRANT",
      gsTestsAttempted: 0,
      gsTotalScore: 0,
      gsAverageScore: 0,
      langStatus: "NOT_ATTEMPTED",
      publicSharing: true,
      joinedAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(); // GROUP_ADMIN
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(); // USER
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore(); // USER

  // Bob saves own profile with optional WhatsApp number
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Amit Kumar",
      email: "amit@example.com",
      whatsappNumber: "+919876543210",
      activeGroupId: GROUP_ID,
      role: "ASPIRANT",
      languagePref: "BILINGUAL",
      publicSharing: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Charlie (another USER) CANNOT read Bob's profile or query group users
  await assertFails(charlieDb.collection("users").doc(BOB_UID).get());
  await assertFails(
    charlieDb.collection("users").where("activeGroupId", "==", GROUP_ID).get()
  );

  // Group Admin (Alice) CAN read Bob's profile and list group users for Manage Users / WhatsApp
  await assertSucceeds(aliceDb.collection("users").doc(BOB_UID).get());
  const adminList = await assertSucceeds(
    aliceDb.collection("users").where("activeGroupId", "==", GROUP_ID).get()
  );
  if (adminList.size !== 1) throw new Error(`Expected Admin to list 1 user, got ${adminList.size}`);

  // Group Admin sets official Admin Contact Profile; Users can read it but NOT modify it
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("adminContact").doc("official").set({
      groupId: GROUP_ID,
      adminUid: ALICE_UID,
      adminName: "Gopal Gupta",
      adminWhatsAppNumber: "+919123456789",
      updatedAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("adminContact").doc("official").get()
  );
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("adminContact").doc("official").update({
      adminWhatsAppNumber: "+910000000000",
      updatedAt: pastDate(1000),
    })
  );
});

test("Doubt Session editing, Helpful answer marking, Poll spam report, and Admin-only App Update releases", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);

  // Bob posts a doubt, Alice replies, Bob marks Alice's reply as Helpful and edits his own doubt
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("doubt_01").set({
      groupId: GROUP_ID,
      targetType: "GENERAL",
      targetId: "group",
      authorUid: BOB_UID,
      authorName: "Amit Kumar",
      message: "How to solve quadratic roots?",
      commentType: "COMMENT",
      createdAt: now,
    })
  );
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("reply_01").set({
      groupId: GROUP_ID,
      targetType: "GENERAL",
      targetId: "group",
      authorUid: ALICE_UID,
      authorName: "Rahul Kumar",
      message: "Use x = (-b ± sqrt(b²-4ac)) / 2a",
      commentType: "SOLUTION",
      parentCommentId: "doubt_01",
      createdAt: pastDate(4000),
    })
  );
  // Bob (author of doubt_01) marks reply_01 helpful & edits own doubt
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("reply_01").update({
      isHelpful: true,
      helpfulMarkedBy: BOB_UID,
      updatedAt: pastDate(1500),
    })
  );
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("doubt_01").update({
      message: "How to solve quadratic roots in Class 10 NCERT?",
      updatedAt: pastDate(1500),
    })
  );
  // Bob CANNOT edit Alice's reply text
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("reply_01").update({
      message: "Tampered reply",
      updatedAt: pastDate(1000),
    })
  );

  // App Updates: USER (Bob) CANNOT publish; ADMIN (Alice) CAN publish; USER CAN read published
  const updatePayload = {
    versionName: "2.1.0",
    versionCode: 2,
    title: "TRE 4.0 v2.1 Update",
    releaseNotes: "Smart 70-Q JSON importer, Set Icons, WhatsApp Support & Stability fixes.",
    updateType: "NORMAL",
    minSupportedVersionCode: 1,
    downloadUrl: "https://example.com/tre4-v2.1.0.apk",
    fileSizeBytes: 18450000,
    sha256Checksum: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    packageName: "com.aistudio.tre4exam.rkqmpz",
    published: true,
    uploadedByUid: ALICE_UID,
    uploadedByName: "Rahul Kumar",
    groupId: GROUP_ID,
    createdAt: now,
    updatedAt: now,
  };
  await assertFails(
    bobDb.collection("appUpdates").doc("rel_bad").set({ ...updatePayload, uploadedByUid: BOB_UID })
  );
  await assertSucceeds(
    aliceDb.collection("appUpdates").doc("rel_2_1_0").set(updatePayload)
  );
  await assertSucceeds(
    bobDb.collection("appUpdates").where("published", "==", true).get()
  );
  await assertFails(
    bobDb.collection("appUpdates").doc("rel_2_1_0").update({ published: false, updatedAt: pastDate(500) })
  );
});

test("TestAttempt accepts optional per-question timing map questionTimesSec and rejects non-map", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(8000);

  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("timed_set_1").set({
      groupId: GROUP_ID,
      title: "Timed Set 1",
      paperType: "COMMON_GS",
      difficulty: "MODERATE",
      status: "PUBLISHED",
      creatorUid: ALICE_UID,
      creatorName: "Rahul Kumar",
      durationMinutes: 30,
      questionCount: 2,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      questions: [{ number: 1, subject: "Mathematics", text: "1+1=?", optionA: "1", optionB: "2", optionC: "3", optionD: "4", optionE: "Not Attempted", correctOption: "B", explanation: "2" }],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      approvedForAll: true,
      approvedMemberIds: [],
      visibleToUids: ["ALL"],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid attempt with questionTimesSec map succeeds
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(`timed_set_1_${BOB_UID}`).set({
      groupId: GROUP_ID,
      testId: "timed_set_1",
      testTitle: "Timed Set 1",
      paperType: "COMMON_GS",
      userId: BOB_UID,
      aspirantName: "Amit Kumar",
      answers: { "0": "B", "1": "A" },
      questionTimesSec: { "0": 18, "1": 34 },
      correctCount: 1,
      incorrectCount: 1,
      dontKnowCount: 0,
      notAttemptedCount: 0,
      totalScore: 0.67,
      maxMarks: 2.0,
      percentage: 33.5,
      negativeDeducted: 0.33,
      subjectScores: { Mathematics: 0.67 },
      timeSpentSeconds: 52,
      submittedAt: pastDate(1000),
    })
  );

  // Invalid attempt with non-map questionTimesSec is rejected
  await assertFails(
    aliceDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(`timed_set_1_${ALICE_UID}`).set({
      groupId: GROUP_ID,
      testId: "timed_set_1",
      testTitle: "Timed Set 1",
      paperType: "COMMON_GS",
      userId: ALICE_UID,
      aspirantName: "Rahul Kumar",
      answers: { "0": "B" },
      questionTimesSec: "invalid_string",
      correctCount: 1,
      incorrectCount: 0,
      dontKnowCount: 0,
      notAttemptedCount: 0,
      totalScore: 1.0,
      maxMarks: 2.0,
      percentage: 50.0,
      negativeDeducted: 0.0,
      subjectScores: { Mathematics: 1.0 },
      timeSpentSeconds: 20,
      submittedAt: pastDate(500),
    })
  );
});

test("v1.2.0 Exam Rules fields, Separated Merit/Language attempt fields, Solution Reattempt isolation, Private Bookmarks, and Admin Issue Resolution", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(); // GROUP_ADMIN
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(); // USER
  const now = pastDate(8000);

  // 1. Admin creates Part 5 Full-Length test with new optional exam rules fields
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("part5_full_1").set({
      groupId: GROUP_ID,
      title: "Part 5 Full Mock (Maths & Science 150Q)",
      paperType: "CLASS_6_8_MATH_SCIENCE",
      setType: "PART_5_FULL_MATH_SCI",
      difficulty: "MODERATE",
      status: "PUBLISHED",
      creatorUid: ALICE_UID,
      creatorName: "Rahul Kumar",
      durationMinutes: 120,
      questionCount: 1,
      positiveMarks: 1.0,
      negativeMarkNumerator: 1,
      negativeMarkDenominator: 3,
      unattemptedPenalty: true,
      optionEMode: "NOT_ATTEMPTED",
      cutoffMarks: 72.0,
      pauseAllowed: true,
      langPassMarks: 9.0,
      langMinHindi: 0.0,
      langMinEnglish: 0.0,
      langFailRankPolicy: "SHOW_WITH_NOT_QUALIFIED_BADGE",
      tieBreakPolicy: "ACCURACY_AND_TIME",
      questions: [{ number: 1, subject: "Mathematics", section: "SUBJECT", text: "2+2=?", optionA: "3", optionB: "4", optionC: "5", optionD: "6", optionE: "Not Attempted", correctOption: "B", explanation: "4" }],
      attemptCount: 0,
      answerReleaseMode: "IMMEDIATE",
      approvedForAll: true,
      approvedMemberIds: [],
      visibleToUids: ["ALL"],
      createdAt: now,
      updatedAt: now,
    })
  );

  // 2. Bob submits Original Attempt with separated Merit (/120) and Qualifying Language fields
  const origAttemptId = `part5_full_1_${BOB_UID}`;
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(origAttemptId).set({
      groupId: GROUP_ID,
      testId: "part5_full_1",
      testTitle: "Part 5 Full Mock (Maths & Science 150Q)",
      paperType: "CLASS_6_8_MATH_SCIENCE",
      setType: "PART_5_FULL_MATH_SCI",
      userId: BOB_UID,
      aspirantName: "Amit Kumar",
      answers: { "0": "B" },
      questionTimesSec: { "0": 15 },
      correctCount: 1,
      incorrectCount: 0,
      dontKnowCount: 0,
      notAttemptedCount: 0,
      totalScore: 84.0,
      maxMarks: 120.0,
      percentage: 70.0,
      negativeDeducted: 2.0,
      meritMarks: 84.0,
      totalMeritMarks: 120.0,
      positiveMarksEarned: 86.0,
      meritCorrectCount: 86,
      meritIncorrectCount: 6,
      meritUnattemptedCount: 28,
      hindiScore: 14.0,
      englishScore: 5.0,
      languageTotalMarks: 19.0,
      languageStatus: "PASS",
      langPassed: true,
      subjectScores: { Mathematics: 28.0, Science: 26.0 },
      timeSpentSeconds: 4200,
      startedAtEpochMillis: Date.now() - 4200000,
      completionStatus: "SUBMITTED",
      submittedAt: pastDate(3000),
    })
  );

  // 3. Bob saves interactive Solution Reattempt separately (NEVER mutates original attempt!)
  const reattemptDocId = `part5_full_1_${BOB_UID}`;
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("solutionReattempts").doc(reattemptDocId).set({
      groupId: GROUP_ID,
      testId: "part5_full_1",
      testTitle: "Part 5 Full Mock (Maths & Science 150Q)",
      userId: BOB_UID,
      aspirantName: "Amit Kumar",
      reattemptAnswers: { "0": "B" },
      correctCount: 1,
      wrongCount: 0,
      totalReattempted: 1,
      updatedAt: pastDate(2000),
    })
  );
  // Bob can update own solution reattempt
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("solutionReattempts").doc(reattemptDocId).update({
      reattemptAnswers: { "0": "B", "1": "A" },
      correctCount: 1,
      wrongCount: 1,
      totalReattempted: 2,
      updatedAt: pastDate(1000),
    })
  );
  // Admin Alice can read Bob's solution reattempt for analytics, while Bob CANNOT write Alice's reattempt
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("solutionReattempts").doc(reattemptDocId).get()
  );
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("solutionReattempts").doc(`part5_full_1_${ALICE_UID}`).set({
      groupId: GROUP_ID,
      testId: "part5_full_1",
      testTitle: "Part 5 Full Mock (Maths & Science 150Q)",
      userId: ALICE_UID,
      aspirantName: "Rahul Kumar",
      reattemptAnswers: { "0": "A" },
      correctCount: 0,
      wrongCount: 1,
      totalReattempted: 1,
      updatedAt: pastDate(1000),
    })
  );
  // Original attempt remains strictly immutable for Bob
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(origAttemptId).update({
      meritMarks: 120.0,
    })
  );

  // 4. Private Bookmarks: owner CRUD succeeds, cross-user read/write denied
  const bmId = `${BOB_UID}_part5_full_1_0`;
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("bookmarks").doc(bmId).set({
      groupId: GROUP_ID,
      userId: BOB_UID,
      testId: "part5_full_1",
      testTitle: "Part 5 Full Mock (Maths & Science 150Q)",
      questionIndex: 0,
      questionNumber: 1,
      subject: "Mathematics",
      questionText: "2+2=?",
      correctOption: "B",
      explanation: "4",
      createdAt: pastDate(2000),
    })
  );
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("bookmarks").where("groupId", "==", GROUP_ID).where("userId", "==", BOB_UID).get()
  );
  await assertFails(
    aliceDb.collection("groups").doc(GROUP_ID).collection("bookmarks").doc(bmId).get()
  );
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("bookmarks").doc(bmId).delete()
  );

  // 5. Admin Resolution of Reported Question Issue on discussions
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("issue_q1").set({
      groupId: GROUP_ID,
      targetType: "TEST",
      targetId: "part5_full_1_Q1",
      authorUid: BOB_UID,
      authorName: "Amit Kumar",
      message: "Q1: Typo in option C",
      commentType: "REPORT_QUESTION",
      createdAt: pastDate(2000),
    })
  );
  // Non-admin Bob cannot resolve reported issue
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("issue_q1").update({
      resolved: true,
      resolvedByUid: BOB_UID,
      resolvedNote: "Self resolved",
      updatedAt: pastDate(1000),
    })
  );
  // Group Admin Alice CAN resolve reported issue
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("discussions").doc("issue_q1").update({
      resolved: true,
      resolvedByUid: ALICE_UID,
      resolvedNote: "Verified and fixed by Admin",
      updatedAt: pastDate(1000),
    })
  );
});

test("v1.3.0 AppUpdate active protection on delete & Unique Banner/Poll ContentViews (Admin-only list)", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(); // GROUP_ADMIN
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(); // USER
  const now = pastDate(8000);

  // 1. Admin creates old inactive release & current active release
  const baseUpdate = {
    title: "BPSSC TRE 4.0 Release",
    releaseNotes: "Safe incremental update",
    updateType: "NORMAL",
    minSupportedVersionCode: 1,
    downloadUrl: "https://example.com/tre4.apk",
    fileSizeBytes: 12000000,
    published: true,
    uploadedByUid: ALICE_UID,
    uploadedByName: "Rahul Kumar",
    groupId: GROUP_ID,
    createdAt: now,
    updatedAt: now,
  };
  await assertSucceeds(
    aliceDb.collection("appUpdates").doc("rel_old_v1").set({
      ...baseUpdate,
      versionName: "1.1.0",
      versionCode: 2,
      isActive: false,
    })
  );
  await assertSucceeds(
    aliceDb.collection("appUpdates").doc("rel_live_v2").set({
      ...baseUpdate,
      versionName: "1.3.0",
      versionCode: 4,
      isActive: true,
    })
  );

  // Non-admin Bob CANNOT delete any update
  await assertFails(bobDb.collection("appUpdates").doc("rel_old_v1").delete());
  // Admin Alice CANNOT delete the active live release
  await assertFails(aliceDb.collection("appUpdates").doc("rel_live_v2").delete());
  // Admin Alice CAN delete the older non-active release
  await assertSucceeds(aliceDb.collection("appUpdates").doc("rel_old_v1").delete());

  // 2. ContentViews: Bob records own deterministic BANNER view; update or spoofing is blocked
  const bobBannerViewId = `BANNER_ca_01_${BOB_UID}`;
  const alicePollViewId = `POLL_poll_01_${ALICE_UID}`;
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("contentViews").doc(bobBannerViewId).set({
      groupId: GROUP_ID,
      contentId: "ca_01",
      contentType: "BANNER",
      userId: BOB_UID,
      userName: "Amit Kumar",
      viewedAt: pastDate(2000),
    })
  );
  // Updating existing view record is strictly blocked (unique view stays immutable)
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("contentViews").doc(bobBannerViewId).update({
      userName: "Changed",
    })
  );
  // Forged docId or userId is rejected
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("contentViews").doc(`BANNER_ca_01_${ALICE_UID}`).set({
      groupId: GROUP_ID,
      contentId: "ca_01",
      contentType: "BANNER",
      userId: BOB_UID,
      userName: "Amit Kumar",
      viewedAt: pastDate(1500),
    })
  );

  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("contentViews").doc(alicePollViewId).set({
      groupId: GROUP_ID,
      contentId: "poll_01",
      contentType: "POLL",
      userId: ALICE_UID,
      userName: "Rahul Kumar",
      viewedAt: pastDate(1000),
    })
  );

  // Regular USER Bob CANNOT list contentViews or read Alice's view record
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("contentViews").where("groupId", "==", GROUP_ID).get()
  );
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("contentViews").doc(alicePollViewId).get()
  );

  // Admin Alice CAN list all contentViews in the group
  const adminViews = await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("contentViews").where("groupId", "==", GROUP_ID).get()
  );
  if (adminViews.size !== 2) throw new Error(`Expected 2 contentViews, saw ${adminViews.size}`);
});

test("v1.4.0 Admin Hiding Language + Common Paper Set 3 (with setNumber & legacy fields) & Live Exams / LiveExamSessions Security Rules", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(); // GROUP_ADMIN
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(); // USER
  const now = pastDate(8000);

  // 1. Seed a published "Language + Common Paper Set 3" test containing setNumber and legacy fields
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("groups").doc(GROUP_ID).collection("tests").doc("test_lang_common_set_3").set({
      groupId: GROUP_ID,
      title: "Language + Common Paper Set 3",
      paperType: "COMBINED",
      difficulty: "MODERATE",
      status: "PUBLISHED",
      creatorUid: ALICE_UID,
      creatorName: "Rahul Kumar",
      durationMinutes: 90,
      questionCount: 70,
      negativeMarkNumerator: 0,
      negativeMarkDenominator: 3,
      unattemptedPenalty: false,
      questions: [
        {
          id: "q1",
          number: 1,
          subject: "GENERAL_STUDIES",
          questionHi: "प्रश्न 1",
          questionEn: "Question 1",
          optionsHi: ["A", "B", "C", "D", "E"],
          optionsEn: ["A", "B", "C", "D", "E"],
          correctOptionIndex: 0,
          explanationHi: "",
          explanationEn: "",
        },
      ],
      attemptCount: 5,
      answerReleaseMode: "IMMEDIATE",
      approvedForAll: true,
      approvedMemberIds: [],
      visibleToUids: ["ALL"],
      setNumber: 3,
      legacyExtraField: "preserved_from_v1",
      createdAt: now,
      updatedAt: now,
    });
  });

  // Non-admin Bob CANNOT hide Language + Common Paper Set 3
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("tests").doc("test_lang_common_set_3").update({
      status: "PENDING",
      approvedForAll: false,
      approvedMemberIds: [],
      visibleToUids: [],
      updatedAt: pastDate(500),
    })
  );

  // Admin Alice CAN hide Language + Common Paper Set 3 without Permission Denied even with setNumber & legacy fields
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("test_lang_common_set_3").update({
      status: "PENDING",
      approvedForAll: false,
      approvedMemberIds: [],
      visibleToUids: [],
      updatedAt: pastDate(500),
    })
  );

  // Admin Alice CAN re-publish Language + Common Paper Set 3
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("tests").doc("test_lang_common_set_3").update({
      status: "PUBLISHED",
      approvedForAll: true,
      approvedMemberIds: [],
      visibleToUids: ["ALL"],
      updatedAt: pastDate(200),
    })
  );

  // 2. Live Exams: Admin schedules and publishes a Live Exam linked via sourceSetId
  const liveExamData = {
    groupId: GROUP_ID,
    sourceSetId: "test_lang_common_set_3",
    sourceSetTitle: "Language + Common Paper Set 3",
    title: "Sunday Mega Live Exam - Language + Common Set 3",
    description: "Synchronized IST Live Exam with Secure Mode",
    category: "LANGUAGE_COMMON",
    paperType: "COMBINED",
    examDateIst: "2026-11-26",
    startTimeIst: "10:00 AM IST",
    startTimeEpochMillis: 1795667400000,
    durationMinutes: 90,
    endTimeEpochMillis: 1795672800000,
    registrationOpenEpochMillis: 1795581000000,
    registrationCloseEpochMillis: 1795667100000,
    resultPublishEpochMillis: 1795676400000,
    resultsPublishedManually: false,
    totalQuestions: 70,
    totalMarks: 70,
    negativeMarkNumerator: 0,
    negativeMarkDenominator: 3,
    eligibilityType: "ALL_USERS",
    maxParticipants: 500,
    lateEntryAllowed: true,
    lateEntryCutoffMinutes: 15,
    secureModeEnabled: true,
    dynamicWatermark: true,
    focusLossDetection: true,
    copyPasteBlock: true,
    navigationExitLock: true,
    warningLimit: 3,
    status: "PUBLISHED",
    createdByUid: ALICE_UID,
    createdByName: "Rahul Kumar",
    createdAt: now,
    updatedAt: now,
  };

  // Non-admin Bob CANNOT create a Live Exam
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("liveExams").doc("live_exam_01").set(liveExamData)
  );

  // Admin Alice CAN create and update a Live Exam
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("liveExams").doc("live_exam_01").set(liveExamData)
  );

  // Student Bob CAN read published Live Exams and register his own LiveExamSession
  const bobSessionId = `live_exam_01_${BOB_UID}`;
  const bobSessionData = {
    groupId: GROUP_ID,
    liveExamId: "live_exam_01",
    sourceSetId: "test_lang_common_set_3",
    userId: BOB_UID,
    userName: "Amit Kumar",
    userEmail: "amit@example.com",
    status: "REGISTERED",
    answeredCount: 0,
    totalQuestions: 70,
    currentQuestionIndex: 0,
    savedAnswers: {},
    violationCount: 0,
    violationLogs: [],
    finalFiveMinAlertShown: false,
    registeredAtEpochMillis: 1795600000000,
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("liveExamSessions").doc(bobSessionId).set(bobSessionData)
  );

  // Student Bob CAN update his session to FLAGGED on reaching 3 violations
  await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("liveExamSessions").doc(bobSessionId).update({
      status: "FLAGGED",
      violationCount: 3,
      violationLogs: ["10:15:02 IST - FOCUS_LOSS (1/3)", "10:18:10 IST - APP_BACKGROUND (2/3)", "10:21:44 IST - BACK_EXIT_ATTEMPT (3/3)"],
      updatedAt: pastDate(100),
    })
  );

  // Student Bob CANNOT self-approve his flagged session
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("liveExamSessions").doc(bobSessionId).update({
      status: "APPROVED",
      updatedAt: pastDate(50),
    })
  );

  // Admin Alice CAN approve or disqualify Bob's flagged session
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("liveExamSessions").doc(bobSessionId).update({
      status: "APPROVED",
      adminModerationNote: "Verified incoming phone call interruption",
      moderatedByUid: ALICE_UID,
      updatedAt: pastDate(20),
    })
  );
});

test("Set-Wise Rank query: authorized Member and Admin can list all eligible attempts filtered strictly by groupId + testId without cross-set leakage", async () => {
  await seedGroupAndMembers();
  const now = pastDate(3000);
  const targetSetId = "set_common_1";
  const otherSetId = "set_common_2";

  // Seed attempts across two different sets via security-rules-disabled admin context
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    const batch = adminDb.batch();
    for (let i = 1; i <= 12; i++) {
      const uid = `aspirant_set1_${i}`;
      const docRef = adminDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(`${targetSetId}_${uid}`);
      batch.set(docRef, {
        groupId: GROUP_ID,
        testId: targetSetId,
        testTitle: "Common Paper Set 1",
        paperType: "COMMON_GS",
        userId: uid,
        aspirantName: `Set1 User ${i}`,
        answers: { "0": "A" },
        correctCount: 30 + (i % 10),
        incorrectCount: 5,
        dontKnowCount: 0,
        notAttemptedCount: 5,
        totalScore: 30.0 + (i % 10),
        maxMarks: 40.0,
        percentage: 75.0,
        negativeDeducted: 0.0,
        subjectScores: { GS: 30.0 },
        timeSpentSeconds: 900,
        submittedAt: now,
      });
    }
    for (let j = 1; j <= 5; j++) {
      const uid = `aspirant_set2_${j}`;
      const docRef = adminDb.collection("groups").doc(GROUP_ID).collection("attempts").doc(`${otherSetId}_${uid}`);
      batch.set(docRef, {
        groupId: GROUP_ID,
        testId: otherSetId,
        testTitle: "Common Paper Set 2",
        paperType: "COMMON_GS",
        userId: uid,
        aspirantName: `Set2 User ${j}`,
        answers: { "0": "B" },
        correctCount: 38,
        incorrectCount: 1,
        dontKnowCount: 0,
        notAttemptedCount: 1,
        totalScore: 38.0,
        maxMarks: 40.0,
        percentage: 95.0,
        negativeDeducted: 0.0,
        subjectScores: { GS: 38.0 },
        timeSpentSeconds: 700,
        submittedAt: now,
      });
    }
    await batch.commit();
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext("outsider_uid_999").firestore();

  // Regular member Bob queries strictly by groupId + targetSetId -> gets ALL 12 attempts for Set 1, ZERO from Set 2
  const bobSnap = await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("attempts")
      .where("groupId", "==", GROUP_ID)
      .where("testId", "==", targetSetId)
      .get()
  );
  assert.equal(bobSnap.size, 12);
  assert.ok(bobSnap.docs.every((d) => d.data().testId === targetSetId));

  // Admin Alice queries strictly by groupId + targetSetId -> gets ALL 12 attempts for Set 1
  const aliceSnap = await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("attempts")
      .where("groupId", "==", GROUP_ID)
      .where("testId", "==", targetSetId)
      .get()
  );
  assert.equal(aliceSnap.size, 12);

  // Unauthorized outsider CANNOT list attempts
  await assertFails(
    outsiderDb.collection("groups").doc(GROUP_ID).collection("attempts")
      .where("groupId", "==", GROUP_ID)
      .where("testId", "==", targetSetId)
      .get()
  );
});

test("Daily Target Set (Aaj Ka Target) banners: Admin creates/updates/deletes for today and next day; Members read only; Outsiders blocked", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const outsiderDb = testEnv.authenticatedContext("outsider_uid_888").firestore();
  const now = pastDate(3000);

  const todayTargetDoc = {
    groupId: GROUP_ID,
    testId: "set_common_1",
    testTitle: "Common Paper Set 1",
    targetDateIst: "2026-11-26",
    bannerTitle: "आज का टारगेट • Aaj Ka Target",
    bannerSubtitle: "आज रात 11:59 बजे से पहले पूरा करें",
    bannerImageBase64: null,
    publishedByUid: ALICE_UID,
    publishedByName: "Rahul Kumar",
    active: true,
    createdAt: now,
    updatedAt: now,
  };

  // Regular member Bob CANNOT create a Daily Target banner
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("dailyTargets").doc("dt_today").set({
      ...todayTargetDoc,
      publishedByUid: BOB_UID,
    })
  );

  // Admin Alice CAN create Today's Target banner and Tomorrow's Next-Day banner
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("dailyTargets").doc("dt_today").set(todayTargetDoc)
  );
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("dailyTargets").doc("dt_tomorrow").set({
      ...todayTargetDoc,
      testId: "set_common_2",
      testTitle: "Common Paper Set 2",
      targetDateIst: "2026-11-27",
      bannerTitle: "कल का टारगेट • Next Day Target",
    })
  );

  // Group member Bob CAN list & read Daily Target banners
  const bobTargets = await assertSucceeds(
    bobDb.collection("groups").doc(GROUP_ID).collection("dailyTargets").where("groupId", "==", GROUP_ID).get()
  );
  assert.equal(bobTargets.size, 2);

  // Regular member Bob CANNOT update or delete Admin's Daily Target banner
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("dailyTargets").doc("dt_today").update({
      bannerTitle: "Hacked Title",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    bobDb.collection("groups").doc(GROUP_ID).collection("dailyTargets").doc("dt_today").delete()
  );

  // Outsider CANNOT read Daily Target banners
  await assertFails(
    outsiderDb.collection("groups").doc(GROUP_ID).collection("dailyTargets").where("groupId", "==", GROUP_ID).get()
  );

  // Admin Alice CAN update and delete Daily Target banners
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("dailyTargets").doc("dt_today").update({
      bannerTitle: "आज का सुपर टारगेट • TRE 4.0",
      updatedAt: pastDate(400),
    })
  );
  await assertSucceeds(
    aliceDb.collection("groups").doc(GROUP_ID).collection("dailyTargets").doc("dt_tomorrow").delete()
  );
});

test("Admin can edit existing and legacy test sets (Save Test Changes) while non-admins and immutable tampering are blocked", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate(10000);

  const sampleQuestion = {
    number: 1,
    subject: "Mathematics",
    topic: "Algebra",
    source: "NCERT Class 8",
    text: "If 2x = 10, find x.",
    optionA: "3",
    optionB: "4",
    optionC: "5",
    optionD: "6",
    optionE: "प्रयास नहीं किया गया / Not Attempted",
    correctOption: "C",
    explanation: "x = 10 / 2 = 5.",
    section: "COMMON",
  };

  // Build a realistic 150-question list (previously exceeded Firestore's 1000-expression limit during MapDiff!)
  const questions150 = Array.from({ length: 150 }, (_, idx) => ({
    ...sampleQuestion,
    number: idx + 1,
    text: `Question #${idx + 1}: If 2x = ${(idx + 1) * 2}, find x.`,
    section: idx < 30 ? "LANGUAGE" : idx < 70 ? "COMMON" : "SUBJECT",
  }));

  // Seed a legacy test document missing optional newer fields & having an extra legacy field
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .doc("legacy_combined_70")
      .set({
        groupId: GROUP_ID,
        title: "Legacy Full 150Q Set 1",
        paperType: "CLASS_6_8_MATH_SCIENCE",
        difficulty: "MODERATE",
        status: "PUBLISHED",
        creatorUid: ALICE_UID,
        creatorName: "A", // legacy short creatorName
        durationMinutes: 120,
        questionCount: 150,
        questions: questions150,
        attemptCount: 3,
        legacyMigrationTag: "v1_seed",
        createdAt: now,
        updatedAt: now,
      });
  });

  // Group Admin (Alice) saves edits on a full 150-question set using the exact TreRepository.updateTestSet payload
  const updatedQuestions150 = questions150.map((q, idx) =>
    idx === 0 ? { ...q, text: "Updated Q1: If 3x = 15, find x." } : q
  );
  await assertSucceeds(
    aliceDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .doc("legacy_combined_70")
      .update({
        title: "Updated Full 150Q Set 1",
        difficulty: "HARD",
        durationMinutes: 120,
        questionCount: 150,
        questions: updatedQuestions150,
        unattemptedPenalty: true,
        answerReleaseMode: "IMMEDIATE",
        iconKey: "combined",
        updatedAt: pastDate(1000),
      })
  );

  // Regular Aspirant (Bob) MUST still be rejected with PERMISSION_DENIED
  await assertFails(
    bobDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .doc("legacy_combined_70")
      .update({
        title: "Bob Hijack Title",
        updatedAt: pastDate(500),
      })
  );

  // Admin MUST NOT mutate immutable creatorUid, attemptCount, or unknown ghost fields
  await assertFails(
    aliceDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .doc("legacy_combined_70")
      .update({
        creatorUid: BOB_UID,
        updatedAt: pastDate(500),
      })
  );
  await assertFails(
    aliceDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .doc("legacy_combined_70")
      .update({
        attemptCount: 999,
        updatedAt: pastDate(500),
      })
  );
  await assertFails(
    aliceDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .doc("legacy_combined_70")
      .update({
        ghostField: "hack",
        updatedAt: pastDate(500),
      })
  );
});

test("7-category default set icons (categoryIcons): Admin CRUD allowed, member read allowed, non-admin write denied", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore(); // GROUP_ADMIN
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore(); // ASPIRANT
  const outsiderDb = testEnv.authenticatedContext("charlie_outsider").firestore();
  const gifDataUri = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";

  // Admin creates category default icon for MATHEMATICS
  await assertSucceeds(
    aliceDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("categoryIcons")
      .doc("MATHEMATICS")
      .set({
        groupId: GROUP_ID,
        categoryKey: "MATHEMATICS",
        iconKey: null,
        customIconBase64: gifDataUri,
        updatedByUid: ALICE_UID,
        updatedAt: pastDate(1000),
      })
  );

  // Invalid category key rejected even for Admin
  await assertFails(
    aliceDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("categoryIcons")
      .doc("UNKNOWN_CAT")
      .set({
        groupId: GROUP_ID,
        categoryKey: "UNKNOWN_CAT",
        iconKey: "CALCULATOR",
        customIconBase64: null,
        updatedByUid: ALICE_UID,
        updatedAt: pastDate(1000),
      })
  );

  // Group member Bob can read & list categoryIcons
  await assertSucceeds(
    bobDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("categoryIcons")
      .doc("MATHEMATICS")
      .get()
  );
  await assertSucceeds(
    bobDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("categoryIcons")
      .where("groupId", "==", GROUP_ID)
      .get()
  );

  // Outsider cannot read
  await assertFails(
    outsiderDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("categoryIcons")
      .doc("MATHEMATICS")
      .get()
  );

  // Regular Aspirant Bob cannot write or delete categoryIcons
  await assertFails(
    bobDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("categoryIcons")
      .doc("SCIENCE")
      .set({
        groupId: GROUP_ID,
        categoryKey: "SCIENCE",
        iconKey: "SCIENCE_FLASK",
        customIconBase64: null,
        updatedByUid: BOB_UID,
        updatedAt: pastDate(1000),
      })
  );
  await assertFails(
    bobDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("categoryIcons")
      .doc("MATHEMATICS")
      .delete()
  );

  // Admin can update and delete category default icon
  await assertSucceeds(
    aliceDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("categoryIcons")
      .doc("MATHEMATICS")
      .delete()
  );
});

test("ExamTest supports animated GIF and transparent PNG bannerImageBase64 on create and admin update", async () => {
  await seedGroupAndMembers();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate(2000);
  const gifBanner = "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7";
  const pngBanner = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=";

  await assertSucceeds(
    aliceDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .doc("test_gif_banner")
      .set({
        groupId: GROUP_ID,
        title: "GIF Banner Set 01",
        paperType: "COMMON_GS",
        difficulty: "MODERATE",
        status: "PUBLISHED",
        creatorUid: ALICE_UID,
        creatorName: "Alice Admin",
        durationMinutes: 30,
        questionCount: 1,
        negativeMarkNumerator: 1,
        negativeMarkDenominator: 3,
        unattemptedPenalty: true,
        questions: [
          {
            id: "q1",
            number: 1,
            subject: "Mathematics",
            text: "2+2?",
            optionA: "3",
            optionB: "4",
            optionC: "5",
            optionD: "6",
            correctOption: "B",
          },
        ],
        attemptCount: 0,
        answerReleaseMode: "IMMEDIATE",
        approvedForAll: true,
        approvedMemberIds: [],
        visibleToUids: ["ALL"],
        bannerImageBase64: gifBanner,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("groups")
      .doc(GROUP_ID)
      .collection("tests")
      .doc("test_gif_banner")
      .update({
        bannerImageBase64: pngBanner,
        updatedAt: pastDate(500),
      })
  );
});

