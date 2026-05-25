package com.scoova.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Pluggable HTTP fetcher: takes (method, url, headers, body) → (statusCode, body). */
typealias RoutingHttp = suspend (
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String?,
) -> Pair<Int, String>

/**
 * Standalone Valhalla routing client for the Scoova routing gateway
 * (`api.scoo-va.info/api/v1/routing`).
 *
 * Eight endpoints: route, optimizedRoute, isochrone, matrix, height
 * (alias `elevation`), mapMatch, locate, status.
 *
 * Pass [locale] (e.g. `"fr"`, `"ar-EG"`, `"pt-BR"`) once and every request
 * carries it as both the `?locale=` query parameter and the `Accept-Language`
 * header. Per-call [RouteOptions.locale] / [IsochroneOptions.locale] overrides.
 * Default `"en"`. Pass [apiKey] — required by the gateway — sent as
 * `X-API-Key` on every request.
 */
class RoutingClient(
    baseUrl: String = "https://api.scoo-va.info/api/v1/routing",
    private val defaultCosting: CostingType = CostingType.SCOOTER,
    private val timeoutMs: Int = 30_000,
    private val locale: String = "en",
    private val apiKey: String? = null,
    private val http: RoutingHttp? = null,
) {
    private val baseUrl: String = baseUrl.trimEnd('/')

    suspend fun route(locations: List<LatLng>, options: RouteOptions = RouteOptions()): JSONObject {
        val effectiveLocale = options.locale ?: locale
        val body = JSONObject().apply {
            put("locations", JSONArray().apply {
                for (l in locations) put(JSONObject().put("lat", l.lat).put("lon", l.lon))
            })
            put("costing", (options.costing ?: defaultCosting).wire)
            put("directions_options", JSONObject()
                .put("units", options.units.wire)
                .put("language", options.language ?: effectiveLocale))
            if (options.simplifiedInstructions) put("simplified_instructions", true)
            options.alternates?.let { put("alternates", it) }
        }
        return post("/route", body, effectiveLocale)
    }

    suspend fun optimizedRoute(locations: List<LatLng>, options: RouteOptions = RouteOptions()): JSONObject {
        val effectiveLocale = options.locale ?: locale
        val body = JSONObject().apply {
            put("locations", JSONArray().apply {
                for (l in locations) put(JSONObject().put("lat", l.lat).put("lon", l.lon))
            })
            put("costing", (options.costing ?: defaultCosting).wire)
            put("directions_options", JSONObject()
                .put("units", options.units.wire)
                .put("language", options.language ?: effectiveLocale))
        }
        return post("/optimized_route", body, effectiveLocale)
    }

    suspend fun isochrone(location: LatLng, options: IsochroneOptions): JSONObject {
        val effectiveLocale = options.locale ?: locale
        val body = JSONObject().apply {
            put("locations", JSONArray().put(JSONObject().put("lat", location.lat).put("lon", location.lon)))
            put("costing", (options.costing ?: defaultCosting).wire)
            put("contours", JSONArray().apply {
                for (c in options.contours) {
                    val o = JSONObject()
                    c.timeMin?.let { o.put("time", it) }
                    c.distanceKm?.let { o.put("distance", it) }
                    put(o)
                }
            })
            put("polygons", options.polygons)
        }
        return post("/isochrone", body, effectiveLocale)
    }

    suspend fun matrix(
        sources: List<LatLng>,
        targets: List<LatLng>,
        costing: CostingType = CostingType.SCOOTER,
    ): JSONObject {
        val body = JSONObject().apply {
            put("sources", JSONArray().apply {
                for (l in sources) put(JSONObject().put("lat", l.lat).put("lon", l.lon))
            })
            put("targets", JSONArray().apply {
                for (l in targets) put(JSONObject().put("lat", l.lat).put("lon", l.lon))
            })
            put("costing", costing.wire)
        }
        return post("/sources_to_targets", body)
    }

    suspend fun height(shape: List<LatLng>, range: Boolean = true): JSONObject {
        val body = JSONObject().apply {
            put("shape", JSONArray().apply {
                for (l in shape) put(JSONObject().put("lat", l.lat).put("lon", l.lon))
            })
            put("range", range)
        }
        return post("/height", body)
    }

    /** Alias for [height] — matches the unified SDK naming. */
    suspend fun elevation(shape: List<LatLng>, range: Boolean = true): JSONObject =
        height(shape, range)

    suspend fun mapMatch(shape: List<LatLng>, costing: CostingType = CostingType.SCOOTER): JSONObject {
        val body = JSONObject().apply {
            put("shape", JSONArray().apply {
                for (l in shape) put(JSONObject().put("lat", l.lat).put("lon", l.lon))
            })
            put("costing", costing.wire)
            put("shape_match", "map_snap")
        }
        return post("/trace_route", body)
    }

    suspend fun locate(locations: List<LatLng>, costing: CostingType = CostingType.SCOOTER): JSONObject {
        val body = JSONObject().apply {
            put("locations", JSONArray().apply {
                for (l in locations) put(JSONObject().put("lat", l.lat).put("lon", l.lon))
            })
            put("costing", costing.wire)
        }
        return post("/locate", body)
    }

    suspend fun status(): JSONObject = get("/status")

    // ─── internals ────────────────────────────────────────────────────────

    private val defaultHttp: RoutingHttp = { method, url, headers, body ->
        withContext(Dispatchers.IO) {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                setRequestProperty("Accept", "application/json")
                for ((k, v) in headers) setRequestProperty(k, v)
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                if (body != null) {
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                }
            }
            try {
                if (body != null) conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                code to text
            } finally {
                conn.disconnect()
            }
        }
    }

    private fun headersFor(perCallLocale: String?): Map<String, String> {
        val h = mutableMapOf<String, String>()
        h["Accept-Language"] = perCallLocale ?: locale
        apiKey?.let { h["X-API-Key"] = it }
        return h
    }

    private fun urlFor(path: String, perCallLocale: String?): String {
        val effective = perCallLocale ?: locale
        val sep = if (path.contains('?')) '&' else '?'
        return "$baseUrl$path${sep}locale=${URLEncoder.encode(effective, "UTF-8")}"
    }

    private suspend fun post(path: String, body: JSONObject, perCallLocale: String? = null): JSONObject =
        request("POST", path, body.toString(), perCallLocale)

    private suspend fun get(path: String, perCallLocale: String? = null): JSONObject =
        request("GET", path, null, perCallLocale)

    private suspend fun request(method: String, path: String, body: String?, perCallLocale: String?): JSONObject {
        val url = urlFor(path, perCallLocale)
        val headers = headersFor(perCallLocale)
        val (code, text) = try {
            (http ?: defaultHttp).invoke(method, url, headers, body)
        } catch (t: Throwable) {
            throw RoutingException.Transport(t)
        }
        if (code !in 200..299) throw RoutingException.Http(code, text)
        return try {
            JSONObject(text)
        } catch (t: Throwable) {
            throw RoutingException.Decode("Invalid JSON: ${t.message}")
        }
    }
}

/** Decode a Valhalla polyline6 string. */
fun decodePolyline(encoded: String, precision: Int = 6): List<LatLng> {
    val coords = mutableListOf<LatLng>()
    val factor = Math.pow(10.0, precision.toDouble())
    var index = 0
    var lat = 0
    var lon = 0
    while (index < encoded.length) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lon += if (result and 1 != 0) (result shr 1).inv() else result shr 1
        coords.add(LatLng(lat / factor, lon / factor))
    }
    return coords
}
