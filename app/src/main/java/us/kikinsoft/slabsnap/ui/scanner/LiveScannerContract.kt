package us.kikinsoft.slabsnap.ui.scanner

import android.graphics.Bitmap
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import us.kikinsoft.slabsnap.ui.mvi.UiEffect
import us.kikinsoft.slabsnap.ui.mvi.UiEvent
import us.kikinsoft.slabsnap.ui.mvi.UiState

/** Front-scan metadata carried through the pipeline until save. */
data class PendingFrontData(
    val borderColor: String,
    val isFoil: Boolean,
)

/** A card successfully scanned and saved in this session. */
data class ScannedCard(
    val stickerCode: String,
    val playerName: String,
    val borderColor: String,
)

/** Dual-scan state machine: front (color/foil) → flip → back (code) → save → pip. */
sealed interface ScanPhase {
    data object ScanningFront : ScanPhase
    data object ExtractingFront : ScanPhase
    data class FlipPrompt(val front: PendingFrontData) : ScanPhase
    data class ScanningBack(val front: PendingFrontData) : ScanPhase
    data class ExtractingBack(val front: PendingFrontData) : ScanPhase
    data class Saving(
        val front: PendingFrontData,
        val stickerCode: String,
    ) : ScanPhase
    data class Error(val message: String) : ScanPhase
}

data class LiveScannerState(
    val hasCameraPermission: Boolean = false,
    val isPermissionDeniedGlobally: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val phase: ScanPhase = ScanPhase.ScanningFront,
    val sessionCards: ImmutableList<ScannedCard> = persistentListOf(),
) : UiState

sealed interface LiveScannerEvent : UiEvent {
    data class OnPermissionResult(val granted: Boolean) : LiveScannerEvent
    data object CheckInitialPermission : LiveScannerEvent
    data class OnCameraError(val message: String) : LiveScannerEvent
    data class OnStabilityReached(val bitmap: Bitmap) : LiveScannerEvent

    /** User tapped "Try Again" on error bottom sheet. */
    data object TryAgain : LiveScannerEvent
}

sealed interface LiveScannerEffect : UiEffect {
    data object RequestCameraPermission : LiveScannerEffect
    data class ShowError(val message: String) : LiveScannerEffect
}
