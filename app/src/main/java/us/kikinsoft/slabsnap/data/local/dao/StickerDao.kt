package us.kikinsoft.slabsnap.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity

@Dao
interface StickerDao {
    @Query("SELECT * FROM stickers")
    fun getAll(): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers WHERE collectionSetId = :collectionSetId")
    fun getByCollectionSet(collectionSetId: Long): Flow<List<StickerEntity>>

    @Query("SELECT * FROM stickers WHERE stickerCode = :stickerCode LIMIT 1")
    suspend fun getByStickerCode(stickerCode: String): StickerEntity?

    @Query(
        """
        SELECT * FROM stickers
        WHERE collectionSetId = :setId
        AND borderColor = 'White'
        AND (playerName LIKE '%' || :query || '%' OR teamName LIKE '%' || :query || '%')
        LIMIT 1
        """,
    )
    suspend fun findBaseStickerByText(
        setId: Long,
        query: String,
    ): StickerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sticker: StickerEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stickers: List<StickerEntity>)

    @Update
    suspend fun update(sticker: StickerEntity)

    @Delete
    suspend fun delete(sticker: StickerEntity)

    @Query("DELETE FROM stickers WHERE id = :id")
    suspend fun deleteById(id: Long)
}
