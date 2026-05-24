package com.scoova.routing

import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val OK_TRIP = """{"trip":{}}"""

class RoutingClientTest {
    @Test fun routeHitsRouteWithSaneDefaults() = runTest {
        var capturedMethod = ""
        var capturedUrl = ""
        var capturedHeaders: Map<String, String> = emptyMap()
        var capturedBody = ""
        val client = RoutingClient(baseUrl = "https://example.test", http = { method, url, headers, body ->
            capturedMethod = method
            capturedUrl = url
            capturedHeaders = headers
            capturedBody = body ?: ""
            200 to OK_TRIP
        })
        client.route(listOf(LatLng(30.0, 31.0), LatLng(31.0, 32.0)))
        assertEquals("POST", capturedMethod)
        assertTrue(capturedUrl.startsWith("https://example.test/route?"))
        assertTrue(capturedUrl.contains("locale=en"))
        assertEquals("en", capturedHeaders["Accept-Language"])
        val body = JSONObject(capturedBody)
        assertEquals(2, body.getJSONArray("locations").length())
        assertEquals("scooter", body.getString("costing"))
        assertEquals("en", body.getJSONObject("directions_options").getString("language"))
    }

    @Test fun respectsCostingLanguageAlternates() = runTest {
        var capturedBody = ""
        val client = RoutingClient(baseUrl = "https://example.test", http = { _, _, _, body ->
            capturedBody = body ?: ""
            200 to OK_TRIP
        })
        client.route(
            locations = listOf(LatLng(30.0, 31.0), LatLng(31.0, 32.0)),
            options = RouteOptions(
                costing = CostingType.PEDESTRIAN,
                language = "ar-EG",
                alternates = 2,
                simplifiedInstructions = true,
            ),
        )
        val body = JSONObject(capturedBody)
        assertEquals("pedestrian", body.getString("costing"))
        assertEquals("ar-EG", body.getJSONObject("directions_options").getString("language"))
        assertEquals(2, body.getInt("alternates"))
        assertEquals(true, body.getBoolean("simplified_instructions"))
    }

    @Test fun clientLocaleFlowsIntoUrlHeaderAndDirectionsOptions() = runTest {
        var capturedUrl = ""
        var capturedHeaders: Map<String, String> = emptyMap()
        var capturedBody = ""
        val client = RoutingClient(
            baseUrl = "https://example.test",
            locale = "fr",
            http = { _, url, headers, body ->
                capturedUrl = url; capturedHeaders = headers; capturedBody = body ?: ""
                200 to OK_TRIP
            },
        )
        client.route(listOf(LatLng(30.0, 31.0), LatLng(31.0, 32.0)))
        assertTrue(capturedUrl.contains("locale=fr"))
        assertEquals("fr", capturedHeaders["Accept-Language"])
        assertEquals("fr", JSONObject(capturedBody).getJSONObject("directions_options").getString("language"))
    }

    @Test fun perCallLocaleOverridesClientDefault() = runTest {
        var capturedUrl = ""
        var capturedHeaders: Map<String, String> = emptyMap()
        var capturedBody = ""
        val client = RoutingClient(
            baseUrl = "https://example.test",
            locale = "fr",
            http = { _, url, headers, body ->
                capturedUrl = url; capturedHeaders = headers; capturedBody = body ?: ""
                200 to OK_TRIP
            },
        )
        client.route(listOf(LatLng(30.0, 31.0), LatLng(31.0, 32.0)),
            options = RouteOptions(locale = "ar-EG"))
        assertTrue(capturedUrl.contains("locale=ar-EG"))
        assertEquals("ar-EG", capturedHeaders["Accept-Language"])
        assertEquals("ar-EG", JSONObject(capturedBody).getJSONObject("directions_options").getString("language"))
    }

    @Test fun apiKeyFlowsIntoHeader() = runTest {
        var capturedHeaders: Map<String, String> = emptyMap()
        val client = RoutingClient(
            baseUrl = "https://example.test",
            apiKey = "demo",
            http = { _, _, headers, _ -> capturedHeaders = headers; 200 to OK_TRIP },
        )
        client.route(listOf(LatLng(30.0, 31.0), LatLng(31.0, 32.0)))
        assertEquals("demo", capturedHeaders["X-API-Key"])
    }

    @Test fun matrixHitsSourcesToTargets() = runTest {
        var capturedUrl = ""
        val client = RoutingClient(baseUrl = "https://example.test", http = { _, url, _, _ ->
            capturedUrl = url
            200 to "{}"
        })
        client.matrix(
            sources = listOf(LatLng(30.0, 31.0)),
            targets = listOf(LatLng(31.0, 32.0)),
        )
        assertTrue(capturedUrl.startsWith("https://example.test/sources_to_targets?"))
    }

    @Test fun isochroneHitsIsochroneWithContours() = runTest {
        var capturedBody = ""
        val client = RoutingClient(baseUrl = "https://example.test", http = { _, _, _, body ->
            capturedBody = body ?: ""
            200 to "{}"
        })
        client.isochrone(
            location = LatLng(30.0, 31.0),
            options = IsochroneOptions(contours = listOf(IsochroneContour(timeMin = 5.0))),
        )
        val body = JSONObject(capturedBody)
        val contours = body.getJSONArray("contours")
        assertEquals(1, contours.length())
        assertEquals(5.0, contours.getJSONObject(0).getDouble("time"))
        assertTrue(body.getBoolean("polygons"))
    }

    @Test fun elevationIsAliasForHeight() = runTest {
        var capturedUrl = ""
        val client = RoutingClient(baseUrl = "https://example.test", http = { _, url, _, _ ->
            capturedUrl = url; 200 to "{}"
        })
        client.elevation(listOf(LatLng(30.0, 31.0)))
        assertTrue(capturedUrl.startsWith("https://example.test/height?"))
    }

    @Test fun mapMatchHitsTraceRoute() = runTest {
        var capturedUrl = ""
        var capturedBody = ""
        val client = RoutingClient(baseUrl = "https://example.test", http = { _, url, _, body ->
            capturedUrl = url; capturedBody = body ?: ""; 200 to OK_TRIP
        })
        client.mapMatch(listOf(LatLng(30.0, 31.0), LatLng(31.0, 32.0)))
        assertTrue(capturedUrl.startsWith("https://example.test/trace_route?"))
        assertEquals("map_snap", JSONObject(capturedBody).getString("shape_match"))
    }

    @Test fun locateHitsLocate() = runTest {
        var capturedUrl = ""
        val client = RoutingClient(baseUrl = "https://example.test", http = { _, url, _, _ ->
            capturedUrl = url; 200 to "{}"
        })
        client.locate(listOf(LatLng(30.0, 31.0)))
        assertTrue(capturedUrl.startsWith("https://example.test/locate?"))
    }

    @Test fun statusHitsStatus() = runTest {
        var capturedMethod = ""
        var capturedUrl = ""
        val client = RoutingClient(baseUrl = "https://example.test", http = { m, url, _, _ ->
            capturedMethod = m; capturedUrl = url; 200 to "{}"
        })
        client.status()
        assertEquals("GET", capturedMethod)
        assertTrue(capturedUrl.startsWith("https://example.test/status?"))
    }

    @Test fun throwsOnNon2xx() = runTest {
        val client = RoutingClient(baseUrl = "https://example.test", http = { _, _, _, _ -> 502 to "boom" })
        val ex = assertFailsWith<RoutingException.Http> {
            client.route(listOf(LatLng(30.0, 31.0), LatLng(31.0, 32.0)))
        }
        assertEquals(502, ex.statusCode)
    }
}

class PolylineTest {
    @Test fun decodesCanonicalFixture() {
        val coords = decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@")
        assertEquals(3, coords.size)
    }
}
