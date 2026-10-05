/*
 * Copyright (C) 2026 Sandboxr Platform
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sandboxr.launcher.cuebar.ui.compose.modifier

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.sandboxr.launcher.cuebar.ui.utils.AiColorUtils.boostChroma
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

@Composable
fun Modifier.animatedActionBorder(
    strokeWidth: Dp,
    cornerRadius: Dp,
    visible: Boolean = true,
): Modifier {
    val rotationAngle = remember { Animatable(INITIAL_ROTATION_DEGREES) }

    val strokeWidthPx = with(LocalDensity.current) { strokeWidth.toPx() }
    val halfStroke = strokeWidthPx / 2f
    val topLeft = Offset(halfStroke, halfStroke)
    val strokeAnimStartColor =
        Color(boostChroma(MaterialTheme.colorScheme.tertiaryContainer.toArgb()))
    val strokeAnimMiddleColor =
        Color(boostChroma(MaterialTheme.colorScheme.secondary.toArgb()))
    val strokeAnimEndColor = Color(boostChroma(MaterialTheme.colorScheme.primary.toArgb()))

    LaunchedEffect(visible) {
        if (visible) {
            launch {
                rotationAngle.snapTo(INITIAL_ROTATION_DEGREES)
                rotationAngle.animateTo(
                    targetValue = rotationAngle.value + 360f,
                    animationSpec = repeatable(
                        iterations = 2,
                        tween(
                            durationMillis = ROTATION_DURATION_MILLIS,
                            easing = LinearEasing,
                        ),
                    ),
                )
            }
        }
    }

    return drawWithContent {
        val currentRotationRad = Math.toRadians(rotationAngle.value.toDouble()).toFloat()
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val gradientWidth = size.width / 2f

        val center = size.center
        val strokeStyle = Stroke(width = strokeWidthPx)

        val cosTheta = cos(currentRotationRad)
        val sinTheta = sin(currentRotationRad)

        val startOffset =
            Offset(x = center.x - gradientWidth * cosTheta, y = center.y - gradientWidth * sinTheta)
        val endOffset =
            Offset(x = center.x + gradientWidth * cosTheta, y = center.y + gradientWidth * sinTheta)

        val gradientBrush =
            Brush.linearGradient(
                GRADIENT_START_FRACTION to strokeAnimStartColor,
                GRADIENT_MIDDLE_FRACTION to strokeAnimMiddleColor,
                GRADIENT_END_FRACTION to strokeAnimEndColor,
                start = startOffset,
                end = endOffset,
                tileMode = TileMode.Clamp,
            )

        drawRoundRect(
            brush = gradientBrush,
            topLeft = topLeft,
            size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
            cornerRadius = CornerRadius(cornerRadiusPx),
            style = strokeStyle,
        )

        drawContent()
    }
}

private const val INITIAL_ROTATION_DEGREES: Float = 45f
private const val GRADIENT_START_FRACTION = 0.3f
private const val GRADIENT_MIDDLE_FRACTION = 0.5f
private const val GRADIENT_END_FRACTION = 0.8f
private const val ROTATION_DURATION_MILLIS: Int = 3000
