## ADDED Requirements

### Requirement: Front scan extracts visible text instead of sticker code

The extraction prompt SHALL instruct the AI to extract `primaryText` (the main name or label visible on the card) and `badgeText` (text on a crest or badge, if present) from the front side. The prompt SHALL explicitly state that the sticker number is not on this side of the card.

#### Scenario: Player card front scan

-   **WHEN** the AI processes a front-side image of a player card showing "Lionel Messi" and an Argentina badge
-   **THEN** `primaryText` SHALL contain "Lionel Messi" (or close approximation) and the AI SHALL NOT attempt to guess a sticker number

#### Scenario: Unreadable card front

-   **WHEN** the AI cannot extract any text from the front side (e.g., foil glare)
-   **THEN** both `primaryText` and `badgeText` SHALL be "UNKNOWN"

### Requirement: Database text resolution from seeded data

The system SHALL resolve a scanned card's identity by querying the local database with extracted front-side text. The `StickerDao` SHALL provide a `findBaseStickerByText(setId, query)` method that searches `playerName` and `teamName` columns.

#### Scenario: Successful text resolution

-   **WHEN** a front scan extracts `primaryText = "Pau Torres"`
-   **AND** the seeded database contains a sticker with `playerName` matching "Pau Torres" in the active collection set
-   **THEN** the system SHALL resolve the sticker's `stickerCode` from the matched database record

#### Scenario: No database match found

-   **WHEN** a front scan extracts text that does not match any seeded sticker in the active collection set
-   **THEN** the system SHALL transition to the back-scan fallback flow (same as the foil/textless path)

### Requirement: Repository exposes text search

The `StickerRepository` interface SHALL expose a method to find a base sticker by text query within a collection set, delegating to the DAO's `findBaseStickerByText`.

#### Scenario: Repository text search delegates to DAO

-   **WHEN** the ViewModel calls the repository's text search method with a set ID and query string
-   **THEN** the repository SHALL return the matching `StickerEntity` or null
