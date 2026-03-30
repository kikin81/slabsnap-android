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
     * Sends the captured card [bitmap] to Gemini Nano and returns structured [ExtractedCardData].
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

    companion object {
        private val EXTRACTION_PROMPT = """
            You are an automated, highly precise optical data extraction system for Panini soccer sticker cards.

            Extract the following fields from the card image:
            - player_name: The full name of the athlete printed on the card.
            - team_country: The national team or club name represented on the card.
            - sticker_number: The alphanumeric serial code (e.g. "ENG 14", "FRA 02", "23").
            - is_foil: true if the card has holographic, shiny, or foil elements; false otherwise.

            Rules:
            - Do not output any text outside of the JSON object.
            - If a value is obscured, illegible, or not present, output the exact string "UNKNOWN".
            - The sticker serial number is typically in a corner or along the bottom margin.
            - The team name is usually accompanied by a national flag or crest.

            Respond with only a JSON object containing exactly these four keys:
            {"player_name": "", "team_country": "", "sticker_number": "", "is_foil": false}
        """.trimIndent()
    }
}

class ExtractionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
