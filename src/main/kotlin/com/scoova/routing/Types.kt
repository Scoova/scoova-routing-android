package com.scoova.routing

data class LatLng(val lat: Double, val lon: Double)

enum class CostingType(val wire: String) {
    AUTO("auto"),
    BICYCLE("bicycle"),
    SCOOTER("scooter"),
    PEDESTRIAN("pedestrian"),
    TRUCK("truck"),
    MOTORCYCLE("motorcycle"),
    MOTOR_SCOOTER("motor_scooter"),
}

enum class Units(val wire: String) { KILOMETERS("kilometers"), MILES("miles") }

/**
 * Per-call route options.
 *
 *  @param language Sent as `directions_options.language`. When `null`, the
 *                  effective locale (per-call → client → `"en"`) is used.
 *  @param locale   Per-call override for the gateway locale. Sent as the
 *                  `?locale=` query parameter and the `Accept-Language`
 *                  header for this single request.
 */
data class RouteOptions(
    val costing: CostingType? = null,
    val language: String? = null,
    val locale: String? = null,
    val units: Units = Units.KILOMETERS,
    val alternates: Int? = null,
    val simplifiedInstructions: Boolean = false,
)

data class IsochroneContour(val timeMin: Double? = null, val distanceKm: Double? = null)

data class IsochroneOptions(
    val contours: List<IsochroneContour>,
    val costing: CostingType? = null,
    val polygons: Boolean = true,
    /** Per-call locale override. */
    val locale: String? = null,
)

sealed class RoutingException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Http(val statusCode: Int, body: String) : RoutingException("HTTP $statusCode: ${body.take(200)}")
    class Decode(message: String) : RoutingException(message)
    class Transport(cause: Throwable) : RoutingException(cause.message ?: "transport error", cause)
}
