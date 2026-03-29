package us.kikinsoft.slabsnap.ui.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@Composable
private fun LiveScannerScreenContent(
    state: LiveScannerState,
    onGrantPermission: () -> Unit,
    onCameraError: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        if (state.hasCameraPermission) {
            CameraPreview(
                onError = onCameraError,
                modifier = Modifier.padding(innerPadding),
            )
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

@Preview(showBackground = true)
@Composable
private fun LiveScannerPermissionPreview() {
    SlabSnapTheme {
        LiveScannerScreenContent(
            state = LiveScannerState(hasCameraPermission = false),
            onGrantPermission = {},
            onCameraError = {},
        )
    }
}
