import Foundation

/// Fetches the live hydrology reading for the Jūra–Ringių k station from
/// meteo.lt. The page https://www.meteo.lt/dabar/vandens-telkiniai/ embeds an
/// hourly snapshot series as JSON inside `const meteoHydroMaps = {...}`; the
/// latest snapshot holds the station's current water level and temperature.
struct MeteoService {
    static let pageURL = URL(string: "https://www.meteo.lt/dabar/vandens-telkiniai/")!
    static let stationSlug = "jura-ringiu-k"

    func fetchLatestReading() async throws -> Reading {
        var request = URLRequest(url: Self.pageURL)
        request.cachePolicy = .reloadIgnoringLocalCacheData
        request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
                         forHTTPHeaderField: "User-Agent")
        request.timeoutInterval = 20

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw URLError(.badServerResponse)
        }

        let html = String(decoding: data, as: UTF8.self)
        guard let json = Self.extractHydroJSON(from: html) else {
            throw URLError(.cannotParseResponse)
        }
        guard let reading = Self.latestReading(slug: Self.stationSlug, json: json) else {
            throw URLError(.cannotParseResponse)
        }
        return reading
    }

    /// Brace-matches the full JSON object that follows `const meteoHydroMaps =`.
    static func extractHydroJSON(from html: String) -> Data? {
        guard let marker = html.range(of: "const meteoHydroMaps =") else { return nil }
        let tail = html[marker.upperBound...]
        guard let start = tail.firstIndex(of: "{") else { return nil }

        var depth = 0
        var inString = false
        var escaped = false
        var index = start

        while index < tail.endIndex {
            let ch = tail[index]
            if inString {
                if escaped {
                    escaped = false
                } else if ch == "\\" {
                    escaped = true
                } else if ch == "\"" {
                    inString = false
                }
            } else if ch == "\"" {
                inString = true
            } else if ch == "{" {
                depth += 1
            } else if ch == "}" {
                depth -= 1
                if depth == 0 {
                    return Data(String(tail[start...index]).utf8)
                }
            }
            index = tail.index(after: index)
        }
        return nil
    }

    /// Finds the timestamp-keyed series (e.g. "2026-10-16 15:00:00") with the
    /// most entries and returns the reading for `slug` from the newest key.
    static func latestReading(slug: String, json: Data) -> Reading? {
        guard let root = try? JSONSerialization.jsonObject(with: json) else { return nil }
        var candidates: [[String: Any]] = []
        collectTimeKeyedDicts(from: root, into: &candidates)
        guard let series = candidates.max(by: { $0.count < $1.count }) else { return nil }
        guard let latestKey = series.keys.sorted().last,
              let stations = series[latestKey] as? [[String: Any]] else { return nil }

        for station in stations where station["slug"] as? String == slug {
            let water = station["water"] as? [String: Any] ?? [:]
            let level = number(water["level"])
            let temp = number(water["temp"])
            let updatedAt = station["updatedAt"] as? String ?? latestKey
            return Reading(levelCM: level, tempC: temp, updatedAt: updatedAt)
        }
        return nil
    }

    private static func number(_ value: Any?) -> Double? {
        if let s = value as? String {
            return Double(s.replacingOccurrences(of: ",", with: "."))
        }
        if let n = value as? NSNumber { return n.doubleValue }
        return nil
    }

    private static func collectTimeKeyedDicts(from value: Any, into results: inout [[String: Any]]) {
        if let dict = value as? [String: Any], !dict.isEmpty {
            let isTimeKeyed = dict.keys.allSatisfy {
                $0.range(of: #"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$",
                         options: .regularExpression) != nil
            }
            if isTimeKeyed {
                results.append(dict)
                return
            }
            for child in dict.values {
                collectTimeKeyedDicts(from: child, into: &results)
            }
        } else if let array = value as? [Any] {
            for child in array {
                collectTimeKeyedDicts(from: child, into: &results)
            }
        }
    }
}