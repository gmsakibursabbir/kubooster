package com.kuduy.kubooster.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shared components. Kept deliberately small: cards, info rows and status
 * chips are the recurring vocabulary of this app. Nothing decorative lives
 * here — no gradients, no glows, no pulsing badges.
 */

private val CardRadius = 18.dp
private val CardShape = RoundedCornerShape(CardRadius)

/** Elevated surface card with the brand border. */
@Composable
fun KubeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val border = MaterialTheme.colorScheme.outlineVariant
    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = CardShape,
            color = container,
            modifier = modifier,
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Box(
            modifier = modifier
                .clip(CardShape)
                .background(container)
                .border(1.dp, border, CardShape),
        ) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

/** Section title. No tracked caps, no gray repetition of the title's meaning. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(bottom = 10.dp),
    )
}

/**
 * Label/value row used across diagnostics and optimization details.
 * Left-aligned; the value column is weighted so numbers align cleanly.
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = valueColor,
            modifier = Modifier.weight(0.58f),
        )
    }
}

/**
 * Status chip with a text label — color is a secondary signal, the words are
 * the primary one (readable in dark mode, for color-blind users, and at 200%).
 */
@Composable
fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(color.copy(alpha = 0.14f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}
