package us.kikinsoft.slabsnap.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import us.kikinsoft.slabsnap.data.local.dao.StickerDao
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity
import us.kikinsoft.slabsnap.domain.repository.StickerRepository

class StickerRepositoryImpl @Inject constructor(private val stickerDao: StickerDao) : StickerRepository {
    override fun getStickers(): Flow<List<StickerEntity>> = stickerDao.getAll()

    override fun getStickersBySet(collectionSetId: Long): Flow<List<StickerEntity>> =
        stickerDao.getByCollectionSet(collectionSetId)

    override fun countUniqueOwned(collectionSetId: Long): Flow<Int> = stickerDao.countUniqueOwned(collectionSetId)

    override suspend fun addSticker(sticker: StickerEntity): Long = stickerDao.insert(sticker)

    override suspend fun updateSticker(sticker: StickerEntity) = stickerDao.update(sticker)

    override suspend fun deleteSticker(sticker: StickerEntity) = stickerDao.delete(sticker)

    override suspend fun deleteStickerById(id: Long) = stickerDao.deleteById(id)

    override suspend fun findBaseStickerByText(
        setId: Long,
        query: String,
    ): StickerEntity? = stickerDao.findBaseStickerByText(setId, query)

    override suspend fun insertParallelVariant(
        baseStickerCode: String,
        collectionSetId: Long,
        borderColor: String,
    ): Long {
        val baseSticker = stickerDao.getByStickerCode(baseStickerCode)
            ?: throw IllegalArgumentException("Base sticker not found: $baseStickerCode")

        return if (borderColor == "White") {
            stickerDao.update(baseSticker.copy(isOwned = true, updatedAt = System.currentTimeMillis()))
            baseSticker.id
        } else {
            stickerDao.insert(
                baseSticker.copy(
                    id = 0,
                    borderColor = borderColor,
                    isOwned = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        }
    }
}
