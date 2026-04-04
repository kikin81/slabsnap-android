package us.kikinsoft.slabsnap.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import us.kikinsoft.slabsnap.data.local.dao.StickerDao
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity

class StickerRepositoryImplTest {

    private val stickerDao: StickerDao = mockk(relaxed = true)
    private val repository = StickerRepositoryImpl(stickerDao)

    private fun baseStickerEntity(
        stickerCode: String = "ARG 10",
        playerName: String = "Messi",
        teamName: String = "Argentina",
    ) = StickerEntity(
        id = 1L,
        stickerCode = stickerCode,
        playerName = playerName,
        teamName = teamName,
        metadata = mapOf("position" to "Forward"),
        collectionSetId = 1L,
        borderColor = "White",
        isOwned = false,
    )

    // region findBaseStickerByText

    @Test
    fun `GIVEN sticker exists matching query WHEN findBaseStickerByText THEN returns matching sticker`() = runTest {
        // Given
        val expected = baseStickerEntity()
        coEvery { stickerDao.findBaseStickerByText(1L, "Messi") } returns expected

        // When
        val result = repository.findBaseStickerByText(1L, "Messi")

        // Then
        assertEquals(expected, result)
        coVerify { stickerDao.findBaseStickerByText(1L, "Messi") }
    }

    @Test
    fun `GIVEN no sticker matches query WHEN findBaseStickerByText THEN returns null`() = runTest {
        // Given
        coEvery { stickerDao.findBaseStickerByText(1L, "Nonexistent") } returns null

        // When
        val result = repository.findBaseStickerByText(1L, "Nonexistent")

        // Then
        assertNull(result)
    }

    // endregion

    // region insertParallelVariant

    @Test
    fun `GIVEN base sticker exists WHEN insertParallelVariant with non-White color THEN inserts new row with color`() =
        runTest {
            // Given
            val baseSticker = baseStickerEntity()
            val insertedSlot = slot<StickerEntity>()
            coEvery { stickerDao.getByStickerCode("ARG 10") } returns baseSticker
            coEvery { stickerDao.insert(capture(insertedSlot)) } returns 2L

            // When
            val id = repository.insertParallelVariant("ARG 10", 1L, "Blue")

            // Then
            assertEquals(2L, id)
            val inserted = insertedSlot.captured
            assertEquals(0L, inserted.id)
            assertEquals("Blue", inserted.borderColor)
            assertTrue(inserted.isOwned)
            assertEquals("ARG 10", inserted.stickerCode)
            assertEquals("Messi", inserted.playerName)
            assertEquals(mapOf("position" to "Forward"), inserted.metadata)
        }

    @Test
    fun `GIVEN base sticker exists WHEN insertParallelVariant with White color THEN marks base as owned`() = runTest {
        // Given
        val baseSticker = baseStickerEntity()
        coEvery { stickerDao.getByStickerCode("ARG 10") } returns baseSticker

        // When
        val id = repository.insertParallelVariant("ARG 10", 1L, "White")

        // Then
        assertEquals(1L, id)
        coVerify {
            stickerDao.update(
                match {
                    it.id == 1L && it.isOwned && it.borderColor == "White"
                },
            )
        }
    }

    @Test
    fun `GIVEN base sticker not found WHEN insertParallelVariant THEN throws IllegalArgumentException`() = runTest {
        // Given
        coEvery { stickerDao.getByStickerCode("INVALID") } returns null

        // When / Then
        val exception = assertThrows<IllegalArgumentException> {
            repository.insertParallelVariant("INVALID", 1L, "Blue")
        }
        assertEquals("Base sticker not found: INVALID", exception.message)
    }

    // endregion
}
