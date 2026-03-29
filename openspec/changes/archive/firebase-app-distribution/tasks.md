# Tasks: Firebase App Distribution

## Code Changes

-   [ ] Add google-services and firebase-appdistribution plugins to `gradle/libs.versions.toml`
-   [ ] Apply plugins in `app/build.gradle.kts` and configure `firebaseAppDistribution` block for debug
-   [ ] Commit `app/google-services.json`
-   [ ] Add `distribute` job to `.github/workflows/release.yaml`
-   [ ] Verify debug build still works with new plugins (`./gradlew assembleDebug`)

## Manual Setup (developer)

-   [ ] Create "internal-testers" group in Firebase Console
-   [ ] Create GCP service account with App Distribution Admin role
-   [ ] Generate JSON key and add as `FIREBASE_SERVICE_ACCOUNT` GitHub Secret (base64-encoded)
-   [ ] Restrict API key in GCP Console (Android package name + SHA-1)

## Validation

-   [ ] Trigger a release and confirm APK appears in Firebase App Distribution console
-   [ ] Confirm tester receives notification via Firebase App Tester app
