package us.kikinsoft.slabsnap.ui.scanner

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import us.kikinsoft.slabsnap.MainDispatcherExtension

class LiveScannerViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = MainDispatcherExtension()
    }

    private fun createViewModel(): LiveScannerViewModel = LiveScannerViewModel()

    @Test
    fun `GIVEN viewModel is created THEN initial state has no camera permission`() {
        // Given
        val viewModel = createViewModel()

        // Then
        assertFalse(viewModel.uiState.value.hasCameraPermission)
        assertFalse(viewModel.uiState.value.isPermissionDeniedGlobally)
    }

    @Test
    fun `GIVEN initial state WHEN CheckInitialPermission event THEN RequestCameraPermission effect is emitted`() =
        runTest {
            // Given
            val viewModel = createViewModel()

            // When / Then
            viewModel.effects.test {
                viewModel.handleEvent(LiveScannerEvent.CheckInitialPermission)
                assertEquals(LiveScannerEffect.RequestCameraPermission, awaitItem())
            }
        }

    @Test
    fun `GIVEN permission not granted WHEN OnPermissionResult with granted true THEN state has camera permission`() {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnPermissionResult(granted = true))

        // Then
        assertTrue(viewModel.uiState.value.hasCameraPermission)
        assertFalse(viewModel.uiState.value.isPermissionDeniedGlobally)
    }

    @Test
    fun `GIVEN permission not granted WHEN OnPermissionResult with granted false THEN state is permission denied`() {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnPermissionResult(granted = false))

        // Then
        assertFalse(viewModel.uiState.value.hasCameraPermission)
        assertTrue(viewModel.uiState.value.isPermissionDeniedGlobally)
    }

    @Test
    fun `GIVEN viewModel exists WHEN OnCameraError event THEN ShowError effect is emitted`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnCameraError("Camera bind failed"))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ShowError)
            assertEquals("Camera bind failed", (effect as LiveScannerEffect.ShowError).message)
        }
    }
}
