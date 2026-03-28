package us.kikinsoft.slabsnap.data.local.dao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.kikinsoft.slabsnap.data.local.SlabSnapDatabase
import us.kikinsoft.slabsnap.data.local.entity.CollectionSetEntity
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity

class StickerDaoTest {

    private lateinit var database: SlabSnapDatabase
    private lateinit var stickerDao: StickerDao
    private lateinit var collectionSetDao: CollectionSetDao

    private val testCollectionSet = CollectionSetEntity(
        name = "FIFA World Cup 2026",
        year = 2026,
        publisher = "Panini",
        totalStickers = 670,
    )

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, SlabSnapDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        stickerDao = database.stickerDao()
        collectionSetDao = database.collectionSetDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    private suspend fun insertCollectionSet(): Long = collectionSetDao.insert(testCollectionSet)

    private fun testSticker(
        collectionSetId: Long,
        stickerCode: String = "ARG 10",
        playerName: String = "Lionel Messi",
    ) = StickerEntity(
        stickerCode = stickerCode,
        playerName = playerName,
        teamName = "Argentina",
        metadata = emptyMap(),
        collectionSetId = collectionSetId,
        isOwned = true,
    )

    @Test
    fun GIVEN_sticker_inserted_WHEN_getAll_THEN_returns_inserted_sticker() = runTest {
        // Given
        val setId = insertCollectionSet()
        stickerDao.insert(testSticker(collectionSetId = setId))

        // When
        val stickers = stickerDao.getAll().first()

        // Then
        assertEquals(1, stickers.size)
        assertEquals("ARG 10", stickers[0].stickerCode)
        assertEquals("Lionel Messi", stickers[0].playerName)
    }

    @Test
    fun GIVEN_sticker_exists_WHEN_getByStickerCode_THEN_returns_matching_sticker() = runTest {
        // Given
        val setId = insertCollectionSet()
        stickerDao.insert(testSticker(collectionSetId = setId, stickerCode = "BRA 9"))

        // When
        val result = stickerDao.getByStickerCode("BRA 9")

        // Then
        assertNotNull(result)
        assertEquals("BRA 9", result!!.stickerCode)
    }

    @Test
    fun GIVEN_empty_database_WHEN_getByStickerCode_THEN_returns_null() = runTest {
        // When
        val result = stickerDao.getByStickerCode("DOES_NOT_EXIST")

        // Then
        assertNull(result)
    }

    @Test
    fun GIVEN_stickers_in_two_sets_WHEN_getByCollectionSet_THEN_returns_only_matching_set() = runTest {
        // Given
        val setId1 = insertCollectionSet()
        val setId2 = collectionSetDao.insert(
            testCollectionSet.copy(name = "Euro 2024", year = 2024),
        )
        stickerDao.insert(testSticker(collectionSetId = setId1, stickerCode = "ARG 10"))
        stickerDao.insert(testSticker(collectionSetId = setId2, stickerCode = "FRA 10"))

        // When
        val stickers = stickerDao.getByCollectionSet(setId1).first()

        // Then
        assertEquals(1, stickers.size)
        assertEquals("ARG 10", stickers[0].stickerCode)
    }

    @Test
    fun GIVEN_sticker_exists_WHEN_deleteById_THEN_sticker_is_removed() = runTest {
        // Given
        val setId = insertCollectionSet()
        val id = stickerDao.insert(testSticker(collectionSetId = setId))

        // When
        stickerDao.deleteById(id)

        // Then
        val stickers = stickerDao.getAll().first()
        assertTrue(stickers.isEmpty())
    }

    @Test
    fun GIVEN_sticker_with_parent_set_WHEN_parent_deleted_THEN_sticker_is_cascade_deleted() = runTest {
        // Given
        val setId = insertCollectionSet()
        stickerDao.insert(testSticker(collectionSetId = setId))

        // When
        database.runInTransaction {
            database.compileStatement("DELETE FROM collection_sets WHERE id = $setId").execute()
        }

        // Then
        val stickers = stickerDao.getAll().first()
        assertTrue(stickers.isEmpty())
    }

    @Test
    fun GIVEN_sticker_with_metadata_WHEN_inserted_and_queried_THEN_metadata_round_trips() = runTest {
        // Given
        val setId = insertCollectionSet()
        val sticker = testSticker(collectionSetId = setId).copy(
            metadata = mapOf("rarity" to "gold", "parallel" to "true"),
        )

        // When
        stickerDao.insert(sticker)

        // Then
        val result = stickerDao.getByStickerCode(sticker.stickerCode)
        assertNotNull(result)
        assertEquals(mapOf("rarity" to "gold", "parallel" to "true"), result!!.metadata)
    }
}
