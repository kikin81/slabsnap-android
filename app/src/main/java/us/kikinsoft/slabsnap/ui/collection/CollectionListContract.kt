package us.kikinsoft.slabsnap.ui.collection

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import us.kikinsoft.slabsnap.ui.mvi.UiEffect
import us.kikinsoft.slabsnap.ui.mvi.UiEvent
import us.kikinsoft.slabsnap.ui.mvi.UiState

data class CollectionListState(
    val stickers: ImmutableList<StickerUiModel> = persistentListOf(),
    val isLoading: Boolean = true,
) : UiState {
    val isEmpty: Boolean get() = !isLoading && stickers.isEmpty()
}

sealed interface CollectionListEvent : UiEvent {
    data object LoadStickers : CollectionListEvent

    data class DeleteSticker(val stickerId: Long) : CollectionListEvent

    data object NavigateToScanner : CollectionListEvent
}

sealed interface CollectionListEffect : UiEffect {
    data object NavigateToScanner : CollectionListEffect

    data class ShowError(val message: String) : CollectionListEffect
}
