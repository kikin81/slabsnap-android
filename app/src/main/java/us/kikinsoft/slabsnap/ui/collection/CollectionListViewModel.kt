package us.kikinsoft.slabsnap.ui.collection

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import us.kikinsoft.slabsnap.domain.repository.StickerRepository
import us.kikinsoft.slabsnap.ui.mvi.MviViewModel
import javax.inject.Inject

@HiltViewModel
class CollectionListViewModel @Inject constructor(private val stickerRepository: StickerRepository) :
    MviViewModel<CollectionListState, CollectionListEvent, CollectionListEffect>(
        CollectionListState(),
    ) {
    init {
        handleEvent(CollectionListEvent.LoadStickers)
    }

    override fun handleEvent(event: CollectionListEvent) {
        when (event) {
            is CollectionListEvent.LoadStickers -> loadStickers()
            is CollectionListEvent.DeleteSticker -> deleteSticker(event.stickerId)
            is CollectionListEvent.NavigateToScanner -> sendEffect(CollectionListEffect.NavigateToScanner)
        }
    }

    private fun loadStickers() {
        stickerRepository
            .getStickers()
            .map { entities ->
                entities.map { entity ->
                    StickerUiModel(
                        id = entity.id,
                        stickerCode = entity.stickerCode,
                        playerName = entity.playerName,
                        teamName = entity.teamName,
                    )
                }.toImmutableList()
            }
            .onEach { stickers ->
                setState { copy(stickers = stickers, isLoading = false) }
            }
            .catch { e ->
                setState { copy(isLoading = false) }
                sendEffect(CollectionListEffect.ShowError(e.message ?: "Failed to load stickers"))
            }
            .launchIn(viewModelScope)
    }

    private fun deleteSticker(stickerId: Long) {
        viewModelScope.launch {
            try {
                stickerRepository.deleteStickerById(stickerId)
            } catch (e: Exception) {
                sendEffect(CollectionListEffect.ShowError(e.message ?: "Failed to delete sticker"))
            }
        }
    }
}
