package us.kikinsoft.slabsnap.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import us.kikinsoft.slabsnap.data.local.SlabSnapDatabase
import us.kikinsoft.slabsnap.data.local.dao.CollectionSetDao
import us.kikinsoft.slabsnap.data.local.dao.StickerDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SlabSnapDatabase =
        Room.databaseBuilder(context, SlabSnapDatabase::class.java, "slabsnap.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideStickerDao(database: SlabSnapDatabase): StickerDao = database.stickerDao()

    @Provides
    fun provideCollectionSetDao(database: SlabSnapDatabase): CollectionSetDao = database.collectionSetDao()
}
