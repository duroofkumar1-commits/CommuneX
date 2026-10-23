package com.communex.dsa.graph;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bellman-Ford shortest path algorithm.
 *
 * Supports negative edge weights and detects negative cycles
 * reachable from the source vertex.
 */
public final class BellmanFordShortestPath {

    // Prevent object creation
    private BellmanFordShortestPath() {
    }

    // Represents a directed, weighted edge
    public record Edge(
            String from,
            String to,
            double weight
    ) {
    }

    // Stores shortest-path distances and cycle detection result
    public record Result(
            Map<String, Double> distances,
            boolean negativeCycleDetected
    ) {
    }

    /**
     * Calculates shortest distances from the source vertex.
     *
     * @param vertices all graph vertices
     * @param edges directed weighted edges
     * @param source starting vertex
     * @return shortest distances and negative-cycle status
     */
    public static Result calculate(
            Collection<String> vertices,
            List<Edge> edges,
            String source
    ) {
        // Prepare the graph's vertex collection
        Set<String> vertexSet = new LinkedHashSet<>(vertices);
        vertexSet.add(source);

        // Initialize every distance to infinity
        Map<String, Double> distances = new LinkedHashMap<>();

        for (String vertex : vertexSet) {
            distances.put(vertex, Double.POSITIVE_INFINITY);
        }

        // Distance from the source to itself is zero
        distances.put(source, 0.0);

        // Relax edges up to |V| - 1 times
        for (int i = 1; i < vertexSet.size(); i++) {

            boolean changed = false;

            for (Edge edge : edges) {

                // Validate edge endpoints
                if (!vertexSet.contains(edge.from())
                        || !vertexSet.contains(edge.to())) {
                    throw new IllegalArgumentException(
                            "Edge references an unknown vertex"
                    );
                }

                double currentDistance = distances.get(edge.from());

                // Relax only edges whose source is reachable
                if (Double.isFinite(currentDistance)) {

                    double newDistance =
                            currentDistance + edge.weight();

                    if (newDistance < distances.get(edge.to())) {
                        distances.put(edge.to(), newDistance);
                        changed = true;
                    }
                }
            }

            // Stop early if no distance changed
            if (!changed) {
                break;
            }
        }

        // Check for a reachable negative-weight cycle
        for (Edge edge : edges) {

            double currentDistance = distances.get(edge.from());

            if (Double.isFinite(currentDistance)) {

                double newDistance =
                        currentDistance + edge.weight();

                if (newDistance < distances.get(edge.to())) {
                    return new Result(
                            Map.copyOf(distances),
                            true
                    );
                }
            }
        }

        // Return final shortest-path distances
        return new Result(
                Map.copyOf(distances),
                false
        );
    }
}