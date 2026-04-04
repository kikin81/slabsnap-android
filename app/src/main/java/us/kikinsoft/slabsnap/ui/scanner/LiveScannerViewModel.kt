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
                setState { copy(phase = ScanPhase.Error(event.message)) }
            }
            is LiveScannerEvent.OnStabilityReached -> {
                when (val phase = uiState.value.phase) {
                    is ScanPhase.Scanning -> extractCardData(event.bitmap)
                    is ScanPhase.ScanningBack -> extractBackCode(event.bitmap, phase.borderColor)
                    else -> Unit // Ignore stability events in non-scanning phases
                }
            }
            is LiveScannerEvent.ScanNext -> {
                setState { copy(phase = ScanPhase.Scanning) }
            }
            is LiveScannerEvent.TryAgain -> {
                setState { copy(phase = ScanPhase.Scanning) }
            }
            is LiveScannerEvent.ScanBack -> {
                val phase = uiState.value.phase
                if (phase is ScanPhase.FlipPrompt) {
                    setState { copy(phase = ScanPhase.ScanningBack(phase.borderColor)) }
                }
            }
        }
    }

    private fun extractCardData(bitmap: Bitmap) {
        if (uiState.value.phase !is ScanPhase.Scanning) return

        setState { copy(phase = ScanPhase.Extracting) }

        viewModelScope.launch {
            try {
                ensureModelAvailable()
                val data = cardDataExtractor.extract(bitmap)

                if (data.primaryText == "UNKNOWN" && data.badgeText == "UNKNOWN") {
                    setState { copy(phase = ScanPhase.FlipPrompt(data.borderColor)) }
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
                    setState {
                        copy(
                            phase = ScanPhase.ShowingResult(
                                sticker = baseSticker,
                                borderColor = data.borderColor,
                            ),
                        )
                    }
                } else {
                    setState { copy(phase = ScanPhase.FlipPrompt(data.borderColor)) }
                }
            } catch (e: Exception) {
                setState {
                    copy(
                        isDownloadingModel = false,
                        phase = ScanPhase.Error(e.message ?: "Failed to extract card data"),
                    )
                }
            }
        }
    }

    private fun extractBackCode(
        bitmap: Bitmap,
        borderColor: String,
    ) {
        if (uiState.value.phase !is ScanPhase.ScanningBack) return

        setState { copy(phase = ScanPhase.ExtractingBack(borderColor)) }

        viewModelScope.launch {
            try {
                ensureModelAvailable()
                val stickerCode = cardDataExtractor.extractBackCode(bitmap)

                // TODO: collectionSetId should come from user selection; using 1L as placeholder
                stickerRepository.insertParallelVariant(
                    baseStickerCode = stickerCode,
                    collectionSetId = 1L,
                    borderColor = borderColor,
                )

                val sticker = stickerRepository.findByStickerCode(stickerCode)
                setState {
                    copy(
                        phase = ScanPhase.ShowingResult(
                            sticker = sticker ?: us.kikinsoft.slabsnap.data.local.entity.StickerEntity(
                                stickerCode = stickerCode,
                                playerName = stickerCode,
                                teamName = "",
                                metadata = emptyMap(),
                                collectionSetId = 1L,
                                borderColor = borderColor,
                            ),
                            borderColor = borderColor,
                        ),
                    )
                }
            } catch (e: Exception) {
                setState {
                    copy(
                        isDownloadingModel = false,
                        phase = ScanPhase.Error(e.message ?: "Failed to read sticker code"),
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
