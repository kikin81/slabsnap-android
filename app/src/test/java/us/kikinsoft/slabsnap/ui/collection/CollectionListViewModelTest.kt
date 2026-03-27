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
    fun `shows loading state before repository emits`() = runTest {
        val loadingFlow = MutableSharedFlow<List<StickerEntity>>()
        every { repository.getStickers() } returns loadingFlow

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.isLoading)

        loadingFlow.emit(emptyList())
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loads stickers on init`() = runTest {
        val stickers = listOf(testSticker(), testSticker(id = 2L, stickerCode = "BRA 9"))
        every { repository.getStickers() } returns flowOf(stickers)

        val viewModel = createViewModel()

        assertEquals(stickers, viewModel.uiState.value.stickers)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `empty state when no stickers`() = runTest {
        every { repository.getStickers() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.stickers.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `emits error effect when loading fails`() = runTest {
        every { repository.getStickers() } returns flow { throw RuntimeException("DB error") }

        val viewModel = createViewModel()

        viewModel.effects.test {
            val effect = awaitItem()
            assertTrue(effect is CollectionListEffect.ShowError)
            assertEquals("DB error", (effect as CollectionListEffect.ShowError).message)
        }
    }

    @Test
    fun `delete sticker calls repository`() = runTest {
        val sticker = testSticker()
        every { repository.getStickers() } returns flowOf(listOf(sticker))
        coEvery { repository.deleteSticker(sticker) } returns Unit

        val viewModel = createViewModel()
        viewModel.handleEvent(CollectionListEvent.DeleteSticker(sticker))

        coVerify { repository.deleteSticker(sticker) }
    }

    @Test
    fun `delete sticker emits error on failure`() = runTest {
        val sticker = testSticker()
        every { repository.getStickers() } returns flowOf(listOf(sticker))
        coEvery { repository.deleteSticker(sticker) } throws RuntimeException("Delete failed")

        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.handleEvent(CollectionListEvent.DeleteSticker(sticker))
            val effect = awaitItem()
            assertTrue(effect is CollectionListEffect.ShowError)
            assertEquals("Delete failed", (effect as CollectionListEffect.ShowError).message)
        }
    }

    @Test
    fun `navigate to scanner emits effect`() = runTest {
        every { repository.getStickers() } returns flowOf(emptyList())

        val viewModel = createViewModel()

        viewModel.effects.test {
            viewModel.handleEvent(CollectionListEvent.NavigateToScanner)
            assertEquals(CollectionListEffect.NavigateToScanner, awaitItem())
        }
    }
}
