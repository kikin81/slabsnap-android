package us.kikinsoft.slabsnap.ui.collection

import androidx.compose.runtime.Immutable

@Immutable
data class StickerUiModel(
    val id: Long,
    val stickerCode: String,
    val playerName: String,
    val teamName: String,
    val isOwned: Boolean = false,
    val borderColor: String = "White",
    val isFoil: Boolean = false,
)
