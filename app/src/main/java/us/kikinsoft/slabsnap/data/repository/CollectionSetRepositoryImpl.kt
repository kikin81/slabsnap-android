package us.kikinsoft.slabsnap.data.repository

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import us.kikinsoft.slabsnap.data.local.dao.CollectionSetDao
import us.kikinsoft.slabsnap.data.local.entity.CollectionSetEntity
import us.kikinsoft.slabsnap.domain.repository.CollectionSetRepository

class CollectionSetRepositoryImpl @Inject constructor(private val collectionSetDao: CollectionSetDao) :
    CollectionSetRepository {
    override fun getCollectionSets(): Flow<List<CollectionSetEntity>> = collectionSetDao.getAll()

    override suspend fun getCollectionSetById(id: Long): CollectionSetEntity? = collectionSetDao.getById(id)

    override suspend fun addCollectionSet(collectionSet: CollectionSetEntity): Long =
        collectionSetDao.insert(collectionSet)
}
