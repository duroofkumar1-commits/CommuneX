
package com.communex.dsa;

import com.communex.dsa.advanced.BloomFilter;
import com.communex.dsa.advanced.FenwickTree;
import com.communex.dsa.advanced.SegmentTree;
import com.communex.dsa.cache.LFUCache;
import com.communex.dsa.cache.LRUCache;
import com.communex.dsa.graph.BellmanFordShortestPath;
import com.communex.dsa.graph.FloydWarshall;
import com.communex.dsa.graph.GraphTraversal;
import com.communex.dsa.graph.MinimumSpanningTree;
import com.communex.dsa.graph.TopologicalSort;
import com.communex.dsa.linkedlist.CircularLinkedList;
import com.communex.dsa.linkedlist.DoublyLinkedList;
import com.communex.dsa.linkedlist.SinglyLinkedList;
import com.communex.dsa.queue.CircularQueue;
import com.communex.dsa.stack.Stack;
import com.communex.dsa.strings.KmpSearch;
import com.communex.dsa.tree.AVLTree;
import com.communex.dsa.tree.BinaryMinHeap;
import com.communex.dsa.tree.BinarySearchTree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests the correctness of the data structures and algorithms
 * implemented in the CommuneX DSA package.
 */
class DataStructuresTest {

    @Test
    void linkedListsMaintainOrderAndLinks() {
        SinglyLinkedList<Integer> singlyList = new SinglyLinkedList<>();

        singlyList.addLast(2);
        singlyList.addFirst(1);
        singlyList.addLast(3);

        assertEquals(List.of(1, 2, 3), toList(singlyList));

        assertTrue(singlyList.remove(2));
        assertEquals(List.of(1, 3), toList(singlyList));

        DoublyLinkedList<Integer> doublyList = new DoublyLinkedList<>();

        doublyList.addLast(1);
        doublyList.addLast(2);

        assertEquals(List.of(1, 2), doublyList.toList());
        assertEquals(1, doublyList.last().previous().value());

        CircularLinkedList<String> circularList =
            new CircularLinkedList<>();

        circularList.add("a");
        circularList.add("b");

        assertEquals("a", circularList.current());
        assertEquals("b", circularList.next());
        assertEquals("a", circularList.next());
    }

    /**
     * Converts an iterable collection into a list for assertions.
     *
     * @param values iterable values
     * @param <T> element type
     * @return list containing the elements in iteration order
     */
    private <T> List<T> toList(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        values.forEach(result::add);
        return result;
    }

    @Test
    void stackAndCircularQueueFollowTheirContracts() {
        Stack<Integer> stack = new Stack<>();

        stack.push(1);
        stack.push(2);

        assertEquals(2, stack.pop());
        assertEquals(1, stack.pop());

        assertThrows(
            NoSuchElementException.class,
            stack::pop
        );

        CircularQueue<Integer> queue = new CircularQueue<>(2);

        queue.enqueue(1);
        queue.enqueue(2);

        assertTrue(queue.isFull());
        assertEquals(1, queue.dequeue());

        queue.enqueue(3);

        assertEquals(2, queue.dequeue());
        assertEquals(3, queue.dequeue());
    }

    @Test
    void searchTreesAndHeapAreOrdered() {
        BinarySearchTree<Integer> bst = new BinarySearchTree<>();
        AVLTree<Integer> avl = new AVLTree<>();

        int[] values = {4, 2, 6, 1, 3, 5, 7, 3};

        for (int value : values) {
            bst.add(value);
            avl.add(value);
        }

        assertEquals(
            List.of(1, 2, 3, 4, 5, 6, 7),
            bst.inOrder()
        );

        assertTrue(avl.contains(6));
        assertEquals(7, bst.size());
        assertEquals(7, avl.size());

        BinaryMinHeap<Integer> heap =
            new BinaryMinHeap<>(Comparator.naturalOrder());

        heap.add(4);
        heap.add(1);
        heap.add(3);

        assertEquals(1, heap.remove());
        assertEquals(3, heap.remove());
    }

    @Test
    void graphAlgorithmsHandleConnectedAndDisconnectedCases() {
        Map<String, List<String>> graph = Map.of(
            "A", List.of("B", "C"),
            "B", List.of("D"),
            "C", List.of(),
            "D", List.of()
        );

        assertEquals(
            List.of("A", "B", "C", "D"),
            GraphTraversal.bfs(graph, "A")
        );

        assertEquals(4, GraphTraversal.dfs(graph, "A").size());

        Map<String, List<String>> directedGraph = Map.of(
            "A", List.of("C"),
            "B", List.of("C"),
            "C", List.of()
        );

        List<String> topologicalOrder =
    TopologicalSort.sort(directedGraph);

assertEquals(3, topologicalOrder.size());
assertTrue(topologicalOrder.containsAll(List.of("A", "B", "C")));
assertTrue(
    topologicalOrder.indexOf("A")
        < topologicalOrder.indexOf("C")
);
assertTrue(
    topologicalOrder.indexOf("B")
        < topologicalOrder.indexOf("C")
);

        assertThrows(
            IllegalArgumentException.class,
            () -> TopologicalSort.sort(
                Map.of(
                    "A", List.of("B"),
                    "B", List.of("A")
                )
            )
        );

        var bellmanFordResult = BellmanFordShortestPath.calculate(
            List.of("A", "B", "C"),
            List.of(
                new BellmanFordShortestPath.Edge("A", "B", 4),
                new BellmanFordShortestPath.Edge("A", "C", 5),
                new BellmanFordShortestPath.Edge("B", "C", -2)
            ),
            "A"
        );

        assertFalse(bellmanFordResult.negativeCycleDetected());
        assertEquals(2.0, bellmanFordResult.distances().get("C"));

        double infinity = Double.POSITIVE_INFINITY;

        double[][] distances = FloydWarshall.distances(
            new double[][] {
                {0, 3, 10},
                {infinity, 0, 2},
                {infinity, infinity, 0}
            }
        );

        assertEquals(5.0, distances[0][2]);

        MinimumSpanningTree.Result unused = null;
    }

    @Test
    void cachesEvictCorrectly() {
        LRUCache<String, Integer> lru = new LRUCache<>(2);

        lru.put("a", 1);
        lru.put("b", 2);
        lru.get("a");
        lru.put("c", 3);

        assertNull(lru.get("b"));

        LFUCache<String, Integer> lfu = new LFUCache<>(2);

        lfu.put("a", 1);
        lfu.put("b", 2);
        lfu.get("a");
        lfu.put("c", 3);

        assertNull(lfu.get("b"));
        assertEquals(1, lfu.get("a"));
    }

    @Test
    void rangeStructuresAndBloomFilterBehave() {
        SegmentTree segmentTree =
            new SegmentTree(new int[] {1, 2, 3, 4});

        assertEquals(9, segmentTree.sum(1, 3));

        segmentTree.update(1, 10);

        assertEquals(17, segmentTree.sum(1, 3));

        FenwickTree fenwickTree =
            new FenwickTree(new int[] {1, 2, 3, 4});

        assertEquals(10, fenwickTree.rangeSum(0, 3));

        fenwickTree.add(1, 4);

        assertEquals(9, fenwickTree.rangeSum(1, 2));

        BloomFilter bloomFilter = new BloomFilter(2048, 5);

        bloomFilter.add("commune");

        assertTrue(bloomFilter.mightContain("commune"));
    }

    @Test
    void kmpFindsMatchesIncludingOverlappingOnes() {
        assertEquals(
            2,
            KmpSearch.indexOf("ababc", "abc")
        );

        assertEquals(
            List.of(0, 1, 2),
            KmpSearch.allIndexes("aaaa", "aa")
        );

        assertEquals(
            -1,
            KmpSearch.indexOf("abc", "z")
        );
    }
}
