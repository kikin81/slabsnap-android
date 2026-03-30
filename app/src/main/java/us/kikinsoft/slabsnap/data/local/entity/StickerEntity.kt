package us.kikinsoft.slabsnap.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stickers",
    indices = [
        Index(
            value = ["collectionSetId", "stickerCode", "borderColor"],
            unique = true,
        ),
    ],
    foreignKeys = [
        ForeignKey(
            entity = CollectionSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["collectionSetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StickerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stickerCode: String,
    val playerName: String,
    val teamName: String,
    val metadata: Map<String, String>,
    val collectionSetId: Long,
    val borderColor: String = "White",
    val isOwned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
