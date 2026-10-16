package lt.ringiu.tiltas.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import lt.ringiu.tiltas.data.BridgeState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.max

private const val FRAME_INTERVAL_NS = 33_000_000L

@Composable
fun BridgeScenery(level: Double?, state: BridgeState, modifier: Modifier = Modifier) {
    val time by produceState(0.0) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last == 0L || now - last >= FRAME_INTERVAL_NS) {
                    last = now
                    value = now / 1_000_000_000.0
                }
            }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        drawScene(level, state, time)
    }
}

private fun waterY(level: Double?, h: Float): Float {
    fun lerp(from: Pair<Double, Float>, to: Pair<Double, Float>, v: Double): Float {
        val f = ((v - from.first) / (to.first - from.first)).toFloat().coerceIn(0f, 1f)
        return from.second + f * (to.second - from.second)
    }
    if (level == null) return h * 0.75f
    return when {
        level < 150.0 -> h * 0.97f
        level < 190.0 -> lerp(150.0 to h * 0.97f, 190.0 to h * 0.66f, level)
        level < 215.0 -> lerp(190.0 to h * 0.66f, 215.0 to h * 0.57f, level)
        level < 240.0 -> lerp(215.0 to h * 0.57f, 240.0 to h * 0.26f, level)
        else -> h * 0.26f
    }
}

private fun waveY(x: Float, base: Float, amp: Float, period: Float, phase: Double): Float =
    base + sin((x / period).toDouble() * 2.0 * PI + phase).toFloat() * amp

private fun DrawScope.drawScene(level: Double?, state: BridgeState, time: Double) {
    val w = size.width
    val h = size.height

    val skyColors = when (state) {
        BridgeState.DRY -> listOf(Color(0xFF5CA8F0), Color(0xFFB8DEFC))
        BridgeState.WET -> listOf(Color(0xFF809EBD), Color(0xFFB8CCDE))
        BridgeState.FLOODED -> listOf(Color(0xFF243352), Color(0xFF4D668C))
        BridgeState.UNKNOWN -> listOf(Color(0xFF9EA1A8), Color(0xFFCCCFD4))
    }
    drawRect(Brush.verticalGradient(skyColors, startY = 0f, endY = h), size = size)

    val waterLine = waterY(level, h)

    if (state == BridgeState.DRY) {
        drawSun(w, h, time)
        drawBirds(w, h, time)
    } else {
        drawClouds(w, h, time, dark = state == BridgeState.FLOODED)
    }

    drawBridge(w, h)

    if (state == BridgeState.FLOODED) drawDebris(w, waterLine, time)

    drawWater(w, h, waterLine, state, time)

    if (state == BridgeState.WET) drawSpray(w, waterLine, time)
    if (state == BridgeState.FLOODED) {
        drawRain(w, h, time, heavy = true)
    } else if (state == BridgeState.WET) {
        drawRain(w, h, time, heavy = false)
    }
}

private fun DrawScope.drawSun(w: Float, h: Float, time: Double) {
    val cx = w * 0.82f
    val cy = h * 0.13f
    val r = w * 0.06f

    drawOval(
        color = Color.Yellow.copy(alpha = 0.25f),
        topLeft = Offset(cx - r * 2.2f, cy - r * 2.2f),
        size = Size(r * 4.4f, r * 4.4f),
    )
    for (i in 0 until 8) {
        val angle = i / 8.0 * 2.0 * PI + time * 0.15
        val dx = cos(angle) * r * 1.5
        val dy = sin(angle) * r * 1.5
        drawLine(
            color = Color.Yellow.copy(alpha = 0.8f),
            start = Offset(cx + (dx * 0.8).toFloat(), cy + (dy * 0.8).toFloat()),
            end = Offset(cx + (dx * 1.25).toFloat(), cy + (dy * 1.25).toFloat()),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
    }
    drawOval(
        color = Color.Yellow,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2f, r * 2f),
    )
}

private fun DrawScope.drawBirds(w: Float, h: Float, time: Double) {
    for (i in 0 until 4) {
        val speed = 18.0 + i * 6.0
        val cycle = (time * speed) % (w + 80.0)
        val x = cycle.toFloat() - 40f
        val y = h * (0.20f + 0.05f * (i % 3)) + sin(time * 1.5 + i).toFloat() * 6f
        val flap = sin(time * 6.0 + i * 1.7).toFloat() * 5f

        val bird = Path().apply {
            moveTo(x - 7f, y - flap)
            quadraticTo(x - 3f, y - 4f, x, y + 2f)
            quadraticTo(x + 3f, y - 4f, x + 7f, y - flap)
        }
        drawPath(
            bird,
            Color.Black.copy(alpha = 0.6f),
            style = Stroke(width = 2f, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawClouds(w: Float, h: Float, time: Double, dark: Boolean) {
    val color = if (dark) {
        Color(0xFF6B6B6B).copy(alpha = 0.92f)
    } else {
        Color(0xFFE0E0E0).copy(alpha = 0.8f)
    }
    val clouds = listOf(
        Triple(0.20f, 0.11f, 1.0f),
        Triple(0.55f, 0.07f, 0.8f),
        Triple(0.80f, 0.16f, 1.15f),
    )
    for ((fx, fy, s) in clouds) {
        val drift = sin(time * 0.05 + fx.toDouble() * 6.0).toFloat() * 10f
        val cx = w * fx + drift
        val cy = h * fy

        val cloud = Path().apply {
            addOval(Rect(cx - 70f * s, cy - 14f * s, cx + 70f * s, cy + 30f * s))
            addOval(Rect(cx - 45f * s, cy - 34f * s, cx + 45f * s, cy + 18f * s))
            addOval(Rect(cx + 15f * s, cy - 26f * s, cx + 95f * s, cy + 18f * s))
        }
        drawPath(cloud, color)
    }
}

private fun DrawScope.drawBridge(w: Float, h: Float) {
    val deckLeft = w * 0.10f
    val deckRight = w * 0.90f
    val deckTop = h * 0.55f
    val deckBottom = h * 0.585f
    val pierWidth = w * 0.05f

    for (cx in listOf(w * 0.24f, w * 0.50f, w * 0.76f)) {
        drawRoundRect(
            color = Color(0xFF595C63),
            topLeft = Offset(cx - pierWidth / 2f, deckBottom),
            size = Size(pierWidth, h - deckBottom),
            cornerRadius = CornerRadius(pierWidth * 0.12f),
        )
    }

    drawRoundRect(
        color = Color(0xFF474D54),
        topLeft = Offset(deckLeft, deckTop),
        size = Size(deckRight - deckLeft, deckBottom - deckTop),
        cornerRadius = CornerRadius(6f, 6f),
    )

    val railTop = deckTop - h * 0.055f
    val posts = Path()
    for (i in 0..14) {
        val x = deckLeft + (deckRight - deckLeft) * i / 14f
        posts.moveTo(x, railTop)
        posts.lineTo(x, deckTop)
    }
    drawPath(
        posts,
        Color.White.copy(alpha = 0.85f),
        style = Stroke(width = max(2f, w * 0.004f)),
    )

    drawLine(
        color = Color.White.copy(alpha = 0.9f),
        start = Offset(deckLeft, railTop),
        end = Offset(deckRight, railTop),
        strokeWidth = max(3f, w * 0.008f),
    )
}

private fun DrawScope.drawWater(
    w: Float,
    h: Float,
    waterLine: Float,
    state: BridgeState,
    time: Double,
) {
    val shades = when (state) {
        BridgeState.FLOODED -> listOf(
            Color(0x1A4773).copy(alpha = 0.88f),
            Color(0xFF0F294D),
        )
        BridgeState.WET -> listOf(
            Color(0x26669E).copy(alpha = 0.85f),
            Color(0xFF144073),
        )
        BridgeState.UNKNOWN -> listOf(
            Color(0x7394A8).copy(alpha = 0.75f),
            Color(0xFF4D6B85),
        )
        BridgeState.DRY -> listOf(
            Color(0x1F6BAD).copy(alpha = 0.9f),
            Color(0xFF1A4D85),
        )
    }
    drawRect(
        brush = Brush.verticalGradient(shades, startY = waterLine, endY = h),
        topLeft = Offset(0f, waterLine),
        size = Size(w, h - waterLine),
    )

    val speeds = if (state == BridgeState.FLOODED) doubleArrayOf(1.6, 2.4) else doubleArrayOf(0.5, 0.8)
    val amps = when (state) {
        BridgeState.FLOODED -> floatArrayOf(10f, 7f)
        BridgeState.WET -> floatArrayOf(8f, 5f)
        else -> floatArrayOf(4f, 3f)
    }
    val periods = if (state == BridgeState.FLOODED) floatArrayOf(130f, 90f) else floatArrayOf(150f, 100f)

    for (i in 0 until 2) {
        val base = waterLine + i * 7f
        val phase = time * speeds[i] + i * 2.1
        val crest = Path().apply {
            moveTo(0f, waveY(0f, base, amps[i], periods[i], phase))
            var x = 0f
            while (x < w + 8f) {
                x += 8f
                lineTo(x, waveY(x, base, amps[i], periods[i], phase))
            }
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(
            crest,
            Color(0.40f + 0.15f * i, 0.62f, 0.85f)
                .copy(alpha = if (state == BridgeState.FLOODED) 0.5f else 0.35f),
        )
    }
}

private fun DrawScope.drawSpray(w: Float, waterLine: Float, time: Double) {
    for (i in 0 until 24) {
        val seedA = (i * 911 % 991) / 991.0
        val seedB = (i * 677 % 983) / 983.0
        val period = 0.7 + seedB * 0.7
        val progress = (time / period + seedA) % 1.0
        val x = (seedA * w).toFloat()
        val bounce = sin(progress * PI).toFloat()
        val y = waterLine - bounce * (6f + seedB.toFloat() * 12f)
        drawOval(
            color = Color.White.copy(alpha = (0.75 * (1 - progress)).toFloat()),
            topLeft = Offset(x, y),
            size = Size(3f, 3f),
        )
    }
}

private fun DrawScope.drawDebris(w: Float, waterLine: Float, time: Double) {
    for (i in 0 until 3) {
        val speed = 60.0 + i * 25.0
        val cycle = (time * speed) % (w + 120.0)
        val x = cycle.toFloat() - 60f
        val bob = sin(time * 2.2 + i).toFloat() * 5f
        val y = waterLine + bob + i * 6f - 6f

        drawRoundRect(
            color = Color((0.45 + 0.05 * i).toFloat(), 0.30f, 0.20f),
            topLeft = Offset(x, y),
            size = Size(60f, 9f),
            cornerRadius = CornerRadius(4.5f, 4.5f),
        )
    }
}

private fun DrawScope.drawRain(w: Float, h: Float, time: Double, heavy: Boolean) {
    val count = if (heavy) 90 else 40
    val drops = Path()
    for (i in 0 until count) {
        val seedX = (i * 137 % 997) / 997.0
        val seedV = (i * 40503 % 983) / 983.0
        val progress = (time * (0.9 + seedV * 0.6) + seedX) % 1.0
        val x = (seedX * w).toFloat() + sin(i + time).toFloat() * 6f
        val y = (progress * h).toFloat()
        drops.moveTo(x, y)
        drops.lineTo(x + 3f, y + 16f)
    }
    drawPath(
        drops,
        Color.White.copy(alpha = if (heavy) 0.35f else 0.25f),
        style = Stroke(width = 2f, cap = StrokeCap.Round),
    )
}

@Preview(device = "spec:width=390dp,height=844dp", showBackground = true)
@Composable
private fun SceneryDryPreview() {
    BridgeScenery(level = 160.0, state = BridgeState.DRY)
}

@Preview(device = "spec:width=390dp,height=844dp", showBackground = true)
@Composable
private fun SceneryWetPreview() {
    BridgeScenery(level = 200.0, state = BridgeState.WET)
}

@Preview(device = "spec:width=390dp,height=844dp", showBackground = true)
@Composable
private fun SceneryFloodedPreview() {
    BridgeScenery(level = 225.0, state = BridgeState.FLOODED)
}

@Preview(device = "spec:width=390dp,height=844dp", showBackground = true)
@Composable
private fun SceneryUnknownPreview() {
    BridgeScenery(level = null, state = BridgeState.UNKNOWN)
}
