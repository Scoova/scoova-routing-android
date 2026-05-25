# Changelog

All notable changes to `info.scoo-va:scoova-routing` are recorded here.
This project follows [Semantic Versioning](https://semver.org/).

## 1.1.1 — 2026-05-25
- Default `baseUrl` switched from the retired `https://routing.scoo-va.info` subdomain to the central gateway at `https://api.scoo-va.info/api/v1/routing`. Callers who explicitly set `baseUrl` are unaffected. The old subdomain returns `ENDPOINT_RETIRED`.

## 1.1.0 — 2026-05-25

### Added

- **Locale support on `RoutingClient`** — pass `locale = "fr"` /
  `"ar-EG"` / `"pt-BR"` once at construction and every request carries it
  as both the `?locale=` query parameter and the `Accept-Language` header.
  Per-call `RouteOptions.locale` / `IsochroneOptions.locale` overrides the
  client default. Default `"en"`.
- **`apiKey` constructor argument** — sent as `X-API-Key` when set, for
  calls routed through the `api.scoo-va.info/v1/routing/*` gateway.
- **`elevation(shape, range?)`** — alias for `height()`, matching the
  unified SDK naming.

### Changed

- **`RoutingHttp` typealias** now takes 4 args
  `(method, url, headers, body)` — the new `headers` map lets the pluggable
  HTTP layer see the `Accept-Language` + `X-API-Key` the client wants to
  send. Anyone implementing a custom `http` lambda must update the
  signature; the headers can simply be merged onto whatever request the
  custom layer builds.

### Endpoints (verified parity across all 5 platforms)

`route`, `optimizedRoute`, `isochrone`, `matrix`, `height` (alias `elevation`),
`mapMatch`, `locate`, `status`.

### Other

- License changed to Apache-2.0.
- Repository URL is now `https://github.com/Scoova/scoova-routing-android`.
- Publishing block (JitPack + GitHub Packages + Maven Central via Sonatype
  OSSRH s01) added — see `build.gradle.kts`. Artifact coordinate is
  `info.scoo-va:scoova-routing:1.1.0`.

## 1.0.0 — 2026-05-04

First public release. Routing client for
`routing.scoo-va.info` with the eight endpoints listed above and a built-in
polyline6 decoder.
