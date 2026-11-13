const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-manznime-abf4f1c4-4e8a-468c-ad50-43dad6248f89";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function nowMinusMs(ms = 2000) {
  return new Date(Date.now() - ms);
}

function validWatchlistPayload(uid, overrides = {}) {
  const ts = nowMinusMs();
  return {
    userId: uid,
    animeId: 21,
    animeTitle: "One Piece",
    animeImageUrl: "https://cdn.myanimelist.net/images/anime/1244/138851.jpg",
    episodeNumber: 1,
    episodeTitle: "I'm Luffy! The Man Who's Gonna Be King of the Pirates!",
    streamUrl: "https://archive.org/download/sample/ep1.mp4",
    genres: ["Action", "Adventure", "Fantasy"],
    note: "Iconic start!",
    watchedSeconds: 120,
    durationSeconds: 1440,
    isFavorite: true,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validHistoryPayload(uid, overrides = {}) {
  const ts = nowMinusMs();
  return {
    userId: uid,
    animeId: 21,
    animeTitle: "One Piece",
    animeImageUrl: "https://cdn.myanimelist.net/images/anime/1244/138851.jpg",
    lastEpisodeNumber: 1,
    lastEpisodeTitle: "I'm Luffy!",
    positionMs: 45000,
    durationMs: 1440000,
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      databaseId: DATABASE_ID,
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

test("Unauthenticated user cannot read or write watchlist_episodes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("watchlist_episodes").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("watchlist_episodes")
      .doc("21_ep_1")
      .set(validWatchlistPayload(ALICE_UID))
  );
});

test("Authenticated user can create, list, update, and delete own watchlist_episodes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("watchlist_episodes")
    .doc("21_ep_1");

  await assertSucceeds(docRef.set(validWatchlistPayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("watchlist_episodes").get()
  );
  await assertSucceeds(
    docRef.update({
      watchedSeconds: 600,
      note: "Updated note",
      updatedAt: nowMinusMs(500),
    })
  );
  await assertSucceeds(docRef.delete());
});

test("Cross-user access is denied for watchlist_episodes and watch_history", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("watchlist_episodes")
      .doc("21_ep_1")
      .set(validWatchlistPayload(ALICE_UID))
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("watchlist_episodes")
      .doc("21_ep_1")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("watchlist_episodes").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("watch_history")
      .doc("anime_21")
      .set(validHistoryPayload(BOB_UID))
  );
});

test("Shadow update with ghost field or spoofed userId fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("watchlist_episodes")
      .doc("21_ep_1")
      .set(validWatchlistPayload(ALICE_UID, { isAdmin: true }))
  );
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("watchlist_episodes")
      .doc("21_ep_2")
      .set(validWatchlistPayload(BOB_UID))
  );
});

test("Authenticated user can create and update own watch_history", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("watch_history")
    .doc("anime_21");

  await assertSucceeds(docRef.set(validHistoryPayload(ALICE_UID)));
  await assertSucceeds(
    docRef.update({
      lastEpisodeNumber: 2,
      lastEpisodeTitle: "Enter the Great Swordsman!",
      positionMs: 120000,
      updatedAt: nowMinusMs(500),
    })
  );
});
