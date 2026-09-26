package com.sandboxr.launcher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sandboxr.launcher.theme.SandboxrColors

/**
 * High-legibility Phosphor-inspired icon glyphs.
 * Built directly with Compose Canvas for zero asset bloat, clean rendering, and zero emojis.
 */
enum class PhosphorIcon {
    LOCK,
    UNLOCK,
    SHIELD,
    TERMINAL,
    CPU,
    BOX,
    GLOBE,
    DATABASE,
    PLUS,
    TRASH,
    GEAR,
    EXPORT,
    CHECK,
    X,
    WARNING,
    COPY
}

@Composable
fun PhosphorIconView(
    icon: PhosphorIcon,
    color: Color = SandboxrColors.TextPrimaryDark,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (icon) {
            PhosphorIcon.LOCK -> drawPhosphorLock(color, stroke, w, h)
            PhosphorIcon.UNLOCK -> drawPhosphorUnlock(color, stroke, w, h)
            PhosphorIcon.SHIELD -> drawPhosphorShield(color, stroke, w, h)
            PhosphorIcon.TERMINAL -> drawPhosphorTerminal(color, stroke, w, h)
            PhosphorIcon.CPU -> drawPhosphorCpu(color, stroke, w, h)
            PhosphorIcon.BOX -> drawPhosphorBox(color, stroke, w, h)
            PhosphorIcon.GLOBE -> drawPhosphorGlobe(color, stroke, w, h)
            PhosphorIcon.DATABASE -> drawPhosphorDatabase(color, stroke, w, h)
            PhosphorIcon.PLUS -> drawPhosphorPlus(color, stroke, w, h)
            PhosphorIcon.TRASH -> drawPhosphorTrash(color, stroke, w, h)
            PhosphorIcon.GEAR -> drawPhosphorGear(color, stroke, w, h)
            PhosphorIcon.EXPORT -> drawPhosphorExport(color, stroke, w, h)
            PhosphorIcon.CHECK -> drawPhosphorCheck(color, stroke, w, h)
            PhosphorIcon.X -> drawPhosphorX(color, stroke, w, h)
            PhosphorIcon.WARNING -> drawPhosphorWarning(color, stroke, w, h)
            PhosphorIcon.COPY -> drawPhosphorCopy(color, stroke, w, h)
        }
    }
}

private fun DrawScope.drawPhosphorLock(color: Color, stroke: Stroke, w: Float, h: Float) {
    val shackle = Path().apply {
        moveTo(w * 0.32f, h * 0.44f)
        lineTo(w * 0.32f, h * 0.30f)
        cubicTo(w * 0.32f, h * 0.12f, w * 0.68f, h * 0.12f, w * 0.68f, h * 0.30f)
        lineTo(w * 0.68f, h * 0.44f)
    }
    drawPath(shackle, color, style = stroke)
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.22f, h * 0.44f),
        size = Size(w * 0.56f, h * 0.46f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
}

private fun DrawScope.drawPhosphorUnlock(color: Color, stroke: Stroke, w: Float, h: Float) {
    val shackle = Path().apply {
        moveTo(w * 0.32f, h * 0.44f)
        lineTo(w * 0.32f, h * 0.28f)
        cubicTo(w * 0.32f, h * 0.10f, w * 0.68f, h * 0.10f, w * 0.68f, h * 0.28f)
    }
    drawPath(shackle, color, style = stroke)
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.22f, h * 0.44f),
        size = Size(w * 0.56f, h * 0.46f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
}

private fun DrawScope.drawPhosphorShield(color: Color, stroke: Stroke, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.12f)
        lineTo(w * 0.84f, h * 0.26f)
        lineTo(w * 0.84f, h * 0.54f)
        cubicTo(w * 0.84f, h * 0.78f, w * 0.5f, h * 0.90f, w * 0.5f, h * 0.90f)
        cubicTo(w * 0.5f, h * 0.90f, w * 0.16f, h * 0.78f, w * 0.16f, h * 0.54f)
        lineTo(w * 0.16f, h * 0.26f)
        close()
    }
    drawPath(path, color, style = stroke)
}

private fun DrawScope.drawPhosphorTerminal(color: Color, stroke: Stroke, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(w * 0.20f, h * 0.32f)
        lineTo(w * 0.44f, h * 0.50f)
        lineTo(w * 0.20f, h * 0.68f)
    }
    drawPath(path, color, style = stroke)
    drawLine(color, Offset(w * 0.54f, h * 0.68f), Offset(w * 0.80f, h * 0.68f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawPhosphorCpu(color: Color, stroke: Stroke, w: Float, h: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.28f, h * 0.28f),
        size = Size(w * 0.44f, h * 0.44f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = stroke
    )
    // Horizontal and vertical pins
    drawLine(color, Offset(w * 0.40f, h * 0.14f), Offset(w * 0.40f, h * 0.28f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.60f, h * 0.14f), Offset(w * 0.60f, h * 0.28f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.40f, h * 0.72f), Offset(w * 0.40f, h * 0.86f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.60f, h * 0.72f), Offset(w * 0.60f, h * 0.86f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawPhosphorBox(color: Color, stroke: Stroke, w: Float, h: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.20f, h * 0.20f),
        size = Size(w * 0.60f, h * 0.60f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
    drawLine(color, Offset(w * 0.20f, h * 0.42f), Offset(w * 0.80f, h * 0.42f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawPhosphorGlobe(color: Color, stroke: Stroke, w: Float, h: Float) {
    drawCircle(color, radius = w * 0.38f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
    drawLine(color, Offset(w * 0.12f, h * 0.5f), Offset(w * 0.88f, h * 0.5f), stroke.width, StrokeCap.Round)
    drawOval(
        color = color,
        topLeft = Offset(w * 0.32f, h * 0.12f),
        size = Size(w * 0.36f, h * 0.76f),
        style = stroke
    )
}

private fun DrawScope.drawPhosphorDatabase(color: Color, stroke: Stroke, w: Float, h: Float) {
    drawOval(color, topLeft = Offset(w * 0.20f, h * 0.16f), size = Size(w * 0.60f, h * 0.22f), style = stroke)
    drawOval(color, topLeft = Offset(w * 0.20f, h * 0.42f), size = Size(w * 0.60f, h * 0.22f), style = stroke)
    drawOval(color, topLeft = Offset(w * 0.20f, h * 0.68f), size = Size(w * 0.60f, h * 0.22f), style = stroke)
    drawLine(color, Offset(w * 0.20f, h * 0.27f), Offset(w * 0.20f, h * 0.79f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.80f, h * 0.27f), Offset(w * 0.80f, h * 0.79f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawPhosphorPlus(color: Color, stroke: Stroke, w: Float, h: Float) {
    drawLine(color, Offset(w * 0.5f, h * 0.20f), Offset(w * 0.5f, h * 0.80f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.20f, h * 0.5f), Offset(w * 0.80f, h * 0.5f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawPhosphorTrash(color: Color, stroke: Stroke, w: Float, h: Float) {
    // Top lid line
    drawLine(color, Offset(w * 0.22f, h * 0.28f), Offset(w * 0.78f, h * 0.28f), stroke.width, StrokeCap.Round)
    // Handle
    val handle = Path().apply {
        moveTo(w * 0.40f, h * 0.28f)
        lineTo(w * 0.40f, h * 0.18f)
        lineTo(w * 0.60f, h * 0.18f)
        lineTo(w * 0.60f, h * 0.28f)
    }
    drawPath(handle, color, style = stroke)
    // Bin body
    val body = Path().apply {
        moveTo(w * 0.28f, h * 0.28f)
        lineTo(w * 0.32f, h * 0.82f)
        cubicTo(w * 0.32f, h * 0.86f, w * 0.36f, h * 0.86f, w * 0.40f, h * 0.86f)
        lineTo(w * 0.60f, h * 0.86f)
        cubicTo(w * 0.64f, h * 0.86f, w * 0.68f, h * 0.86f, w * 0.68f, h * 0.82f)
        lineTo(w * 0.72f, h * 0.28f)
    }
    drawPath(body, color, style = stroke)
}

private fun DrawScope.drawPhosphorGear(color: Color, stroke: Stroke, w: Float, h: Float) {
    drawCircle(color, radius = w * 0.22f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
    val teethCount = 6
    val dt = (2.0 * Math.PI) / teethCount
    for (i in 0 until teethCount) {
        val angle = i * dt
        val r1 = w * 0.32f
        val r2 = w * 0.44f
        val x1 = (w * 0.5f + r1 * Math.cos(angle)).toFloat()
        val y1 = (h * 0.5f + r1 * Math.sin(angle)).toFloat()
        val x2 = (w * 0.5f + r2 * Math.cos(angle)).toFloat()
        val y2 = (h * 0.5f + r2 * Math.sin(angle)).toFloat()
        drawLine(color, Offset(x1, y1), Offset(x2, y2), stroke.width, StrokeCap.Round)
    }
}

private fun DrawScope.drawPhosphorExport(color: Color, stroke: Stroke, w: Float, h: Float) {
    // Arrow up
    val arrow = Path().apply {
        moveTo(w * 0.34f, h * 0.34f)
        lineTo(w * 0.50f, h * 0.18f)
        lineTo(w * 0.66f, h * 0.34f)
    }
    drawPath(arrow, color, style = stroke)
    drawLine(color, Offset(w * 0.5f, h * 0.18f), Offset(w * 0.5f, h * 0.62f), stroke.width, StrokeCap.Round)
    // Base tray
    val tray = Path().apply {
        moveTo(w * 0.22f, h * 0.52f)
        lineTo(w * 0.22f, h * 0.82f)
        lineTo(w * 0.78f, h * 0.82f)
        lineTo(w * 0.78f, h * 0.52f)
    }
    drawPath(tray, color, style = stroke)
}

private fun DrawScope.drawPhosphorCheck(color: Color, stroke: Stroke, w: Float, h: Float) {
    val check = Path().apply {
        moveTo(w * 0.22f, h * 0.52f)
        lineTo(w * 0.42f, h * 0.72f)
        lineTo(w * 0.78f, h * 0.30f)
    }
    drawPath(check, color, style = stroke)
}

private fun DrawScope.drawPhosphorX(color: Color, stroke: Stroke, w: Float, h: Float) {
    drawLine(color, Offset(w * 0.26f, h * 0.26f), Offset(w * 0.74f, h * 0.74f), stroke.width, StrokeCap.Round)
    drawLine(color, Offset(w * 0.74f, h * 0.26f), Offset(w * 0.26f, h * 0.74f), stroke.width, StrokeCap.Round)
}

private fun DrawScope.drawPhosphorWarning(color: Color, stroke: Stroke, w: Float, h: Float) {
    val tri = Path().apply {
        moveTo(w * 0.5f, h * 0.18f)
        lineTo(w * 0.84f, h * 0.80f)
        lineTo(w * 0.16f, h * 0.80f)
        close()
    }
    drawPath(tri, color, style = stroke)
    drawLine(color, Offset(w * 0.5f, h * 0.42f), Offset(w * 0.5f, h * 0.58f), stroke.width, StrokeCap.Round)
    drawCircle(color, radius = 1.8f.dp.toPx(), center = Offset(w * 0.5f, h * 0.70f))
}

private fun DrawScope.drawPhosphorCopy(color: Color, stroke: Stroke, w: Float, h: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.32f, h * 0.32f),
        size = Size(w * 0.48f, h * 0.48f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = stroke
    )
    val backSheet = Path().apply {
        moveTo(w * 0.24f, h * 0.68f)
        lineTo(w * 0.20f, h * 0.68f)
        lineTo(w * 0.20f, h * 0.20f)
        lineTo(w * 0.68f, h * 0.20f)
        lineTo(w * 0.68f, h * 0.24f)
    }
    drawPath(backSheet, color, style = stroke)
}
