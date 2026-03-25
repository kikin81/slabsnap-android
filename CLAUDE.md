# CLAUDE.md

## Project Overview

SlabSnap is an Android app for tracking soccer player cards using on-device AI to extract card data from camera input.

Package: `us.kikinsoft.slabsnap`

## Build & Run

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew test                  # Run unit tests
./gradlew connectedAndroidTest  # Run instrumented tests
```

## Tech Stack & Conventions

-   **Language:** Kotlin
-   **UI:** Jetpack Compose (BOM-managed versions)
-   **Navigation:** Navigation 3
-   **DI:** Hilt
-   **Architecture:** MVI (Model-View-Intent)
-   **Camera:** CameraX
-   **ML:** ML Kit GenAI (on-device, Pixel Tensor chips)
-   **Persistence:** Room
-   **Min SDK:** 26 | **Target SDK:** 36
-   **AGP:** 9.1.0 | **Kotlin:** 2.2.10 | **Gradle:** 9.3.1

## Architecture Guidelines

-   Follow MVI pattern: UI emits Intents, ViewModel processes them through a reducer, State flows back to UI
-   Use Hilt for all dependency injection — constructor injection preferred
-   One Activity architecture with Compose Navigation 3 for screen routing
-   Repository pattern for data access (Room DAOs behind repository interfaces)

## Code Style

-   Use version catalog (`gradle/libs.versions.toml`) for all dependency versions
-   Compose UI in `ui/` package, organized by feature
-   Keep ViewModels free of Android framework dependencies where possible
