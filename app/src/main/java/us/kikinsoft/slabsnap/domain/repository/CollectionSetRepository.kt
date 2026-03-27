package us.kikinsoft.slabsnap.domain.repository

import kotlinx.coroutines.flow.Flow
import us.kikinsoft.slabsnap.data.local.entity.CollectionSetEntity

interface CollectionSetRepository {
    fun getCollectionSets(): Flow<List<CollectionSetEntity>>

    suspend fun getCollectionSetById(id: Long): CollectionSetEntity?

    suspend fun addCollectionSet(collectionSet: CollectionSetEntity): Long
}
