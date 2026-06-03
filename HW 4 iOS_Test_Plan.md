# HeyThere! — iOS Test Plan & Implementation Report

**Project:** HeyThere! — location-based social app (iOS: SwiftUI; backend: Firebase)
**Course:** IN4MATX 43 · **Author:** Inesh Agarwal

> **Snapshot policy:** Every section with numbers carries its own `Last updated: YYYY-MM-DD (commit ____)` line. Metrics may drift after submission — the dates are the source of truth.

---

# Part 1 — Test Plan (Strategic)

This is the *design* of testing for HeyThere! iOS. It describes what we would ideally test, even where the current implementation doesn't reach yet.

## 1.1 Scope: what's in, what's out (and why)

| ✅ In scope | Why this matters |
|---|---|
| **Data models** (`User`, `PrivacySettings`, `LocationData`, `BlackoutZone`, `Message`) | Their defaults and `Codable` round-trips are depended on app-wide; a wrong default (e.g. `usePreciseLocation` flipping to `true`) is a silent, high-blast-radius privacy bug. |
| **Age-gate logic** (`ProfileView.userAge`) | Directly controls whether the Save button is enabled and whether the Interests section is visible; an off-by-one means under-18 users can post interests and appear on the map. |
| **`ChatManager` message logic** (sort, send guard, delete) | Core chat feature; message ordering and empty-message guard must be correct. |
| **`MapView` zero-lat filter** | Filters `publicLatitude == 0` pins — a wrong filter shows ghost/logged-out users or hides real ones. |
| **`LoginView` validation** (`resetPassword` empty-email guard) | Only auth-layer logic we own; the rest is delegated to Firebase. |
| **Compose UI flows** (login toggle, profile save button state, settings sign-out) | Where most user interaction happens; validated with XCUITest on the Simulator. |

| ❌ Out of scope | Why excluded |
|---|---|
| Firebase Auth / Firestore / Storage internals | Third-party BaaS — Google tests their own service; we mock the boundary or use the Firebase Emulator, never live prod. |
| `CLLocationManager` / real GPS hardware | Device/network non-deterministic; we mock `CLLocation` inputs instead. |
| `ImagePicker` / camera roll | UIKit image picker requires a physical camera or photo library; not unit-testable. |
| MapKit tile rendering | Third-party SDK; not our code. |
| `IDVerificationView` upload flow end-to-end | Depends on Firebase Storage and a human reviewer; verified manually in the demo. |

## 1.2 Quality goals — what "good enough" looks like

- **No crash / unhandled exception** in the happy paths of: app launch, login, profile save, settings sign-out, map open, chat open.
- **Privacy invariant:** `PrivacySettings.usePreciseLocation` is **OFF** by default — locked by a test so a refactor can't silently flip it.
- **Age-gate integrity:** the Save Profile button is **disabled** for users under 18 and **enabled** for users 18+; this is enforced by tests.
- **Zero-pin filter:** users with `publicLatitude == 0` are **never** shown on the map.
- **Message ordering:** `ChatManager` always returns messages sorted **newest-first**.
- **CI green on every push;** the local unit suite runs in **under 10 seconds** on the Simulator.
- **Target ≥ 70% line coverage** on the logic layer we own (data models, age-gate, chat sort logic, map filter).

## 1.3 Risks & priorities

| Area | Why it's risky / costly | Priority |
|---|---|---|
| `usePreciseLocation` default flips to `true` | Broadcasts exact GPS to all users — privacy leak | **H** |
| Age-gate off-by-one (17 saves, 18 blocked) | Minors can post interests and appear on the map | **H** |
| `LocationManager` writes `(0,0)` when auth fails | Ghost pin at Null Island appears on every user's map | **H** |
| `ChatManager` sort order wrong | Messages appear out of order in the UI | **M** |
| `resetPassword` called with empty email | Should show an error; instead might silently fire a bad request | **M** |
| `fetchAllUsers` zero-lat filter off | Deleted / logged-out users linger as map pins | **M** |
| `Codable` round-trip on `User` / `Message` | Firestore data silently drops fields on decode | **M** |
| Profile save button enabled while `isSaving` | Double-submit race condition to Firestore | **L** |
| iOS ↔ Android feature-parity drift | iOS writes raw GPS; Android applies a 500 m noise offset — inconsistent privacy behavior | **L** |

## 1.4 Strategy — test types and approach per component

**Unit test:** exercises a *single* function or `struct`/`class` in isolation — no Firebase, no network, no device sensors — and asserts its output for given inputs (e.g. a model's default values, or one calculated age).

**Integration test:** exercises *several units/components cooperating across a boundary* — e.g. `Codable` encoding/decoding together with `JSONEncoder`/`JSONDecoder` — to confirm they work together without hitting real infrastructure.

| Component | Test types | Framework | Why this fit |
|---|---|---|---|
| Swift data models | Unit | **XCTest** (in-process) | Pure Swift structs — fast, deterministic, no iOS runtime needed. |
| Age-gate logic (`userAge`) | Unit | **XCTest** | Pure `Calendar` math — no UI or Firebase needed. |
| `ChatManager` sort / send guard | Unit | **XCTest** | In-memory array logic; no Firestore connection. |
| `MapView` zero-lat filter | Unit | **XCTest** | Pure `compactMap` filter logic extracted from the view. |
| `LoginView` empty-email guard | Unit | **XCTest** | Pure string guard — no Auth call needed. |
| `Codable` round-trip (`User`, `Message`) | Integration | **XCTest** | Exercises `JSONEncoder` + `JSONDecoder` boundary together. |
| Firebase Auth / Firestore | Integration *(planned)* | **Firebase Emulator Suite** | Third-party; test against the emulator, never prod. |
| SwiftUI UI flows | UI *(planned)* | **XCUITest** | Validates real user flows on the iOS Simulator. |

## 1.5 Environment & assumptions

- **Xcode 15+**, Swift 5.9, **iOS 17 Simulator** (iPhone 15 or similar). Unit tests run in-process — no device required.
- **Nothing live is contacted in tests:** Firebase is not initialized in the unit test target; no network, no GPS.
- **Fresh test data per test** — each test builds its own model instances; no shared global state.
- **Build prerequisites (gitignored secrets):** `GoogleService-Info.plist` must be present in the app target for the scheme to compile. Obtain from the team before a fresh-clone run.
- **CI (planned):** GitHub Actions on macOS with Xcode running `xcodebuild test -scheme HeyThere -destination 'platform=iOS Simulator,name=iPhone 15'` on every push.

## 1.6 Team roles

| Member | Owns which test categories / components |
|---|---|
| **Inesh Agarwal** | All iOS testing: XCTest unit tests, XCUITest UI flows, Firebase Emulator integration tests |
