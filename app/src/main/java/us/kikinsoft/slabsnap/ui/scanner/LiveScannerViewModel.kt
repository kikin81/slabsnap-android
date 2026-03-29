package us.kikinsoft.slabsnap.ui.scanner

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import us.kikinsoft.slabsnap.ui.mvi.MviViewModel

@HiltViewModel
class LiveScannerViewModel @Inject constructor() :
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
            }
        }
    }
}
