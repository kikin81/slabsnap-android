package us.kikinsoft.slabsnap.data.mlkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured data extracted from the front of a scanned sticker card by the on-device
 * Gemini Nano model.
 *
 * Fields use "UNKNOWN" as a sentinel when the model cannot confidently read the value
 * (e.g. glare, motion blur, or obscured text).
 */
@Serializable
data class ExtractedCardData(
    @SerialName("primary_text") val primaryText: String,
    @SerialName("badge_text") val badgeText: String,
    @SerialName("border_color") val borderColor: String,
    @SerialName("is_foil") val isFoil: Boolean,
)
