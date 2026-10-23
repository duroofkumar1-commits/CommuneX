
package com.communex.dsa;

import com.communex.dsa.graph.DijkstraShortestPath;
import com.communex.dsa.graph.UnionFind;
import com.communex.dsa.searching.BinarySearch;
import com.communex.dsa.searching.LinearSearch;
import com.communex.dsa.strings.Trie;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests searching algorithms, trie operations, shortest-path
 * calculations, and the Union-Find data structure.
 */
class SearchAndGraphTest {

    @Test
    void searchesReturnExpectedIndexOrMinusOne() {
        assertEquals(
            1,
            LinearSearch.indexOf(new int[] {8, 4, 7}, 4)
        );

        assertEquals(
            -1,
            LinearSearch.indexOf(new int[] {8, 4, 7}, 3)
        );

        assertEquals(
            2,
            BinarySearch.indexOf(new int[] {1, 4, 7, 9}, 7)
        );

        assertEquals(
            -1,
            BinarySearch.indexOf(new int[] {1, 4, 7, 9}, 2)
        );
    }

    @Test
    void trieSuggestsCaseInsensitivePrefixes() {
        Trie trie = new Trie();

        trie.insert("Teaching");
        trie.insert("Team");
        trie.insert("Shelter");

        assertTrue(trie.contains("TEACHING"));
        assertTrue(trie.startsWith("tea"));

        assertEquals(
            List.of("teaching", "team"),
            trie.suggest("tea", 5)
        );

        assertEquals(
            List.of(),
            trie.suggest("unknown", 5)
        );
    }

    @Test
    void dijkstraReturnsShortestPath() {
        Map<String, List<DijkstraShortestPath.Edge>> graph = Map.of(
            "A",
            List.of(
                new DijkstraShortestPath.Edge("B", 8),
                new DijkstraShortestPath.Edge("C", 2)
            ),
            "B",
            List.of(
                new DijkstraShortestPath.Edge("D", 1)
            ),
            "C",
            List.of(
                new DijkstraShortestPath.Edge("B", 1),
                new DijkstraShortestPath.Edge("D", 9)
            ),
            "D",
            List.of()
        );

        var result = DijkstraShortestPath.find(graph, "A", "D");

        assertTrue(result.reachable());

        assertEquals(
            List.of("A", "C", "B", "D"),
            result.nodes()
        );

        assertEquals(4.0, result.totalWeight());
    }

    @Test
    void dijkstraHandlesUnreachableAndRejectsNegativeWeight() {
        Map<String, List<DijkstraShortestPath.Edge>> graph = Map.of(
            "A",
            List.<DijkstraShortestPath.Edge>of(),
            "B",
            List.<DijkstraShortestPath.Edge>of()
        );

        var result = DijkstraShortestPath.find(graph, "A", "B");

        assertFalse(result.reachable());

        assertThrows(
            IllegalArgumentException.class,
            () -> new DijkstraShortestPath.Edge("B", -1)
        );
    }

    @Test
    void unionFindConnectsComponents() {
        UnionFind unionFind = new UnionFind(5);

        assertTrue(unionFind.union(0, 1));
        assertFalse(unionFind.union(1, 0));

        assertTrue(unionFind.connected(0, 1));
        assertFalse(unionFind.connected(0, 3));

        unionFind.union(1, 3);

        assertTrue(unionFind.connected(0, 3));
    }
}
