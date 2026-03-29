# Design: Firebase App Distribution

## Architecture

```
  Push to main
       │
       ▼
  ┌──────────────────────────────┐
  │  release.yaml                │
  │                              │
  │  Job: release                │
  │    semantic-release          │──── bumps version, creates tag
  │                              │
  │  Job: distribute             │
  │    needs: [release]          │
  │    ┌─────────────────────┐   │
  │    │ Decode SA key       │   │  ← from GH Secret
  │    │ Setup JDK           │   │
  │    │ assembleDebug       │   │  ← build APK
  │    │ appDistribution     │   │  ← upload to Firebase
  │    │   UploadDebug       │   │
  │    └─────────────────────┘   │
  └──────────────────────────────┘
       │
       ▼
  Firebase Console → Testers notified
```

## File Changes

### 1. `gradle/libs.versions.toml`

Add versions and plugin aliases:

-   `google-services` plugin (required for any Firebase integration)
-   `firebase-appdistribution` Gradle plugin

### 2. `app/build.gradle.kts`

-   Apply `com.google.gms.google-services` plugin
-   Apply `com.google.firebase.appdistribution` plugin
-   Configure `firebaseAppDistribution` block on debug build type:
    -   `groups = "internal-testers"` (tester group from Firebase Console)
    -   `releaseNotesFile` or inline release notes with commit SHA

### 3. `build.gradle.kts` (project-level)

-   May need to apply google-services plugin at project level depending on AGP 9.x conventions

### 4. `.github/workflows/release.yaml`

Add a `distribute` job that:

1. Checks out code
2. Decodes `FIREBASE_SERVICE_ACCOUNT` secret → writes to temp file
3. Sets `GOOGLE_APPLICATION_CREDENTIALS` env var
4. Sets up JDK
5. Runs `./gradlew assembleDebug appDistributionUploadDebug`

The job uses `needs: [release]` so it only runs after a successful version bump.

### 5. `app/google-services.json`

Already present (user added it). Commit as-is.

## Manual Setup Required

These steps must be done by the developer in external consoles:

1. **Firebase Console**: Create tester group "internal-testers"
2. **GCP Console**: Create service account with `Firebase App Distribution Admin` role
3. **GCP Console**: Generate JSON key for that service account
4. **GitHub**: Add secret `FIREBASE_SERVICE_ACCOUNT` (base64-encoded JSON key)
5. **GCP Console**: Restrict the API key in `google-services.json` to Android apps + package name + SHA-1

## Security Considerations

-   Service account key is base64-encoded in GitHub Secrets (encrypted at rest, not exposed in logs)
-   Scope the service account to ONLY `roles/firebaseappdistribution.admin` — no other permissions
-   Rotate key every 90 days (set a calendar reminder)
-   `google-services.json` API key is safe for public repos but should be restricted in GCP Console
