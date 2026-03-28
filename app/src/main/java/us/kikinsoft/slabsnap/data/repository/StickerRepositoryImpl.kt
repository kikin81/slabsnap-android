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

    override suspend fun addSticker(sticker: StickerEntity): Long = stickerDao.insert(sticker)

    override suspend fun updateSticker(sticker: StickerEntity) = stickerDao.update(sticker)

    override suspend fun deleteSticker(sticker: StickerEntity) = stickerDao.delete(sticker)
}
