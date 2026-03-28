package us.kikinsoft.slabsnap.di

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import us.kikinsoft.slabsnap.domain.repository.CollectionSetRepository
import us.kikinsoft.slabsnap.domain.repository.StickerRepository

@HiltAndroidTest
class HiltDiGraphTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var stickerRepository: StickerRepository

    @Inject
    lateinit var collectionSetRepository: CollectionSetRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun GIVEN_hilt_graph_WHEN_inject_called_THEN_sticker_repository_is_provided() {
        assertNotNull(stickerRepository)
    }

    @Test
    fun GIVEN_hilt_graph_WHEN_inject_called_THEN_collection_set_repository_is_provided() {
        assertNotNull(collectionSetRepository)
    }
}
