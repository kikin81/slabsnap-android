package us.kikinsoft.slabsnap.ui.collection

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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

private val quickAddColors = listOf("White", "Blue", "Red", "Purple", "Green", "Black")

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
        onSelectFilter = { viewModel.handleEvent(CollectionListEvent.SetFilter(it)) },
        onClickQuickAdd = { viewModel.handleEvent(CollectionListEvent.OnQuickAddClicked(it)) },
        onDismissSheet = { viewModel.handleEvent(CollectionListEvent.DismissQuickAdd) },
        onSelectColor = { viewModel.handleEvent(CollectionListEvent.OnColorSelected(it)) },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionListContent(
    state: CollectionListState,
    onScanClick: () -> Unit,
    onSelectFilter: (FilterMode) -> Unit,
    onClickQuickAdd: (StickerUiModel) -> Unit,
    onDismissSheet: () -> Unit,
    onSelectColor: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onScanClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.collection_scan_sticker))
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
            else -> {
                val filteredStickers = filterStickers(state.stickers, state.filter)
                StickerGrid(
                    stickers = filteredStickers,
                    state = state,
                    onSelectFilter = onSelectFilter,
                    onClickQuickAdd = onClickQuickAdd,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }

        if (state.selectedStickerForAdd != null) {
            ColorPickerBottomSheet(
                stickerCode = state.selectedStickerForAdd.stickerCode,
                onSelectColor = onSelectColor,
                onDismiss = onDismissSheet,
            )
        }
    }
}

@Composable
private fun StickerGrid(
    stickers: List<StickerUiModel>,
    state: CollectionListState,
    onSelectFilter: (FilterMode) -> Unit,
    onClickQuickAdd: (StickerUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            ProgressHeader(
                ownedCount = state.uniqueOwnedCount,
                totalCount = state.totalInSet,
            )
        }
        item(span = { GridItemSpan(2) }) {
            FilterRow(
                selectedFilter = state.filter,
                onSelectFilter = onSelectFilter,
            )
        }
        items(
            items = stickers,
            key = { it.id },
        ) { sticker ->
            StickerGridItem(
                sticker = sticker,
                onQuickAddClick = { onClickQuickAdd(sticker) },
            )
        }
    }
}

@Composable
private fun ProgressHeader(
    ownedCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = stringResource(R.string.collection_progress, ownedCount, totalCount),
                style = MaterialTheme.typography.headlineMedium,
            )
            if (totalCount > 0) {
                Text(
                    text = "${(ownedCount * 100 / totalCount)}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { if (totalCount > 0) ownedCount.toFloat() / totalCount else 0f },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FilterRow(
    selectedFilter: FilterMode,
    onSelectFilter: (FilterMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedFilter == FilterMode.ALL,
            onClick = { onSelectFilter(FilterMode.ALL) },
            label = { Text(stringResource(R.string.collection_filter_all)) },
        )
        FilterChip(
            selected = selectedFilter == FilterMode.MISSING,
            onClick = { onSelectFilter(FilterMode.MISSING) },
            label = { Text(stringResource(R.string.collection_filter_missing)) },
        )
        FilterChip(
            selected = selectedFilter == FilterMode.PARALLELS,
            onClick = { onSelectFilter(FilterMode.PARALLELS) },
            label = { Text(stringResource(R.string.collection_filter_parallels)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerBottomSheet(
    stickerCode: String,
    onSelectColor: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Text(
                text = "$stickerCode — ${stringResource(R.string.collection_pick_color)}",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(16.dp))
            quickAddColors.forEach { color ->
                TextButton(
                    onClick = { onSelectColor(color) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = color,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

private fun filterStickers(
    stickers: ImmutableList<StickerUiModel>,
    filter: FilterMode,
): List<StickerUiModel> = when (filter) {
    FilterMode.ALL -> stickers
    FilterMode.MISSING -> stickers.filter { !it.isOwned }
    FilterMode.PARALLELS -> stickers.filter { it.isOwned && it.borderColor != "White" }
}

@Preview(showBackground = true)
@Preview(name = "Dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectionListEmptyPreview() {
    SlabSnapTheme {
        CollectionListContent(
            state = CollectionListState(
                stickers = persistentListOf(
                    StickerUiModel(1L, "ARG 20", "Lionel Messi", "Argentina"),
                    StickerUiModel(2L, "FRA 19", "Kylian Mbappé", "France"),
                    StickerUiModel(3L, "BRA 17", "Neymar Jr", "Brazil"),
                ),
                isLoading = false,
                uniqueOwnedCount = 0,
                totalInSet = 670,
            ),
            onScanClick = {},
            onSelectFilter = {},
            onClickQuickAdd = {},
            onDismissSheet = {},
            onSelectColor = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionListWithOwnedPreview() {
    SlabSnapTheme {
        CollectionListContent(
            state = CollectionListState(
                stickers = persistentListOf(
                    StickerUiModel(
                        1L,
                        "ARG 20",
                        "Lionel Messi",
                        "Argentina",
                        isOwned = true,
                        borderColor = "Blue",
                    ),
                    StickerUiModel(2L, "FRA 19", "Kylian Mbappé", "France"),
                    StickerUiModel(
                        3L,
                        "BRA 17",
                        "Neymar Jr",
                        "Brazil",
                        isOwned = true,
                        borderColor = "White",
                    ),
                    StickerUiModel(4L, "GER 15", "Joshua Kimmich", "Germany"),
                ),
                isLoading = false,
                uniqueOwnedCount = 15,
                totalInSet = 670,
            ),
            onScanClick = {},
            onSelectFilter = {},
            onClickQuickAdd = {},
            onDismissSheet = {},
            onSelectColor = {},
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
            onSelectFilter = {},
            onClickQuickAdd = {},
            onDismissSheet = {},
            onSelectColor = {},
        )
    }
}
