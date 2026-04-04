package us.kikinsoft.slabsnap.data.local.dao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import us.kikinsoft.slabsnap.data.local.SlabSnapDatabase
import us.kikinsoft.slabsnap.data.local.entity.CollectionSetEntity
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity

/**
 * Integration tests that seed the full qatarset.json into an in-memory Room DB
 * and verify the exact queries the scanner extraction chain uses.
 *
 * If the seed data format or DAO queries change in a way that breaks the
 * scanner→DB lookup, these tests will catch it.
 */
class StickerLookupIntegrationTest {

    @Serializable
    private data class SeedSticker(
        @SerialName("stickerCode") val stickerCode: String,
        @SerialName("playerName") val playerName: String,
        @SerialName("teamName") val teamName: String,
        @SerialName("type") val type: String,
    )

    private lateinit var database: SlabSnapDatabase
    private lateinit var stickerDao: StickerDao
    private lateinit var collectionSetDao: CollectionSetDao
    private var collectionSetId: Long = 0

    @Before
    fun setup() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, SlabSnapDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        stickerDao = database.stickerDao()
        collectionSetDao = database.collectionSetDao()

        // Seed the full dataset exactly as DatabaseSeeder does
        collectionSetId = collectionSetDao.insert(
            CollectionSetEntity(
                name = "FIFA World Cup Qatar 2022",
                year = 2022,
                publisher = "Panini",
                totalStickers = 670,
            ),
        )

        val json = context.assets.open("qatarset.json").bufferedReader().use { it.readText() }
        val seedStickers = Json.decodeFromString<List<SeedSticker>>(json)

        val entities = seedStickers.map { seed ->
            StickerEntity(
                stickerCode = seed.stickerCode,
                playerName = seed.playerName,
                teamName = seed.teamName,
                metadata = if (seed.type == "foil") mapOf("is_foil" to "true") else emptyMap(),
                collectionSetId = collectionSetId,
                borderColor = "White",
                isOwned = false,
            )
        }
        stickerDao.insertAll(entities)
    }

    @After
    fun teardown() {
        database.close()
    }

    // region Back-code exact lookup (getByStickerCode)

    @Test
    fun GIVEN_seeded_data_WHEN_getByStickerCode_with_no_spaces_THEN_returns_match() = runTest {
        val result = stickerDao.getByStickerCode("ARG20")
        assertNotNull(result)
        assertEquals("Lionel Messi", result!!.playerName)
    }

    @Test
    fun GIVEN_seeded_data_WHEN_getByStickerCode_with_spaces_THEN_returns_null() = runTest {
        // This verifies that space-containing codes DON'T match — the normalizer must strip them
        val result = stickerDao.getByStickerCode("ARG 20")
        assertNull(result)
    }

    @Test
    fun GIVEN_seeded_data_WHEN_getByStickerCode_for_special_cards_THEN_returns_match() = runTest {
        val stadium = stickerDao.getByStickerCode("FWC8")
        assertNotNull(stadium)
        assertEquals("Ahmad Bin Ali Stadium", stadium!!.playerName)

        val zero = stickerDao.getByStickerCode("00")
        assertNotNull(zero)
        assertEquals("Panini", zero!!.playerName)
    }

    // endregion

    // region Front-scan text lookup (findBaseStickerByText)

    @Test
    fun GIVEN_seeded_data_WHEN_findBaseStickerByText_with_player_name_THEN_returns_match() = runTest {
        val result = stickerDao.findBaseStickerByText(collectionSetId, "Lionel Messi")
        assertNotNull(result)
        assertEquals("ARG20", result!!.stickerCode)
    }

    @Test
    fun GIVEN_seeded_data_WHEN_findBaseStickerByText_with_partial_name_THEN_returns_match() = runTest {
        // Gemini might extract just "Messi" instead of "Lionel Messi"
        val result = stickerDao.findBaseStickerByText(collectionSetId, "Messi")
        assertNotNull(result)
        assertEquals("ARG20", result!!.stickerCode)
    }

    @Test
    fun GIVEN_seeded_data_WHEN_findBaseStickerByText_with_team_name_THEN_returns_a_team_sticker() = runTest {
        // Gemini might read badge text "Argentina" from a crest
        val result = stickerDao.findBaseStickerByText(collectionSetId, "Argentina")
        assertNotNull(result)
        assertEquals("Argentina", result!!.teamName)
    }

    @Test
    fun GIVEN_owned_parallel_exists_WHEN_findBaseStickerByText_THEN_returns_base_white_only() = runTest {
        // Insert a Blue parallel of Messi
        val base = stickerDao.getByStickerCode("ARG20")!!
        stickerDao.insert(
            base.copy(id = 0, borderColor = "Blue", isOwned = true),
        )

        // The LIKE query must still return the White base, not the Blue parallel
        val result = stickerDao.findBaseStickerByText(collectionSetId, "Lionel Messi")
        assertNotNull(result)
        assertEquals("White", result!!.borderColor)
    }

    // endregion

    // region Progress counter (countUniqueOwned)

    @Test
    fun GIVEN_no_stickers_owned_WHEN_countUniqueOwned_THEN_returns_zero() = runTest {
        val count = stickerDao.countUniqueOwned(collectionSetId).first()
        assertEquals(0, count)
    }

    @Test
    fun GIVEN_one_sticker_owned_WHEN_countUniqueOwned_THEN_returns_one() = runTest {
        val messi = stickerDao.getByStickerCode("ARG20")!!
        stickerDao.update(messi.copy(isOwned = true))

        val count = stickerDao.countUniqueOwned(collectionSetId).first()
        assertEquals(1, count)
    }

    @Test
    fun GIVEN_same_sticker_owned_in_two_colors_WHEN_countUniqueOwned_THEN_counts_as_one() = runTest {
        // Own the White base
        val messi = stickerDao.getByStickerCode("ARG20")!!
        stickerDao.update(messi.copy(isOwned = true))

        // Also own a Blue parallel
        stickerDao.insert(
            messi.copy(id = 0, borderColor = "Blue", isOwned = true),
        )

        val count = stickerDao.countUniqueOwned(collectionSetId).first()
        assertEquals(1, count)
    }

    // endregion

    // region Full dataset sanity

    @Test
    fun GIVEN_seeded_data_WHEN_getByCollectionSet_THEN_returns_670_stickers() = runTest {
        val all = stickerDao.getByCollectionSet(collectionSetId).first()
        assertEquals(670, all.size)
    }

    // endregion
}
