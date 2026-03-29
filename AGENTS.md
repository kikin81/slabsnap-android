# Agent Instructions

This project uses **bd** (beads) for issue tracking. Run `bd onboard` to get started.

**See CLAUDE.md for detailed tech stack, architecture patterns, and conventions.**

## Project Context

SlabSnap is a Kotlin/Android app using Jetpack Compose and MVI architecture. The codebase is organized around clear data/domain/ui layers with Hilt for DI.

## Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --status in_progress  # Claim work
bd close <id>         # Complete work
bd sync               # Sync with git
```

## Build & Test Commands

```bash
./gradlew assembleDebug        # Build debug APK
./gradlew test                  # Run unit tests (JUnit5 with mockk)
./gradlew connectedAndroidTest  # Run instrumented tests
./gradlew spotlessCheck         # Check code formatting
./gradlew spotlessApply         # Auto-fix formatting
```

## Codebase Essentials

**Directory Structure:**

-   `data/` — Room entities, DAOs, repository implementations, local converters
-   `domain/` — Repository interfaces (abstract service boundaries)
-   `ui/` — Compose screens, ViewModels, theme; organized by feature
-   `di/` — Hilt modules (Database, Repository, App-level bindings)
-   `navigation/` — Navigation 3 routes and NavHost setup

**MVI Pattern & Testing:**

-   UI emits `*Event` sealed interfaces → `*ViewModel` (extends `MviViewModel<State, Event, Effect>`) processes via `setState()` reducer
-   State flows as `StateFlow<*State>` where `*State` implements `UiState` marker
-   Effects are one-shot via `Channel<*Effect>` for side effects (navigation, toasts)
-   Unit tests use `turbine` to collect state/effect flows; mock repositories via mockk; use JUnit5 `@Test`
-   Instrumented tests use Hilt's `@HiltAndroidTest` + `HiltTestRunner` custom test runner
-   Test ViewModels with `MainDispatcherExtension` to control coroutine dispatchers

## Landing the Plane (Session Completion)

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed):
    ```bash
    ./gradlew spotlessCheck  # Must pass code formatting checks
    ./gradlew test           # Run unit tests — all must pass
    ```
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
    ```bash
    git pull --rebase
    bd sync
    git push
    git status  # MUST show "up to date with origin"
    ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**

-   Work is NOT complete until `git push` succeeds
-   NEVER stop before pushing - that leaves work stranded locally
-   NEVER say "ready to push when you are" - YOU must push
-   If push fails, resolve and retry until it succeeds

<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:ca08a54f -->

## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

-   Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
-   Run `bd prime` for detailed command reference and session close protocol
-   Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

## Session Completion

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed):
    ```bash
    ./gradlew spotlessCheck  # Must pass code formatting checks
    ./gradlew test           # Run unit tests — all must pass
    ```
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
    ```bash
    git pull --rebase
    bd sync
    git push
    git status  # MUST show "up to date with origin"
    ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**

-   Work is NOT complete until `git push` succeeds
-   NEVER stop before pushing - that leaves work stranded locally
-   NEVER say "ready to push when you are" - YOU must push
-   If push fails, resolve and retry until it succeeds
<!-- END BEADS INTEGRATION -->
