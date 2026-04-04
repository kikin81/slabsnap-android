## ADDED Requirements

### Requirement: Two-phase scanning with back-scan fallback

When front-side text extraction fails to identify a card (both `primaryText` and `badgeText` are "UNKNOWN", or no database match is found), the system SHALL transition to a back-scan state, retaining the `borderColor` captured from the front scan.

#### Scenario: Foil card triggers back-scan

-   **WHEN** the AI extracts `primaryText = "UNKNOWN"` and `badgeText = "UNKNOWN"` from a front scan
-   **THEN** the ViewModel SHALL set `needsBackScan = true` and store the extracted `borderColor` in `pendingBorderColor`

#### Scenario: No DB match triggers back-scan

-   **WHEN** the AI extracts text from the front but no matching sticker is found in the database
-   **THEN** the ViewModel SHALL set `needsBackScan = true` and store the extracted `borderColor` in `pendingBorderColor`

### Requirement: Back-scan extracts sticker code only

The `CardDataExtractor` SHALL provide an `extractBackCode(bitmap)` method that extracts only the alphanumeric sticker code (e.g., "ARG 2", "FWC 9") from the back of the card. It SHALL return a plain string, not JSON.

#### Scenario: Successful back code extraction

-   **WHEN** the back-scan method processes an image of a card back showing "ARG 2"
-   **THEN** it SHALL return the string "ARG 2"

### Requirement: Back-scan resolution combines code with pending border color

After a successful back-scan extraction, the ViewModel SHALL combine the extracted `stickerCode` with the `pendingBorderColor` from the front scan to resolve and persist the sticker.

#### Scenario: Complete two-phase resolution

-   **WHEN** a back scan extracts `stickerCode = "GER 2"`
-   **AND** `pendingBorderColor` is "Blue"
-   **THEN** the system SHALL insert/update a sticker with `stickerCode = "GER 2"`, `borderColor = "Blue"`, and `isOwned = true`
-   **AND** the system SHALL reset `needsBackScan = false` and `pendingBorderColor = null`

### Requirement: State cleanup after resolution

After any successful card resolution (front-only or two-phase), the ViewModel SHALL clear all pending state (`pendingBorderColor`, `needsBackScan`) to prevent leakage into the next scan.

#### Scenario: State reset after front-only resolution

-   **WHEN** a card is successfully resolved via front-scan text lookup
-   **THEN** `needsBackScan` SHALL be `false` and `pendingBorderColor` SHALL be `null`

#### Scenario: State reset after back-scan resolution

-   **WHEN** a card is successfully resolved via back-scan fallback
-   **THEN** `needsBackScan` SHALL be `false` and `pendingBorderColor` SHALL be `null`

### Requirement: UI displays back-scan instruction

When `needsBackScan` is true, the scanner UI SHALL display a clear instruction telling the user to flip the card and scan the back.

#### Scenario: Back-scan UI state

-   **WHEN** the ViewModel state has `needsBackScan = true`
-   **THEN** the UI SHALL show text instructing the user to "Scan the back of the sticker"
-   **AND** when stability is reached, the bitmap SHALL be routed to `extractBackCode()` instead of the main extraction method
