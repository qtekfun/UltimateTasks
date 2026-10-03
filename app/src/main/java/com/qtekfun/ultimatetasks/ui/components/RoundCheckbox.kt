// SPDX-FileCopyrightText: 2026 UltimateTasks contributors
// SPDX-License-Identifier: GPL-3.0-or-later

package com.qtekfun.ultimatetasks.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.qtekfun.ultimatetasks.ui.theme.CheckboxTokens

/** The round checkbox of Apple Reminders: an outline in the list color, filled when done. */
@Composable
fun RoundCheckbox(
    checked: Boolean,
    color: Color,
    description: String,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val fill by animateFloatAsState(if (checked) 1f else 0f, label = "check")
    val toggle = onCheckedChange?.let {
        Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = it)
    } ?: Modifier
    Box(
        modifier = modifier.size(48.dp).then(toggle).semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(24.dp)) {
            val stroke = 2.dp.toPx()
            drawCircle(color, radius = size.minDimension / 2 - stroke / 2, style = Stroke(stroke))
            if (fill > 0f) {
                drawCircle(color, radius = (size.minDimension / 2 - stroke * 2) * fill)
                val check = Path().apply {
                    CheckboxTokens.tick.forEachIndexed { index, (x, y) ->
                        if (index ==
                            0
                        ) {
                            moveTo(size.width * x, size.height * y)
                        } else {
                            lineTo(
                                size.width * x,
                                size.height * y
                            )
                        }
                    }
                }
                drawPath(
                    check,
                    Color.White.copy(alpha = fill),
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            if (onCheckedChange ==
                null
            ) {
                drawCircle(
                    color.copy(alpha = 0.2f),
                    center = Offset(
                        size.width / 2,
                        size.height / 2
                    )
                )
            }
        }
    }
}
