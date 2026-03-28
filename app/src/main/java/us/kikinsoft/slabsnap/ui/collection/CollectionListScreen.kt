package us.kikinsoft.slabsnap.ui.collection

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import us.kikinsoft.slabsnap.R
import us.kikinsoft.slabsnap.ui.theme.SlabSnapTheme

@Composable
fun CollectionListScreen(
    onNavigateToScanner: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CollectionListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnNavigateToScanner by rememberUpdatedState(onNavigateToScanner)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is CollectionListEffect.NavigateToScanner -> currentOnNavigateToScanner()
                is CollectionListEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    CollectionListContent(
        state = state,
        onScanClick = onNavigateToScanner,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun CollectionListContent(
    state: CollectionListState,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onScanClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.collection_scan_sticker),
                )
            }
        },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.isEmpty -> {
                EmptyState(modifier = Modifier.padding(innerPadding))
            }
            else -> {
                StickerGrid(
                    stickers = state.stickers,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.collection_empty_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.collection_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun StickerGrid(
    stickers: ImmutableList<StickerUiModel>,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(
            items = stickers,
            key = { it.id },
        ) { sticker ->
            StickerCard(sticker = sticker)
        }
    }
}

@Preview(showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectionListEmptyPreview() {
    SlabSnapTheme {
        CollectionListContent(
            state = CollectionListState(isLoading = false),
            onScanClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionListPopulatedPreview() {
    SlabSnapTheme {
        CollectionListContent(
            state = CollectionListState(
                stickers = persistentListOf(
                    StickerUiModel(
                        id = 1L,
                        stickerCode = "ARG 10",
                        playerName = "Lionel Messi",
                        teamName = "Argentina",
                    ),
                    StickerUiModel(
                        id = 2L,
                        stickerCode = "POR 7",
                        playerName = "Cristiano Ronaldo",
                        teamName = "Portugal",
                    ),
                    StickerUiModel(
                        id = 3L,
                        stickerCode = "FRA 10",
                        playerName = "Kylian Mbappe",
                        teamName = "France",
                    ),
                ),
                isLoading = false,
            ),
            onScanClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionListLoadingPreview() {
    SlabSnapTheme {
        CollectionListContent(
            state = CollectionListState(),
            onScanClick = {},
        )
    }
}
