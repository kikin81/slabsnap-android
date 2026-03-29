package us.kikinsoft.slabsnap.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import us.kikinsoft.slabsnap.ui.collection.CollectionListScreen
import us.kikinsoft.slabsnap.ui.scanner.LiveScannerScreen

@Composable
fun SlabSnapNavigation() {
    val backStack = rememberNavBackStack(CollectionListRoute)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
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
