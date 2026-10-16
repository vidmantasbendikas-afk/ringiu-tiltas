package lt.ringiu.tiltas.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object MeteoService {
    const val PAGE_URL = "https://www.meteo.lt/dabar/vandens-telkiniai/"
    const val STATION_SLUG = "jura-ringiu-k"

    private val TIME_KEY = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")

    fun fetchLatestReading(): Reading {
        val html = downloadPage()
        val json = extractHydroJSON(html)
            ?: throw IOException("Nepavyko rasti meteoHydroMaps duomenų")
        return latestReading(STATION_SLUG, json)
            ?: throw IOException("Stoties $STATION_SLUG duomenų nėra")
    }

    private fun downloadPage(): String {
        val connection = URL(PAGE_URL).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 20_000
            connection.readTimeout = 20_000
            connection.setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)",
            )
            if (connection.responseCode !in 200..299) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    fun extractHydroJSON(html: String): JSONObject? {
        val marker = "const meteoHydroMaps ="
        val markerIndex = html.indexOf(marker)
        if (markerIndex == -1) return null
        val tail = html.substring(markerIndex + marker.length)
        val start = tail.indexOf('{')
        if (start == -1) return null

        var depth = 0
        var inString = false
        var escaped = false
        var end = -1
        for (index in start until tail.length) {
            val ch = tail[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    ch == '\\' -> escaped = true
                    ch == '"' -> inString = false
                }
            } else {
                when (ch) {
                    '"' -> inString = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            end = index
                            break
                        }
                    }
                }
            }
        }
        if (end == -1) return null
        return try {
            JSONObject(tail.substring(start, end + 1))
        } catch (e: JSONException) {
            null
        }
    }

    fun latestReading(slug: String, json: JSONObject): Reading? {
        val candidates = mutableListOf<JSONObject>()
        collectTimeKeyedDicts(json, candidates)
        val series = candidates.maxByOrNull { it.length() } ?: return null
        val latestKey = series.keys().asSequence().sorted().lastOrNull() ?: return null
        val stations = series.optJSONArray(latestKey) ?: return null

        for (i in 0 until stations.length()) {
            val station = stations.optJSONObject(i) ?: continue
            if (station.optString("slug") != slug) continue
            val water = station.optJSONObject("water")
            val level = number(water?.opt("level"))
            val temp = number(water?.opt("temp"))
            val updatedAt = (station.opt("updatedAt") as? String) ?: latestKey
            return Reading(levelCM = level, tempC = temp, updatedAt = updatedAt)
        }
        return null
    }

    private fun number(value: Any?): Double? = when (value) {
        is String -> value.replace(",", ".").toDoubleOrNull()
        is Number -> value.toDouble()
        else -> null
    }

    private fun collectTimeKeyedDicts(value: Any?, results: MutableList<JSONObject>) {
        when (value) {
            is JSONObject -> {
                if (value.length() == 0) return
                val isTimeKeyed = value.keys().asSequence().all { TIME_KEY.matches(it) }
                if (isTimeKeyed) {
                    results.add(value)
                    return
                }
                for (key in value.keys().asSequence()) {
                    collectTimeKeyedDicts(value.opt(key), results)
                }
            }
            is JSONArray -> {
                for (i in 0 until value.length()) {
                    collectTimeKeyedDicts(value.opt(i), results)
                }
            }
        }
    }
}
