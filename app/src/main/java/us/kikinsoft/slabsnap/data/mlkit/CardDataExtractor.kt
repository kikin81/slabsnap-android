package us.kikinsoft.slabsnap.data.mlkit

import android.graphics.Bitmap
import com.google.mlkit.genai.common.DownloadStatus
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
 * Extracts structured card data from captured [Bitmap]s using the on-device Gemini Nano
 * model via the ML Kit GenAI Prompt API.
 *
 * The scanning pipeline is two-step:
 * 1. [extractFront] — extracts border color and foil status from the card front.
 * 2. [extractBackCode] — extracts the alphanumeric sticker code from the card back.
 */
@Singleton
class CardDataExtractor @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    private val generativeModel: GenerativeModel by lazy {
        Generation.getClient()
    }

    suspend fun checkAvailability(): Int = generativeModel.checkStatus()

    suspend fun downloadModel() {
        val terminal = generativeModel.download()
            .filter { it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed }
            .first()

        if (terminal is DownloadStatus.DownloadFailed) {
            throw ExtractionException("Model download failed")
        }
    }

    /**
     * Extracts border color and foil status from the FRONT of a sticker card.
     */
    suspend fun extractFront(bitmap: Bitmap): ExtractedCardData {
        val request = generateContentRequest(
            ImagePart(bitmap),
            TextPart(FRONT_PROMPT),
        ) {
            temperature = 0.0f
            topK = 10
            maxOutputTokens = 64
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
     * Extracts the alphanumeric sticker code from the BACK of a card (e.g. "ARG20", "FWC9").
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

        val rawCode = response.candidates.firstOrNull()?.text?.trim()
            ?: throw ExtractionException("Model returned empty response for back code")

        return rawCode.replace(" ", "")
    }

    companion object {
        private val FRONT_PROMPT = """
            You are looking at the FRONT side of a Panini soccer sticker card. Extract only these two visual attributes:

            - border_color: The color of the decorative border framing the sticker image. Common values: White, Blue, Red, Purple, Green, Black. Default to "White" if unsure.
            - is_foil: true if the card has holographic, shiny, or foil elements; false otherwise.

            Respond with only a JSON object:
            {"border_color": "White", "is_foil": false}
        """.trimIndent()

        private val BACK_CODE_PROMPT = """
            Extract the short alphanumeric sticker code from the top-left area of this card back image. The code consists of a 2-3 letter country/category abbreviation immediately followed by a number, with no space (e.g. "ARG2", "MAR1", "FWC9", "GER2", "USA16").

            Return ONLY the code string, nothing else. No JSON, no explanation.
        """.trimIndent()
    }
}

class ExtractionException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
