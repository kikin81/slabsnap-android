package us.kikinsoft.slabsnap.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import us.kikinsoft.slabsnap.R
import us.kikinsoft.slabsnap.data.local.entity.StickerEntity
import us.kikinsoft.slabsnap.ui.theme.SlabSnapTheme

@Composable
fun LiveScannerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LiveScannerViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onNavigateBack)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        viewModel.handleEvent(LiveScannerEvent.OnPermissionResult(isGranted))
    }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LiveScannerEffect.RequestCameraPermission -> {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA,
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        viewModel.handleEvent(LiveScannerEvent.OnPermissionResult(true))
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                is LiveScannerEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.handleEvent(LiveScannerEvent.CheckInitialPermission)
    }

    LiveScannerScreenContent(
        state = state,
        onGrantPermission = {
            viewModel.handleEvent(LiveScannerEvent.CheckInitialPermission)
        },
        onCameraError = { message ->
            viewModel.handleEvent(LiveScannerEvent.OnCameraError(message))
        },
        onCardStabilize = { bitmap ->
            viewModel.handleEvent(LiveScannerEvent.OnStabilityReached(bitmap))
        },
        onScanNext = { viewModel.handleEvent(LiveScannerEvent.ScanNext) },
        onTryAgain = { viewModel.handleEvent(LiveScannerEvent.TryAgain) },
        onScanBack = { viewModel.handleEvent(LiveScannerEvent.ScanBack) },
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveScannerScreenContent(
    state: LiveScannerState,
    onGrantPermission: () -> Unit,
    onCameraError: (String) -> Unit,
    onCardStabilize: (android.graphics.Bitmap) -> Unit,
    onScanNext: () -> Unit,
    onTryAgain: () -> Unit,
    onScanBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val phase = state.phase
    val isAnalyzerPaused = phase !is ScanPhase.Scanning && phase !is ScanPhase.ScanningBack

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.hasCameraPermission) {
            Box(modifier = Modifier.padding(innerPadding)) {
                CameraPreview(
                    isAnalyzerPaused = isAnalyzerPaused,
                    onError = onCameraError,
                    onCardStabilize = onCardStabilize,
                    modifier = Modifier.fillMaxSize(),
                )

                // Scrim overlay for extracting / downloading phases
                when (phase) {
                    is ScanPhase.Extracting,
                    is ScanPhase.ExtractingBack,
                    -> {
                        ScannerOverlay(
                            message = if (state.isDownloadingModel) {
                                stringResource(R.string.scanner_downloading_model)
                            } else {
                                stringResource(R.string.scanner_extracting)
                            },
                        )
                    }
                    else -> Unit
                }
            }

            // Bottom sheets for result / error / flip prompt phases
            when (phase) {
                is ScanPhase.ShowingResult -> {
                    ResultBottomSheet(
                        sticker = phase.sticker,
                        borderColor = phase.borderColor,
                        onScanNext = onScanNext,
                    )
                }
                is ScanPhase.Error -> {
                    ErrorBottomSheet(
                        message = phase.message,
                        onTryAgain = onTryAgain,
                    )
                }
                is ScanPhase.FlipPrompt -> {
                    FlipPromptBottomSheet(
                        borderColor = phase.borderColor,
                        onScanBack = onScanBack,
                        onTryAgain = onTryAgain,
                    )
                }
                else -> Unit
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.scanner_permission_required))
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onGrantPermission) {
                        Text(stringResource(R.string.scanner_grant_permission))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultBottomSheet(
    sticker: StickerEntity,
    borderColor: String,
    onScanNext: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onScanNext,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.scanner_result_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    R.string.scanner_result_player,
                    sticker.playerName,
                    sticker.stickerCode,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.scanner_result_variant, borderColor),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onScanNext,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scanner_save_and_scan_next))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorBottomSheet(
    message: String,
    onTryAgain: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onTryAgain,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.scanner_error_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onTryAgain,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scanner_try_again))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlipPromptBottomSheet(
    borderColor: String,
    onScanBack: () -> Unit,
    onTryAgain: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onTryAgain,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.scanner_flip_card),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.scanner_result_variant, borderColor),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onScanBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scanner_scan_back_action))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onTryAgain,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.scanner_try_again))
            }
        }
    }
}

@Composable
private fun ScannerOverlay(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveScannerPermissionPreview() {
    SlabSnapTheme {
        LiveScannerScreenContent(
            state = LiveScannerState(hasCameraPermission = false),
            onGrantPermission = {},
            onCameraError = {},
            onCardStabilize = {},
            onScanNext = {},
            onTryAgain = {},
            onScanBack = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerOverlayExtractingPreview() {
    SlabSnapTheme {
        ScannerOverlay(message = "Analyzing card data\u2026")
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerOverlayDownloadingPreview() {
    SlabSnapTheme {
        ScannerOverlay(message = "Downloading AI model (one-time setup)\u2026")
    }
}
