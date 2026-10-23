package com.communex.dsa.graph;

import com.communex.dsa.tree.BinaryMinHeap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Dijkstra shortest path for a weighted directed graph with non-negative edge weights.
 *  The project's BinaryMinHeap is used as the priority queue (rather than java.util.PriorityQueue).
 */
public final class DijkstraShortestPath {
    private DijkstraShortestPath() { }

    public record Edge(String to, double weight) {
        public Edge {
            if (to == null || to.isBlank()) throw new IllegalArgumentException("Destination is required");
            if (!Double.isFinite(weight) || weight < 0)
                throw new IllegalArgumentException("Weight must be finite and non-negative");
        }
    }

    public record PathResult(List<String> nodes, double totalWeight, boolean reachable) { }

    public static PathResult find(Map<String, List<Edge>> graph, String start, String target) {
        if (graph == null || start == null || target == null
                || !graph.containsKey(start) || !graph.containsKey(target)) {
            return new PathResult(List.of(), Double.POSITIVE_INFINITY, false);
        }

        Map<String, Double> distance = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        BinaryMinHeap<Visit> queue = new BinaryMinHeap<>(Comparator.comparingDouble(Visit::distance));
        distance.put(start, 0.0);
        queue.add(new Visit(start, 0.0));

        while (!queue.isEmpty()) {
            Visit current = queue.remove();
            if (current.distance() > distance.getOrDefault(current.node(), Double.POSITIVE_INFINITY)) continue;
            if (current.node().equals(target)) break;

            for (Edge edge : graph.getOrDefault(current.node(), List.of())) {
                if (!graph.containsKey(edge.to())) continue;
                double candidate = current.distance() + edge.weight();
                if (candidate < distance.getOrDefault(edge.to(), Double.POSITIVE_INFINITY)) {
                    distance.put(edge.to(), candidate);
                    previous.put(edge.to(), current.node());
                    queue.add(new Visit(edge.to(), candidate));
                }
            }
        }

        if (!distance.containsKey(target)) return new PathResult(List.of(), Double.POSITIVE_INFINITY, false);

        List<String> path = new ArrayList<>();
        String at = target;
        path.add(at);
        while (!at.equals(start)) {
            at = previous.get(at);
            if (at == null) return new PathResult(List.of(), Double.POSITIVE_INFINITY, false);
            path.add(at);
        }
        Collections.reverse(path);
        return new PathResult(List.copyOf(path), distance.get(target), true);
    }

    private record Visit(String node, double distance) { }
}
