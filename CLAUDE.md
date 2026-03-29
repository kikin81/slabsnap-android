# CLAUDE.md

## Project Overview

SlabSnap is an Android app for tracking soccer player cards using on-device AI to extract card data from camera input.

Package: `us.kikinsoft.slabsnap`

## Build & Run

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew assembleRelease      # Build release APK (R8 minified)
./gradlew test                 # Run unit tests
./gradlew connectedAndroidTest # Run instrumented tests
./gradlew spotlessCheck        # Lint check
./gradlew spotlessApply        # Auto-fix lint
```

## Tech Stack

-   **Language:** Kotlin 2.3.20
-   **UI:** Jetpack Compose (BOM 2026.03.01)
-   **Navigation:** Navigation 3
-   **DI:** Hilt 2.59.2
-   **Architecture:** MVI (Model-View-Intent)
-   **Camera:** CameraX
-   **ML:** ML Kit GenAI (on-device, Pixel Tensor chips)
-   **Persistence:** Room 2.8.4
-   **Analytics:** Firebase Analytics
-   **Distribution:** Firebase App Distribution (debug builds via CI)
-   **Min SDK:** 26 | **Target SDK:** 36
-   **AGP:** 9.1.0 | **KSP:** 2.3.6 | **JVM Target:** 21 (Zulu)

## Project Structure

```
app/src/main/java/us/kikinsoft/slabsnap/
├── data/
│   ├── local/           # Room DB, DAOs, entities, converters
│   ├── repository/      # Repository implementations
│   └── seed/            # Debug seed data (DatabaseSeeder)
├── di/                  # Hilt modules (Database, Repository, App)
├── domain/repository/   # Repository interfaces
├── navigation/          # Routes and SlabSnapNavigation
└── ui/
    ├── collection/      # Collection list screen (MVI)
    ├── mvi/             # MVI base classes
    ├── scanner/         # Camera scanner screen
    └── theme/           # Material 3 theme
```

## Architecture Guidelines

-   Follow MVI pattern: UI emits Events → ViewModel reduces → State flows back to UI
-   Use Hilt for all DI — constructor injection preferred
-   One Activity architecture with Compose Navigation 3 for screen routing
-   Repository pattern: Room DAOs behind repository interfaces
-   Base `MviViewModel` class for all ViewModels

## Data Model

-   **CollectionSetEntity** — collection sets (name, year, publisher, totalStickers)
-   **StickerEntity** — stickers with FK to collection set (stickerCode, playerName, teamName, metadata map, isOwned)
-   Metadata is a flexible `Map<String, String>` stored as JSON (keys: position, rarity, parallel)

## Code Style

-   Version catalog (`gradle/libs.versions.toml`) for all dependency versions
-   Spotless with KTLint (android_studio style, trailing commas, compose rules)
-   Compose UI organized by feature in `ui/` package
-   Keep ViewModels free of Android framework dependencies where possible
-   Tests use Given-When-Then structure

## CI/CD

-   **ci.yaml** — lint, test (JaCoCo coverage), build on PRs and pushes to main
-   **release.yaml** — semantic-release versioning + Firebase App Distribution upload on new releases
-   JDK version centralized: `.java-version` (CI) and `.sdkmanrc` (local dev) both target Zulu 21
-   JVM compile target centralized in `gradle.properties` (`slabsnap.jvmTarget`)

## Release Configuration

-   R8 minification and resource shrinking enabled for release builds
-   ProGuard rules for: stack traces, Kotlin Serialization, Room
-   Debug builds include seed data (12 stickers, 4 teams) via `DatabaseSeeder`
-   Versioning managed by semantic-release via `gradle.properties`
