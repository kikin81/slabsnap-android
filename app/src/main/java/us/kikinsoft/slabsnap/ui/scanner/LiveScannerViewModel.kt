package us.kikinsoft.slabsnap.ui.scanner

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.common.FeatureStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
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

    companion object {
        private const val COLLECTION_SET_ID = 1L
    }

    override fun handleEvent(event: LiveScannerEvent) {
        when (event) {
            is LiveScannerEvent.CheckInitialPermission ->
                sendEffect(LiveScannerEffect.RequestCameraPermission)
            is LiveScannerEvent.OnPermissionResult ->
                setState {
                    copy(
                        hasCameraPermission = event.granted,
                        isPermissionDeniedGlobally = !event.granted,
                    )
                }
            is LiveScannerEvent.OnCameraError ->
                setState { copy(phase = ScanPhase.Error(event.message)) }
            is LiveScannerEvent.OnStabilityReached ->
                onStabilityReached(event.bitmap)
            is LiveScannerEvent.TryAgain ->
                setState { copy(phase = ScanPhase.ScanningFront) }
        }
    }

    private fun onStabilityReached(bitmap: Bitmap) {
        when (val phase = uiState.value.phase) {
            is ScanPhase.ScanningFront -> extractFront(bitmap)
            is ScanPhase.FlipPrompt -> {
                // Auto-detect: next stable frame after flip prompt = back side
                setState { copy(phase = ScanPhase.ScanningBack(phase.front)) }
                extractBack(bitmap, phase.front)
            }
            is ScanPhase.ScanningBack -> extractBack(bitmap, phase.front)
            else -> Unit
        }
    }

    private fun extractFront(bitmap: Bitmap) {
        if (uiState.value.phase !is ScanPhase.ScanningFront) return
        setState { copy(phase = ScanPhase.ExtractingFront) }

        viewModelScope.launch {
            try {
                ensureModelAvailable()
                val data = cardDataExtractor.extractFront(bitmap)
                val front = PendingFrontData(
                    borderColor = data.borderColor,
                    isFoil = data.isFoil,
                )
                // Always transition to FlipPrompt — back scan is mandatory
                setState { copy(phase = ScanPhase.FlipPrompt(front)) }
            } catch (e: Exception) {
                setState {
                    copy(
                        isDownloadingModel = false,
                        phase = ScanPhase.Error(
                            e.message ?: "Failed to extract card data",
                        ),
                    )
                }
            }
        }
    }

    private fun extractBack(
        bitmap: Bitmap,
        front: PendingFrontData,
    ) {
        setState { copy(phase = ScanPhase.ExtractingBack(front)) }

        viewModelScope.launch {
            try {
                ensureModelAvailable()
                val stickerCode = cardDataExtractor.extractBackCode(bitmap)

                setState { copy(phase = ScanPhase.Saving(front, stickerCode)) }

                stickerRepository.insertParallelVariant(
                    baseStickerCode = stickerCode,
                    collectionSetId = COLLECTION_SET_ID,
                    borderColor = front.borderColor,
                )

                val sticker = stickerRepository.findByStickerCode(stickerCode)
                val scannedCard = ScannedCard(
                    stickerCode = stickerCode,
                    playerName = sticker?.playerName ?: stickerCode,
                    borderColor = front.borderColor,
                )

                setState {
                    copy(
                        phase = ScanPhase.ScanningFront,
                        sessionCards = (sessionCards + scannedCard).toImmutableList(),
                    )
                }
            } catch (e: Exception) {
                setState {
                    copy(
                        isDownloadingModel = false,
                        phase = ScanPhase.Error(
                            e.message ?: "Failed to read sticker code",
                        ),
                    )
                }
            }
        }
    }

    private suspend fun ensureModelAvailable() {
        when (cardDataExtractor.checkAvailability()) {
            FeatureStatus.AVAILABLE -> return
            FeatureStatus.DOWNLOADABLE, FeatureStatus.DOWNLOADING -> {
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
