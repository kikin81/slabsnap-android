package us.kikinsoft.slabsnap.data.mlkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured data extracted from the FRONT of a scanned sticker card by the on-device
 * Gemini Nano model.
 *
 * The front scan focuses only on visual attributes (border color and foil status).
 * The sticker code is always extracted from the BACK in a separate step.
 */
@Serializable
data class ExtractedCardData(
    @SerialName("border_color") val borderColor: String,
    @SerialName("is_foil") val isFoil: Boolean,
)
