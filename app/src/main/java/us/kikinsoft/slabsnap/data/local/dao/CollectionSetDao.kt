package us.kikinsoft.slabsnap.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import us.kikinsoft.slabsnap.data.local.entity.CollectionSetEntity

@Dao
interface CollectionSetDao {
    @Query("SELECT * FROM collection_sets")
    fun getAll(): Flow<List<CollectionSetEntity>>

    @Query("SELECT * FROM collection_sets WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CollectionSetEntity?

    @Query("SELECT COUNT(*) FROM collection_sets")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(collectionSet: CollectionSetEntity): Long
}
