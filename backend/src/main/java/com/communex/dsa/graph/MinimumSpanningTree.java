package com.communex.dsa.graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Minimum spanning tree algorithms for undirected weighted graphs.
 *
 * Provides:
 * 1. Kruskal's algorithm
 * 2. Prim's algorithm
 */
public final class MinimumSpanningTree {

    // Prevent object creation
    private MinimumSpanningTree() {
    }

    // Represents an undirected weighted edge
    public record Edge(
            int from,
            int to,
            double weight
    ) {
    }

    // Stores the selected edges and spanning-tree information
    public record Result(
            List<Edge> edges,
            double totalWeight,
            boolean spansAllVertices
    ) {
    }

    /**
     * Computes a minimum spanning forest using Kruskal's algorithm.
     *
     * @param vertices number of vertices, indexed from 0 to vertices - 1
     * @param edges weighted edges of the graph
     * @return selected edges, their total weight, and spanning status
     */
    public static Result kruskal(
            int vertices,
            List<Edge> edges
    ) {
        if (vertices < 0) {
            throw new IllegalArgumentException(
                    "Vertex count cannot be negative"
            );
        }

        // Process edges in ascending order of weight
        List<Edge> sortedEdges = new ArrayList<>(edges);

        sortedEdges.sort(
                Comparator.comparingDouble(Edge::weight)
        );

        UnionFind unionFind = new UnionFind(vertices);

        List<Edge> selectedEdges = new ArrayList<>();
        double totalWeight = 0.0;

        // Select edges that do not create a cycle
        for (Edge edge : sortedEdges) {

            check(edge, vertices);

            if (unionFind.union(edge.from(), edge.to())) {
                selectedEdges.add(edge);
                totalWeight += edge.weight();

                // Stop after selecting enough edges for a spanning tree
                if (selectedEdges.size() == Math.max(0, vertices - 1)) {
                    break;
                }
            }
        }

        boolean spansAllVertices = vertices == 0
                || selectedEdges.size() == vertices - 1;

        return new Result(
                List.copyOf(selectedEdges),
                totalWeight,
                spansAllVertices
        );
    }

    /**
     * Computes a minimum spanning tree using Prim's algorithm.
     *
     * @param vertices number of vertices, indexed from 0 to vertices - 1
     * @param edges weighted edges of the graph
     * @param start starting vertex
     * @return selected edges, their total weight, and spanning status
     */
    public static Result prim(
            int vertices,
            List<Edge> edges,
            int start
    ) {
        if (vertices < 0
                || start < 0
                || (start >= vertices && vertices > 0)) {
            throw new IllegalArgumentException("Invalid vertex/start");
        }

        if (vertices == 0) {
            return new Result(
                    List.of(),
                    0.0,
                    true
            );
        }

        // Build the adjacency list for the undirected graph
        List<List<Edge>> graph = new ArrayList<>();

        for (int i = 0; i < vertices; i++) {
            graph.add(new ArrayList<>());
        }

        for (Edge edge : edges) {
            check(edge, vertices);

            graph.get(edge.from()).add(edge);

            // Add the reverse direction for the undirected graph
            graph.get(edge.to()).add(
                    new Edge(
                            edge.to(),
                            edge.from(),
                            edge.weight()
                    )
            );
        }

        boolean[] visited = new boolean[vertices];

        // Prioritize edges with the smallest weight
        PriorityQueue<Edge> queue = new PriorityQueue<>(
                Comparator.comparingDouble(Edge::weight)
        );

        List<Edge> selectedEdges = new ArrayList<>();
        double totalWeight = 0.0;

        // Start from the requested vertex
        visited[start] = true;
        queue.addAll(graph.get(start));

        // Expand the tree using the cheapest available edge
        while (!queue.isEmpty()
                && selectedEdges.size() < vertices - 1) {

            Edge edge = queue.remove();

            // Skip edges leading to an already visited vertex
            if (visited[edge.to()]) {
                continue;
            }

            visited[edge.to()] = true;
            selectedEdges.add(edge);
            totalWeight += edge.weight();

            // Add outgoing edges from the newly visited vertex
            for (Edge nextEdge : graph.get(edge.to())) {

                if (!visited[nextEdge.to()]) {
                    queue.add(nextEdge);
                }
            }
        }

        boolean spansAllVertices =
                selectedEdges.size() == vertices - 1;

        return new Result(
                List.copyOf(selectedEdges),
                totalWeight,
                spansAllVertices
        );
    }

    /**
     * Validates the vertices and weight of an edge.
     */
    private static void check(
            Edge edge,
            int vertices
    ) {
        if (edge.from() < 0
                || edge.to() < 0
                || edge.from() >= vertices
                || edge.to() >= vertices
                || !Double.isFinite(edge.weight())) {

            throw new IllegalArgumentException("Invalid edge");
        }
    }
}