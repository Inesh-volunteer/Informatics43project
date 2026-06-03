# HeyThere! — Test Plan & Implementation Report

**Project:** HeyThere! — a location-based social app (Android: Kotlin + Jetpack Compose; iOS: SwiftUI; backend: Firebase).
**Course:** IN4MATX 43 · **Team:** Inesh Agarwal, Sinjon Dearborn, Jayden Jieyu Lee, Annika Liu, Justin Gia Tran

> **Snapshot policy:** Every section with numbers carries its own `Last updated: YYYY-MM-DD (commit ____)` line. Metrics may drift after submission — the dates are the source of truth.

---

# Part 1 — Test Plan (Strategic)

This is the *design* of testing for HeyThere!. It describes what we would ideally test, even where the current implementation (Part 2) doesn't reach yet.

## 1.1 Scope: what's in, what's out (and why)

| ✅ In scope | Why this matters |
|---|---|
| **Data models** (`User`, `PrivacySettings`, `BlackoutZone`, `LocationData`) | Their defaults and value-equality are depended on app-wide (every `currentUser.copy(...)`, every `LocationData` change that triggers a map redraw); a wrong default is a silent, high-blast-radius bug. |
| **Location privacy engine** (`LocationUtils.applyLocationNoise`) | Privacy-critical: it must keep the broadcast point inside the ~500 m noise radius and persist a *stable* offset so users don't "teleport." A bug here leaks real location or breaks the map. |
| **Nearby-user discovery** (activity/tag filtering, distance ranking) | The core feature — deciding who shows up on the map and in what order. *(Planned; see gap in §2.6.)* |
| **Blackout-zone broadcast rule** (hide location inside a zone) | A user-facing privacy promise: inside a saved zone you must disappear. *(Planned.)* |
| **Auth form + flows** (login / sign-up validation, success/failure) | Gateway to the app; empty/invalid credentials and failure paths must behave. *(Planned.)* |
| **Compose UI flows** (login, profile save, settings toggles) | Where most user interaction happens. *(Planned — instrumented.)* |

| ❌ Out of scope | Why excluded |
|---|---|
| Firebase Auth / Firestore / Storage internals | Third-party BaaS — Google tests their own service; we mock the boundary or use the Firebase Emulator, never live prod. |
| Google Maps SDK rendering / map tiles | Third-party; not our code. |
| Real GPS hardware / FusedLocationProvider device behavior | Requires physical-device field testing; not unit-testable. |
| Geocoder address lookup (adding a blackout zone) | Device/network system service; non-deterministic. |
| Coil image loading + image-upload performance | Third-party + network; verified manually in the demo. |
| **iOS (SwiftUI) client** | Separate toolchain (XCTest); this report covers the **Android** module. Tracked as a separate testing pass. |

## 1.2 Quality goals — what "good enough" looks like

- **No crash / unhandled exception** in the happy paths of: app launch, login, profile save, settings change, opening the map.
- **Privacy invariant:** a non-precise broadcast is **always within 500 m** of the true location and **never the exact point**; inside a blackout zone the broadcast is the hidden sentinel `(0, 0)`.
- **Location stability:** a user's noised location does **not change between app launches** (offset persisted to disk).
- **Safe-by-default settings:** precise location **OFF** and background updates **OFF** by default; these defaults are locked by tests so a refactor can't silently flip them.
- **Activity correctness:** only users seen within the last **10 minutes** and broadcasting appear as "active."
- **CI green on every push;** the local unit + integration suite runs in **under 10 seconds**.
- **Target ≥ 70 % line coverage** on the logic layer we own (data models + `LocationUtils`).

## 1.3 Risks & priorities

| Area | Why it's risky / costly | Priority |
|---|---|---|
| Location-noise math or persistence wrong | Leaks real location, or user "teleports" each launch — privacy + trust | **H** |
| Blackout-zone distance check wrong | User broadcasts inside a zone they meant to hide — broken privacy promise | **H** |
| Auth edge cases (empty/invalid creds, token expiry) | Lockout or security exposure (mostly delegated to Firebase) | **H** |
| Stale-user filter (10-min window) off-by-one | Ghost users linger, or active users vanish from the map | **M** |
| Concurrent profile writes (Firestore last-write-wins) | One device's save overwrites another's | **M** |
| Distance ranking / interest-sort ordering | "Nearby" list shows the wrong order | **M** |
| Compose state / recomposition bugs | UI glitches, stale data on screen | **M** |
| Map pin bitmap creation on unusual images | Possible crash rendering a marker | **L** |
| Android ↔ iOS feature-parity drift | Inconsistent behavior across clients | **L** |

## 1.4 Strategy — test types and approach per component

**Unit test (our definition):** exercises a *single* function or class in isolation — no Android runtime, no network — and asserts its output for given inputs (e.g., a model's default values, or one calculation).

**Integration test (our definition):** exercises *several units/components cooperating across a boundary* — e.g., the noise algorithm together with `SharedPreferences` persistence and the Android `Context` — to confirm they work together (here still on the JVM with the framework mocked, not on a device).

| Component | Test types | Framework | Why this fit |
|---|---|---|---|
| Kotlin data models | Unit | **JUnit4** (JVM) | Pure logic — fast, deterministic, no Android needed. |
| Location privacy engine (`LocationUtils`) | Integration | **JUnit4 + Mockito** (JVM) | Needs a `Context`/`SharedPreferences` boundary; we mock the `Context` and fake prefs in-memory to run off-device. |
| Discovery + blackout logic (in `MainActivity` Composables) | Unit / Integration *(planned)* | JUnit4 / Compose UI Test | Logic is currently embedded in `@Composable`s and calls `Location.distanceBetween` — no JVM seam yet (see §2.6). |
| Compose UI screens | UI / instrumented *(planned)* | **androidx.compose.ui.test + Espresso** | Validates real user flows on an emulator. |
| Firebase (Auth / Firestore / Storage) | Integration *(planned)* | **Firebase Emulator Suite** / Mockito | Third-party; test against the emulator, never prod. |
| iOS client (SwiftUI) | Unit / UI *(planned)* | **XCTest** | Separate toolchain; out of scope for this report. |

## 1.5 Environment & assumptions

- **JDK 21** runs Gradle (toolchain is AGP 9.2.1 / Gradle 9.4.1). *Note: the machine's default JDK 26 is too new for this toolchain — use JDK 21.*
- Android **compileSdk 36 / minSdk 26**. Local unit + integration tests run on the **JVM** via `testDebugUnitTest` — **no emulator required**.
- **Nothing live is contacted in tests:** the Android `Context` is mocked (Mockito) and `SharedPreferences` is an in-memory fake; no Firebase, no network, no GPS.
- **Fresh test data per test** — each test builds its own `User`/prefs; no shared global state.
- **Build prerequisites (gitignored secrets):** `app/google-services.json` and `android/secrets.properties` must be present or Gradle configuration fails. Obtain them from the team before a fresh-clone run.
- **CI (planned):** GitHub Actions on Ubuntu with JDK 21 running `./gradlew :app:testDebugUnitTest` + coverage on every push.

## 1.6 Team roles

> Proposed ownership for the testing effort — the team adjusts as needed.

| Member | Owns which test categories / components |
|---|---|
| **Jayden Jieyu Lee** | Test lead — data-model unit tests, `LocationUtils` integration tests, JaCoCo coverage tooling, this TEST_PLAN. |
| **Sinjon Dearborn** | Location-privacy + discovery logic (noise, blackout, distance ranking) tests. |
| **Annika Liu** | Compose UI / instrumented flows (login, profile save, settings). |
| **Justin Gia Tran** | Auth flows + Firestore data-integrity tests; CI pipeline. |
| **Inesh Agarwal** | iOS (XCTest) + Firebase Emulator integration tests. |

---

# Part 2 — Tests Implemented + Report

## 2.1 Required minimums

| Category | Required? | Minimum | Status |
|---|---|---|---|
| Unit tests | Required | ≥ 5 | ✅ **8** |
| Integration tests | Required | ≥ 3 | ✅ **3** |

## 2.3 Tests by category (what we wrote)

**Last updated: 2026-05-31 (working tree on commit `c1a1efa`; test code uncommitted)**

| Category | Count | 2+ examples |
|---|---|---|
| **Unit** | 8 | `privacySettings_defaultsArePrivacySafe` — locks precise-OFF / background-OFF defaults; `blackoutZone_defaultRadiusIs200Meters`; `user_copyOverridesOnlyTargetedField`; `locationData_hasValueEquality` |
| **Integration** | 3 | `firstCall_persistsOffset_andStaysWithinPrivacyRadius` — noise math + prefs write + 500 m bound; `repeatedCalls_returnTheSameLocation_noTeleport` — cross-call persistence (the "stable offset" fix); `storedOffset_isReusedAcrossDifferentExactLocations` |

All 8 unit tests live in `UserModelTest`; all 3 integration tests in `LocationUtilsTest` (real `LocationUtils.applyLocationNoise` driven through a mocked `Context` + in-memory `SharedPreferences`).

## 2.4 Where the tests live + how to run them

```
android/app/src/
├── test/java/com/yolojj333/heythere/          # local JVM tests (no emulator)
│   ├── models/UserModelTest.kt                # 8 unit tests
│   └── utils/
│       ├── LocationUtilsTest.kt               # 3 integration tests
│       └── FakeSharedPreferences.kt           # test-only in-memory SharedPreferences
└── androidTest/java/com/yolojj333/heythere/   # instrumented (needs a device/emulator)
    └── ExampleInstrumentedTest.kt             # app-context smoke test (package-bug fixed)
```

**Run commands** (from the repo root; requires JDK 21 and the gitignored secrets — see §1.5):

```bash
cd android

# Unit + integration tests (JVM, no emulator):
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ANDROID_HOME="$HOME/Android/Sdk" \
  ./gradlew :app:testDebugUnitTest

# Coverage HTML report:
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ANDROID_HOME="$HOME/Android/Sdk" \
  ./gradlew :app:createDebugUnitTestCoverageReport
# -> android/app/build/reports/coverage/test/debug/index.html
#    (a committed copy lives in /coverage/index.html)

# Instrumented test (needs a connected device/emulator):
#   ./gradlew :app:connectedDebugAndroidTest
```

**Approximate run-times** — *Last updated: 2026-05-31 (commit `c1a1efa`)*

| Category | Time | Where it runs |
|---|---|---|
| Unit (`UserModelTest`) | ~0.04 s | local + CI |
| Integration (`LocationUtilsTest`) | ~1.9 s (incl. Mockito/ByteBuddy init) | local + CI |
| Full `testDebugUnitTest` task | ~6–31 s (cold compile) | local + CI |

## 2.5 Coverage achieved

**Last updated: 2026-05-31 (working tree on commit `c1a1efa`)** · Tool: **JaCoCo 0.8.12** via AGP `enableUnitTestCoverage` (`createDebugUnitTestCoverageReport`). HTML committed at `/coverage/index.html`.

| Test type | Scope | Coverage |
|---|---|---|
| Unit | `models/` package (User, PrivacySettings, BlackoutZone, LocationData) | **100 % lines** (30/30) |
| Integration | `utils/LocationUtils` | **100 % lines** (19/19), 100 % instructions |
| **Combined (overall module)** | entire `:app` module | **5 % lines** (49/1074) · 3 % instructions |

**What's NOT covered, and why.** The overall module number is low *by design*: ~90 % of the module's instructions are **Jetpack Compose UI** — `MainActivity` (≈6,300 instr.) and the `ui/` screens (≈6,300 instr.) — plus `FirebaseManager` (Firebase I/O) and the theme. These show **0 %** because:

- **Compose UI** can only be exercised by instrumented / Compose-UI tests on an emulator (out of scope for this JVM pass — §1.1).
- **FirebaseManager** is thin I/O over a live backend; meaningful tests need the Firebase Emulator (planned — §2.6).
- The app's **discovery/ranking/blackout logic is embedded inside those Composables**, so it can't be reached from a JVM test without refactoring the app or instrumenting it (§2.6).

The code we *targeted* — every data model and the privacy engine — is at **100 % line coverage**.

## 2.6 Plan-vs-implementation gap

| What the plan called for | What we actually shipped | What blocked us / what we'd add next |
|---|---|---|
| Unit/integration tests for **nearby-user discovery + blackout broadcast** (core feature) | None | Logic is inlined in `@Composable`s and uses `android.location.Location.distanceBetween`; we chose to **not modify app code** to add a test seam. Next: extract the filter/rank/hide rules (or cover via Robolectric/Compose-UI) and table-test them. |
| **Compose UI** instrumented flows (login, profile save, settings) | 1 instrumented smoke test (app-context); **not executed** here | No emulator/CI device in this environment. Next: run `connectedDebugAndroidTest` on a CI emulator. |
| **Auth** validation + success/failure flow tests | None | Validation is delegated to Firebase; flow needs the Auth emulator. Next: add Firebase Auth Emulator tests. |
| **Firebase** integration (profile save/load, image upload) | Mocked boundary only | Avoided live prod. Next: wire the Firebase Emulator Suite. |
| **iOS** XCTest pass | None | Android-focused report. Next: separate iOS testing pass (Inesh). |
| **≥ 70 %** coverage on the owned logic layer | **100 % lines** on models + `LocationUtils` (overall module 5 %) | Met for the targeted layer; module-wide is capped by the un-instrumented UI/Firebase code above. |

---

# Part 3 — Reflection

**1. What did the tests catch that we missed before?** A concrete, real bug: `ExampleInstrumentedTest` asserted the app's package was `com.yolojj333.beacon`, but the app was renamed from "Beacon" to "HeyThere!" and its package is now `com.yolojj333.heythere` (the old name still survives in `BeaconTheme`/`BeaconMapScreen`). That assertion would **fail the moment the instrumented suite ran on a device** — a stale leftover from the rename. We fixed it. Writing the distance assertions also surfaced that the in-app distance formatting uses a locale-dependent `String.format` (it would print `"1,2 km"` in some locales); we logged it as a gap rather than change UI code.

**2. What was hardest to test?** The app's *core* logic — discovery filtering, distance ranking, and the blackout broadcast rule — is written inline inside `@Composable` functions and calls `Location.distanceBetween`, so there is no seam to invoke it from a plain JVM test without either refactoring the app or booting an emulator. We deliberately kept app code unchanged, so this became our main documented gap. Even `LocationUtils` needed a mocked `Context` plus an in-memory `SharedPreferences` fake to drive its persistence path off-device.

**3. What test would we add next?** Make the nearby-user discovery testable (extract or instrument it) and table-test the filter/rank/hide rules — including the 10-minute activity window and the blackout boundary — then add Firebase-Emulator integration tests for profile save/load and auth.

**4. Where did Claude help, and where did it get things wrong?** Help: it verified the toolchain actually runs (catching that **JDK 21**, not the default JDK 26, was required), designed the off-device seam (mock `Context` + fake `SharedPreferences`) so no emulator was needed, and measured + honestly reported coverage. Wrong: it first over-reached by proposing a new "domain layer" — refactoring working app code purely to make it testable — which we rejected in favor of testing the existing code as-is; the plan-vs-implementation gap (§2.6) is the honest cost of that (correct) choice.
