# Data structures and algorithms

Java implementations live in `backend/src/main/java/com/communex/dsa`; tests live in `backend/src/test/java/com/communex/dsa`.

| Topic | Classes | Meaningful connection |
|---|---|---|
| Bubble/Selection/Insertion/Merge/Quick/Heap/Counting/Radix/Bucket sort | `sorting/*Sort` | Educational/algorithm correctness tests; production lists use database ordering when appropriate |
| Linear/Binary search | `searching/*Search` | Sequence lookup |
| Trie + KMP | `strings/Trie`, `KmpSearch` | Prefix suggestions and string search |
| Singly/Doubly/Circular linked lists | `linkedlist/*` | Sequence models; music player uses a doubly-linked playlist model |
| Stack | `stack/Stack` | Editor undo/redo concept; UI stores bounded undo/redo history |
| Queue/Circular queue/Deque | `queue/*` | Request-processing/scheduling concepts |
| Binary heap / BST / AVL | `tree/*` | Priority ordering and ordered-data concepts |
| BFS/DFS, Dijkstra, Bellman-Ford, Floyd-Warshall | `graph/*` | Maze traversal and shortest-path algorithm foundation |
| MST, Union-Find, topological sort | `graph/*` | Network connectivity/dependency concepts |
| LRU/LFU | `cache/*` | Cache policy implementations; do not deploy without evidence of need |
| Fenwick/Segment tree/Bloom filter/monotonic structures | `advanced/*` | Range-query/probabilistic/window algorithm study and tests |

Nine sorting implementations and core data-structure/search/graph tests are present. Run `mvn test` from `backend` when Maven is available. Maven was not installed in the artifact-building environment, so these Java tests were not executed here.
