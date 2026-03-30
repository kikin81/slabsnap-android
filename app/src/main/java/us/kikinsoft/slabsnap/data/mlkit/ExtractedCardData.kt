package us.kikinsoft.slabsnap.data.mlkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured data extracted from a scanned sticker card by the on-device Gemini Nano model.
 *
 * Fields use "UNKNOWN" as a sentinel when the model cannot confidently read the value
 * (e.g. glare, motion blur, or obscured text).
 */
@Serializable
data class ExtractedCardData(
    @SerialName("player_name") val playerName: String,
    @SerialName("team_country") val teamCountry: String,
    @SerialName("sticker_number") val stickerNumber: String,
    @SerialName("is_foil") val isFoil: Boolean,
)
