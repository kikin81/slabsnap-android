package us.kikinsoft.slabsnap

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import us.kikinsoft.slabsnap.data.seed.DatabaseSeeder

@HiltAndroidApp
class SlabSnapApplication : Application() {

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            CoroutineScope(Dispatchers.IO).launch {
                databaseSeeder.seedIfEmpty()
            }
        }
    }
}
