## ADDED Requirements

### Requirement: ImageAnalysis use case bound to camera pipeline

The system SHALL bind an `ImageAnalysis` use case alongside the `Preview` use case when the camera preview is active, using `STRATEGY_KEEP_ONLY_LATEST` backpressure strategy.

#### Scenario: Camera preview starts with analysis enabled

-   **WHEN** the user grants camera permission and the preview is displayed
-   **THEN** both `Preview` and `ImageAnalysis` use cases SHALL be bound to the camera lifecycle

#### Scenario: Camera resources released on dispose

-   **WHEN** the camera preview composable leaves composition
-   **THEN** the `StickerAnalyzer` SHALL be closed and all camera use cases SHALL be unbound

### Requirement: StickerAnalyzer wired to ImageAnalysis

The system SHALL create a `StickerAnalyzer` instance and set it as the analyzer on the `ImageAnalysis` use case, so that each camera frame is processed by ML Kit Object Detection.

#### Scenario: Frames flow to analyzer

-   **WHEN** the camera is active and producing frames
-   **THEN** `StickerAnalyzer.analyze()` SHALL be called with each `ImageProxy`

### Requirement: Bounding box coordinate mapping

The system SHALL map ML Kit bounding box coordinates from image-buffer space to `PreviewView` UI coordinates, accounting for image rotation and FILL_CENTER scale type.

#### Scenario: Portrait mode coordinate mapping

-   **WHEN** ML Kit detects an object with rotation degrees of 90
-   **THEN** the system SHALL swap source width/height before computing the scale transform and produce a `RectF` in view coordinates

#### Scenario: Bounding box matches visual position

-   **WHEN** a card is detected in the camera frame
-   **THEN** the overlay rectangle SHALL visually align with the card's position on screen

### Requirement: Real-time overlay drawn on camera preview

The system SHALL draw a green rounded-rect stroke overlay on a Compose `Canvas` layered over the camera preview when a card is detected.

#### Scenario: Card detected shows overlay

-   **WHEN** ML Kit detects an object in the current frame
-   **THEN** a green rounded-rect outline SHALL be drawn at the mapped bounding box coordinates

#### Scenario: Card lost hides overlay

-   **WHEN** ML Kit does not detect any object in the current frame
-   **THEN** the overlay SHALL not be drawn

### Requirement: Bounding box state is local to CameraPreview

The bounding box position SHALL be held as local Compose state (`mutableStateOf`) within the `CameraPreview` composable. It SHALL NOT flow through the MVI ViewModel.

#### Scenario: High-frequency updates without MVI overhead

-   **WHEN** the bounding box updates at ~30fps
-   **THEN** only the Canvas overlay SHALL recompose, without triggering ViewModel state updates
