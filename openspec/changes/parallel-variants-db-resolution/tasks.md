## 1. Schema & Data Layer

-   [x] 1.1 Add `borderColor: String = "White"` field to `StickerEntity`
-   [x] 1.2 Update `StickerEntity` indices to composite unique index `(collectionSetId, stickerCode, borderColor)`
-   [x] 1.3 Bump database version in `SlabSnapDatabase` (destructive migration)
-   [x] 1.4 Add `findBaseStickerByText(setId: Long, query: String): StickerEntity?` query to `StickerDao`
-   [x] 1.5 Verify `DatabaseSeeder` seeds all stickers with `borderColor = "White"` (should be automatic from default)

## 2. Repository Layer

-   [x] 2.1 Add `findBaseStickerByText(setId: Long, query: String): StickerEntity?` to `StickerRepository` interface
-   [x] 2.2 Add `insertParallelVariant(baseStickerCode: String, collectionSetId: Long, borderColor: String)` to `StickerRepository` interface — copies base sticker data, sets the border color, marks `isOwned = true`
-   [x] 2.3 Implement both new methods in `StickerRepositoryImpl`

## 3. Extraction Model & Prompt

-   [x] 3.1 Refactor `ExtractedCardData` — replace `playerName`, `teamCountry`, `stickerNumber` with `primaryText`, `badgeText`, `borderColor`; keep `isFoil`
-   [x] 3.2 Rewrite `CardDataExtractor.EXTRACTION_PROMPT` to extract `primaryText`, `badgeText`, `borderColor`, and `isFoil` from front-side images; explicitly instruct AI not to guess sticker number
-   [x] 3.3 Add `extractBackCode(bitmap: Bitmap): String` method to `CardDataExtractor` with a prompt that extracts only the alphanumeric sticker code from the back

## 4. ViewModel Two-Phase Flow

-   [x] 4.1 Add `needsBackScan: Boolean` and `pendingBorderColor: String?` to `LiveScannerState`
-   [x] 4.2 Add new events to `LiveScannerEvent`: back-scan stability event
-   [x] 4.3 Add new effects to `LiveScannerEffect`: flip-card instruction, resolution success with sticker details
-   [x] 4.4 Inject `StickerRepository` into `LiveScannerViewModel`
-   [x] 4.5 Implement front-scan flow: extract → text resolution via repository → persist variant (or transition to back-scan state)
-   [x] 4.6 Implement back-scan flow: extract code → combine with `pendingBorderColor` → persist → reset pending state
-   [x] 4.7 Ensure state cleanup (nullify `pendingBorderColor`, reset `needsBackScan`) after every successful resolution

## 5. Scanner UI

-   [x] 5.1 Update `LiveScannerScreen` to show "Scan the back of the sticker" instruction when `needsBackScan` is true
-   [x] 5.2 Route stability bitmap to `extractBackCode()` when in back-scan state (instead of main extraction)
-   [x] 5.3 Update success snackbar/feedback to reflect new extracted data shape (no more `stickerNumber` from front)

## 6. Tests

-   [x] 6.1 Update `LiveScannerViewModel` unit tests for two-phase extraction flow (front-only resolution, foil fallback, no-match fallback, back-scan resolution)
-   [x] 6.2 Add repository tests for `findBaseStickerByText` and `insertParallelVariant`
-   [x] 6.3 Update any existing `ExtractedCardData` tests for the new field structure
