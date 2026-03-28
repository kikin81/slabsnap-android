package us.kikinsoft.slabsnap.ui.scanner

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import us.kikinsoft.slabsnap.R
import us.kikinsoft.slabsnap.ui.theme.SlabSnapTheme

@Composable
fun LiveScannerScreen(onNavigateBack: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onNavigateBack)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.scanner_title))
    }
}

@Preview(showBackground = true)
@Composable
private fun LiveScannerScreenPreview() {
    SlabSnapTheme {
        LiveScannerScreen(onNavigateBack = {})
    }
}
