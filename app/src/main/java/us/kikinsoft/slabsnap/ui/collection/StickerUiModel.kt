package us.kikinsoft.slabsnap.ui.collection

import androidx.compose.runtime.Immutable

@Immutable
data class StickerUiModel(
    val id: Long,
    val stickerCode: String,
    val playerName: String,
    val teamName: String,
)
