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
        stickerCode: String = "ARG20",
        playerName: String = "Lionel Messi",
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
    fun `GIVEN viewModel is created THEN initial phase is ScanningFront`() {
        // Given
        val viewModel = createViewModel()

        // Then
        assertEquals(ScanPhase.ScanningFront, viewModel.uiState.value.phase)
        assertTrue(viewModel.uiState.value.sessionCards.isEmpty())
    }

    @Test
    fun `GIVEN initial state WHEN CheckInitialPermission THEN RequestCameraPermission effect emitted`() = runTest {
        // Given
        val viewModel = createViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.CheckInitialPermission)
            assertEquals(LiveScannerEffect.RequestCameraPermission, awaitItem())
        }
    }

    @Test
    fun `GIVEN permission not granted WHEN OnPermissionResult true THEN state has camera permission`() {
        // Given
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnPermissionResult(granted = true))

        // Then
        assertTrue(viewModel.uiState.value.hasCameraPermission)
    }

    @Test
    fun `GIVEN viewModel WHEN OnCameraError THEN phase transitions to Error`() {
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

    // region Front scan → FlipPrompt (always)

    @Test
    fun `GIVEN ScanningFront WHEN OnStabilityReached THEN transitions to ExtractingFront`() {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extractFront(bitmap) } coAnswers {
            kotlinx.coroutines.delay(1000)
            ExtractedCardData("White", false)
        }
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        assertEquals(ScanPhase.ExtractingFront, viewModel.uiState.value.phase)
    }

    @Test
    fun `GIVEN front extraction succeeds WHEN OnStabilityReached THEN always transitions to FlipPrompt`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extractFront(bitmap) } returns ExtractedCardData("Blue", false)
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.FlipPrompt)
            val front = (phase as ScanPhase.FlipPrompt).front
            assertEquals("Blue", front.borderColor)
            assertFalse(front.isFoil)
        }
    }

    @Test
    fun `GIVEN foil card WHEN front extracted THEN FlipPrompt carries isFoil true`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extractFront(bitmap) } returns ExtractedCardData("White", true)
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.FlipPrompt)
            assertTrue((phase as ScanPhase.FlipPrompt).front.isFoil)
        }
    }

    // endregion

    // region FlipPrompt → auto-detect → back scan → save

    @Test
    fun `GIVEN FlipPrompt WHEN OnStabilityReached THEN extracts back code and saves`() = runTest {
        // Given — front scan completed
        val frontBitmap = mockk<Bitmap>()
        val backBitmap = mockk<Bitmap>()
        val baseSticker = baseStickerEntity()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extractFront(frontBitmap) } returns ExtractedCardData("Blue", false)
        coEvery { cardDataExtractor.extractBackCode(backBitmap) } returns "ARG20"
        coEvery { stickerRepository.findByStickerCode("ARG20") } returns baseSticker
        coEvery { stickerRepository.insertParallelVariant("ARG20", 1L, "Blue") } returns 2L
        val viewModel = createViewModel()

        // Front scan → FlipPrompt
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(frontBitmap))
        viewModel.uiState.test { awaitItem() } // FlipPrompt

        // When — auto-detect flip: next stable frame
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(backBitmap))

        // Then — saves and auto-resumes
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(ScanPhase.ScanningFront, state.phase)
            assertEquals(1, state.sessionCards.size)
            assertEquals("ARG20", state.sessionCards[0].stickerCode)
            assertEquals("Lionel Messi", state.sessionCards[0].playerName)
            assertEquals("Blue", state.sessionCards[0].borderColor)
        }
        coVerify { stickerRepository.insertParallelVariant("ARG20", 1L, "Blue") }
    }

    @Test
    fun `GIVEN full dual scan WHEN completed THEN session counter increments`() = runTest {
        // Given
        val frontBitmap1 = mockk<Bitmap>()
        val backBitmap1 = mockk<Bitmap>()
        val frontBitmap2 = mockk<Bitmap>()
        val backBitmap2 = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extractFront(frontBitmap1) } returns ExtractedCardData("Blue", false)
        coEvery { cardDataExtractor.extractBackCode(backBitmap1) } returns "ARG20"
        coEvery { stickerRepository.findByStickerCode("ARG20") } returns baseStickerEntity()
        coEvery { stickerRepository.insertParallelVariant("ARG20", 1L, "Blue") } returns 2L
        coEvery { cardDataExtractor.extractFront(frontBitmap2) } returns ExtractedCardData("White", false)
        coEvery { cardDataExtractor.extractBackCode(backBitmap2) } returns "FRA19"
        coEvery { stickerRepository.findByStickerCode("FRA19") } returns baseStickerEntity(
            stickerCode = "FRA19",
            playerName = "Kylian Mbappé",
            teamName = "France",
        )
        coEvery { stickerRepository.insertParallelVariant("FRA19", 1L, "White") } returns 3L
        val viewModel = createViewModel()

        // First card
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(frontBitmap1))
        viewModel.uiState.test { awaitItem() }
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(backBitmap1))
        viewModel.uiState.test { awaitItem() }

        // Second card
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(frontBitmap2))
        viewModel.uiState.test { awaitItem() }
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(backBitmap2))

        // Then
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(2, state.sessionCards.size)
            assertEquals("ARG20", state.sessionCards[0].stickerCode)
            assertEquals("FRA19", state.sessionCards[1].stickerCode)
        }
    }

    // endregion

    // region Error handling

    @Test
    fun `GIVEN front extraction fails WHEN OnStabilityReached THEN phase is Error`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extractFront(bitmap) } throws RuntimeException("AI inference failed")
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
    fun `GIVEN back extraction fails WHEN scanning back THEN phase is Error`() = runTest {
        // Given
        val frontBitmap = mockk<Bitmap>()
        val backBitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extractFront(frontBitmap) } returns ExtractedCardData("White", false)
        coEvery { cardDataExtractor.extractBackCode(backBitmap) } throws RuntimeException("Can't read code")
        val viewModel = createViewModel()

        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(frontBitmap))
        viewModel.uiState.test { awaitItem() }

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(backBitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.Error)
            assertEquals("Can't read code", (phase as ScanPhase.Error).message)
        }
    }

    @Test
    fun `GIVEN Error phase WHEN TryAgain THEN resets to ScanningFront`() {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extractFront(bitmap) } throws RuntimeException("fail")
        val viewModel = createViewModel()
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // When
        viewModel.handleEvent(LiveScannerEvent.TryAgain)

        // Then
        assertEquals(ScanPhase.ScanningFront, viewModel.uiState.value.phase)
    }

    // endregion

    // region Model availability

    @Test
    fun `GIVEN model downloadable WHEN scanning THEN downloads then extracts`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.DOWNLOADABLE
        coEvery { cardDataExtractor.downloadModel() } returns Unit
        coEvery { cardDataExtractor.extractFront(bitmap) } returns ExtractedCardData("White", false)
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then
        viewModel.uiState.test {
            val phase = awaitItem().phase
            assertTrue(phase is ScanPhase.FlipPrompt)
        }
        coVerify { cardDataExtractor.downloadModel() }
    }

    @Test
    fun `GIVEN model unavailable WHEN scanning THEN phase transitions to Error`() = runTest {
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

    // endregion

    // region Duplicate extraction guard

    @Test
    fun `GIVEN already extracting WHEN OnStabilityReached again THEN ignored`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extractFront(bitmap) } coAnswers {
            kotlinx.coroutines.delay(1000)
            ExtractedCardData("White", false)
        }
        val viewModel = createViewModel()

        // When — fire twice rapidly
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Then — only one extraction
        coVerify(exactly = 1) { cardDataExtractor.extractFront(bitmap) }
    }

    // endregion
}
