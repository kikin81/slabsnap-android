package us.kikinsoft.slabsnap.ui.collection

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import us.kikinsoft.slabsnap.ui.mvi.UiEffect
import us.kikinsoft.slabsnap.ui.mvi.UiEvent
import us.kikinsoft.slabsnap.ui.mvi.UiState

enum class FilterMode { ALL, MISSING, PARALLELS }

data class CollectionListState(
    val stickers: ImmutableList<StickerUiModel> = persistentListOf(),
    val filter: FilterMode = FilterMode.ALL,
    val uniqueOwnedCount: Int = 0,
    val totalInSet: Int = 670,
    val selectedStickerForAdd: StickerUiModel? = null,
    val isLoading: Boolean = true,
) : UiState

sealed interface CollectionListEvent : UiEvent {
    data object LoadStickers : CollectionListEvent

    data object NavigateToScanner : CollectionListEvent

    data class SetFilter(val filter: FilterMode) : CollectionListEvent

    data class OnQuickAddClicked(val sticker: StickerUiModel) : CollectionListEvent

    data object DismissQuickAdd : CollectionListEvent

    data class OnColorSelected(val borderColor: String) : CollectionListEvent
}

sealed interface CollectionListEffect : UiEffect {
    data object NavigateToScanner : CollectionListEffect

    data class ShowError(val message: String) : CollectionListEffect
}
