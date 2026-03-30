package us.kikinsoft.slabsnap.ui.scanner

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import us.kikinsoft.slabsnap.data.mlkit.CardDataExtractor
import us.kikinsoft.slabsnap.ui.mvi.MviViewModel

@HiltViewModel
class LiveScannerViewModel @Inject constructor(private val cardDataExtractor: CardDataExtractor) :
    MviViewModel<LiveScannerState, LiveScannerEvent, LiveScannerEffect>(
        LiveScannerState(),
    ) {

    override fun handleEvent(event: LiveScannerEvent) {
        when (event) {
            is LiveScannerEvent.CheckInitialPermission -> {
                sendEffect(LiveScannerEffect.RequestCameraPermission)
            }
            is LiveScannerEvent.OnPermissionResult -> {
                setState {
                    copy(
                        hasCameraPermission = event.granted,
                        isPermissionDeniedGlobally = !event.granted,
                    )
                }
            }
            is LiveScannerEvent.OnCameraError -> {
                sendEffect(LiveScannerEffect.ShowError(event.message))
            }
            is LiveScannerEvent.OnStabilityReached -> {
                setState { copy(isStable = true) }
                extractCardData(event.bitmap)
            }
            is LiveScannerEvent.ResetExtraction -> {
                setState {
                    copy(
                        isStable = false,
                        isExtracting = false,
                        isDownloadingModel = false,
                        extractedData = null,
                    )
                }
            }
        }
    }

    private fun extractCardData(bitmap: Bitmap) {
        if (uiState.value.isExtracting || uiState.value.isDownloadingModel) return

        viewModelScope.launch {
            try {
                ensureModelAvailable()
                setState { copy(isExtracting = true) }
                val data = cardDataExtractor.extract(bitmap)
                setState { copy(isExtracting = false, extractedData = data) }
                sendEffect(LiveScannerEffect.ExtractionSuccess(data))
            } catch (e: Exception) {
                setState { copy(isExtracting = false, isDownloadingModel = false) }
                sendEffect(
                    LiveScannerEffect.ShowError(
                        e.message ?: "Failed to extract card data",
                    ),
                )
            }
        }
    }

    private suspend fun ensureModelAvailable() {
        when (cardDataExtractor.checkAvailability()) {
            FeatureStatus.AVAILABLE -> return
            FeatureStatus.DOWNLOADABLE -> {
                setState { copy(isDownloadingModel = true) }
                cardDataExtractor.downloadModel()
                setState { copy(isDownloadingModel = false) }
            }
            FeatureStatus.DOWNLOADING -> {
                setState { copy(isDownloadingModel = true) }
                cardDataExtractor.downloadModel()
                setState { copy(isDownloadingModel = false) }
            }
            else -> throw IllegalStateException(
                "AI model is not available on this device",
            )
        }
    }
}
