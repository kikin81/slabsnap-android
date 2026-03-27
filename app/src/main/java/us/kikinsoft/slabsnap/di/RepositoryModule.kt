package us.kikinsoft.slabsnap.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import us.kikinsoft.slabsnap.data.repository.CollectionSetRepositoryImpl
import us.kikinsoft.slabsnap.data.repository.StickerRepositoryImpl
import us.kikinsoft.slabsnap.domain.repository.CollectionSetRepository
import us.kikinsoft.slabsnap.domain.repository.StickerRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindStickerRepository(impl: StickerRepositoryImpl): StickerRepository

    @Binds
    abstract fun bindCollectionSetRepository(impl: CollectionSetRepositoryImpl): CollectionSetRepository
}
