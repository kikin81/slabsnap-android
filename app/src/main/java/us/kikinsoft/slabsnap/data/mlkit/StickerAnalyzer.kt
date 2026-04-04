package us.kikinsoft.slabsnap.data.mlkit

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlin.math.abs

/**
 * CameraX [ImageAnalysis.Analyzer] that uses ML Kit Object Detection to locate a sticker card
 * in the camera feed and capture a stable frame for downstream processing (e.g. Gemini Nano).
 *
 * **How it works:**
 * 1. Each frame is passed to ML Kit's object detector in [STREAM_MODE][ObjectDetectorOptions.STREAM_MODE].
 * 2. When an object is detected, [onCardDetected] fires with the bounding box (useful for drawing
 *    an overlay).
 * 3. If the bounding box stays within [STABILITY_TOLERANCE_PX] pixels across
 *    [STABILITY_THRESHOLD_FRAMES] consecutive frames, the card is considered "held steady" and
 *    [onStabilityReached] fires with a [Bitmap] capture of that frame.
 *
 * **Design decisions:**
 * - *Tolerance (15 px)*: ~1% of a 1080p stream. Tight enough to reject blur from motion,
 *   loose enough to absorb natural hand tremor.
 * - *Threshold (12 frames)*: ~400 ms at 30 fps. Fast enough to feel responsive, long enough
 *   for the camera to settle focus and exposure.
 * - *Coordinates*: Bounding boxes are compared in image-buffer space (not UI space), so no
 *   coordinate mapping is needed at this stage.
 *
 * **Threading:** CameraX delivers frames serially via [ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST]
 * by default. ML Kit callbacks land on the main thread. Mutable state ([lastBoundingBox],
 * [stableFrameCount]) is safe under these assumptions.
 *
 * **Lifecycle:** Call [close] when the analyzer is no longer needed to release the ML Kit
 * detector's native resources.
 *
 * @param onCardDetected called on each frame where a card is detected, with its bounding [Rect],
 *   the image dimensions, and rotation degrees — needed for coordinate mapping to UI space.
 * @param onCardLost called when no object is detected in the current frame — useful for clearing
 *   the overlay.
 * @param onStabilityReached called once the card has been held steady, with a [Bitmap] of the
 *   stable frame. Resets internally to avoid double-triggering.
 */
class StickerAnalyzer(
    private val onCardDetected: (rect: Rect, imageWidth: Int, imageHeight: Int, rotationDegrees: Int) -> Unit,
    private val onCardLost: () -> Unit,
    private val onStabilityReached: (Bitmap) -> Unit,
) : ImageAnalysis.Analyzer {

    companion object {
        // Pixels the card can "wiggle"
        private const val STABILITY_TOLERANCE_PX = 15

        // ~400ms at 30fps
        private const val STABILITY_THRESHOLD_FRAMES = 12
    }

    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .build()

    private val detector = ObjectDetection.getClient(options)

    // stability tracking
    private var lastBoundingBox: Rect? = null
    private var stableFrameCount = 0

    /**
     * When `true`, the analyzer drops every frame without processing.
     * Set to `true` internally the instant stability is reached (self-pausing),
     * and reset to `false` externally via [resume] when the UI is ready for the next scan.
     */
    var isPaused: Boolean = false
        private set

    /** Re-enables frame processing after the analyzer has self-paused. */
    fun resume() {
        isPaused = false
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isPaused) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        detector.process(image)
            .addOnSuccessListener { objects ->
                val detectedCard = objects.firstOrNull()?.boundingBox

                if (detectedCard != null) {
                    onCardDetected(
                        detectedCard,
                        imageProxy.width,
                        imageProxy.height,
                        imageProxy.imageInfo.rotationDegrees,
                    )
                    checkStability(detectedCard, imageProxy)
                } else {
                    stableFrameCount = 0
                    onCardLost()
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        detector.close()
    }

    private fun checkStability(
        currentBox: Rect,
        imageProxy: ImageProxy,
    ) {
        val lastBox = lastBoundingBox

        if (lastBox != null && isWithinTolerance(currentBox, lastBox)) {
            stableFrameCount++

            if (stableFrameCount >= STABILITY_THRESHOLD_FRAMES) {
                isPaused = true
                val bitmap = imageProxy.toBitmap()
                onStabilityReached(bitmap)

                stableFrameCount = 0
                lastBoundingBox = null
            }
        } else {
            stableFrameCount = 0
        }
        lastBoundingBox = currentBox
    }

    private fun isWithinTolerance(
        rect1: Rect,
        rect2: Rect,
    ): Boolean = abs(rect1.left - rect2.left) < STABILITY_TOLERANCE_PX &&
        abs(rect1.top - rect2.top) < STABILITY_TOLERANCE_PX &&
        abs(rect1.right - rect2.right) < STABILITY_TOLERANCE_PX &&
        abs(rect1.bottom - rect2.bottom) < STABILITY_TOLERANCE_PX
}
