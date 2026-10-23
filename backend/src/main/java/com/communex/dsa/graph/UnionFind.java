package com.communex.dsa.graph;

/**
 * Disjoint Set Union (DSU), also known as Union-Find.
 *
 * Uses path compression and union by rank
 * to make set operations more efficient.
 */
public final class UnionFind {

    private final int[] parent;
    private final byte[] rank;

    // Initialize every element as its own set
    public UnionFind(int size) {

        if (size < 0) {
            throw new IllegalArgumentException(
                    "Size cannot be negative"
            );
        }

        parent = new int[size];
        rank = new byte[size];

        for (int i = 0; i < size; i++) {
            parent[i] = i;
        }
    }

    /**
     * Finds the representative of the set containing the value.
     * Path compression flattens the tree during traversal.
     */
    public int find(int value) {

        check(value);

        if (parent[value] != value) {
            parent[value] = find(parent[value]);
        }

        return parent[value];
    }

    /**
     * Merges the sets containing a and b.
     *
     * @return true if two different sets were merged;
     *         false if both elements were already connected.
     */
    public boolean union(int a, int b) {

        int rootA = find(a);
        int rootB = find(b);

        // Both elements already belong to the same set
        if (rootA == rootB) {
            return false;
        }

        // Attach the lower-rank tree below the higher-rank tree
        if (rank[rootA] < rank[rootB]) {
            parent[rootA] = rootB;

        } else if (rank[rootA] > rank[rootB]) {
            parent[rootB] = rootA;

        } else {
            parent[rootB] = rootA;
            rank[rootA]++;
        }

        return true;
    }

    // Check whether two elements belong to the same set
    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    // Validate the element index
    private void check(int value) {

        if (value < 0 || value >= parent.length) {
            throw new IndexOutOfBoundsException(
                    "Index out of range: " + value
            );
        }
    }
}