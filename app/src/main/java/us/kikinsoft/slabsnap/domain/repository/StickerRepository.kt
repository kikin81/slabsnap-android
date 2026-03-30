package us.kikinsoft.slabsnap.domain.repository

import kotlinx.coroutines.flow.Flow
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity

interface StickerRepository {
    fun getStickers(): Flow<List<StickerEntity>>

    fun getStickersBySet(collectionSetId: Long): Flow<List<StickerEntity>>

    suspend fun addSticker(sticker: StickerEntity): Long

    suspend fun updateSticker(sticker: StickerEntity)

    suspend fun deleteSticker(sticker: StickerEntity)

    suspend fun deleteStickerById(id: Long)

    suspend fun findBaseStickerByText(
        setId: Long,
        query: String,
    ): StickerEntity?

    suspend fun insertParallelVariant(
        baseStickerCode: String,
        collectionSetId: Long,
        borderColor: String,
    ): Long
}
