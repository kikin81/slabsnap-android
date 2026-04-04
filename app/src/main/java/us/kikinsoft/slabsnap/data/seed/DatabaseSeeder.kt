package us.kikinsoft.slabsnap.data.seed

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import us.kikinsoft.slabsnap.data.local.dao.CollectionSetDao
import us.kikinsoft.slabsnap.data.local.dao.StickerDao
import us.kikinsoft.slabsnap.data.local.entity.CollectionSetEntity
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity

@Serializable
private data class SeedSticker(
    @SerialName("stickerCode") val stickerCode: String,
    @SerialName("playerName") val playerName: String,
    @SerialName("teamName") val teamName: String,
    @SerialName("type") val type: String,
)

@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val collectionSetDao: CollectionSetDao,
    private val stickerDao: StickerDao,
) {
    suspend fun seedIfEmpty() {
        if (collectionSetDao.count() > 0) return

        val collectionSetId = collectionSetDao.insert(
            CollectionSetEntity(
                name = "FIFA World Cup Qatar 2022",
                year = 2022,
                publisher = "Panini",
                totalStickers = 670,
            ),
        )

        val json = context.assets.open("qatarset.json").bufferedReader().use { it.readText() }
        val seedStickers = Json.decodeFromString<List<SeedSticker>>(json)

        val entities = seedStickers.map { seed ->
            StickerEntity(
                stickerCode = seed.stickerCode,
                playerName = seed.playerName,
                teamName = seed.teamName,
                metadata = if (seed.type == "foil") mapOf("is_foil" to "true") else emptyMap(),
                collectionSetId = collectionSetId,
                borderColor = "White",
                isOwned = false,
            )
        }

        stickerDao.insertAll(entities)
        Log.d("DatabaseSeeder", "Seeded ${entities.size} stickers for Qatar 2022")
    }
}
