package us.kikinsoft.slabsnap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import us.kikinsoft.slabsnap.navigation.SlabSnapNavigation
import us.kikinsoft.slabsnap.ui.theme.SlabSnapTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SlabSnapTheme {
                SlabSnapNavigation()
            }
        }
    }
}
