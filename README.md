# SlabSnap

An Android application for tracking and managing soccer player cards. Point your camera at a card, and SlabSnap uses on-device AI to extract player details automatically.

## Features

- **Card Scanning** — Use your device camera to scan soccer player cards in real-time. AI extracts key data: player name, country, card rarity, and more.
- **Card Collection** — Scanned cards are stored locally in a Room database for browsing and managing your collection.

## Tech Stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose (latest BOM) |
| Navigation | Navigation 3 |
| DI | Hilt |
| Architecture | MVI |
| Camera | CameraX |
| AI/ML | ML Kit GenAI (on-device, targeting Pixel devices with Tensor chips) |
| Persistence | Room |

## Requirements

- Android API 26+
- Pixel device with Google Tensor chip (for ML Kit GenAI features)

## Building

```bash
./gradlew assembleDebug
```

## Project Structure

```
app/src/main/java/us/kikinsoft/slabsnap/
```

This is currently an empty Android project. Features will be built in phases.

## Status

**Phase: Initial Setup**

The project scaffolding is in place with Jetpack Compose configured. Core features (scanning, collection management) are forthcoming.
