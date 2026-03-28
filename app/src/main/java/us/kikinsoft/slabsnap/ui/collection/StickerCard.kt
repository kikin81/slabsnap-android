package us.kikinsoft.slabsnap.ui.collection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import us.kikinsoft.slabsnap.ui.theme.SlabSnapTheme

@Composable
fun StickerCard(sticker: StickerUiModel, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .semantics(mergeDescendants = true) {},
        ) {
            Text(
                text = sticker.stickerCode,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = sticker.playerName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = sticker.teamName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StickerCardPreview() {
    SlabSnapTheme {
        StickerCard(
            sticker = StickerUiModel(
                id = 1L,
                stickerCode = "ARG 10",
                playerName = "Lionel Messi",
                teamName = "Argentina",
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}
