## ADDED Requirements

### Requirement: Sticker identity includes border color

A sticker SHALL be uniquely identified by the composite key `(collectionSetId, stickerCode, borderColor)`. The `StickerEntity` SHALL include a `borderColor: String` field defaulting to `"White"`.

#### Scenario: Base stickers seeded as White

-   **WHEN** the database is seeded with collection set stickers
-   **THEN** all seeded stickers SHALL have `borderColor = "White"`

#### Scenario: Unique index prevents duplicate variants

-   **WHEN** an insert is attempted for a sticker with the same `collectionSetId`, `stickerCode`, and `borderColor` as an existing row
-   **THEN** the insert SHALL be treated as a conflict (replace strategy)

#### Scenario: Different border colors are distinct rows

-   **WHEN** a user owns both a White and a Blue variant of sticker "ARG 10"
-   **THEN** two separate rows SHALL exist in the `stickers` table with the same `stickerCode` but different `borderColor` values

### Requirement: Parallel variant insertion copies base sticker data

When a non-White parallel variant is detected, the repository SHALL create a new sticker row by copying the base (White) sticker's `playerName`, `teamName`, `metadata`, and `collectionSetId`, setting the detected `borderColor`, and marking `isOwned = true`.

#### Scenario: Scanning a Blue parallel of a known sticker

-   **WHEN** the system resolves a scanned card as stickerCode "ARG 10" with borderColor "Blue"
-   **AND** a base White "ARG 10" sticker exists in the database
-   **THEN** the repository SHALL insert a new row with `stickerCode = "ARG 10"`, `borderColor = "Blue"`, `isOwned = true`, and the same `playerName`, `teamName`, and `metadata` as the base sticker

#### Scenario: Scanning a White variant of a known sticker

-   **WHEN** the system resolves a scanned card as stickerCode "ARG 10" with borderColor "White"
-   **AND** a base White "ARG 10" sticker already exists
-   **THEN** the repository SHALL update the existing White row to set `isOwned = true`

### Requirement: ExtractedCardData includes border color

The `ExtractedCardData` model SHALL include a `borderColor: String` field representing the color of the border framing the sticker (e.g., White, Blue, Red, Purple, Green, Black).

#### Scenario: Front scan extracts border color

-   **WHEN** the AI processes a front-side card image
-   **THEN** the extracted data SHALL include the detected `borderColor`, defaulting to `"White"` if the AI is unsure
