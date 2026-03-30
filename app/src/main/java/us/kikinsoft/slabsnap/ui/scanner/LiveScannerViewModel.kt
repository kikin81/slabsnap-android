package us.kikinsoft.slabsnap.ui.scanner

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import us.kikinsoft.slabsnap.data.mlkit.CardDataExtractor
import us.kikinsoft.slabsnap.domain.repository.StickerRepository
import us.kikinsoft.slabsnap.ui.mvi.MviViewModel

@HiltViewModel
class LiveScannerViewModel @Inject constructor(
    private val cardDataExtractor: CardDataExtractor,
    private val stickerRepository: StickerRepository,
) : MviViewModel<LiveScannerState, LiveScannerEvent, LiveScannerEffect>(
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
                if (uiState.value.needsBackScan) {
                    extractBackCode(event.bitmap)
                } else {
                    extractCardData(event.bitmap)
                }
            }
            is LiveScannerEvent.ResetExtraction -> {
                setState {
                    copy(
                        isStable = false,
                        isExtracting = false,
                        isDownloadingModel = false,
                        extractedData = null,
                        needsBackScan = false,
                        pendingBorderColor = null,
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

                if (data.primaryText == "UNKNOWN" && data.badgeText == "UNKNOWN") {
                    transitionToBackScan(data.borderColor)
                    return@launch
                }

                val searchText = if (data.primaryText != "UNKNOWN") data.primaryText else data.badgeText
                // TODO: collectionSetId should come from user selection; using 1L as placeholder
                val baseSticker = stickerRepository.findBaseStickerByText(1L, searchText)

                if (baseSticker != null) {
                    stickerRepository.insertParallelVariant(
                        baseStickerCode = baseSticker.stickerCode,
                        collectionSetId = baseSticker.collectionSetId,
                        borderColor = data.borderColor,
                    )
                    clearPendingState()
                    sendEffect(
                        LiveScannerEffect.ResolutionSuccess(
                            playerName = baseSticker.playerName,
                            stickerCode = baseSticker.stickerCode,
                            borderColor = data.borderColor,
                        ),
                    )
                } else {
                    transitionToBackScan(data.borderColor)
                }
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

    private fun extractBackCode(bitmap: Bitmap) {
        if (uiState.value.isExtracting) return

        viewModelScope.launch {
            try {
                ensureModelAvailable()
                setState { copy(isExtracting = true) }
                val stickerCode = cardDataExtractor.extractBackCode(bitmap)
                val borderColor = uiState.value.pendingBorderColor ?: "White"

                // TODO: collectionSetId should come from user selection; using 1L as placeholder
                stickerRepository.insertParallelVariant(
                    baseStickerCode = stickerCode,
                    collectionSetId = 1L,
                    borderColor = borderColor,
                )

                setState { copy(isExtracting = false) }
                clearPendingState()
                sendEffect(
                    LiveScannerEffect.ResolutionSuccess(
                        playerName = stickerCode,
                        stickerCode = stickerCode,
                        borderColor = borderColor,
                    ),
                )
            } catch (e: Exception) {
                setState { copy(isExtracting = false) }
                sendEffect(
                    LiveScannerEffect.ShowError(
                        e.message ?: "Failed to read sticker code",
                    ),
                )
            }
        }
    }

    private fun transitionToBackScan(borderColor: String) {
        setState {
            copy(
                needsBackScan = true,
                pendingBorderColor = borderColor,
                isStable = false,
            )
        }
        sendEffect(LiveScannerEffect.ShowFlipCard(borderColor))
    }

    private fun clearPendingState() {
        setState {
            copy(
                needsBackScan = false,
                pendingBorderColor = null,
            )
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
