package us.kikinsoft.slabsnap.data.mlkit

import android.graphics.Bitmap
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.GenerateContentResponse
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.GenerativeModel
import com.google.mlkit.genai.prompt.ImagePart
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * Extracts structured card data from a captured [Bitmap] using the on-device Gemini Nano
 * model via the ML Kit GenAI Prompt API.
 *
 * Call [checkAvailability] before [extract] to verify the model is ready on the device.
 */
@Singleton
class CardDataExtractor @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    private val generativeModel: GenerativeModel by lazy {
        Generation.getClient()
    }

    /**
     * Checks whether the on-device Gemini Nano model is available for inference.
     *
     * @return the [FeatureStatus] int constant — callers should handle
     * [FeatureStatus.DOWNLOADABLE] and [FeatureStatus.UNAVAILABLE].
     */
    suspend fun checkAvailability(): Int = generativeModel.checkStatus()

    /**
     * Triggers model download and suspends until complete.
     *
     * @throws Exception if download fails.
     */
    suspend fun downloadModel() {
        val terminal = generativeModel.download()
            .filter { it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed }
            .first()

        if (terminal is DownloadStatus.DownloadFailed) {
            throw ExtractionException("Model download failed")
        }
    }

    /**
     * Sends the captured front-side card [bitmap] to Gemini Nano and returns structured
     * [ExtractedCardData] containing visible text and border color.
     *
     * @throws ExtractionException if the model returns unparseable output.
     * @throws Exception if inference fails at the platform level.
     */
    suspend fun extract(bitmap: Bitmap): ExtractedCardData {
        val request = generateContentRequest(
            ImagePart(bitmap),
            TextPart(EXTRACTION_PROMPT),
        ) {
            temperature = 0.0f
            topK = 10
            maxOutputTokens = 256
        }

        val response: GenerateContentResponse = generativeModel.generateContent(request)

        val text = response.candidates.firstOrNull()?.text
            ?: throw ExtractionException("Model returned empty response")

        return try {
            json.decodeFromString<ExtractedCardData>(text)
        } catch (e: Exception) {
            throw ExtractionException("Failed to parse model response: $text", e)
        }
    }

    /**
     * Extracts the alphanumeric sticker code from the back of a card (e.g. "ARG 2", "FWC 9").
     *
     * @return the sticker code string, or throws if extraction fails.
     */
    suspend fun extractBackCode(bitmap: Bitmap): String {
        val request = generateContentRequest(
            ImagePart(bitmap),
            TextPart(BACK_CODE_PROMPT),
        ) {
            temperature = 0.0f
            topK = 10
            maxOutputTokens = 64
        }

        val response: GenerateContentResponse = generativeModel.generateContent(request)

        return response.candidates.firstOrNull()?.text?.trim()
            ?: throw ExtractionException("Model returned empty response for back code")
    }

    companion object {
        private val EXTRACTION_PROMPT = """
            You are an automated, highly precise optical data extraction system for Panini soccer sticker cards. You are looking at the FRONT side of a sticker.

            Extract the following fields from the card image:
            - primary_text: The main name or text visible on the card (e.g. player name, team name, stadium name). This is the most prominent text.
            - badge_text: Any text on a crest, badge, or emblem if present (e.g. national federation name). If none, use "UNKNOWN".
            - border_color: The color of the border framing the sticker. Common values: White, Blue, Red, Purple, Green, Black. Default to "White" if unsure.
            - is_foil: true if the card has holographic, shiny, or foil elements; false otherwise.

            Rules:
            - Do not output any text outside of the JSON object.
            - If a text value is obscured, illegible, or not present, output the exact string "UNKNOWN".
            - Do NOT attempt to guess the sticker number. It is NOT on this side of the card.
            - The border color refers to the decorative frame around the sticker image, not the card background.

            Respond with only a JSON object containing exactly these four keys:
            {"primary_text": "", "badge_text": "", "border_color": "White", "is_foil": false}
        """.trimIndent()

        private val BACK_CODE_PROMPT = """
            Extract the short alphanumeric sticker code from the top-left area of this card back image. The code consists of a 2-3 letter country/category abbreviation followed by a space and a number (e.g. "ARG 2", "MAR 1", "FWC 9", "GER 2", "USA 16").

            Return ONLY the code string, nothing else. No JSON, no explanation.
        """.trimIndent()
    }
}

class ExtractionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
