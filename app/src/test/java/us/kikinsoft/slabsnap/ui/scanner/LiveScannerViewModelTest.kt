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
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity
import us.kikinsoft.slabsnap.data.mlkit.CardDataExtractor
import us.kikinsoft.slabsnap.data.mlkit.ExtractedCardData
import us.kikinsoft.slabsnap.domain.repository.StickerRepository

class LiveScannerViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = MainDispatcherExtension()
    }

    private val cardDataExtractor: CardDataExtractor = mockk()
    private val stickerRepository: StickerRepository = mockk(relaxed = true)

    private fun createViewModel(): LiveScannerViewModel = LiveScannerViewModel(cardDataExtractor, stickerRepository)

    private fun baseStickerEntity(
        stickerCode: String = "ARG 10",
        playerName: String = "Messi",
        teamName: String = "Argentina",
    ) = StickerEntity(
        id = 1L,
        stickerCode = stickerCode,
        playerName = playerName,
        teamName = teamName,
        metadata = emptyMap(),
        collectionSetId = 1L,
        borderColor = "White",
        isOwned = false,
    )

    // region Permission & basic events

    @Test
    fun `GIVEN viewModel is created THEN initial phase is Scanning`() {
        // Given
        val viewModel = createViewModel()

        // Then
        assertFalse(viewModel.uiState.value.hasCameraPermission)
        assertFalse(viewModel.uiState.value.isPermissionDeniedGlobally)
        assertEquals(ScanPhase.Scanning, viewModel.uiState.value.phase)
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
    fun `GIVEN viewModel exists WHEN OnCameraError event THEN phase transitions to Error`() {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnCameraError("Camera bind failed"))

        // Then
        val phase = viewModel.uiState.value.phase
        assertTrue(phase is ScanPhase.Error)
        assertEquals("Camera bind failed", (phase as ScanPhase.Error).message)
    }

    // endregion

    // region Front-scan resolution (text match found)

    @Test
    fun `GIVEN model available WHEN OnStabilityReached THEN phase transitions to Extracting`() {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } coAnswers {
            kotlinx.coroutines.delay(1000)
            ExtractedCardData("Messi", "Argentina", "White", false)
        }
        coEvery { stickerRepository.findBaseStickerByText(any(), any()) } returns null
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        assertEquals(ScanPhase.Extracting, viewModel.uiState.value.phase)
    }

    @Test
    fun `GIVEN front scan matches a base sticker WHEN OnStabilityReached THEN phase is ShowingResult`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("Messi", "UNKNOWN", "Blue", false)
        val baseSticker = baseStickerEntity()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted
        coEvery { stickerRepository.findBaseStickerByText(1L, "Messi") } returns baseSticker
        coEvery {
            stickerRepository.insertParallelVariant("ARG 10", 1L, "Blue")
        } returns 2L
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then — wait for coroutine to complete
        viewModel.uiState.test {
            val state = awaitItem()
            val phase = state.phase
            assertTrue(phase is ScanPhase.ShowingResult)
            val result = phase as ScanPhase.ShowingResult
            assertEquals("Messi", result.sticker.playerName)
            assertEquals("ARG 10", result.sticker.stickerCode)
            assertEquals("Blue", result.borderColor)
        }
    }

    @Test
    fun `GIVEN front scan matches WHEN resolution succeeds THEN parallel variant is persisted`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("Messi", "UNKNOWN", "Red", false)
        val baseSticker = baseStickerEntity()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted
        coEvery { stickerRepository.findBaseStickerByText(1L, "Messi") } returns baseSticker
        coEvery {
            stickerRepository.insertParallelVariant("ARG 10", 1L, "Red")
        } returns 2L
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then — wait for the state to settle
        viewModel.uiState.test {
            awaitItem() // ShowingResult
        }
        coVerify { stickerRepository.insertParallelVariant("ARG 10", 1L, "Red") }
    }

    // endregion

    // region Front-scan → back-scan fallback (no text match)

    @Test
    fun `GIVEN front scan has no text match WHEN OnStabilityReached THEN phase transitions to FlipPrompt`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("Messi", "UNKNOWN", "Purple", false)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted
        coEvery { stickerRepository.findBaseStickerByText(1L, "Messi") } returns null
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.FlipPrompt)
            assertEquals("Purple", (phase as ScanPhase.FlipPrompt).borderColor)
        }
    }

    @Test
    fun `GIVEN front scan returns all UNKNOWN text WHEN OnStabilityReached THEN transitions to FlipPrompt`() = runTest {
        // Given — foil card with no readable text
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("UNKNOWN", "UNKNOWN", "Green", true)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.FlipPrompt)
            assertEquals("Green", (phase as ScanPhase.FlipPrompt).borderColor)
        }
    }

    // endregion

    // region User action events

    @Test
    fun `GIVEN phase is FlipPrompt WHEN ScanBack event THEN phase transitions to ScanningBack`() {
        // Given
        val viewModel = createViewModel()
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("UNKNOWN", "UNKNOWN", "Blue", true)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted

        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // When
        viewModel.handleEvent(LiveScannerEvent.ScanBack)

        // Then
        val phase = viewModel.uiState.value.phase
        assertTrue(phase is ScanPhase.ScanningBack)
        assertEquals("Blue", (phase as ScanPhase.ScanningBack).borderColor)
    }

    @Test
    fun `GIVEN phase is ShowingResult WHEN ScanNext event THEN phase resets to Scanning`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("Messi", "UNKNOWN", "White", false)
        val baseSticker = baseStickerEntity()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted
        coEvery { stickerRepository.findBaseStickerByText(1L, "Messi") } returns baseSticker
        coEvery { stickerRepository.insertParallelVariant("ARG 10", 1L, "White") } returns 2L
        val viewModel = createViewModel()

        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
        viewModel.uiState.test {
            awaitItem() // ShowingResult
        }

        // When
        viewModel.handleEvent(LiveScannerEvent.ScanNext)

        // Then
        assertEquals(ScanPhase.Scanning, viewModel.uiState.value.phase)
    }

    @Test
    fun `GIVEN phase is Error WHEN TryAgain event THEN phase resets to Scanning`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } throws RuntimeException("AI inference failed")
        val viewModel = createViewModel()

        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.Error)
        }

        // When
        viewModel.handleEvent(LiveScannerEvent.TryAgain)

        // Then
        assertEquals(ScanPhase.Scanning, viewModel.uiState.value.phase)
    }

    // endregion

    // region Back-scan resolution

    @Test
    fun `GIVEN in ScanningBack phase WHEN OnStabilityReached THEN extracts back code and shows result`() = runTest {
        // Given — simulate front scan that transitioned to FlipPrompt, then ScanBack
        val frontBitmap = mockk<Bitmap>()
        val backBitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("UNKNOWN", "UNKNOWN", "Blue", true)
        val baseSticker = baseStickerEntity()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(frontBitmap) } returns extracted
        coEvery { cardDataExtractor.extractBackCode(backBitmap) } returns "ARG10"
        coEvery { stickerRepository.findByStickerCode("ARG10") } returns baseSticker
        coEvery {
            stickerRepository.insertParallelVariant("ARG10", 1L, "Blue")
        } returns 3L
        val viewModel = createViewModel()

        // Front scan → FlipPrompt
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(frontBitmap))
        viewModel.uiState.test { awaitItem() } // FlipPrompt

        // User taps "Scan Back" → ScanningBack
        viewModel.handleEvent(LiveScannerEvent.ScanBack)
        assertEquals(ScanPhase.ScanningBack("Blue"), viewModel.uiState.value.phase)

        // When — back scan
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(backBitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.ShowingResult)
            val result = phase as ScanPhase.ShowingResult
            assertEquals("ARG 10", result.sticker.stickerCode)
            assertEquals("Blue", result.borderColor)
        }
        coVerify { cardDataExtractor.extractBackCode(backBitmap) }
        coVerify { stickerRepository.insertParallelVariant("ARG10", 1L, "Blue") }
    }

    // endregion

    // region Model availability

    @Test
    fun `GIVEN model available WHEN extraction fails THEN phase transitions to Error`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } throws RuntimeException("AI inference failed")
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.Error)
            assertEquals("AI inference failed", (phase as ScanPhase.Error).message)
        }
    }

    @Test
    fun `GIVEN model downloadable WHEN OnStabilityReached THEN downloads model then extracts`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("Mbappe", "France", "White", false)
        val baseSticker = baseStickerEntity(
            stickerCode = "FRA 07",
            playerName = "Mbappe",
            teamName = "France",
        )
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.DOWNLOADABLE
        coEvery { cardDataExtractor.downloadModel() } returns Unit
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted
        coEvery { stickerRepository.findBaseStickerByText(1L, "Mbappe") } returns baseSticker
        coEvery {
            stickerRepository.insertParallelVariant("FRA 07", 1L, "White")
        } returns 2L
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.ShowingResult)
        }
        coVerify { cardDataExtractor.downloadModel() }
    }

    @Test
    fun `GIVEN model unavailable WHEN OnStabilityReached THEN phase transitions to Error`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.UNAVAILABLE
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.Error)
            assertEquals(
                "AI model is not available on this device",
                (phase as ScanPhase.Error).message,
            )
        }
    }

    @Test
    fun `GIVEN model downloading WHEN OnStabilityReached THEN downloads then extracts`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("Salah", "Egypt", "White", false)
        val baseSticker = baseStickerEntity(
            stickerCode = "EGY 10",
            playerName = "Salah",
            teamName = "Egypt",
        )
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.DOWNLOADING
        coEvery { cardDataExtractor.downloadModel() } returns Unit
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted
        coEvery { stickerRepository.findBaseStickerByText(1L, "Salah") } returns baseSticker
        coEvery {
            stickerRepository.insertParallelVariant("EGY 10", 1L, "White")
        } returns 2L
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.ShowingResult)
        }
        coVerify { cardDataExtractor.downloadModel() }
    }

    // endregion

    // region Duplicate extraction guard

    @Test
    fun `GIVEN already extracting WHEN OnStabilityReached again THEN second extraction is ignored`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } coAnswers {
            kotlinx.coroutines.delay(1000)
            ExtractedCardData("Messi", "Argentina", "White", false)
        }
        coEvery { stickerRepository.findBaseStickerByText(any(), any()) } returns null
        val viewModel = createViewModel()

        // When — fire stability twice rapidly
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then — extract should only be called once (second event ignored because phase is Extracting)
        coVerify(exactly = 1) { cardDataExtractor.extract(bitmap) }
    }

    // endregion

    // region Badge text fallback

    @Test
    fun `GIVEN primaryText is UNKNOWN but badgeText is available WHEN front scan THEN uses badgeText for lookup`() =
        runTest {
            // Given
            val bitmap = mockk<Bitmap>()
            val extracted = ExtractedCardData("UNKNOWN", "Argentina", "White", false)
            val baseSticker = baseStickerEntity()
            coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
            coEvery { cardDataExtractor.extract(bitmap) } returns extracted
            coEvery { stickerRepository.findBaseStickerByText(1L, "Argentina") } returns baseSticker
            coEvery {
                stickerRepository.insertParallelVariant("ARG 10", 1L, "White")
            } returns 2L
            val viewModel = createViewModel()

            // When
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

            // Then
            viewModel.uiState.test {
                val phase = awaitItem().phase
                assertTrue(phase is ScanPhase.ShowingResult)
            }
            coVerify { stickerRepository.findBaseStickerByText(1L, "Argentina") }
        }

    // endregion
}
