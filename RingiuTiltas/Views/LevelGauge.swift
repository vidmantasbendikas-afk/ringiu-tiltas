import SwiftUI

/// Horizontal gauge over 150–240 cm with colored zones for the three bridge
/// states and an animated marker at the live level.
struct LevelGauge: View {
    let level: Double?

    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            let map: (Double) -> CGFloat = { value in
                width * CGFloat((value - LevelThresholds.gaugeMin) /
                                (LevelThresholds.gaugeMax - LevelThresholds.gaugeMin))
            }

            VStack(alignment: .leading, spacing: 4) {
                ZStack(alignment: .topLeading) {
                    HStack(spacing: 0) {
                        Rectangle()
                            .fill(Color.green.opacity(0.8))
                            .frame(width: map(190))
                        Rectangle()
                            .fill(Color.orange.opacity(0.9))
                            .frame(width: map(215) - map(190))
                        Rectangle()
                            .fill(Color.red.opacity(0.85))
                            .frame(maxWidth: .infinity)
                    }
                    .clipShape(Capsule())

                    if let level {
                        Circle()
                            .fill(.white)
                            .frame(width: 12, height: 12)
                            .shadow(radius: 3)
                            .offset(x: map(level) - 6, y: 2)
                            .animation(.spring(response: 0.6, dampingFraction: 0.75),
                                       value: level)
                    }
                }
                .frame(height: 16)

                ZStack(alignment: .topLeading) {
                    ForEach([190.0, 215.0], id: \.self) { threshold in
                        VStack(spacing: 1) {
                            Rectangle()
                                .fill(.white.opacity(0.7))
                                .frame(width: 1, height: 6)
                            Text(String(format: "%.0f", threshold))
                                .font(.caption2)
                                .foregroundStyle(.white.opacity(0.85))
                        }
                        .offset(x: map(threshold) - 0.5)
                    }
                }
                .frame(height: 20)
            }
        }
        .frame(height: 42)
    }
}