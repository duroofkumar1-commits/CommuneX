package com.communex.dsa.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Performs topological sorting using Kahn's algorithm.
 *
 * Throws an exception if the directed graph contains a cycle.
 */
public final class TopologicalSort {

    // Prevent object creation
    private TopologicalSort() {
    }

    /**
     * Returns a topological ordering of the graph's vertices.
     *
     * @param graph adjacency-list representation of a directed graph
     * @param <T> vertex type
     * @return immutable list containing the topological order
     * @throws IllegalArgumentException if the graph contains a cycle
     */
    public static <T> List<T> sort(
            Map<T, ? extends Collection<T>> graph
    ) {

        // Step 1: Initialize the indegree of each vertex
        Map<T, Integer> indegree = new LinkedHashMap<>();

        graph.forEach((vertex, neighbors) -> {

            indegree.putIfAbsent(vertex, 0);

            // Count incoming edges for neighboring vertices
            for (T neighbor : neighbors) {
                indegree.merge(neighbor, 1, Integer::sum);
            }
        });

        // Step 2: Add vertices with zero indegree to the queue
        ArrayDeque<T> queue = new ArrayDeque<>();

        indegree.forEach((vertex, degree) -> {

            if (degree == 0) {
                queue.addLast(vertex);
            }
        });

        // Step 3: Process vertices using Kahn's algorithm
        List<T> result = new ArrayList<>();

        while (!queue.isEmpty()) {

            T vertex = queue.removeFirst();

            result.add(vertex);

            Collection<T> neighbors;

            if (graph.containsKey(vertex)) {
                neighbors = graph.get(vertex);
            } else {
                neighbors = List.of();
            }

            // Reduce indegree for every outgoing edge
            for (T neighbor : neighbors) {

                int updatedDegree = indegree.get(neighbor) - 1;

                indegree.put(neighbor, updatedDegree);

                // Enqueue the vertex when all incoming edges are processed
                if (updatedDegree == 0) {
                    queue.addLast(neighbor);
                }
            }
        }

        // Step 4: Detect a directed cycle
        if (result.size() != indegree.size()) {
            throw new IllegalArgumentException(
                    "Graph contains a directed cycle"
            );
        }

        // Return an immutable topological ordering
        return List.copyOf(result);
    }
}