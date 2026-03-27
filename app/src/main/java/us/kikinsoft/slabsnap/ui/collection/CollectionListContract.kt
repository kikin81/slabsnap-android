package us.kikinsoft.slabsnap.ui.collection

import us.kikinsoft.slabsnap.data.local.entity.StickerEntity
import us.kikinsoft.slabsnap.ui.mvi.UiEffect
import us.kikinsoft.slabsnap.ui.mvi.UiEvent
import us.kikinsoft.slabsnap.ui.mvi.UiState

data class CollectionListState(val stickers: List<StickerEntity> = emptyList(), val isLoading: Boolean = true) :
    UiState {
    val isEmpty: Boolean get() = !isLoading && stickers.isEmpty()
}

sealed interface CollectionListEvent : UiEvent {
    data object LoadStickers : CollectionListEvent

    data class DeleteSticker(val sticker: StickerEntity) : CollectionListEvent

    data object NavigateToScanner : CollectionListEvent
}

sealed interface CollectionListEffect : UiEffect {
    data object NavigateToScanner : CollectionListEffect

    data class ShowError(val message: String) : CollectionListEffect
}
