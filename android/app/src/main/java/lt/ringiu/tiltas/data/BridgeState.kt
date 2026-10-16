package lt.ringiu.tiltas.data

import androidx.compose.ui.graphics.Color

object LevelThresholds {
    const val GAUGE_MIN = 150.0
    const val GAUGE_MAX = 240.0
    const val WET_START = 190.0
    const val FLOOD_START = 215.0

    fun stateForLevel(level: Double?): BridgeState = when {
        level == null -> BridgeState.UNKNOWN
        level > FLOOD_START -> BridgeState.FLOODED
        level >= WET_START -> BridgeState.WET
        else -> BridgeState.DRY
    }
}

data class Reading(
    val levelCM: Double?,
    val tempC: Double?,
    val updatedAt: String,
)

enum class BridgeState(
    val emoji: String,
    val title: String,
    val subtitle: String,
    val tint: Color,
) {
    UNKNOWN("❓", "Nėra duomenų", "Nepavyko nuskaityti vandens lygio", Color(0xFF8E8E93)),
    DRY("☀️", "Sausas tiltas", "Vandens lygis žemiau 190 cm – saugu pereiti", Color(0xFF34C759)),
    WET("💧", "Šlapia", "190–215 cm – vanduo siekia tiltą", Color(0xFFFF9500)),
    FLOODED("🌊", "Užtvindytas", "Virš 215 cm – tiltas po vandeniu", Color(0xFFFF3B30)),
}
