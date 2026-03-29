## Context

SlabSnap's camera scanner currently binds only a `Preview` use case. The `StickerAnalyzer` (ML Kit Object Detection in STREAM_MODE) exists at `data/mlkit/` but isn't connected to the camera pipeline. Users see a live preview but receive no feedback that the app detects a card. The stability-tracking logic inside `StickerAnalyzer` (12 frames / ~400ms threshold) produces a `Bitmap` when the card is held steady, intended for downstream Gemini Nano processing.

CameraX version is 1.6.0. The project uses MVI architecture with `MviViewModel<State, Event, Effect>`. The UI layer is Jetpack Compose with Material 3.

## Goals / Non-Goals

**Goals:**

-   Wire `StickerAnalyzer` into the CameraX pipeline via `ImageAnalysis` use case
-   Show a real-time bounding box overlay that tracks the detected card
-   Map coordinates correctly from image-buffer space to `PreviewView` UI space
-   Surface the stability-reached event through MVI for downstream processing

**Non-Goals:**

-   Gemini Nano integration (downstream consumer of the Bitmap — separate task)
-   Front camera support or mirroring logic
-   Custom ML Kit model training or fine-tuning detection
-   Overlay animations or visual polish beyond a basic rounded rect

## Decisions

### 1. Bounding box as local Compose state, not MVI state

The bounding box updates at ~30fps. It is pure UI feedback — no ViewModel, repository, or use case needs to react to it. Routing it through `handleEvent` → `setState` → `StateFlow` → `collectAsStateWithLifecycle` adds unnecessary indirection.

**Decision:** Hold the bounding box as `mutableStateOf<RectF?>` inside `CameraPreview`. Only `onStabilityReached` (which produces a `Bitmap` with business meaning) flows through MVI.

**Alternative considered:** Full MVI for the box. Rejected because `StateFlow` conflation at 30fps would drop frames, and the ViewModel has no logic to apply to the box position.

### 2. Manual coordinate transform, not `camera-mlkit-vision`

CameraX 1.6.0 does not provide `CoordinateTransform`, `ImageProxy.toOutputTransform()`, or `PreviewView.outputTransform`. The `camera-mlkit-vision` artifact offers `MlKitAnalyzer` with `COORDINATE_SYSTEM_VIEW_REFERENCED`, but adopting it would require rewriting `StickerAnalyzer` to fit its API and discarding the stability-tracking logic.

**Decision:** Manual transform using `ImageProxy` dimensions, rotation degrees, and `PreviewView` size with FILL_CENTER scale math (`max(scaleX, scaleY)` + centering offset).

**Alternative considered:** Adding `camera-mlkit-vision` dependency. Rejected to avoid rewriting the existing analyzer and adding a dependency for a ~15-line transform function.

### 3. CameraPreview owns the analyzer and overlay

`CameraPreview` is the only composable with access to `PreviewView` (needed for coordinate mapping dimensions) and `ProcessCameraProvider` (needed to bind `ImageAnalysis`). Making it own both the analyzer lifecycle and the overlay Canvas keeps the coordinate transform self-contained.

**Decision:** `CameraPreview` creates the `StickerAnalyzer`, binds it, draws the overlay, and calls `analyzer.close()` on dispose.

**Alternative considered:** Creating the analyzer in the ViewModel or Screen and passing it down. Rejected because the coordinate transform requires `PreviewView` dimensions that only `CameraPreview` has.

### 4. Executor for ImageAnalysis

ML Kit Object Detection is lightweight enough to run on the main executor for STREAM_MODE with `STRATEGY_KEEP_ONLY_LATEST`. This avoids creating a dedicated executor thread and keeps callback threading simple (main thread, matching `mutableStateOf` expectations).

**Decision:** Use `ContextCompat.getMainExecutor(context)` for the analyzer executor.

## Risks / Trade-offs

-   **[30fps Canvas recomposition]** The Canvas recomposes every ~33ms when a card is detected. Compose Canvas recomposition skips layout and only runs the draw phase, so this should be performant. If profiling shows jank, migrate to `Modifier.drawWithContent` to avoid a separate composition scope. **Mitigation:** Monitor with Layout Inspector recomposition counts.

-   **[Box flicker on detection loss]** When ML Kit stops detecting, `onCardDetected` stops firing and the box disappears instantly. This may feel abrupt. **Mitigation:** Acceptable for now; a timeout-based fade can be added later without architectural changes.

-   **[Thread safety of mutableStateOf]** The analyzer callback fires on the main executor, and `mutableStateOf` is backed by `SnapshotMutableState` which is thread-safe. If the executor changes in the future, this assumption breaks. **Mitigation:** Document the threading constraint in the code.
