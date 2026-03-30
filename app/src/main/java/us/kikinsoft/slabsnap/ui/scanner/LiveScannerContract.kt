package us.kikinsoft.slabsnap.ui.scanner

import android.graphics.Bitmap
import us.kikinsoft.slabsnap.data.mlkit.ExtractedCardData
import us.kikinsoft.slabsnap.ui.mvi.UiEffect
import us.kikinsoft.slabsnap.ui.mvi.UiEvent
import us.kikinsoft.slabsnap.ui.mvi.UiState

data class LiveScannerState(
    val hasCameraPermission: Boolean = false,
    val isPermissionDeniedGlobally: Boolean = false,
    val isStable: Boolean = false,
    val isExtracting: Boolean = false,
    val isDownloadingModel: Boolean = false,
    val extractedData: ExtractedCardData? = null,
    val needsBackScan: Boolean = false,
    val pendingBorderColor: String? = null,
) : UiState

sealed interface LiveScannerEvent : UiEvent {

    data class OnPermissionResult(val granted: Boolean) : LiveScannerEvent

    data object CheckInitialPermission : LiveScannerEvent

    data class OnCameraError(val message: String) : LiveScannerEvent

    data class OnStabilityReached(val bitmap: Bitmap) : LiveScannerEvent

    data object ResetExtraction : LiveScannerEvent
}

sealed interface LiveScannerEffect : UiEffect {
    data object RequestCameraPermission : LiveScannerEffect

    data class ShowError(val message: String) : LiveScannerEffect

    data class ShowFlipCard(val borderColor: String) : LiveScannerEffect

    data class ResolutionSuccess(
        val playerName: String,
        val stickerCode: String,
        val borderColor: String,
    ) : LiveScannerEffect
}
