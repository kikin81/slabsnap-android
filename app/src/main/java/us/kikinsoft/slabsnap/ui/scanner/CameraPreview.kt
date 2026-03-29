package us.kikinsoft.slabsnap.ui.scanner

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.max
import us.kikinsoft.slabsnap.R
import us.kikinsoft.slabsnap.data.mlkit.StickerAnalyzer

@Composable
fun CameraPreview(
    onError: (String) -> Unit,
    onCardStabilize: (Bitmap) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val currentOnError by rememberUpdatedState(onError)
    val currentOnCardStabilize by rememberUpdatedState(onCardStabilize)
    val cameraErrorFallback = stringResource(R.string.scanner_camera_error_fallback)

    var detectedBox by remember { mutableStateOf<RectF?>(null) }

    val analyzer = remember {
        StickerAnalyzer(
            onCardDetected = { imageRect, imageWidth, imageHeight, rotationDegrees ->
                val viewWidth = previewView.width.toFloat()
                val viewHeight = previewView.height.toFloat()
                if (viewWidth > 0 && viewHeight > 0) {
                    detectedBox = mapToViewCoordinates(
                        imageRect,
                        imageWidth,
                        imageHeight,
                        rotationDegrees,
                        viewWidth,
                        viewHeight,
                    )
                }
            },
            onCardLost = { detectedBox = null },
            onStabilityReached = { bitmap ->
                currentOnCardStabilize(bitmap)
            },
        )
    }

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(context), analyzer)
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                )
            } catch (exc: Exception) {
                Log.e("CameraPreview", "Camera bind failed", exc)
                currentOnError(exc.message ?: cameraErrorFallback)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            analyzer.close()
            cameraProviderFuture.get().unbindAll()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        detectedBox?.let { box ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    color = Color.Green,
                    topLeft = Offset(box.left, box.top),
                    size = Size(box.width(), box.height()),
                    cornerRadius = CornerRadius(8.dp.toPx()),
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
    }
}

/**
 * Maps a bounding box from ML Kit image-buffer coordinates to [PreviewView] UI coordinates.
 *
 * ML Kit's [InputImage.fromMediaImage] applies the rotation, so the bounding box is already
 * in the rotated coordinate system. The source dimensions are the [ImageProxy]'s raw
 * width/height — we swap them here for 90/270 degree rotations.
 *
 * [PreviewView] uses FILL_CENTER by default: the image is scaled to fill the view (using
 * `max(scaleX, scaleY)`) and centered, which may crop edges. We replicate that math to
 * map the bounding box correctly.
 */
private fun mapToViewCoordinates(
    imageRect: Rect,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int,
    viewWidth: Float,
    viewHeight: Float,
): RectF {
    // InputImage applies rotation, so source dims must account for the swap
    val isRotated = rotationDegrees == 90 || rotationDegrees == 270
    val sourceW = if (isRotated) imageHeight.toFloat() else imageWidth.toFloat()
    val sourceH = if (isRotated) imageWidth.toFloat() else imageHeight.toFloat()

    // FILL_CENTER: scale to fill the view, then center (may crop edges)
    val scale = max(viewWidth / sourceW, viewHeight / sourceH)
    val dx = (viewWidth - sourceW * scale) / 2f
    val dy = (viewHeight - sourceH * scale) / 2f

    return RectF(
        imageRect.left * scale + dx,
        imageRect.top * scale + dy,
        imageRect.right * scale + dx,
        imageRect.bottom * scale + dy,
    )
}
