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
import org.junit.jupiter.api.Assertions.assertNull
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

    // endregion

    // region Front-scan resolution (text match found)

    @Test
    fun `GIVEN model available WHEN OnStabilityReached THEN state isStable and isExtracting`() {
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
        assertTrue(viewModel.uiState.value.isStable)
        assertTrue(viewModel.uiState.value.isExtracting)
    }

    @Test
    fun `GIVEN front scan matches a base sticker WHEN OnStabilityReached THEN ResolutionSuccess effect emitted`() =
        runTest {
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

            // When / Then
            viewModel.effects.test {
                viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
                val effect = awaitItem()
                assertTrue(effect is LiveScannerEffect.ResolutionSuccess)
                val success = effect as LiveScannerEffect.ResolutionSuccess
                assertEquals("Messi", success.playerName)
                assertEquals("ARG 10", success.stickerCode)
                assertEquals("Blue", success.borderColor)
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
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            awaitItem() // ResolutionSuccess
        }

        // Then
        coVerify { stickerRepository.insertParallelVariant("ARG 10", 1L, "Red") }
    }

    @Test
    fun `GIVEN front scan matches WHEN resolution succeeds THEN pending state is cleared`() = runTest {
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
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            awaitItem()
        }

        // Then
        assertFalse(viewModel.uiState.value.needsBackScan)
        assertNull(viewModel.uiState.value.pendingBorderColor)
    }

    // endregion

    // region Front-scan → back-scan fallback (no text match)

    @Test
    fun `GIVEN front scan has no text match WHEN OnStabilityReached THEN transitions to back-scan state`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("Messi", "UNKNOWN", "Purple", false)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted
        coEvery { stickerRepository.findBaseStickerByText(1L, "Messi") } returns null
        val viewModel = createViewModel()

        // When
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ShowFlipCard)
            assertEquals("Purple", (effect as LiveScannerEffect.ShowFlipCard).borderColor)
        }

        // Then
        assertTrue(viewModel.uiState.value.needsBackScan)
        assertEquals("Purple", viewModel.uiState.value.pendingBorderColor)
    }

    @Test
    fun `GIVEN front scan returns all UNKNOWN text WHEN OnStabilityReached THEN transitions to back-scan`() = runTest {
        // Given — foil card with no readable text
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("UNKNOWN", "UNKNOWN", "Green", true)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted
        val viewModel = createViewModel()

        // When
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ShowFlipCard)
            assertEquals("Green", (effect as LiveScannerEffect.ShowFlipCard).borderColor)
        }

        // Then
        assertTrue(viewModel.uiState.value.needsBackScan)
        assertEquals("Green", viewModel.uiState.value.pendingBorderColor)
    }

    // endregion

    // region Back-scan resolution

    @Test
    fun `GIVEN in back-scan state WHEN OnStabilityReached THEN extracts back code and persists variant`() = runTest {
        // Given — simulate front scan that transitioned to back-scan
        val frontBitmap = mockk<Bitmap>()
        val backBitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("UNKNOWN", "UNKNOWN", "Blue", true)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(frontBitmap) } returns extracted
        coEvery { cardDataExtractor.extractBackCode(backBitmap) } returns "ARG 10"
        coEvery {
            stickerRepository.insertParallelVariant("ARG 10", 1L, "Blue")
        } returns 3L
        val viewModel = createViewModel()

        // Front scan → transitions to back-scan state
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(frontBitmap))
            awaitItem() // ShowFlipCard
        }

        // When — back scan
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(backBitmap))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ResolutionSuccess)
            val success = effect as LiveScannerEffect.ResolutionSuccess
            assertEquals("ARG 10", success.stickerCode)
            assertEquals("Blue", success.borderColor)
        }

        // Then
        coVerify { cardDataExtractor.extractBackCode(backBitmap) }
        coVerify { stickerRepository.insertParallelVariant("ARG 10", 1L, "Blue") }
        assertFalse(viewModel.uiState.value.needsBackScan)
        assertNull(viewModel.uiState.value.pendingBorderColor)
    }

    // endregion

    // region Model availability

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

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ResolutionSuccess)
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

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
            val effect = awaitItem()
            assertTrue(effect is LiveScannerEffect.ResolutionSuccess)
        }
        coVerify { cardDataExtractor.downloadModel() }
    }

    // endregion

    // region Duplicate extraction guard & reset

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

        // Then — extract should only be called once
        coVerify(exactly = 1) { cardDataExtractor.extract(bitmap) }
    }

    @Test
    fun `GIVEN extraction completed WHEN ResetExtraction event THEN state is cleared`() = runTest {
        // Given
        val bitmap = mockk<Bitmap>()
        val extracted = ExtractedCardData("Messi", "UNKNOWN", "White", false)
        coEvery { cardDataExtractor.checkAvailability() } returns FeatureStatus.AVAILABLE
        coEvery { cardDataExtractor.extract(bitmap) } returns extracted
        coEvery { stickerRepository.findBaseStickerByText(any(), any()) } returns null
        val viewModel = createViewModel()
        viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))

        // Wait for extraction to complete
        viewModel.effects.test {
            awaitItem() // ShowFlipCard
            cancelAndIgnoreRemainingEvents()
        }

        // When
        viewModel.handleEvent(LiveScannerEvent.ResetExtraction)

        // Then
        assertFalse(viewModel.uiState.value.isStable)
        assertFalse(viewModel.uiState.value.isExtracting)
        assertFalse(viewModel.uiState.value.isDownloadingModel)
        assertNull(viewModel.uiState.value.extractedData)
        assertFalse(viewModel.uiState.value.needsBackScan)
        assertNull(viewModel.uiState.value.pendingBorderColor)
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

            // When / Then
            viewModel.effects.test {
                viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
                val effect = awaitItem()
                assertTrue(effect is LiveScannerEffect.ResolutionSuccess)
            }
            coVerify { stickerRepository.findBaseStickerByText(1L, "Argentina") }
        }

    // endregion
}
