import SwiftUI

/// Thresholds (cm) for the Jūra–Ringių k station.
enum LevelThresholds {
    static let gaugeMin: Double = 150
    static let gaugeMax: Double = 240

    /// 215 >= level >= 190
    static let wetStart: Double = 190
    /// level > 215
    static let floodStart: Double = 215

    static func state(forLevel level: Double?) -> BridgeState {
        guard let level else { return .unknown }
        if level > floodStart { return .flooded }
        if level >= wetStart { return .wet }
        return .dry
    }
}

/// One hydrology reading for the station: water level in cm,
/// water temperature in °C and the source update timestamp.
struct Reading: Equatable {
    var levelCM: Double?
    var tempC: Double?
    var updatedAt: String
}

enum BridgeState: String, CaseIterable {
    case unknown, dry, wet, flooded

    var title: String {
        switch self {
        case .unknown: return "Nėra duomenų"
        case .dry: return "Sausas tiltas"
        case .wet: return "Šlapia"
        case .flooded: return "Užtvindytas"
        }
    }

    var subtitle: String {
        switch self {
        case .unknown:
            return "Nepavyko nuskaityti vandens lygio"
        case .dry:
            return "Vandens lygis žemiau 190 cm – saugu pereiti"
        case .wet:
            return "190–215 cm – vanduo siekia tiltą"
        case .flooded:
            return "Virš 215 cm – tiltas po vandeniu"
        }
    }

    var emoji: String {
        switch self {
        case .unknown: return "❓"
        case .dry: return "☀️"
        case .wet: return "💧"
        case .flooded: return "🌊"
        }
    }

    var tint: Color {
        switch self {
        case .unknown: return .gray
        case .dry: return .green
        case .wet: return .orange
        case .flooded: return .red
        }
    }
}