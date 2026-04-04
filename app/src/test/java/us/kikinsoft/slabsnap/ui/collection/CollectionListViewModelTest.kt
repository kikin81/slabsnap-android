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
import org.junit.jupiter.api.Assertions.assertNull
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

    private fun stubRepository(
        stickers: List<StickerEntity> = emptyList(),
        uniqueOwned: Int = 0,
    ) {
        every { repository.getStickersBySet(1L) } returns flowOf(stickers)
        every { repository.countUniqueOwned(1L) } returns flowOf(uniqueOwned)
    }

    private fun createViewModel(): CollectionListViewModel = CollectionListViewModel(repository)

    private fun testSticker(
        id: Long = 1L,
        stickerCode: String = "ARG 10",
        playerName: String = "Lionel Messi",
        teamName: String = "Argentina",
        isOwned: Boolean = false,
        borderColor: String = "White",
        metadata: Map<String, String> = emptyMap(),
    ) = StickerEntity(
        id = id,
        stickerCode = stickerCode,
        playerName = playerName,
        teamName = teamName,
        metadata = metadata,
        collectionSetId = 1L,
        isOwned = isOwned,
        borderColor = borderColor,
    )

    // region Loading

    @Test
    fun `GIVEN repository has not emitted WHEN viewModel is created THEN state is loading`() = runTest {
        // Given
        val stickersFlow = MutableSharedFlow<List<StickerEntity>>()
        val ownedFlow = MutableSharedFlow<Int>()
        every { repository.getStickersBySet(1L) } returns stickersFlow
        every { repository.countUniqueOwned(1L) } returns ownedFlow

        // When
        val viewModel = createViewModel()

        // Then
        assertTrue(viewModel.uiState.value.isLoading)

        stickersFlow.emit(emptyList())
        ownedFlow.emit(0)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // endregion

    // region Sticker loading and mapping

    @Test
    fun `GIVEN repository returns stickers WHEN viewModel is created THEN state maps entities to UI models`() =
        runTest {
            // Given
            val stickers = listOf(
                testSticker(isOwned = true, borderColor = "Blue"),
                testSticker(id = 2L, stickerCode = "BRA 9", playerName = "Richarlison", teamName = "Brazil"),
            )
            stubRepository(stickers = stickers, uniqueOwned = 1)

            // When
            val viewModel = createViewModel()

            // Then
            val uiModels = viewModel.uiState.value.stickers
            assertEquals(2, uiModels.size)
            assertTrue(uiModels[0].isOwned)
            assertEquals("Blue", uiModels[0].borderColor)
            assertFalse(uiModels[1].isOwned)
        }

    @Test
    fun `GIVEN sticker has is_foil metadata WHEN mapped THEN UI model isFoil is true`() = runTest {
        // Given
        val sticker = testSticker(metadata = mapOf("is_foil" to "true"))
        stubRepository(stickers = listOf(sticker))

        // When
        val viewModel = createViewModel()

        // Then
        assertTrue(viewModel.uiState.value.stickers[0].isFoil)
    }

    @Test
    fun `GIVEN repository throws WHEN viewModel loads THEN error effect is emitted`() = runTest {
        // Given
        every { repository.getStickersBySet(1L) } returns flow { throw RuntimeException("DB error") }
        every { repository.countUniqueOwned(1L) } returns flowOf(0)

        // When
        val viewModel = createViewModel()

        // Then
        viewModel.effects.test {
            val effect = awaitItem()
            assertTrue(effect is CollectionListEffect.ShowError)
            assertEquals("DB error", (effect as CollectionListEffect.ShowError).message)
        }
    }

    // endregion

    // region Progress

    @Test
    fun `GIVEN repository returns owned count WHEN viewModel loads THEN progress is reflected in state`() = runTest {
        // Given
        val stickers = listOf(
            testSticker(isOwned = true),
            testSticker(id = 2L, stickerCode = "BRA 9", playerName = "Richarlison", teamName = "Brazil"),
        )
        stubRepository(stickers = stickers, uniqueOwned = 1)

        // When
        val viewModel = createViewModel()

        // Then
        assertEquals(1, viewModel.uiState.value.uniqueOwnedCount)
        assertEquals(2, viewModel.uiState.value.totalInSet)
    }

    // endregion

    // region Filtering

    @Test
    fun `GIVEN filter is ALL WHEN SetFilter to MISSING THEN state filter updates`() = runTest {
        // Given
        stubRepository()
        val viewModel = createViewModel()

        // When
        viewModel.handleEvent(CollectionListEvent.SetFilter(FilterMode.MISSING))

        // Then
        assertEquals(FilterMode.MISSING, viewModel.uiState.value.filter)
    }

    // endregion

    // region Quick Add

    @Test
    fun `GIVEN an unowned sticker WHEN OnQuickAddClicked THEN selectedStickerForAdd is set`() = runTest {
        // Given
        stubRepository(stickers = listOf(testSticker()))
        val viewModel = createViewModel()
        val stickerUi = viewModel.uiState.value.stickers[0]

        // When
        viewModel.handleEvent(CollectionListEvent.OnQuickAddClicked(stickerUi))

        // Then
        assertEquals(stickerUi, viewModel.uiState.value.selectedStickerForAdd)
    }

    @Test
    fun `GIVEN quick add sheet open WHEN DismissQuickAdd THEN selectedStickerForAdd is null`() = runTest {
        // Given
        stubRepository(stickers = listOf(testSticker()))
        val viewModel = createViewModel()
        viewModel.handleEvent(CollectionListEvent.OnQuickAddClicked(viewModel.uiState.value.stickers[0]))

        // When
        viewModel.handleEvent(CollectionListEvent.DismissQuickAdd)

        // Then
        assertNull(viewModel.uiState.value.selectedStickerForAdd)
    }

    @Test
    fun `GIVEN quick add sheet open WHEN OnColorSelected THEN repository inserts variant and sheet closes`() = runTest {
        // Given
        stubRepository(stickers = listOf(testSticker()))
        coEvery { repository.insertParallelVariant("ARG 10", 1L, "Blue") } returns 2L
        val viewModel = createViewModel()
        viewModel.handleEvent(CollectionListEvent.OnQuickAddClicked(viewModel.uiState.value.stickers[0]))

        // When
        viewModel.handleEvent(CollectionListEvent.OnColorSelected("Blue"))

        // Then
        coVerify { repository.insertParallelVariant("ARG 10", 1L, "Blue") }
        assertNull(viewModel.uiState.value.selectedStickerForAdd)
    }

    @Test
    fun `GIVEN quick add fails WHEN OnColorSelected THEN error effect is emitted`() = runTest {
        // Given
        stubRepository(stickers = listOf(testSticker()))
        coEvery {
            repository.insertParallelVariant("ARG 10", 1L, "Red")
        } throws RuntimeException("Insert failed")
        val viewModel = createViewModel()
        viewModel.handleEvent(CollectionListEvent.OnQuickAddClicked(viewModel.uiState.value.stickers[0]))

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(CollectionListEvent.OnColorSelected("Red"))
            val effect = awaitItem()
            assertTrue(effect is CollectionListEffect.ShowError)
            assertEquals("Insert failed", (effect as CollectionListEffect.ShowError).message)
        }
    }

    // endregion

    // region Navigation

    @Test
    fun `GIVEN viewModel exists WHEN NavigateToScanner event THEN navigation effect is emitted`() = runTest {
        // Given
        stubRepository()
        val viewModel = createViewModel()

        // When / Then
        viewModel.effects.test {
            viewModel.handleEvent(CollectionListEvent.NavigateToScanner)
            assertEquals(CollectionListEffect.NavigateToScanner, awaitItem())
        }
    }

    // endregion
}
