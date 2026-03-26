package us.kikinsoft.slabsnap.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "collection_sets")
data class CollectionSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val year: Int,
    val publisher: String,
    val totalStickers: Int,
)
