package us.kikinsoft.slabsnap.ui.scanner

import android.graphics.Bitmap
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity
import us.kikinsoft.slabsnap.ui.mvi.UiEffect
import us.kikinsoft.slabsnap.ui.mvi.UiEvent
import us.kikinsoft.slabsnap.ui.mvi.UiState

/** Explicit state machine phases for the single-scan flow. */
sealed interface ScanPhase {
    data object Scanning : ScanPhase
    data object Extracting : ScanPhase
    data class ShowingResult(
        val sticker: StickerEntity,
        val borderColor: String,
    ) : ScanPhase
    data class Error(val message: String) : ScanPhase
    data class FlipPrompt(val borderColor: String) : ScanPhase
    data class ScanningBack(val borderColor: String) : ScanPhase
    data class ExtractingBack(val borderColor: String) : ScanPhase
}

data class LiveScannerState(
    val hasCameraPermission: Boolean = false,
    val isPermissionDeniedGlobally: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val phase: ScanPhase = ScanPhase.Scanning,
) : UiState

sealed interface LiveScannerEvent : UiEvent {

    data class OnPermissionResult(val granted: Boolean) : LiveScannerEvent

    data object CheckInitialPermission : LiveScannerEvent

    data class OnCameraError(val message: String) : LiveScannerEvent

    data class OnStabilityReached(val bitmap: Bitmap) : LiveScannerEvent

    /** User tapped "Save & Scan Next" on the result bottom sheet. */
    data object ScanNext : LiveScannerEvent

    /** User tapped "Try Again" on error or result bottom sheet. */
    data object TryAgain : LiveScannerEvent

    /** User tapped "Scan Back" on the flip prompt bottom sheet. */
    data object ScanBack : LiveScannerEvent
}

sealed interface LiveScannerEffect : UiEffect {
    data object RequestCameraPermission : LiveScannerEffect

    data class ShowError(val message: String) : LiveScannerEffect
}
