package us.kikinsoft.slabsnap.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import us.kikinsoft.slabsnap.ui.collection.CollectionListScreen
import us.kikinsoft.slabsnap.ui.scanner.LiveScannerScreen

@Composable
fun SlabSnapNavigation() {
    val backStack = remember { mutableStateListOf<Any>(CollectionListRoute) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<CollectionListRoute> {
                CollectionListScreen(
                    onNavigateToScanner = { backStack.add(LiveScannerRoute) },
                )
            }
            entry<LiveScannerRoute> {
                LiveScannerScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
