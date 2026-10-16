import SwiftUI

struct MainView: View {
    @StateObject private var model = BridgeViewModel()

    var body: some View {
        ZStack {
            BridgeScenery(level: model.reading?.levelCM, state: model.state)
                .animation(.easeInOut(duration: 1.2), value: model.state)

            VStack(spacing: 16) {
                statusCard
                Spacer()
                gaugeCard
            }
            .padding(20)
        }
        .task { await model.refresh() }
        .refreshable { await model.refresh() }
    }

    private var statusCard: some View {
        VStack(spacing: 6) {
            Text(model.state.emoji)
                .font(.system(size: 56))
                .shadow(color: .black.opacity(0.3), radius: 4, y: 2)

            Text(model.state.title)
                .font(.system(size: 34, weight: .heavy, design: .rounded))
                .foregroundStyle(.white)
                .shadow(color: .black.opacity(0.35), radius: 3, y: 1)

            Text(model.state.subtitle)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(.white.opacity(0.92))
                .multilineTextAlignment(.center)

            if let reading = model.reading {
                Rectangle()
                    .fill(.white.opacity(0.3))
                    .frame(height: 1)
                    .padding(.vertical, 2)

                Text("Vandens lygis: \(reading.levelCM?.formatted(.number.precision(.fractionLength(1))) ?? "–") cm")
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.white)

                if let temp = reading.tempC {
                    Text("Vandens temperatūra: \(temp.formatted(.number.precision(.fractionLength(1)))) °C")
                        .font(.callout)
                        .foregroundStyle(.white.opacity(0.9))
                }

                Text("Duomenys atnaujinti: \(reading.updatedAt)")
                    .font(.caption)
                    .foregroundStyle(.white.opacity(0.8))
            }

            if let error = model.errorMessage {
                Label(error, systemImage: "wifi.exclamationmark")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.red)
                    .padding(.top, 2)
            }

            if model.isLoading && model.reading == nil {
                ProgressView()
                    .padding(.top, 4)
            }
        }
        .padding(22)
        .background(
            RoundedRectangle(cornerRadius: 24, style: .continuous)
                .fill(.ultraThinMaterial)
                .shadow(color: .black.opacity(0.2), radius: 12, y: 6)
        )
        .animation(.spring(response: 0.5, dampingFraction: 0.8), value: model.state)
        .animation(.spring(response: 0.5, dampingFraction: 0.8), value: model.reading)
    }

    private var gaugeCard: some View {
        VStack(alignment: .leading, spacing: 10) {
            LevelGauge(level: model.reading?.levelCM)

            legend

            HStack {
                Button {
                    Task { await model.refresh() }
                } label: {
                    Label("Atnaujinti", systemImage: "arrow.clockwise")
                        .font(.subheadline.weight(.semibold))
                }
                .buttonStyle(.borderedProminent)
                .tint(model.state.tint)

                Spacer()

                if model.isLoading {
                    ProgressView()
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 20, style: .continuous)
                .fill(.ultraThinMaterial)
                .shadow(color: .black.opacity(0.15), radius: 8, y: 4)
        )
    }

    private var legend: some View {
        HStack(spacing: 12) {
            legendItem(.green, "< 190 sausas")
            legendItem(.orange, "190–215 šlapia")
            legendItem(.red, "> 215 užtvindytas")
        }
        .padding(.bottom, 2)
    }

    private func legendItem(_ color: Color, _ text: String) -> some View {
        HStack(spacing: 4) {
            Circle()
                .fill(color)
                .frame(width: 8, height: 8)
            Text(text)
                .font(.caption2)
                .foregroundStyle(.white.opacity(0.9))
        }
    }
}