# CommuneX map routing: DSA-backed routes with a road-routing fallback

The existing map UI, map tiles, markers, place search, route-mode buttons, route line styling, and the Google Maps directions link are retained.

## How route calculation works

1. `MapExplorer.jsx` calls `fetchAllRoutes()` in `frontend/src/features/map/routing.js`.
2. The frontend first requests `GET /api/map/route` with the start and destination coordinates.
3. `DsaMapRoutingService` retrieves nearby road nodes and road ways from OpenStreetMap's Overpass service.
4. CommuneX builds a weighted adjacency-list graph from road segments and calculates routes with `DijkstraShortestPath` and `BinaryMinHeap`. Car routing respects mapped one-way streets and motor-vehicle access; walking has its own road/path filter.
5. If the local route endpoint fails, times out, or returns invalid geometry, the frontend asks the public Valhalla OpenStreetMap router for a profile-specific road route (`auto`, `motorcycle`, or `pedestrian`). The fallback returns GeoJSON road geometry for MapLibre rather than connecting the endpoints with a straight line.
6. A straight-line estimate is no longer automatically substituted for a failed route. If both route services fail, the UI shows the error and keeps the Google Maps directions link available.

Motorbike time is an estimate, traffic and closures are not included, and the fallback router's travel profile may produce a route that differs from CommuneX's Dijkstra result. The public Valhalla demo service is rate-limited and intended for reasonable demo usage; production deployments should use a properly provisioned or self-hosted routing service.

## Important distinction

CommuneX's local graph + Dijkstra + binary min-heap remains the primary route calculator. Valhalla is only a road-following fallback, used when the local route cannot be returned. The separate “Open in Google Maps” link remains an optional external turn-by-turn navigation shortcut.
