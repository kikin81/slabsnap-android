# Firebase App Distribution for Debug Builds

## Problem

SlabSnap has no mechanism for distributing builds to testers. There is no Play Store listing yet, and side-loading APKs manually is friction-heavy. The CI pipeline builds debug APKs but discards them.

## Solution

Integrate Firebase App Distribution into the existing release workflow so that every semantic-release version automatically uploads a debug APK to Firebase, where testers can install it via the Firebase App Tester app.

## Decisions

| Decision               | Choice                                                       | Rationale                                                                                                   |
| ---------------------- | ------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------- |
| Auth method            | Service account JSON key in GitHub Secrets                   | Simple, universal plugin compatibility. WIF is overkill for a solo project. Migrate to WIF later if needed. |
| Build variant          | Debug                                                        | No signing config exists for release yet. Debug APK is sufficient for internal testing.                     |
| Trigger                | Release workflow (`release.yaml`)                            | Distribute on new semantic-release versions, not every push. Testers get "blessed" builds.                  |
| Plugin vs CLI          | Gradle plugin (`com.google.firebase.appdistribution`)        | More idiomatic for Android. Keeps distribution config in build logic.                                       |
| `google-services.json` | Committed to repo                                            | Safe for public repos. API key is restricted and embedded in APK binaries anyway.                           |
| Publish task           | Leave existing no-op. Add separate step in release workflow. | Cleaner separation. The no-op satisfies semantic-release; Firebase upload is a distinct CI concern.         |

## Scope

### In scope

-   Add Google Services + Firebase App Distribution Gradle plugins
-   Configure App Distribution for debug build type
-   Add distribution step to `release.yaml` workflow
-   Service account key setup (manual, documented)

### Out of scope

-   Release build signing / keystore configuration
-   Firebase Crashlytics, Analytics, or other Firebase products
-   In-app update alerts (App Distribution SDK)
-   Workload Identity Federation
-   Play Store distribution

## Non-goals

-   Replacing semantic-release or changing the versioning strategy
-   Distributing release (minified) builds — no signing config yet
