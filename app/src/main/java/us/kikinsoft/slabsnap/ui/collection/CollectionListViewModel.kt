package us.kikinsoft.slabsnap.ui.collection

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import us.kikinsoft.slabsnap.domain.repository.StickerRepository
import us.kikinsoft.slabsnap.ui.mvi.MviViewModel

@HiltViewModel
class CollectionListViewModel @Inject constructor(private val stickerRepository: StickerRepository) :
    MviViewModel<CollectionListState, CollectionListEvent, CollectionListEffect>(
        CollectionListState(),
    ) {

    companion object {
        // TODO: collectionSetId should come from user selection
        private const val COLLECTION_SET_ID = 1L
    }

    init {
        handleEvent(CollectionListEvent.LoadStickers)
    }

    override fun handleEvent(event: CollectionListEvent) {
        when (event) {
            is CollectionListEvent.LoadStickers -> loadStickers()
            is CollectionListEvent.NavigateToScanner ->
                sendEffect(CollectionListEffect.NavigateToScanner)
            is CollectionListEvent.SetFilter ->
                setState { copy(filter = event.filter) }
            is CollectionListEvent.OnQuickAddClicked ->
                setState { copy(selectedStickerForAdd = event.sticker) }
            is CollectionListEvent.DismissQuickAdd ->
                setState { copy(selectedStickerForAdd = null) }
            is CollectionListEvent.OnColorSelected ->
                quickAdd(event.borderColor)
        }
    }

    private fun loadStickers() {
        combine(
            stickerRepository.getStickersBySet(COLLECTION_SET_ID),
            stickerRepository.countUniqueOwned(COLLECTION_SET_ID),
        ) { entities, uniqueOwned ->
            val uiModels = entities.map { entity ->
                StickerUiModel(
                    id = entity.id,
                    stickerCode = entity.stickerCode,
                    playerName = entity.playerName,
                    teamName = entity.teamName,
                    isOwned = entity.isOwned,
                    borderColor = entity.borderColor,
                    isFoil = entity.metadata["is_foil"] == "true",
                )
            }.toImmutableList()
            uiModels to uniqueOwned
        }
            .onEach { (stickers, uniqueOwned) ->
                setState {
                    copy(
                        stickers = stickers,
                        uniqueOwnedCount = uniqueOwned,
                        totalInSet = stickers.distinctBy { it.stickerCode }.size,
                        isLoading = false,
                    )
                }
            }
            .catch { e ->
                setState { copy(isLoading = false) }
                sendEffect(
                    CollectionListEffect.ShowError(e.message ?: "Failed to load stickers"),
                )
            }
            .launchIn(viewModelScope)
    }

    private fun quickAdd(borderColor: String) {
        val sticker = uiState.value.selectedStickerForAdd ?: return
        setState { copy(selectedStickerForAdd = null) }

        viewModelScope.launch {
            try {
                stickerRepository.insertParallelVariant(
                    baseStickerCode = sticker.stickerCode,
                    collectionSetId = COLLECTION_SET_ID,
                    borderColor = borderColor,
                )
            } catch (e: Exception) {
                sendEffect(
                    CollectionListEffect.ShowError(
                        e.message ?: "Failed to add sticker",
                    ),
                )
            }
        }
    }
}
