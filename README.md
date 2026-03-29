# SlabSnap

[![CI](https://github.com/kikin81/slabsnap-android/actions/workflows/ci.yaml/badge.svg)](https://github.com/kikin81/slabsnap-android/actions/workflows/ci.yaml)
[![Release](https://img.shields.io/github/v/release/kikin81/slabsnap-android)](https://github.com/kikin81/slabsnap-android/releases/latest)

An Android application for tracking and managing soccer player cards. Point your camera at a card, and SlabSnap uses on-device AI to extract player details automatically.

## Features

-   **Card Scanning** — Use your device camera to scan soccer player cards in real-time. AI extracts key data: player name, country, card rarity, and more.
-   **Card Collection** — Scanned cards are stored locally in a Room database for browsing and managing your collection.
-   **Debug Seed Data** — Debug builds come pre-loaded with 12 sample stickers across Argentina, Brazil, France, and Germany for development and testing.

## Tech Stack

| Layer        | Technology                                   |
| ------------ | -------------------------------------------- |
| Language     | Kotlin 2.3.20                                |
| UI           | Jetpack Compose (BOM 2026.03.01)             |
| Navigation   | Navigation 3                                 |
| DI           | Hilt                                         |
| Architecture | MVI (Model-View-Intent)                      |
| Camera       | CameraX                                      |
| AI/ML        | ML Kit GenAI (on-device, Pixel Tensor chips) |
| Persistence  | Room                                         |
| Analytics    | Firebase Analytics                           |
| Distribution | Firebase App Distribution                    |
| CI/CD        | GitHub Actions (semantic-release)            |

## Requirements

-   Android API 26+
-   Pixel device with Google Tensor chip (for ML Kit GenAI features)
-   JDK 21 (use `sdk env` with SDKMAN or install Zulu 21)

## Building

```bash
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK (R8 minified)
./gradlew test             # Unit tests
```

## Project Structure

```
app/src/main/java/us/kikinsoft/slabsnap/
├── data/
│   ├── local/           # Room database, DAOs, entities
│   ├── repository/      # Repository implementations
│   └── seed/            # Debug seed data
├── di/                  # Hilt dependency injection modules
├── domain/repository/   # Repository interfaces
├── navigation/          # Routes and navigation graph
└── ui/
    ├── collection/      # Collection list screen
    ├── mvi/             # MVI base classes
    ├── scanner/         # Camera scanner screen
    └── theme/           # Material 3 theme
```

## CI/CD

Pushes to `main` trigger:

1. **CI** — Lint (Spotless/KTLint), unit tests with coverage, debug build
2. **Release** — Semantic versioning with automatic Firebase App Distribution upload

Debug APKs are distributed to testers via Firebase App Distribution on every new release.

## Status

The project has core infrastructure in place: Room database with collection/sticker entities, MVI architecture, Hilt DI, Firebase integration, CI/CD pipeline with automated distribution, and R8-optimized release builds. Active development on scanning and collection management features.
