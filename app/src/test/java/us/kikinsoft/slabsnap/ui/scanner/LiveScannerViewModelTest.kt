package us.kikinsoft.slabsnap.ui.scanner

import android.graphics.Bitmap
import app.cash.turbine.test
import com.google.mlkit.genai.common.FeatureStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import us.kikinsoft.slabsnap.MainDispatcherExtension
import us.kikinsoft.slabsnap.data.mlkit.CardDataExtractor
import us.kikinsoft.slabsnap.data.mlkit.ExtractedCardData

class LiveScannerViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = MainDispatcherExtension()
    }

    private val cardDataExtractor: CardDataExtractor = mockk()

    private fun createViewModel(): LiveScannerViewModel = LiveScannerViewModel(cardDataExtractor)

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

    @Test
    fun `GIVEN model available WHEN OnStabilityReached THEN state isStable and isExtracting`() {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } coAnswers {
            kotlinx.coroutines.delay(1000)
            ExtractedCardData("Messi", "Argentina", "ARG 10", false)
        }
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        assertTrue(viewModel.uiState.value.isStable)
        assertTrue(viewModel.uiState.value.isExtracting)
    }

    @Test
    fun `GIVEN model available WHEN extraction succeeds THEN state has extractedData`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val expectedData = ExtractedCardData("Messi", "Argentina", "ARG 10", false)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns expectedData
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertFalse(state.isExtracting)
            assertEquals(expectedData, state.extractedData)
        }
    }

    @Test
    fun `GIVEN model available WHEN extraction succeeds THEN ExtractionSuccess effect emitted`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val expectedData = ExtractedCardData("Messi", "Argentina", "ARG 10", false)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns expectedData
        val viewModel = createViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ExtractionSuccess)
            assertEquals(expectedData, (effect as LiveScannerEffect.ExtractionSuccess).data)
        }
    }

    @Test
    fun `GIVEN model available WHEN extraction fails THEN ShowError effect emitted`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } throws RuntimeException("AI inference failed")
        val viewModel = createViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ShowError)
            assertEquals("AI inference failed", (effect as LiveScannerEffect.ShowError).message)
        }
    }

    @Test
    fun `GIVEN model downloadable WHEN OnStabilityReached THEN downloads model then extracts`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val expectedData = ExtractedCardData("Mbappe", "France", "FRA 07", false)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.DOWNLOADABLE
        coEvery { cardDataExtractor.downloadModel() } returns Unit
        coEvery { cardDataExtractor.extract(bitmap) } returns expectedData
        val viewModel = createViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ExtractionSuccess)
            assertEquals(expectedData, (effect as LiveScannerEffect.ExtractionSuccess).data)
        }
        coVerify { cardDataExtractor.downloadModel() }
    }

    @Test
    fun `GIVEN model unavailable WHEN OnStabilityReached THEN ShowError effect emitted`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.UNAVAILABLE
        val viewModel = createViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ShowError)
            assertEquals(
                "AI model is not available on this device",
                (effect as LiveScannerEffect.ShowError).message,
            )
        }
    }

    @Test
    fun `GIVEN model downloading WHEN OnStabilityReached THEN downloads then extracts`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val expectedData = ExtractedCardData("Salah", "Egypt", "EGY 10", false)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.DOWNLOADING
        coEvery { cardDataExtractor.downloadModel() } returns Unit
        coEvery { cardDataExtractor.extract(bitmap) } returns expectedData
        val viewModel = createViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ExtractionSuccess)
            assertEquals(expectedData, (effect as LiveScannerEffect.ExtractionSuccess).data)
        }
        coVerify { cardDataExtractor.downloadModel() }
    }

    @Test
    fun `GIVEN already extracting WHEN OnStabilityReached again THEN second extraction is ignored`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } coAnswers {
            kotlinx.coroutines.delay(1000)
            ExtractedCardData("Messi", "Argentina", "ARG 10", false)
        }
        val viewModel = createViewModel()

        // When — fire stability twice rapidly
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then — extract should only be called once
        coVerify(exactly = 1) { cardDataExtractor.extract(bitmap) }
    }

    @Test
    fun `GIVEN extraction completed WHEN ResetExtraction event THEN state is cleared`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val data = ExtractedCardData("Messi", "Argentina", "ARG 10", false)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns data
        val viewModel = createViewModel()
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Wait for extraction to complete
        viewModel.uiState.test {
            skipItems(1)
            cancelAndIgnoreRemainingEvents()
        }

        // When
        viewModel.handleEvent(LiveScannerEvent.ResetExtraction)

        // Then
        assertFalse(viewModel.uiState.value.isStable)
        assertFalse(viewModel.uiState.value.isExtracting)
        assertFalse(viewModel.uiState.value.isDownloadingModel)
        assertEquals(null, viewModel.uiState.value.extractedData)
    }
}
