package us.kikinsoft.slabsnap.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import us.kikinsoft.slabsnap.data.local.converter.Converters
import us.kikinsoft.slabsnap.data.local.dao.CollectionSetDao
import us.kikinsoft.slabsnap.data.local.dao.StickerDao
import us.kikinsoft.slabsnap.data.local.entity.CollectionSetEntity
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity

@Database(
    entities = [StickerEntity::class, CollectionSetEntity::class],
    version = 1,
)
@TypeConverters(Converters::class)
abstract class SlabSnapDatabase : RoomDatabase() {
    abstract fun stickerDao(): StickerDao

    abstract fun collectionSetDao(): CollectionSetDao
}
