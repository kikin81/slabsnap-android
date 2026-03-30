## Context

SlabSnap currently extracts card data by sending a front-side camera capture to Gemini Nano, which attempts to read `playerName`, `teamCountry`, `stickerNumber`, and `isFoil` in one pass. This works for clearly printed player cards but fails on two fronts:

1. **The sticker code is only on the back.** The AI guesses `stickerNumber` from the front, which is unreliable — the code (e.g., "ARG 10") is physically printed on the reverse side only.
2. **Parallel variants aren't tracked.** Panini stickers come in colored-border rarities (White, Blue, Red, Purple, Green, Black 1/1). The current schema treats `stickerCode` as the identity, so owning both a White and a Blue "ARG 10" can't be represented.

Additionally, non-player stickers (team photos, stadiums, crests/foils) have little to no readable text on the front, making text-only extraction fail entirely for those cards.

The ViewModel currently has no repository dependency — it extracts data but doesn't persist it.

## Goals / Non-Goals

**Goals:**

-   Extract text and border color from the front scan (not the sticker code)
-   Resolve card identity by querying seeded database records with extracted text
-   Support a back-scan fallback when front-side text extraction fails (foil/textless cards)
-   Track parallel variants as distinct owned items via `(stickerCode, borderColor)` composite key
-   Wire the ViewModel to the repository for persistence

**Non-Goals:**

-   Batch/multi-card scanning (one card at a time)
-   Automatic collection set detection (user selects the active set)
-   Online/cloud-based card recognition
-   UI for browsing or filtering by border color (future work)
-   Room migration strategy — using destructive migration (DB version 1, app is pre-release)

## Decisions

### 1. Front-scan extracts text + color, not the sticker code

**Decision:** Rewrite the Gemini Nano prompt to extract `primaryText` (player name or visible label), `badgeText` (crest/badge text if any), `borderColor`, and `isFoil`. Explicitly instruct the AI not to guess the sticker number.

**Rationale:** The code is physically on the back. Asking the AI to guess it introduces errors. Instead, we use the extracted text to look up the card in our pre-seeded database.

**Alternative considered:** OCR the back of every card. Rejected because it doubles the scanning burden for 600+ stickers.

### 2. Database text resolution via seeded data

**Decision:** Add a `findBaseStickerByText(setId, query)` DAO method that searches `playerName` and `teamName` columns with LIKE matching. The ViewModel calls this with `primaryText` from the front scan to hydrate the full sticker identity.

**Rationale:** The app already seeds all stickers in a collection set. Matching extracted text against known records avoids needing the code from the front entirely.

**Alternative considered:** Fuzzy matching / FTS. Overkill for this stage — LIKE with a single query term is sufficient given the small dataset (~600 rows).

### 3. Two-phase scan with retained border color

**Decision:** When front-side text extraction yields no usable text (both `primaryText` and `badgeText` are "UNKNOWN"), the ViewModel transitions to a `needsBackScan` state. The `borderColor` captured from the front is stored in `pendingBorderColor`. When the user flips the card and the back stabilizes, a secondary extractor method (`extractBackCode`) reads just the alphanumeric code. The code is then combined with the pending border color to resolve and persist the sticker.

**Rationale:** Foil/holographic crests blind the AI's text extraction, but the border color is still visible on the front. Retaining it across the two-phase flow avoids asking the user to identify the color manually.

### 4. Parallel variants as new rows, not updates to base rows

**Decision:** When a user scans a Blue parallel of "ARG 10", the repository copies the base White sticker's data, sets `borderColor = "Blue"` and `isOwned = true`, and inserts it as a new row. The base White row remains unchanged (it represents the checklist item).

**Rationale:** This preserves the seeded checklist (all White/base variants) as the canonical reference while allowing users to own multiple color variants of the same card. The unique index `(collectionSetId, stickerCode, borderColor)` enforces no duplicates.

### 5. CardDataExtractor gets a second method, not a second class

**Decision:** Add `extractBackCode(bitmap): String` to the existing `CardDataExtractor` rather than creating a separate extractor class.

**Rationale:** Both methods use the same Gemini Nano model instance. A single class keeps the model lifecycle simple and avoids duplicate availability/download logic.

### 6. Destructive migration (no Room migration file)

**Decision:** Bump the DB version and continue using `fallbackToDestructiveMigration()`. No migration file.

**Rationale:** The app is pre-release with only debug/seed data. No user data needs preserving. A proper migration strategy will be needed before public release.

## Risks / Trade-offs

-   **[Text resolution accuracy]** LIKE matching on `primaryText` may fail for partial or garbled AI output. → Mitigation: Fall back to back-scan state if no DB match is found, same as the foil path.
-   **[Border color detection by AI]** Gemini Nano may misidentify border colors, especially for subtle shades or under poor lighting. → Mitigation: Default to "White" if unsure (instruction in prompt). Users can correct later (future feature).
-   **[Destructive migration]** Any manually added data is lost on schema change. → Mitigation: Acceptable for pre-release. Will add proper migrations before public launch.
-   **[Back-scan state leakage]** If `pendingBorderColor` isn't cleaned up after resolution, it could bleed into the next card's scan. → Mitigation: Explicitly nullify pending state after successful back-scan resolution.
