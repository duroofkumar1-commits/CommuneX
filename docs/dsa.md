# Data structures and algorithms in CommuneX

This document distinguishes algorithms that are **called by a live application feature** from implementations kept as study examples or covered by tests. A class existing in the repository does not, by itself, mean the production application uses it.

## Live application integrations

| Feature | Data structure / algorithm | Actual use |
|---|---|---|
| Map Explorer (backend) | Adjacency-list graph + Dijkstra + custom Binary Min-Heap | Computes a minimum-distance route over the road graph using non-negative distance weights. The route is shortest by distance, not necessarily fastest by travel time. |
| Maze Solver (frontend) | Randomized DFS maze generation + braided corridors; BFS; left-first DFS; Dijkstra; A* | BFS minimizes number of moves. DFS explores the left branch first. Dijkstra and A* minimize weighted terrain-entry cost. The UI shows visited cells, final path, explored count, and route cost. |
| Maze Chase / Pac-Man (frontend) | Grid graph + BFS | Each ghost computes a shortest-step route to the player's current cell and moves one step along that route. |
| Chess Arena (frontend) | Move generation + alpha-beta minimax | The opponent searches a simplified arcade chess tree. This mode does not implement castling, en passant, or full check/checkmate legality. |
| Sudoku Lab (frontend) | Backtracking + bit masks + minimum-remaining-values (MRV) | The Solve action runs the solver. Puzzle generation checks that the generated puzzle has exactly one solution (up to the solver's two-solution cutoff). |
| Tower Defense (frontend) | Grid graph + BFS + binary min-heap priority queue | BFS computes each level's enemy route through blocked cells. Turrets use a min-heap to prioritize the most advanced in-range enemy, with distance as a tie-breaker. |
| Portal Puzzle (frontend) | State-space BFS with portal transitions | Finds a shortest move-count route while treating each portal hop as one transition. |
| Word Grid / Wordle (frontend) | Hash map / frequency counting | Evaluates exact matches first, then consumes remaining letter counts so duplicate letters are marked correctly. |
| Minesweeper (frontend) | BFS flood fill with a head-index queue | Reveals contiguous zero cells and their numbered boundary cells without repeatedly shifting an array. |
| Pocket City (frontend) | Grid graph + BFS connected-component traversal | Recomputes the road network connected to the main road and only counts adjacent houses/services on that connected network. |
| Music Player (frontend) | Doubly linked playlist | Previous/next track traversal uses `prev` and `next` node links in `MusicPlayer.jsx`. |
| Text Editor (frontend) | Stack behaviour | Undo and redo use push/pop-style array histories in `TextEditor.jsx`. These are JavaScript arrays, not the backend Java `Stack` class. |

## Other arcade implementations and accurate descriptions

| Game | Actual implementation |
|---|---|
| Tetris | 2D board matrix, collision detection, rotation, and line compaction. The current game does not use a piece queue. |
| Blockcraft | Grid traversal, resource counters, and state updates. It does not use a hash-map inventory. |
| Snake | Array-based head/tail updates and collision checks. The body is not a linked-list node structure. |
| 2048 | 2D matrix transforms, single-pass merges, and state transitions. |
| Rubik's Cube | Sticker permutation, face rotation, and solved-state validation. No state-space solver is currently used. |
| Candy Match | 2D grid pattern matching and a Set of cells to clear. |
| Solitaire | Array-based stock, waste, tableau, and foundation piles with stack-like operations. The tableau is not a linked list. |

## Java DSA study library

Java implementations live in `backend/src/main/java/com/communex/dsa`; tests live in `backend/src/test/java/com/communex/dsa`.

| Topic | Classes / packages | Runtime status |
|---|---|---|
| Sorting | Bubble, Selection, Insertion, Merge, Quick, Heap, Counting, Radix, Bucket | Study implementations and tests; production lists use database ordering where appropriate. |
| Searching | Linear and Binary Search | Reusable study implementations; not the global-search request path. |
| Strings | Trie and KMP | Study implementations/tests; not the current global-search implementation. |
| Linked lists | Singly, Doubly, Circular | Java study implementations; Music Player's linked playlist is implemented separately in JavaScript. |
| Stack | Stack | Java study implementation; Text Editor uses JavaScript array stacks. |
| Queues | Queue, Circular Queue, Deque | Study implementations/tests; not currently wired as production request schedulers. |
| Trees | Binary Min-Heap, BST, AVL | Binary Min-Heap is used by backend Dijkstra; BST/AVL are study implementations. |
| Graphs | BFS/DFS, Dijkstra, Bellman-Ford, Floyd-Warshall, MST, Union-Find, topological sort | Dijkstra is used by Map Explorer. Other algorithms are reusable study implementations/tests unless a caller is explicitly added. |
| Cache | LRU and LFU | Study implementations/tests; not the live Map Explorer cache policy. |
| Advanced | Fenwick Tree, Segment Tree, Bloom Filter, monotonic queue/stack | Study implementations/tests; not currently connected to a live CommuneX feature. |

## Verification commands

From `frontend`:

```bash
npm test
npm run build
```

From `backend`:

```bash
mvn test
```

Keep the Java DSA study library if it is part of the coursework/learning objective. Do not describe a study class as a production integration until a live feature actually calls it.
