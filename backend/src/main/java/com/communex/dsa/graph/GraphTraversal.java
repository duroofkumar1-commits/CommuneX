package com.communex.dsa.graph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Breadth-first search (BFS) and depth-first search (DFS)
 * for adjacency-list graphs.
 */
public final class GraphTraversal {

    // Prevent object creation
    private GraphTraversal() {
    }

    /**
     * Performs breadth-first traversal from the starting vertex.
     *
     * @param graph adjacency-list representation of the graph
     * @param start starting vertex
     * @return vertices in BFS traversal order
     */
    public static <T> List<T> bfs(
            Map<T, ? extends Collection<T>> graph,
            T start
    ) {
        // Return an empty list if the starting vertex is absent
        if (!graph.containsKey(start)) {
            return List.of();
        }

        Set<T> visited = new LinkedHashSet<>();
        ArrayDeque<T> queue = new ArrayDeque<>();

        // Initialize the BFS queue
        queue.addLast(start);
        visited.add(start);

        // Process vertices in FIFO order
        while (!queue.isEmpty()) {

            T vertex = queue.removeFirst();

            Collection<T> adjacentVertices =
                    getNeighbors(graph, vertex);

            for (T neighbor : adjacentVertices) {

                // Enqueue each vertex only once
                if (visited.add(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }

        return List.copyOf(visited);
    }

    /**
     * Performs depth-first traversal from the starting vertex.
     *
     * @param graph adjacency-list representation of the graph
     * @param start starting vertex
     * @return vertices in DFS traversal order
     */
    public static <T> List<T> dfs(
            Map<T, ? extends Collection<T>> graph,
            T start
    ) {
        // Return an empty list if the starting vertex is absent
        if (!graph.containsKey(start)) {
            return List.of();
        }

        List<T> result = new ArrayList<>();
        Set<T> visited = new LinkedHashSet<>();

        // Start recursive DFS traversal
        dfsVisit(graph, start, visited, result);

        return List.copyOf(result);
    }

    /**
     * Recursively visits vertices using depth-first search.
     *
     * @param graph graph adjacency list
     * @param vertex current vertex
     * @param visited vertices already visited
     * @param result traversal order
     */
    private static <T> void dfsVisit(
            Map<T, ? extends Collection<T>> graph,
            T vertex,
            Set<T> visited,
            List<T> result
    ) {
        // Stop if this vertex has already been visited
        if (!visited.add(vertex)) {
            return;
        }

        // Record the current vertex
        result.add(vertex);

        Collection<T> adjacentVertices =
                getNeighbors(graph, vertex);

        // Visit adjacent vertices in their existing order
        for (T neighbor : adjacentVertices) {
            dfsVisit(graph, neighbor, visited, result);
        }
    }

    /**
     * Retrieves the neighbors of a vertex.
     * Returns an empty collection when the vertex is not a graph key.
     */
    private static <T> Collection<T> getNeighbors(
            Map<T, ? extends Collection<T>> graph,
            T vertex
    ) {
        if (graph.containsKey(vertex)) {
            return graph.get(vertex);
        }

        return List.of();
    }
}