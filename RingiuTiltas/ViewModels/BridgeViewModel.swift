import Foundation

@MainActor
final class BridgeViewModel: ObservableObject {
    @Published private(set) var reading: Reading?
    @Published private(set) var isLoading = false
    @Published private(set) var errorMessage: String?

    var state: BridgeState {
        LevelThresholds.state(forLevel: reading?.levelCM)
    }

    private let service = MeteoService()

    func refresh() async {
        isLoading = true
        defer { isLoading = false }
        do {
            reading = try await service.fetchLatestReading()
            errorMessage = nil
        } catch {
            if reading == nil {
                errorMessage = "Nepavyko gauti duomenų iš meteo.lt"
            }
        }
    }
}