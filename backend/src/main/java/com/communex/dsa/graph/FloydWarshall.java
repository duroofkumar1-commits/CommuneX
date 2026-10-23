package com.communex.dsa.graph;

/**
 * Floyd-Warshall algorithm for calculating all-pairs shortest paths.
 *
 * Uses an adjacency matrix representation and supports negative edge
 * weights, provided the graph does not contain negative-weight cycles
 * for meaningful shortest-path distances.
 */
public final class FloydWarshall {

    // Prevent object creation
    private FloydWarshall() {
    }

    /**
     * Calculates the shortest distances between every pair of vertices.
     *
     * @param input adjacency matrix; use positive infinity for absent edges
     * @return a new matrix containing the shortest distances
     * @throws IllegalArgumentException if the matrix is null or not square
     */
    public static double[][] distances(double[][] input) {

        // Validate the input matrix
        if (input == null) {
            throw new IllegalArgumentException(
                    "Matrix is required"
            );
        }

        int size = input.length;
        double[][] distances = new double[size][size];

        // Validate each row and copy the input matrix
        for (int i = 0; i < size; i++) {

            if (input[i] == null || input[i].length != size) {
                throw new IllegalArgumentException(
                        "Matrix must be square"
                );
            }

            distances[i] = input[i].clone();
        }

        // Consider each vertex as an intermediate vertex
        for (int k = 0; k < size; k++) {

            // Examine every source vertex
            for (int i = 0; i < size; i++) {

                // Examine every destination vertex
                for (int j = 0; j < size; j++) {

                    // Update distance only when both subpaths are finite
                    if (Double.isFinite(distances[i][k])
                            && Double.isFinite(distances[k][j])) {

                        double newDistance =
                                distances[i][k] + distances[k][j];

                        distances[i][j] = Math.min(
                                distances[i][j],
                                newDistance
                        );
                    }
                }
            }
        }

        return distances;
    }
}