package us.kikinsoft.slabsnap.ui.collection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import us.kikinsoft.slabsnap.R
import us.kikinsoft.slabsnap.ui.theme.SlabSnapTheme

private val borderColorMap = mapOf(
    "Blue" to Color(0xFF2196F3),
    "Red" to Color(0xFFF44336),
    "Purple" to Color(0xFF9C27B0),
    "Green" to Color(0xFF4CAF50),
    "Black" to Color(0xFF212121),
    "Gold" to Color(0xFFFFD700),
)

@Composable
fun StickerGridItem(
    sticker: StickerUiModel,
    onQuickAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isOwned = sticker.isOwned
    val accentColor = borderColorMap[sticker.borderColor]
    val border = if (isOwned && accentColor != null) {
        BorderStroke(2.dp, accentColor)
    } else {
        null
    }

    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (isOwned) 1f else 0.4f),
            border = border,
            colors = if (!isOwned) {
                CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                )
            } else {
                CardDefaults.cardColors()
            },
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
                if (isOwned && accentColor != null) {
                    Text(
                        text = sticker.borderColor,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        if (!isOwned) {
            IconButton(
                onClick = onQuickAddClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.collection_quick_add),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StickerGridItemUnownedPreview() {
    SlabSnapTheme {
        StickerGridItem(
            sticker = StickerUiModel(
                id = 1L,
                stickerCode = "ARG 20",
                playerName = "Lionel Messi",
                teamName = "Argentina",
                isOwned = false,
            ),
            onQuickAddClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StickerGridItemOwnedWhitePreview() {
    SlabSnapTheme {
        StickerGridItem(
            sticker = StickerUiModel(
                id = 1L,
                stickerCode = "ARG 20",
                playerName = "Lionel Messi",
                teamName = "Argentina",
                isOwned = true,
                borderColor = "White",
            ),
            onQuickAddClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StickerGridItemOwnedBluePreview() {
    SlabSnapTheme {
        StickerGridItem(
            sticker = StickerUiModel(
                id = 1L,
                stickerCode = "FRA 19",
                playerName = "Kylian Mbappé",
                teamName = "France",
                isOwned = true,
                borderColor = "Blue",
            ),
            onQuickAddClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
