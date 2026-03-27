package us.kikinsoft.slabsnap.ui.collection

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import us.kikinsoft.slabsnap.MainDispatcherExtension
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity
import us.kikinsoft.slabsnap.domain.repository.StickerRepository

class CollectionListViewModelTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherExtension = MainDispatcherExtension()
    }

    private val repository: StickerRepository = mockk()

    private fun createViewModel(): CollectionListViewModel = CollectionListViewModel(repository)

    private fun testSticker(id: Long = 1L, stickerCode: String = "ARG 10", playerName: String = "Lionel Messi") =
        StickerEntity(
            id = id,
            stickerCode = stickerCode,
            playerName = playerName,
            teamName = "Argentina",
            metadata = emptyMap(),
            collectionSetId = 1L,
            isOwned = true,
        )

    @Test
    fun `GIVEN repository has not emitted WHEN viewModel is created THEN state is loading`() = runTest {
        // Given
        val loadingFlow = MutableSharedFlow<List<StickerEntity>>()
        every { repository.getStickers() } returns loadingFlow

        // When
        val viewModel = createViewModel()

        // Then
        assertTrue(viewModel.uiState.value.isLoading)

        loadingFlow.emit(emptyList())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `GIVEN repository returns stickers WHEN viewModel is created THEN state contains stickers`() = runTest {
        // Given
        val stickers = listOf(testSticker(), testSticker(id = 2L, stickerCode = "BRA 9"))
        every { repository.getStickers() } returns flowOf(stickers)

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(stickers, viewModel.uiState.value.stickers)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `GIVEN repository returns empty list WHEN viewModel is created THEN state is empty`() = runTest {
        // Given
        every { repository.getStickers() } returns flowOf(emptyList())

        // When
        val viewModel = createViewModel()

        // Then
        assertTrue(viewModel.uiState.value.stickers.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `GIVEN repository throws WHEN viewModel loads THEN error effect is emitted`() = runTest {
        // Given
        every { repository.getStickers() } returns flow { throw RuntimeException("DB error") }

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.effects.test {
            val effect = awaitItem()
            assertTrue(effect is CollectionListEffect.ShowError)
            assertEquals("DB error", (effect as CollectionListEffect.ShowError).message)
        }
    }

    @Test
    fun `GIVEN a sticker exists WHEN DeleteSticker event is handled THEN repository delete is called`() = runTest {
        // Given
        val sticker = testSticker()
        every { repository.getStickers() } returns flowOf(listOf(sticker))
        coEvery { repository.deleteSticker(sticker) } returns Unit
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(CollectionListEvent.DeleteSticker(sticker))

        // Then
        coVerify { repository.deleteSticker(sticker) }
    }

    @Test
    fun `GIVEN delete will fail WHEN DeleteSticker event is handled THEN error effect is emitted`() = runTest {
        // Given
        val sticker = testSticker()
        every { repository.getStickers() } returns flowOf(listOf(sticker))
        coEvery { repository.deleteSticker(sticker) } throws RuntimeException("Delete failed")
        val viewModel = createViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(CollectionListEvent.DeleteSticker(sticker))
            val effect = awaitItem()
            assertTrue(effect is CollectionListEffect.ShowError)
            assertEquals("Delete failed", (effect as CollectionListEffect.ShowError).message)
        }
    }

    @Test
    fun `GIVEN viewModel exists WHEN NavigateToScanner event is handled THEN navigation effect is emitted`() = runTest {
        // Given
        every { repository.getStickers() } returns flowOf(emptyList())
        val viewModel = createViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(CollectionListEvent.NavigateToScanner)
            assertEquals(CollectionListEffect.NavigateToScanner, awaitItem())
        }
    }
}
