package us.kikinsoft.slabsnap.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import us.kikinsoft.slabsnap.R
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
        onTryAgain = { viewModel.handleEvent(LiveScannerEvent.TryAgain) },
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
    onTryAgain: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val phase = state.phase
    val isAnalyzerPaused = phase is ScanPhase.ExtractingFront ||
        phase is ScanPhase.ExtractingBack ||
        phase is ScanPhase.Saving ||
        phase is ScanPhase.Error

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

                // Step indicator HUD at top
                StepIndicator(
                    phase = phase,
                    isDownloadingModel = state.isDownloadingModel,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp),
                )

                // Extracting scrim overlay
                when (phase) {
                    is ScanPhase.ExtractingFront,
                    is ScanPhase.ExtractingBack,
                    is ScanPhase.Saving,
                    -> {
                        ScannerOverlay(
                            message = when {
                                state.isDownloadingModel ->
                                    stringResource(R.string.scanner_downloading_model)
                                phase is ScanPhase.Saving ->
                                    stringResource(R.string.scanner_saving)
                                phase is ScanPhase.ExtractingFront ->
                                    stringResource(R.string.scanner_extracting_front)
                                else ->
                                    stringResource(R.string.scanner_extracting_back)
                            },
                        )
                    }
                    is ScanPhase.FlipPrompt -> {
                        FlipOverlay()
                    }
                    else -> Unit
                }

                // Session counter pip (bottom-start)
                SessionPip(
                    sessionCards = state.sessionCards,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                )
            }

            // Error bottom sheet
            if (phase is ScanPhase.Error) {
                ErrorBottomSheet(
                    message = phase.message,
                    onTryAgain = onTryAgain,
                )
            }
        } else {
            PermissionPrompt(
                onGrantPermission = onGrantPermission,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun StepIndicator(
    phase: ScanPhase,
    isDownloadingModel: Boolean,
    modifier: Modifier = Modifier,
) {
    val text = when (phase) {
        is ScanPhase.ScanningFront, is ScanPhase.ExtractingFront ->
            stringResource(R.string.scanner_step_front)
        is ScanPhase.FlipPrompt ->
            stringResource(R.string.scanner_step_flip)
        is ScanPhase.ScanningBack, is ScanPhase.ExtractingBack ->
            stringResource(R.string.scanner_step_back)
        else -> null
    }

    if (text != null && !isDownloadingModel) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.85f),
            shape = MaterialTheme.shapes.medium,
            modifier = modifier,
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun FlipOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(
                text = stringResource(R.string.scanner_step_flip),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp),
            )
        }
    }
}

@Composable
private fun SessionPip(
    sessionCards: ImmutableList<ScannedCard>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = sessionCards.isNotEmpty(),
        enter = scaleIn() + fadeIn(),
        modifier = modifier,
    ) {
        Box {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val lastCard = sessionCards.lastOrNull()
                    if (lastCard != null) {
                        Text(
                            text = lastCard.stickerCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            if (sessionCards.size > 0) {
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        text = stringResource(
                            R.string.scanner_session_count,
                            sessionCards.size,
                        ),
                    )
                }
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

@Composable
private fun PermissionPrompt(
    onGrantPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
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

@Preview(showBackground = true)
@Composable
private fun LiveScannerPermissionPreview() {
    SlabSnapTheme {
        LiveScannerScreenContent(
            state = LiveScannerState(hasCameraPermission = false),
            onGrantPermission = {},
            onCameraError = {},
            onCardStabilize = {},
            onTryAgain = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ScannerOverlayPreview() {
    SlabSnapTheme {
        ScannerOverlay(message = "Reading card\u2026")
    }
}

@Preview(showBackground = true)
@Composable
private fun SessionPipPreview() {
    SlabSnapTheme {
        SessionPip(
            sessionCards = persistentListOf(
                ScannedCard("ARG20", "Lionel Messi", "Blue"),
                ScannedCard("FRA19", "Kylian Mbappé", "White"),
            ),
        )
    }
}
