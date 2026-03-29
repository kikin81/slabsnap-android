## ADDED Requirements

### Requirement: Stability reached event flows through MVI

The system SHALL surface the `StickerAnalyzer.onStabilityReached` callback as an MVI event (`OnStabilityReached`) containing the captured `Bitmap`, handled by the `LiveScannerViewModel`.

#### Scenario: Card held steady triggers MVI event

-   **WHEN** the `StickerAnalyzer` determines the card has been stable for the threshold duration
-   **THEN** `LiveScannerEvent.OnStabilityReached` SHALL be emitted with the captured `Bitmap`

#### Scenario: ViewModel updates state on stability

-   **WHEN** the ViewModel receives an `OnStabilityReached` event
-   **THEN** the state SHALL update `isStable` to `true`

### Requirement: LiveScannerState includes stability tracking

`LiveScannerState` SHALL include an `isStable: Boolean` field, defaulting to `false`, that reflects whether the card has been held steady long enough for capture.

#### Scenario: Initial state is not stable

-   **WHEN** the scanner screen first loads
-   **THEN** `isStable` SHALL be `false`

### Requirement: onStabilityReached callback wired through screen layer

`LiveScannerScreen` SHALL pass an `onStabilityReached` callback through `LiveScannerScreenContent` to `CameraPreview`, forwarding the `Bitmap` to the ViewModel via `handleEvent`.

#### Scenario: Bitmap forwarded from CameraPreview to ViewModel

-   **WHEN** `CameraPreview` receives a stability callback with a `Bitmap`
-   **THEN** the callback SHALL invoke `viewModel.handleEvent(OnStabilityReached(bitmap))`
