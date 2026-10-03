// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

/**
 * A label at the start and its value at the end, as in Settings rows. With large fonts both
 * wrap instead of one squeezing the other: see [splitWidths].
 */
@Composable
fun LabelValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Layout(
        contents = listOf(
            { Text(label) },
            { Text(value, color = valueColor, textAlign = TextAlign.End) }
        ),
        modifier = modifier
    ) { (labels, values), constraints ->
        val label = labels.single()
        val value = values.single()
        val (labelWidth, valueWidth) = splitWidths(
            constraints.maxWidth,
            GAP.roundToPx(),
            label.maxIntrinsicWidth(Constraints.Infinity),
            value.maxIntrinsicWidth(Constraints.Infinity)
        )
        val placedLabel = label.measure(Constraints(maxWidth = labelWidth))
        val placedValue = value.measure(Constraints(maxWidth = valueWidth))
        val height = maxOf(placedLabel.height, placedValue.height, constraints.minHeight)
        layout(constraints.maxWidth, height) {
            placedLabel.placeRelative(0, (height - placedLabel.height) / 2)
            placedValue.placeRelative(
                constraints.maxWidth - placedValue.width,
                (height - placedValue.height) / 2
            )
        }
    }
}

private val GAP = 12.dp

/**
 * Widths for a label and a value that need [labelNeed] and [valueNeed] in [max] with [gap]
 * between them. Both keep their size when they fit; otherwise the shorter one keeps it if it
 * takes at most half, and the other wraps in the rest; if both are long they share half each.
 */
fun splitWidths(max: Int, gap: Int, labelNeed: Int, valueNeed: Int): Pair<Int, Int> {
    val room = (max - gap).coerceAtLeast(0)
    val half = room / 2
    return when {
        labelNeed + valueNeed <= room -> labelNeed to valueNeed
        labelNeed <= half -> labelNeed to room - labelNeed
        valueNeed <= half -> room - valueNeed to valueNeed
        else -> half to room - half
    }
}
