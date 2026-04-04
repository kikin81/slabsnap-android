package us.kikinsoft.slabsnap.data.seed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Validates format invariants of the seed data in qatarset.json.
 *
 * These tests catch breaking changes to the seed data that would silently
 * break the scanner's extraction → DB lookup chain without failing any
 * mocked unit test.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SeedDataContractTest {

    @Serializable
    private data class SeedSticker(
        @SerialName("stickerCode") val stickerCode: String,
        @SerialName("playerName") val playerName: String,
        @SerialName("teamName") val teamName: String,
        @SerialName("type") val type: String,
    )

    private lateinit var stickers: List<SeedSticker>

    @BeforeAll
    fun loadSeedData() {
        val json = this::class.java.classLoader!!
            .getResourceAsStream("qatarset.json")!!
            .bufferedReader()
            .use { it.readText() }
        stickers = Json.decodeFromString<List<SeedSticker>>(json)
    }

    @Test
    fun `GIVEN seed data THEN total count is 670 base stickers`() {
        assertEquals(670, stickers.size)
    }

    @Test
    fun `GIVEN seed data THEN no stickerCode contains spaces`() {
        // The back-code extraction normalizes spaces, but if the seed data
        // itself has spaces, getByStickerCode exact match will never work.
        val withSpaces = stickers.filter { " " in it.stickerCode }
        assertTrue(withSpaces.isEmpty()) {
            "Sticker codes must not contain spaces. Found: ${withSpaces.map { it.stickerCode }}"
        }
    }

    @Test
    fun `GIVEN seed data THEN every stickerCode matches expected pattern`() {
        // Valid patterns: "00", "FWC1"-"FWC29", "QAT1"-"QAT20", etc.
        val pattern = Regex("^(\\d{1,2}|[A-Z]{2,3}\\d{1,2})$")
        val mismatches = stickers.filter { !pattern.matches(it.stickerCode) }
        assertTrue(mismatches.isEmpty()) {
            "Sticker codes must match pattern [A-Z]{2,3}\\d{1,2} or \\d{1,2}. " +
                "Mismatches: ${mismatches.map { it.stickerCode }}"
        }
    }

    @Test
    fun `GIVEN seed data THEN no duplicate stickerCodes exist`() {
        val duplicates = stickers.groupBy { it.stickerCode }
            .filter { it.value.size > 1 }
            .keys
        assertTrue(duplicates.isEmpty()) {
            "Duplicate sticker codes found: $duplicates"
        }
    }

    @Test
    fun `GIVEN seed data THEN every sticker has a non-blank playerName`() {
        val blanks = stickers.filter { it.playerName.isBlank() }
        assertTrue(blanks.isEmpty()) {
            "Stickers with blank playerName: ${blanks.map { it.stickerCode }}"
        }
    }

    @Test
    fun `GIVEN seed data THEN every sticker has a non-blank teamName`() {
        val blanks = stickers.filter { it.teamName.isBlank() }
        assertTrue(blanks.isEmpty()) {
            "Stickers with blank teamName: ${blanks.map { it.stickerCode }}"
        }
    }

    @Test
    fun `GIVEN seed data THEN type is only foil or dash`() {
        val invalid = stickers.filter { it.type != "foil" && it.type != "-" }
        assertTrue(invalid.isEmpty()) {
            "Invalid types found (only 'foil' and '-' allowed): " +
                "${invalid.map { "${it.stickerCode}=${it.type}" }}"
        }
    }

    @Test
    fun `GIVEN seed data THEN 32 country teams have exactly 20 stickers each`() {
        val countryTeams = setOf(
            "Qatar", "Ecuador", "Senegal", "Netherlands", "England", "Iran",
            "USA", "Wales", "Argentina", "Saudi Arabia", "Mexico", "Poland",
            "France", "Australia", "Denmark", "Tunisia", "Spain", "Costa Rica",
            "Germany", "Japan", "Belgium", "Canada", "Morocco", "Croatia",
            "Brazil", "Serbia", "Switzerland", "Cameroon", "Portugal", "Ghana",
            "Uruguay", "Korea Republic",
        )
        countryTeams.forEach { team ->
            val count = stickers.count { it.teamName == team }
            assertEquals(20, count) {
                "$team has $count stickers, expected 20"
            }
        }
    }

    @Test
    fun `GIVEN seed data THEN every country team has exactly 1 foil Team Logo`() {
        val countryTeams = stickers
            .filter { it.playerName == "Team Logo" }
            .map { it.teamName }

        assertEquals(32, countryTeams.size)
        countryTeams.forEach { team ->
            val logo = stickers.first { it.teamName == team && it.playerName == "Team Logo" }
            assertEquals("foil", logo.type) {
                "$team Team Logo should be foil but was '${logo.type}'"
            }
        }
    }

    @Test
    fun `GIVEN seed data THEN well-known players are findable by name`() {
        // These are the exact playerName values the front-scan LIKE query must match.
        // If someone normalizes names (e.g. removes accents), these tests catch it.
        val expectedPlayers = mapOf(
            "ARG20" to "Lionel Messi",
            "BRA17" to "Neymar Jr",
            "FRA19" to "Kylian Mbappé",
            "POR18" to "Cristiano Ronaldo",
            "ENG18" to "Harry Kane",
        )
        expectedPlayers.forEach { (code, name) ->
            val sticker = stickers.find { it.stickerCode == code }
            assertEquals(name, sticker?.playerName) {
                "Expected $code to have playerName '$name' but was '${sticker?.playerName}'"
            }
        }
    }
}
