# scoova-routing-android

Standalone Valhalla routing client for `routing.scoo-va.info`. JVM library —
works in Android apps **and** server-side Kotlin / KMP projects.

## Install

JitPack:

```kotlin
repositories { maven { url = uri("https://jitpack.io") } }
dependencies { implementation("com.github.Scoova:scoova-routing-android:1.1.0") }
```

Maven Central (once the namespace claim is complete):

```kotlin
dependencies { implementation("info.scoo-va:scoova-routing:1.1.0") }
```

## Usage

```kotlin
import com.scoova.routing.*

val client = RoutingClient(
    locale = "ar-EG",                       // every request gets ?locale=ar-EG + Accept-Language
    apiKey = System.getenv("SCOOVA_API_KEY"),
)

val result = client.route(
    locations = listOf(LatLng(30.04, 31.24), LatLng(30.06, 31.25)),
    options = RouteOptions(costing = CostingType.SCOOTER),
)
val path = decodePolyline(result.getJSONArray("trip")
    .getJSONObject(0).getString("shape"))
```

## Endpoints

`route`, `optimizedRoute`, `isochrone`, `matrix`, `height` (alias `elevation`),
`mapMatch`, `locate`, `status`.

## Locale

Pass a `locale` once on the client and every call carries it as both the
`?locale=` query parameter and the `Accept-Language` HTTP header. Per-call
`RouteOptions.locale` overrides the client default. The server falls back to
`en` for any unsupported code.

## Build + test

```sh
gradle build --quiet --no-daemon
gradle test  --quiet --no-daemon
```

Repo: <https://github.com/Scoova/scoova-routing-android>.
License: Apache-2.0.
