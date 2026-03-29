package us.kikinsoft.slabsnap.ui.scanner

import us.kikinsoft.slabsnap.ui.mvi.UiEffect
import us.kikinsoft.slabsnap.ui.mvi.UiEvent
import us.kikinsoft.slabsnap.ui.mvi.UiState

data class LiveScannerState(
    val hasCameraPermission: Boolean = false,
    val isPermissionDeniedGlobally: Boolean = false,
) : UiState

sealed interface LiveScannerEvent : UiEvent {

    data class OnPermissionResult(val granted: Boolean) : LiveScannerEvent

    data object CheckInitialPermission : LiveScannerEvent

    data class OnCameraError(val message: String) : LiveScannerEvent
}

sealed interface LiveScannerEffect : UiEffect {
    data object RequestCameraPermission : LiveScannerEffect

    data class ShowError(val message: String) : LiveScannerEffect
}
