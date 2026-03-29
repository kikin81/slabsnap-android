package us.kikinsoft.slabsnap.data.seed

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import us.kikinsoft.slabsnap.data.local.dao.CollectionSetDao
import us.kikinsoft.slabsnap.data.local.dao.StickerDao
import us.kikinsoft.slabsnap.data.local.entity.CollectionSetEntity
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity

@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val collectionSetDao: CollectionSetDao,
    private val stickerDao: StickerDao,
) {
    suspend fun seedIfEmpty() {
        val prefs = context.getSharedPreferences("slabsnap_seed", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return

        val collectionSetId = collectionSetDao.insert(collectionSet())
        stickerDao.insertAll(stickers(collectionSetId))

        prefs.edit().putBoolean("seeded", true).apply()
        Log.d("DatabaseSeeder", "Seeded database with sample stickers")
    }

    private fun collectionSet() = CollectionSetEntity(
        name = "FIFA World Cup 2026",
        year = 2026,
        publisher = "Panini",
        totalStickers = 670,
    )

    private fun stickers(collectionSetId: Long): List<StickerEntity> = listOf(
        StickerEntity(
            stickerCode = "ARG 1",
            playerName = "Emiliano Martínez",
            teamName = "Argentina",
            metadata = mapOf("position" to "GK", "rarity" to "base"),
            collectionSetId = collectionSetId,
            isOwned = true,
        ),
        StickerEntity(
            stickerCode = "ARG 10",
            playerName = "Lionel Messi",
            teamName = "Argentina",
            metadata = mapOf("position" to "FW", "rarity" to "gold", "parallel" to "true"),
            collectionSetId = collectionSetId,
            isOwned = true,
        ),
        StickerEntity(
            stickerCode = "ARG 11",
            playerName = "Ángel Di María",
            teamName = "Argentina",
            metadata = mapOf("position" to "FW", "rarity" to "base"),
            collectionSetId = collectionSetId,
        ),
        StickerEntity(
            stickerCode = "BRA 9",
            playerName = "Richarlison",
            teamName = "Brazil",
            metadata = mapOf("position" to "FW", "rarity" to "base"),
            collectionSetId = collectionSetId,
            isOwned = true,
        ),
        StickerEntity(
            stickerCode = "BRA 10",
            playerName = "Neymar Jr",
            teamName = "Brazil",
            metadata = mapOf("position" to "FW", "rarity" to "silver", "parallel" to "true"),
            collectionSetId = collectionSetId,
        ),
        StickerEntity(
            stickerCode = "BRA 7",
            playerName = "Vinícius Jr",
            teamName = "Brazil",
            metadata = mapOf("position" to "FW", "rarity" to "gold"),
            collectionSetId = collectionSetId,
            isOwned = true,
        ),
        StickerEntity(
            stickerCode = "FRA 7",
            playerName = "Antoine Griezmann",
            teamName = "France",
            metadata = mapOf("position" to "FW", "rarity" to "base"),
            collectionSetId = collectionSetId,
        ),
        StickerEntity(
            stickerCode = "FRA 10",
            playerName = "Kylian Mbappé",
            teamName = "France",
            metadata = mapOf("position" to "FW", "rarity" to "gold", "parallel" to "true"),
            collectionSetId = collectionSetId,
            isOwned = true,
        ),
        StickerEntity(
            stickerCode = "FRA 4",
            playerName = "Raphaël Varane",
            teamName = "France",
            metadata = mapOf("position" to "DF", "rarity" to "base"),
            collectionSetId = collectionSetId,
        ),
        StickerEntity(
            stickerCode = "GER 1",
            playerName = "Manuel Neuer",
            teamName = "Germany",
            metadata = mapOf("position" to "GK", "rarity" to "silver"),
            collectionSetId = collectionSetId,
            isOwned = true,
        ),
        StickerEntity(
            stickerCode = "GER 8",
            playerName = "Toni Kroos",
            teamName = "Germany",
            metadata = mapOf("position" to "MF", "rarity" to "base"),
            collectionSetId = collectionSetId,
        ),
        StickerEntity(
            stickerCode = "GER 10",
            playerName = "Jamal Musiala",
            teamName = "Germany",
            metadata = mapOf("position" to "MF", "rarity" to "gold"),
            collectionSetId = collectionSetId,
            isOwned = true,
        ),
    )
}
