## 1. MVI Contract Updates

-   [x] 1.1 Add `isStable: Boolean = false` to `LiveScannerState` in `LiveScannerContract.kt`
-   [x] 1.2 Add `OnStabilityReached(bitmap: Bitmap)` event to `LiveScannerEvent`
-   [x] 1.3 Handle `OnStabilityReached` in `LiveScannerViewModel` — set `isStable = true`

## 2. CameraPreview — Bind ImageAnalysis and Analyzer

-   [x] 2.1 Add `onCardStabilize: (Bitmap) -> Unit` callback parameter to `CameraPreview`
-   [x] 2.2 Create `StickerAnalyzer` in a `remember` block with `onCardDetected`, `onCardLost`, and `onStabilityReached` callbacks
-   [x] 2.3 Build `ImageAnalysis` use case with `STRATEGY_KEEP_ONLY_LATEST` and set the analyzer
-   [x] 2.4 Bind `ImageAnalysis` alongside `Preview` in `bindToLifecycle`
-   [x] 2.5 Call `analyzer.close()` in `onDispose` alongside `unbindAll()`

## 3. Coordinate Transform

-   [x] 3.1 Add private `mapToViewCoordinates` function that maps `Rect` from image-buffer space to `RectF` in view space, accounting for rotation and FILL_CENTER scaling
-   [x] 3.2 Wire `onCardDetected` callback to transform the bounding box and set local `mutableStateOf<RectF?>`

## 4. Overlay Rendering

-   [x] 4.1 Wrap `AndroidView` and `Canvas` in a `Box` layout
-   [x] 4.2 Draw green rounded-rect stroke on the `Canvas` when `detectedBox` is non-null
-   [x] 4.3 Clear overlay (null state) when no detection in current frame

## 5. Screen Wiring

-   [x] 5.1 Add `onCardStabilize` parameter to `LiveScannerScreenContent`
-   [x] 5.2 Pass `onCardStabilize` callback from `LiveScannerScreen` through to `CameraPreview`, forwarding to `viewModel.handleEvent(OnStabilityReached(bitmap))`
-   [x] 5.3 Update `@Preview` composable to include new `onCardStabilize` parameter

## 6. Tests

-   [x] 6.1 Add unit test: `OnStabilityReached` event sets `isStable = true` in ViewModel
-   [x] 6.2 Verify all existing tests still pass (`./gradlew testDebugUnitTest`)

## 7. Verification

-   [x] 7.1 `./gradlew spotlessCheck compileDebugKotlin` passes
-   [x] 7.2 On-device: point camera at a card — green overlay tracks the card
-   [x] 7.3 Hold card steady ~400ms — stability state updates (verify via log or debugger)
