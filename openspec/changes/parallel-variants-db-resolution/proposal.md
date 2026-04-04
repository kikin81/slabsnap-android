## Why

Panini World Cup stickers have two physical realities our current extraction flow doesn't handle: (1) the sticker code is only printed on the **back**, not the front, so the AI prompt asking for `sticker_number` from a front scan is unreliable, and (2) stickers come in **parallel variants** distinguished by border color (White, Blue, Red, Purple, Green, Black 1/1), meaning a sticker is no longer uniquely identified by `stickerCode` alone but by the composite key `(stickerCode, borderColor)`. We need to refactor the extraction pipeline to stop guessing the code from the front, resolve identity via database text lookup, support a back-scan fallback for foil/textless cards, and track parallel variants as distinct owned items.

## What Changes

-   **ExtractedCardData model**: Replace `playerName`, `teamCountry`, `stickerNumber` fields with `primaryText`, `badgeText`, `borderColor`. Keep `isFoil`.
-   **Extraction prompt**: Rewrite to extract visible text and border color from the front; explicitly tell the AI not to guess the sticker number. Add a secondary back-scan prompt that extracts only the alphanumeric code.
-   **StickerEntity schema**: Add `borderColor` column (default `"White"`). Update unique index to enforce `(collectionSetId, stickerCode, borderColor)`. **BREAKING** schema change (DB version bump or destructive migration).
-   **StickerDao**: Add `findBaseStickerByText()` query for text-based resolution against seeded data.
-   **StickerRepository**: Expose the new text-search method and a "copy-and-mark-owned" method for inserting parallel variants.
-   **LiveScannerViewModel**: Inject repository. Implement two-phase flow: front scan resolves via DB text lookup; if text extraction fails (foil), transition to back-scan state retaining the captured `borderColor`. After resolution, insert/update the specific parallel variant as owned.
-   **LiveScannerScreen UI**: Show "flip the card" instruction when `needsBackScan` is true; route the stable bitmap to the back-scan extractor in that state.

## Capabilities

### New Capabilities

-   `parallel-variant-tracking`: Support border-color-based parallel variants as distinct owned stickers in the database schema and repository layer.
-   `db-text-resolution`: Resolve a scanned card's identity by querying seeded sticker data with extracted front-side text, eliminating the need to read the sticker code from the front.
-   `back-scan-fallback`: Two-phase scanning flow where foil/textless cards trigger a "scan the back" UI state to capture the sticker code, while retaining the border color from the front scan.

### Modified Capabilities

_(No existing specs to modify)_

## Impact

-   **Database**: Schema change to `stickers` table — new `borderColor` column and updated unique index. Requires migration or destructive rebuild.
-   **Data layer**: `ExtractedCardData`, `CardDataExtractor`, `StickerEntity`, `StickerDao`, `StickerRepository`, `StickerRepositoryImpl` all modified.
-   **UI layer**: `LiveScannerViewModel` (new state fields, repository dependency, two-phase logic), `LiveScannerContract` (new state/event/effect definitions), `LiveScannerScreen` (conditional back-scan UI).
-   **Seed data**: `DatabaseSeeder` stickers should all seed with `borderColor = "White"` (already the default).
-   **Tests**: Existing ViewModel and extractor tests will need updates to match new data shapes and flow.
