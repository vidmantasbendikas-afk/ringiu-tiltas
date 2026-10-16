package lt.ringiu.tiltas.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import lt.ringiu.tiltas.data.LevelThresholds

private val ZONE_GREEN = Color(0xFF34C759).copy(alpha = 0.8f)
private val ZONE_ORANGE = Color(0xFFFF9500).copy(alpha = 0.9f)
private val ZONE_RED = Color(0xFFFF3B30).copy(alpha = 0.85f)

@Composable
fun LevelGauge(level: Double?, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.height(42.dp)) {
        val width = maxWidth
        fun map(value: Double): Dp =
            width * ((value - LevelThresholds.GAUGE_MIN) /
                (LevelThresholds.GAUGE_MAX - LevelThresholds.GAUGE_MIN)).toFloat()

        Column {
            Box(Modifier.fillMaxWidth().height(16.dp)) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(percent = 50)),
                ) {
                    Box(Modifier.width(map(190.0)).fillMaxHeight().background(ZONE_GREEN))
                    Box(
                        Modifier
                            .width(map(215.0) - map(190.0))
                            .fillMaxHeight()
                            .background(ZONE_ORANGE),
                    )
                    Box(Modifier.weight(1f).fillMaxHeight().background(ZONE_RED))
                }
                if (level != null) {
                    val dotX by animateDpAsState(
                        targetValue = map(level) - 6.dp,
                        animationSpec = spring(dampingRatio = 0.75f),
                        label = "gaugeDot",
                    )
                    Box(
                        Modifier
                            .offset(x = dotX, y = 2.dp)
                            .size(12.dp)
                            .shadow(3.dp, CircleShape)
                            .background(Color.White, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(20.dp)) {
                for (threshold in listOf(190.0, 215.0)) {
                    Column(Modifier.offset(x = map(threshold) - 0.5.dp)) {
                        Box(
                            Modifier
                                .width(1.dp)
                                .height(6.dp)
                                .background(Color.White.copy(alpha = 0.7f)),
                        )
                        Text(
                            text = "%.0f".format(threshold),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
    }
}
