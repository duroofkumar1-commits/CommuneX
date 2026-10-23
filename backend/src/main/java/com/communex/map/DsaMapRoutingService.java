package com.communex.map;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.communex.dsa.graph.DijkstraShortestPath;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class DsaMapRoutingService {

    // Public Overpass mirrors. Avoid the maps.mail.ru mirror because its TLS
    // certificate failed validation in the configured Java runtime.
    private static final List<String> OVERPASS = List.of(
        "https://overpass-api.de/api/interpreter",
        "https://lz4.overpass-api.de/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"

    );

    // POI/building lookups use the same tested public mirrors. This is a
    // server-to-server request, so browser CORS settings do not apply.
    private static final List<String> MAP_DATA_OVERPASS = List.of(
        "https://overpass-api.de/api/interpreter",
        "https://lz4.overpass-api.de/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter"


    );

    private static final String START = "__START__";
    private static final String TARGET = "__TARGET__";

    private static final int MAX_ELEMENTS = 180_000;
    private static final double MAX_ROUTE_KM = 160;
    private static final double MAX_SNAP_KM = 1.5;
    private static final int SNAP_CANDIDATES = 8;

    private static final Set<String> RESTRICTED =
        Set.of("no", "private", "agricultural", "forestry");

    // Keep query fragments server-owned: clients can select only known map layers.
    private static final Map<String, List<String>> POI_FILTERS = Map.ofEntries(
        Map.entry("hospital", List.of("nwr[\"amenity\"=\"hospital\"]")),
        Map.entry("clinic", List.of("nwr[\"amenity\"~\"^(clinic|doctors)$\"]")),
        Map.entry("pharmacy", List.of("nwr[\"amenity\"=\"pharmacy\"]", "nwr[\"shop\"=\"chemist\"]")),
        Map.entry("mall", List.of("nwr[\"shop\"=\"mall\"]")),
        Map.entry("shop", List.of("nwr[\"shop\"]", "nwr[\"amenity\"=\"marketplace\"]")),
        Map.entry("worship", List.of("nwr[\"amenity\"=\"place_of_worship\"]")),
        Map.entry("police", List.of("nwr[\"amenity\"=\"police\"]")),
        Map.entry("fire", List.of("nwr[\"amenity\"=\"fire_station\"]")),
        Map.entry("school", List.of("nwr[\"amenity\"~\"^(school|college|university)$\"]")),
        Map.entry("bank", List.of("nwr[\"amenity\"~\"^(bank|atm)$\"]")),
        Map.entry("fuel", List.of("nwr[\"amenity\"=\"fuel\"]")),
        Map.entry("food", List.of("nwr[\"amenity\"~\"^(restaurant|fast_food|cafe)$\"]"))
    );

    private final ObjectMapper mapper;
    private final HttpClient client;

    public DsaMapRoutingService(ObjectMapper mapper) {
        this.mapper = mapper;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public RouteResponse route(
        double fromLat, double fromLng,
        double toLat, double toLng
    ) {
        validate(fromLat, fromLng);
        validate(toLat, toLng);

        Coordinate from = new Coordinate(fromLat, fromLng);
        Coordinate to = new Coordinate(toLat, toLng);
        double direct = distance(from, to);

        if (direct < 0.001) {
            throw error("Start and destination must be different.",
                HttpStatus.BAD_REQUEST);
        }

        if (direct > MAX_ROUTE_KM) {
            throw error("Road routing is limited to 160 km.",
                HttpStatus.BAD_REQUEST);
        }

        RoadNetwork network = loadRoadNetwork(from, to, direct);

        RouteOption car = calculate(network, from, to, Mode.CAR);
        RouteOption bike = calculate(network, from, to, Mode.BIKE);
        RouteOption foot = calculate(network, from, to, Mode.FOOT);

        return new RouteResponse(
            car, bike, foot,
            "OpenStreetMap + CommuneX Dijkstra + BinaryMinHeap"
        );
    }

    /** Fetch POIs/buildings using validated request fields and server-owned QL. */
    public JsonNode fetchMapData(MapOverpassRequest request) {
        // The frontend axios timeout is 50 seconds; keep this bounded below it.
        return fetch(buildMapDataQuery(request), Duration.ofSeconds(18), "map-data");
    }

    private String buildMapDataQuery(MapOverpassRequest request) {
        if (request == null || request.bbox() == null) {
            throw error("A map bounding box is required.", HttpStatus.BAD_REQUEST);
        }

        MapOverpassRequest.BoundingBox box = request.bbox();
        double south = box.south();
        double west = box.west();
        double north = box.north();
        double east = box.east();
        if (!Double.isFinite(south) || !Double.isFinite(west)
            || !Double.isFinite(north) || !Double.isFinite(east)
            || south < -90 || north > 90 || west < -180 || east > 180
            || south >= north || west >= east
            || north - south > 3 || east - west > 3) {
            throw error("The map bounding box is invalid or too large.",
                HttpStatus.BAD_REQUEST);
        }

        String bbox = String.format(Locale.ROOT,
            "%.5f,%.5f,%.5f,%.5f", south, west, north, east);
        if ("buildings".equals(request.type())) {
            return "[out:json][timeout:25];way[\"building\"](" + bbox
                + ");out geom 1500;";
        }

        if (!"pois".equals(request.type())) {
            throw error("Map data type must be 'pois' or 'buildings'.",
                HttpStatus.BAD_REQUEST);
        }

        List<String> requested = request.categories() == null
            ? List.of()
            : new ArrayList<>(new LinkedHashSet<>(request.categories()));
        if (requested.isEmpty() || requested.size() > POI_FILTERS.size()) {
            throw error("Select one or more valid map categories.",
                HttpStatus.BAD_REQUEST);
        }

        StringBuilder query = new StringBuilder("[out:json][timeout:25];(");
        for (String category : requested) {
            List<String> filters = category == null
                ? null : POI_FILTERS.get(category);
            if (filters == null) {
                throw error("Unsupported map category: " + category,
                    HttpStatus.BAD_REQUEST);
            }
            for (String filter : filters) {
                query.append(filter).append('(').append(bbox).append(");");
            }
        }
        return query.append(");out tags center 2000;").toString();
    }

    private RoadNetwork loadRoadNetwork(
        Coordinate from, Coordinate to, double direct
    ) {
        double midLat = (from.lat + to.lat) / 2;
        double padKm = Math.min(20, Math.max(3, direct * 0.25));
        double latPad = padKm / 111.32;
        double cos = Math.max(0.15,
            Math.cos(Math.toRadians(midLat)));
        double lngPad = padKm / (111.32 * cos);

        double south = Math.max(-90,
            Math.min(from.lat, to.lat) - latPad);
        double north = Math.min(90,
            Math.max(from.lat, to.lat) + latPad);
        double west = Math.max(-180,
            Math.min(from.lng, to.lng) - lngPad);
        double east = Math.min(180,
            Math.max(from.lng, to.lng) + lngPad);

        String bbox = String.format(Locale.ROOT,
            "%.6f,%.6f,%.6f,%.6f",
            south, west, north, east);

        String query = "[out:json][timeout:25];"
            + "(way[highway](" + bbox + "););"
            + "out body;>;out skel qt;";

        JsonNode elements = fetch(query).path("elements");

        if (!elements.isArray() || elements.isEmpty()) {
            throw error("No mapped roads were found.",
                HttpStatus.SERVICE_UNAVAILABLE);
        }

        if (elements.size() > MAX_ELEMENTS) {
            throw error("Too many road elements. Choose closer points.",
                HttpStatus.SERVICE_UNAVAILABLE);
        }

        Map<String, Coordinate> coordinates = new HashMap<>();
        List<RoadWay> ways = new ArrayList<>();

        for (JsonNode element : elements) {
            String type = element.path("type").asText();

            if ("node".equals(type)
                && element.has("lat") && element.has("lon")) {

                coordinates.put(element.path("id").asText(),
                    new Coordinate(
                        element.path("lat").asDouble(),
                        element.path("lon").asDouble()
                    ));

            } else if ("way".equals(type)
                && element.path("nodes").isArray()) {

                String highway =
                    element.path("tags").path("highway").asText("");

                if (highway.isBlank()) continue;

                Map<String, String> tags = new HashMap<>();
                element.path("tags").fields().forEachRemaining(e ->
                    tags.put(e.getKey(), e.getValue().asText()));

                List<String> ids = new ArrayList<>();
                for (JsonNode node : element.path("nodes")) {
                    ids.add(node.asText());
                }

                if (ids.size() >= 2) {
                    ways.add(new RoadWay(ids, tags));
                }
            }
        }

        if (ways.isEmpty() || coordinates.isEmpty()) {
            throw error("Could not construct the road graph.",
                HttpStatus.SERVICE_UNAVAILABLE);
        }

        return new RoadNetwork(coordinates, ways);
    }

    private JsonNode fetch(String query) {
        return fetch(query, Duration.ofSeconds(14), "road-network");
    }

    /**
     * Try configured public Overpass mirrors sequentially under one overall
     * deadline. This is more respectful to public instances than duplicating
     * every expensive road/POI query to all mirrors at once.
     */
    private JsonNode fetch(String query, Duration totalTimeout, String purpose) {
        String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        long deadline = System.nanoTime() + totalTimeout.toNanos();
        List<String> failures = new ArrayList<>();
        Throwable last = null;
        List<String> endpoints = "map-data".equals(purpose)
            ? MAP_DATA_OVERPASS
            : OVERPASS;

        for (int i = 0; i < endpoints.size(); i++) {
            long remainingNanos = deadline - System.nanoTime();
            if (remainingNanos <= 0) break;


            long remainingMs = Math.max(
                1,
                TimeUnitNanos.toMillis(remainingNanos)
            );

            // Allocate the remaining deadline fairly across remaining mirrors.
            // This keeps browser requests from being cancelled before the backend
            // finishes trying all known endpoints. A healthy mirror can still use
            // its fair share, while dead mirrors cannot consume the full deadline.
            int mirrorsLeft = endpoints.size() - i;
            long fairShareMs = Math.max(1_000L, remainingMs / mirrorsLeft);
            long attemptCapMs = "map-data".equals(purpose) ? 5_000L : 4_000L;
            long attemptMs = Math.min(Math.min(attemptCapMs, fairShareMs), remainingMs);
            String endpoint = endpoints.get(i);


            try {
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofMillis(attemptMs))
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("User-Agent", "CommuneX-MapExplorer/1.0 (local development)")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

                HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new IOException("HTTP " + response.statusCode() + bodySummary(response.body()));
                }

                JsonNode json = mapper.readTree(response.body());
                if (!json.path("elements").isArray() || json.has("remark")) {
                    throw new IOException("Overpass returned incomplete data" + bodySummary(response.body()));
                }
                return json;

            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw error("The OpenStreetMap " + purpose + " lookup was interrupted.",
                    HttpStatus.SERVICE_UNAVAILABLE);
            } catch (Exception failure) {
                last = failure;
                failures.add(endpoint + ": " + shortError(failure));
            }
        }

        String details = failures.isEmpty()
            ? "No mirror returned a response before the overall timeout."
            : String.join(" | ", failures);
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "OpenStreetMap Overpass " + purpose + " query failed. " + details,
            last);
    }

    private static String shortError(Throwable error) {
        if (error == null) return "unknown error";
        String message = error.getMessage();
        if (message == null || message.isBlank()) message = error.getClass().getSimpleName();
        String normalized = message.replaceAll("\\s+", " ").trim();
        return normalized.substring(0, Math.min(180, normalized.length()));
    }

    private static String bodySummary(String body) {
        if (body == null || body.isBlank()) return "";
        String normalized = body.replaceAll("\\s+", " ").trim();
        return ": " + normalized.substring(0, Math.min(100, normalized.length()));
    }

    /** Avoid an extra library dependency for converting a positive duration. */
    private static final class TimeUnitNanos {
        private TimeUnitNanos() {}
        static long toMillis(long nanos) { return nanos / 1_000_000L; }
    }

    private RouteOption calculate(
        RoadNetwork network,
        Coordinate from,
        Coordinate to,
        Mode mode
    ) {
        Map<String, List<DijkstraShortestPath.Edge>> graph =
            new HashMap<>();

        Map<String, Double> speeds = new HashMap<>();
        Map<String, Map<String, Integer>> edgePositions = new HashMap<>();
        Set<String> nodes = new HashSet<>();

        for (RoadWay way : network.ways) {
            if (!allowed(way.tags, mode)) continue;

            List<String> ids = way.ids;
            String oneway = way.tags.getOrDefault("oneway", "")
                .toLowerCase(Locale.ROOT);

            boolean reverse = mode != Mode.FOOT
                && "-1".equals(oneway);

            boolean forward = mode != Mode.FOOT
                && !reverse
                && (Set.of("yes", "1", "true").contains(oneway)
                    || (oneway.isBlank()
                        && "roundabout".equals(
                            way.tags.get("junction"))));

            double speed = speed(way.tags, mode);

            for (int i = 0; i < ids.size() - 1; i++) {
                String a = ids.get(i);
                String b = ids.get(i + 1);

                Coordinate ca = network.coordinates.get(a);
                Coordinate cb = network.coordinates.get(b);

                if (ca == null || cb == null || a.equals(b)) continue;

                double km = distance(ca, cb);
                if (km <= 0 || !Double.isFinite(km)) continue;

                nodes.add(a);
                nodes.add(b);

                if (reverse) {
                    addEdge(graph, speeds, edgePositions, b, a, km, speed);
                } else {
                    addEdge(graph, speeds, edgePositions, a, b, km, speed);
                }

                if (!forward && !reverse) {
                    addEdge(graph, speeds, edgePositions, b, a, km, speed);
                }
            }
        }

        if (nodes.isEmpty()) {
            throw error("No suitable roads for this travel mode.",
                HttpStatus.SERVICE_UNAVAILABLE);
        }

        List<String> starts = nearestCandidates(
            from, nodes, network.coordinates);
        List<String> targets = nearestCandidates(
            to, nodes, network.coordinates);

        if (starts.isEmpty() || targets.isEmpty()) {
            throw error("Could not find nearby road nodes.",
                HttpStatus.SERVICE_UNAVAILABLE);
        }

        graph.put(START, new ArrayList<>());
        graph.put(TARGET, new ArrayList<>());

        double connectorSpeed = mode == Mode.FOOT ? 4.5 : 15.0;

        for (String node : starts) {
            double km = distance(from, network.coordinates.get(node));
            addEdge(graph, speeds, edgePositions, START, node, km, connectorSpeed);
        }

        for (String node : targets) {
            double km = distance(network.coordinates.get(node), to);
            addEdge(graph, speeds, edgePositions, node, TARGET, km, connectorSpeed);
        }

        DijkstraShortestPath.PathResult result =
            DijkstraShortestPath.find(graph, START, TARGET);

        if (!result.reachable()) {
            throw error("No connected road route was found.",
                HttpStatus.SERVICE_UNAVAILABLE);
        }

        List<List<Double>> points = new ArrayList<>();
        double minutes = 0;

        List<String> path = result.nodes();

        for (int i = 0; i < path.size(); i++) {
            String id = path.get(i);
            Coordinate c;

            if (START.equals(id)) {
                c = from;
            } else if (TARGET.equals(id)) {
                c = to;
            } else {
                c = network.coordinates.get(id);
            }

            if (c != null) {
                List<Double> point = List.of(c.lng, c.lat);
                if (points.isEmpty()
                    || !points.get(points.size() - 1).equals(point)) {
                    points.add(point);
                }
            }

            if (i + 1 < path.size()) {
                String next = path.get(i + 1);
                Map<String, Integer> positions = edgePositions.get(id);
                Integer edgeIndex = positions == null ? null : positions.get(next);
                double km = edgeIndex == null
                    ? 0
                    : graph.getOrDefault(id, List.of()).get(edgeIndex).weight();

                minutes += km / Math.max(1, speeds.getOrDefault(
                    key(id, next), connectorSpeed)) * 60;
            }
        }

        if (points.size() < 2) {
            throw error("Invalid route geometry.",
                HttpStatus.SERVICE_UNAVAILABLE);
        }

        return new RouteOption(
            result.totalWeight(),
            minutes,
            new Geometry("LineString", points),
            false
        );
    }

    private List<String> nearestCandidates(
        Coordinate point,
        Set<String> ids,
        Map<String, Coordinate> coordinates
    ) {
        return ids.stream()
            .filter(id -> coordinates.containsKey(id))
            .filter(id -> distance(point, coordinates.get(id))
                <= MAX_SNAP_KM)
            .sorted(Comparator.comparingDouble(
                id -> distance(point, coordinates.get(id))))
            .limit(SNAP_CANDIDATES)
            .collect(Collectors.toList());
    }

    private void addEdge(
        Map<String, List<DijkstraShortestPath.Edge>> graph,
        Map<String, Double> speeds,
        Map<String, Map<String, Integer>> edgePositions,
        String from,
        String to,
        double km,
        double speed
    ) {
        List<DijkstraShortestPath.Edge> edges =
            graph.computeIfAbsent(from, ignored -> new ArrayList<>());
        Map<String, Integer> positions =
            edgePositions.computeIfAbsent(from, ignored -> new HashMap<>());

        // Use a hash index instead of scanning the adjacency list on each insert.
        Integer existingIndex = positions.get(to);
        if (existingIndex != null) {
            DijkstraShortestPath.Edge existing = edges.get(existingIndex);
            if (existing.weight() <= km) {
                return;
            }
            // Replace in place so recorded edge indexes remain valid.
            edges.set(existingIndex, new DijkstraShortestPath.Edge(to, km));
        } else {
            positions.put(to, edges.size());
            edges.add(new DijkstraShortestPath.Edge(to, km));
        }

        speeds.put(key(from, to), speed);
    }

    private boolean allowed(Map<String, String> tags, Mode mode) {
        String highway = tags.getOrDefault("highway", "")
            .toLowerCase(Locale.ROOT);

        if (mode == Mode.FOOT) {
            if (Set.of("motorway", "motorway_link",
                "construction", "proposed", "abandoned",
                "razed", "raceway").contains(highway)
                && !"yes".equalsIgnoreCase(tags.get("foot"))) {
                return false;
            }

            if (restricted(tags.get("access"))
                && !"yes".equalsIgnoreCase(tags.get("foot"))) {
                return false;
            }

            return !"no".equalsIgnoreCase(tags.get("foot"))
                && !"private".equalsIgnoreCase(tags.get("foot"));
        }

        if (Set.of("footway", "path", "pedestrian", "steps",
            "cycleway", "bridleway", "corridor", "platform",
            "construction", "proposed", "abandoned", "razed",
            "raceway", "elevator").contains(highway)) {
            return false;
        }

        if (mode == Mode.BIKE
            && Set.of("motorway", "motorway_link").contains(highway)) {
            return false;
        }

        for (String key : List.of("access", "vehicle", "motor_vehicle")) {
            if (restricted(tags.get(key))) return false;
        }

        return true;
    }

    private double speed(Map<String, String> tags, Mode mode) {
        String highway = tags.getOrDefault("highway", "");

        if (mode == Mode.FOOT) {
            return Set.of("path", "footway", "pedestrian").contains(highway)
                ? 4.0 : 4.8;
        }

        if (mode == Mode.BIKE) {
            return switch (highway) {
                case "primary", "primary_link" -> 30;
                case "secondary", "secondary_link" -> 28;
                case "tertiary", "tertiary_link" -> 25;
                case "residential", "living_street" -> 22;
                default -> 20;
            };
        }

        return switch (highway) {
            case "motorway" -> 80;
            case "motorway_link", "trunk" -> 60;
            case "primary", "trunk_link" -> 45;
            case "secondary", "primary_link", "tertiary" -> 30;
            case "residential", "living_street", "unclassified" -> 22;
            default -> 20;
        };
    }

    private boolean restricted(String value) {
        return value != null
            && RESTRICTED.contains(value.toLowerCase(Locale.ROOT));
    }

    private String key(String from, String to) {
        return from + "->" + to;
    }

    private double distance(Coordinate a, Coordinate b) {
        double lat1 = Math.toRadians(a.lat);
        double lat2 = Math.toRadians(b.lat);
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.lng - a.lng);

        double h = Math.pow(Math.sin(dLat / 2), 2)
            + Math.cos(lat1) * Math.cos(lat2)
            * Math.pow(Math.sin(dLng / 2), 2);

        return 6371.0088 * 2 * Math.atan2(
            Math.sqrt(h), Math.sqrt(Math.max(0, 1 - h)));
    }

    private void validate(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng)
            || lat < -90 || lat > 90
            || lng < -180 || lng > 180) {
            throw error("Valid latitude and longitude are required.",
                HttpStatus.BAD_REQUEST);
        }
    }

    private ResponseStatusException error(
        String message, HttpStatus status
    ) {
        return new ResponseStatusException(status, message);
    }

    private enum Mode { CAR, BIKE, FOOT }

    private record Coordinate(double lat, double lng) {}
    private record RoadWay(List<String> ids, Map<String, String> tags) {}
    private record RoadNetwork(
        Map<String, Coordinate> coordinates, List<RoadWay> ways
    ) {}

    public record Geometry(
        String type, List<List<Double>> coordinates
    ) {}

    public record RouteOption(
        double distanceKm, double minutes,
        Geometry geometry, boolean approximate
    ) {}

    public record RouteResponse(
        RouteOption car, RouteOption bike,
        RouteOption foot, String engine
    ) {}
}
