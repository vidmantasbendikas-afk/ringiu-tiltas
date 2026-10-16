package lt.ringiu.tiltas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import lt.ringiu.tiltas.data.BridgeState
import lt.ringiu.tiltas.data.LevelThresholds
import lt.ringiu.tiltas.data.Reading
import java.util.Locale

private val LT_LOCALE = Locale.forLanguageTag("lt")

private fun formatOneDecimal(value: Double): String =
    String.format(LT_LOCALE, "%.1f", value)

private fun Modifier.glassCard(cornerRadius: Int): Modifier = this
    .shadow(8.dp, RoundedCornerShape(cornerRadius.dp))
    .clip(RoundedCornerShape(cornerRadius.dp))
    .background(Color.White.copy(alpha = 0.16f))

@Composable
fun MainView(model: BridgeViewModel = viewModel()) {
    val reading by model.reading.collectAsState()
    val isLoading by model.isLoading.collectAsState()
    val errorMessage by model.errorMessage.collectAsState()
    val state = LevelThresholds.stateForLevel(reading?.levelCM)

    Box(Modifier.fillMaxSize()) {
        BridgeScenery(
            level = reading?.levelCM,
            state = state,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(
                state = state,
                reading = reading,
                errorMessage = errorMessage,
                isLoading = isLoading,
            )
            Spacer(Modifier.weight(1f))
            GaugeCard(
                state = state,
                level = reading?.levelCM,
                isLoading = isLoading,
                onRefresh = model::refresh,
            )
        }
    }
}

@Composable
private fun StatusCard(
    state: BridgeState,
    reading: Reading?,
    errorMessage: String?,
    isLoading: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(24)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.emoji,
            fontSize = 56.sp,
            color = Color.White,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.3f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                    blurRadius = 4f,
                ),
            ),
        )
        Text(
            text = state.title,
            fontSize = 34.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.35f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 3f,
                ),
            ),
        )
        Text(
            text = state.subtitle,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.92f),
            textAlign = TextAlign.Center,
        )

        if (reading != null) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 2.dp),
                thickness = 1.dp,
                color = Color.White.copy(alpha = 0.3f),
            )
            Text(
                text = "Vandens lygis: " +
                    (reading.levelCM?.let { formatOneDecimal(it) } ?: "–") +
                    " cm",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            reading.tempC?.let { temp ->
                Text(
                    text = "Vandens temperatūra: ${formatOneDecimal(temp)} °C",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Text(
                text = "Duomenys atnaujinti: ${reading.updatedAt}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
        }

        if (errorMessage != null) {
            Text(
                text = "⚠ $errorMessage",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFF453A),
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        if (isLoading && reading == null) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(20.dp),
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun GaugeCard(
    state: BridgeState,
    level: Double?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glassCard(20)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LevelGauge(level = level, modifier = Modifier.fillMaxWidth())

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendDot(Color(0xFF34C759), "< 190 sausas")
            LegendDot(Color(0xFFFF9500), "190–215 šlapia")
            LegendDot(Color(0xFFFF3B30), "> 215 užtvindytas")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = state.tint),
            ) {
                Text(
                    text = "⟳ Atnaujinti",
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.weight(1f))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.9f),
        )
    }
}
