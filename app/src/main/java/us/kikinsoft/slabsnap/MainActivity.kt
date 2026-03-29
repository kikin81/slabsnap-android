package us.kikinsoft.slabsnap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import dagger.hilt.android.AndroidEntryPoint
import us.kikinsoft.slabsnap.navigation.SlabSnapNavigation
import us.kikinsoft.slabsnap.ui.theme.SlabSnapTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAnalytics = Firebase.analytics
        enableEdgeToEdge()
        setContent {
            SlabSnapTheme {
                SlabSnapNavigation()
            }
        }
    }
}
