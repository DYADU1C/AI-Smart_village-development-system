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
const ADMIN_EMAIL = "yadavdurgesh5492@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validVillagePayload(uid, createdAt, updatedAt) {
  return {
    userId: uid,
    name: "Hiware Bazar",
    district: "Ahmednagar",
    state: "Maharashtra",
    latitude: 19.0462,
    longitude: 74.5931,
    areaSqKm: 9.77,
    dataSourceType: "VERIFIED_FIELD",
    population: {
      total: 1250,
      maleCount: 640,
      femaleCount: 610,
      households: 280,
      youthRatioPct: 34.5,
      workingAgePct: 61.2,
    },
    agriculture: {
      soilType: "Black Cotton",
      annualRainfallMm: 520.0,
      avgTempC: 27.5,
      primarySeason: "Kharif",
      irrigatedLandPct: 78.0,
      cultivableHectares: 640.0,
      majorCrops: "Jowar, Bajra, Onion, Pomegranate",
    },
    healthcare: {
      primaryHealthCenters: 1,
      subCenters: 1,
      doctorsCount: 2,
      nursesCount: 4,
      hospitalBeds: 10,
      ambulanceAvailable: true,
      avgDistanceToHospitalKm: 4.5,
      immunizationPct: 96.0,
    },
    education: {
      primarySchools: 2,
      secondarySchools: 1,
      literacyRatePct: 89.0,
      femaleLiteracyPct: 84.0,
      teachersCount: 16,
      digitalClassrooms: 4,
      dropoutRatePct: 2.1,
    },
    infrastructure: {
      pavedRoadPct: 92.0,
      electricityHoursPerDay: 22.0,
      cleanDrinkingWaterPct: 95.0,
      sanitationCoveragePct: 98.0,
      internetCoveragePct: 76.0,
      solarStreetLights: 64,
    },
    scores: {
      overallScore: 86.4,
      agricultureScore: 83.2,
      healthcareScore: 85.0,
      educationScore: 88.1,
      infrastructureScore: 89.3,
      developmentLevel: "Model Smart Village",
      weakestSector: "Agriculture",
    },
    createdAt,
    updatedAt,
  };
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

test("Unauthenticated users are denied across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("villages").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("villages")
      .doc("v1")
      .set(validVillagePayload(ALICE_UID, now, now))
  );
});

test("User profile creation, update, and privilege escalation denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 2000);

  // Cannot self-assign Admin role
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      roleLabel: "Admin",
      district: "Pune",
      department: "Rural Dev",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid Village Officer creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      roleLabel: "Village Officer",
      district: "Pune",
      department: "Rural Dev",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update succeeds; escalating to Admin fails
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      district: "Satara",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      roleLabel: "Admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can CRUD villages, analyses, and schemes with Kotlin-aligned payloads", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const updated = new Date(Date.now() - 1000);

  // Village create (NO @DocumentId 'id' field)
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("villages")
      .doc("village_1")
      .set(validVillagePayload(ALICE_UID, created, created))
  );

  // Village update preserving createdAt
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("villages")
      .doc("village_1")
      .update({
        name: "Hiware Bazar Updated",
        updatedAt: updated,
      })
  );

  // Reject ghost field / @DocumentId 'id' injection
  const badVillage = validVillagePayload(ALICE_UID, created, created);
  badVillage.id = "village_2";
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("villages")
      .doc("village_2")
      .set(badVillage)
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("villages")
      .doc("village_3")
      .set(validVillagePayload(ALICE_UID, new Date(Date.now() + 60000), created))
  );

  // List villages succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("villages").get()
  );

  // Analysis create & list
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("analyses")
      .doc("analysis_1")
      .set({
        userId: ALICE_UID,
        villageId: "village_1",
        villageName: "Hiware Bazar",
        dataSourceType: "VERIFIED_FIELD",
        overallScore: 86.4,
        developmentLevel: "Model Smart Village",
        weakSectors: ["Agriculture"],
        mlModelSummary: "k-NN tier: Model Smart Village; Regression Score: 86.4",
        aiExecutiveSummary: "Prioritize drip irrigation expansion.",
        recommendations: ["Expand micro-irrigation subsidies"],
        priorityActions: ["Install 10 solar water pumps"],
        createdAt: created,
        updatedAt: created,
      })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("analyses").get()
  );

  // Scheme create & list
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("schemes")
      .doc("scheme_1")
      .set({
        userId: ALICE_UID,
        name: "Jal Jeevan Mission",
        ministry: "Ministry of Jal Shakti",
        sector: "Infrastructure",
        eligibility: "All rural households without functional tap connections.",
        benefits: "Functional Household Tap Connection (FHTC) @ 55 LPCD.",
        officialWebsite: "https://jaljeevanmission.gov.in",
        verifiedOfficial: true,
        createdAt: created,
        updatedAt: created,
      })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("schemes").get()
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice data", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db
      .collection("users")
      .doc(ALICE_UID)
      .collection("villages")
      .doc("alice_v1")
      .set(validVillagePayload(ALICE_UID, created, created));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("villages").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("villages")
      .doc("alice_v1")
      .update({ name: "Hijacked", updatedAt: new Date(Date.now() - 500) })
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("villages")
      .doc("alice_v1")
      .delete()
  );
});

test("Verified admin email can access while unverified email spoof is rejected", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("villages")
      .doc("alice_v1")
      .set(validVillagePayload(ALICE_UID, created, created));
  });

  // Email spoofing with email_verified: false must fail
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  await assertFails(
    spoofDb.collection("users").doc(ALICE_UID).collection("villages").get()
  );

  // Verified admin succeeds
  const adminDb = testEnv.authenticatedContext("admin_uid", {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  await assertSucceeds(
    adminDb.collection("users").doc(ALICE_UID).collection("villages").get()
  );
});
