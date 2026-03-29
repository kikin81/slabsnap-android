## Why

The `StickerAnalyzer` detects sticker cards via ML Kit Object Detection but isn't wired into the CameraX pipeline yet. Users see a camera preview but get no visual feedback that the app recognizes a card. Without a bounding box overlay, there's no way to guide the user to hold the card steady for capture.

## What Changes

-   Bind `ImageAnalysis` use case alongside `Preview` in `CameraPreview` so frames flow to `StickerAnalyzer`
-   Map ML Kit bounding box coordinates from image-buffer space to `PreviewView` UI coordinates using manual scale/rotation math
-   Draw a green rounded-rect overlay on a Compose `Canvas` layered over the camera preview
-   Add `onStabilityReached` callback through MVI so the captured `Bitmap` can be forwarded to downstream processing (Gemini Nano)
-   Add `isStable` state to reflect when the card has been held steady long enough for capture

## Capabilities

### New Capabilities

-   `detection-overlay`: Real-time bounding box overlay drawn on the camera preview when ML Kit detects an object, with coordinate mapping from image-buffer to view space
-   `stability-capture`: MVI event flow for when the card is held steady, producing a Bitmap for downstream AI processing

### Modified Capabilities

## Impact

-   `CameraPreview.kt` — substantial rewrite: binds `ImageAnalysis`, creates `StickerAnalyzer`, draws Canvas overlay, handles coordinate transform
-   `LiveScannerContract.kt` — new state field (`isStable`), new event (`OnStabilityReached`), stability callback
-   `LiveScannerViewModel.kt` — handles new event
-   `LiveScannerScreen.kt` — passes new `onStabilityReached` callback through to `CameraPreview`
-   `LiveScannerViewModelTest.kt` — new test for stability event
-   No new dependencies — uses existing CameraX 1.6.0 APIs and `android.graphics.RectF`
