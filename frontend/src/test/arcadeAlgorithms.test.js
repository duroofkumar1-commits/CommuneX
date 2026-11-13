import { describe, expect, it } from "vitest";
import {
  countSudokuSolutions,
  findBestChessMove,
  getWordleFeedback,
  MinPriorityQueue,
  solveGridPath,
  solvePortalPath,
  solveSudoku,
} from "../utils/dsa/arcadeAlgorithms";

const makeGrid = (rows) => rows.map((row) => row.split("").map((cell) => (cell === "#" ? 1 : 0)));

describe("arcade pathfinding", () => {
  it("BFS visits level by level and returns a shortest-step path", () => {
    const grid = makeGrid([".....", ".###.", "....."]);
    const result = solveGridPath(grid, { x: 0, y: 1 }, { x: 4, y: 1 }, "BFS");
    expect(result.path.length - 1).toBe(6);
    expect(result.visited[1]).toEqual({ x: 0, y: 0 });
  });

  it("DFS prioritizes the left branch before other neighbours", () => {
    const grid = makeGrid(["...", "...", "..."]);
    const result = solveGridPath(grid, { x: 1, y: 1 }, { x: 2, y: 2 }, "DFS");
    expect(result.visited[1]).toEqual({ x: 0, y: 1 });
  });

  it("Dijkstra and A* return the same minimum weighted cost", () => {
    const grid = makeGrid([".....", ".....", "....."]);
    const costAt = (x, y) => (y === 1 && x > 0 && x < 4 ? 9 : 1);
    const start = { x: 0, y: 1 };
    const end = { x: 4, y: 1 };
    const dijkstra = solveGridPath(grid, start, end, "Dijkstra", { costAt });
    const aStar = solveGridPath(grid, start, end, "A*", { costAt, minStepCost: 1 });
    const bfs = solveGridPath(grid, start, end, "BFS", { costAt });
    expect(aStar.cost).toBe(dijkstra.cost);
    expect(bfs.path.length).toBe(5);
    expect(bfs.path.every((point) => point.y === 1)).toBe(true);
    expect(dijkstra.cost).toBe(6);
    expect(dijkstra.path.some((point) => point.y !== 1)).toBe(true);
  });

  it("rejects negative or non-finite edge costs", () => {
    const grid = makeGrid(["..."]);
    expect(() => solveGridPath(grid, { x: 0, y: 0 }, { x: 2, y: 0 }, "Dijkstra", { costAt: () => -1 })).toThrow(RangeError);
  });

  it("BFS solves a portal transition as one move", () => {
    const grid = makeGrid([".....", ".....", "....."]);
    const result = solvePortalPath(
      grid,
      { x: 0, y: 1 },
      { x: 4, y: 1 },
      [{ x: 1, y: 1 }, { x: 3, y: 1 }],
    );
    expect(result.path).toEqual([
      { x: 0, y: 1 },
      { x: 3, y: 1 },
      { x: 4, y: 1 },
    ]);
    expect(result.moves).toBe(2);
  });
});

describe("arcade non-pathfinding algorithms", () => {
  it("min-priority queue removes the lowest priority first", () => {
    const queue = new MinPriorityQueue();
    queue.push("slow", 10);
    queue.push("urgent", 1);
    queue.push("middle", 5);
    expect(queue.pop().value).toBe("urgent");
    expect(queue.pop().value).toBe("middle");
    expect(queue.pop().value).toBe("slow");
  });

  it("solves a Sudoku and detects duplicate initial clues", () => {
    const puzzle = [
      [5, 3, 0, 0, 7, 0, 0, 0, 0],
      [6, 0, 0, 1, 9, 5, 0, 0, 0],
      [0, 9, 8, 0, 0, 0, 0, 6, 0],
      [8, 0, 0, 0, 6, 0, 0, 0, 3],
      [4, 0, 0, 8, 0, 3, 0, 0, 1],
      [7, 0, 0, 0, 2, 0, 0, 0, 6],
      [0, 6, 0, 0, 0, 0, 2, 8, 0],
      [0, 0, 0, 4, 1, 9, 0, 0, 5],
      [0, 0, 0, 0, 8, 0, 0, 7, 9],
    ];
    const result = solveSudoku(puzzle);
    expect(result.solutions).toBe(1);
    expect(result.solution[0]).toEqual([5, 3, 4, 6, 7, 8, 9, 1, 2]);
    const invalid = puzzle.map((row) => row.slice());
    invalid[0][2] = 5;
    expect(solveSudoku(invalid).solution).toBeNull();
  });

  it("counts Sudoku solutions up to a caller-provided limit", () => {
    const solved = [
      [1,2,3,4,5,6,7,8,9], [4,5,6,7,8,9,1,2,3], [7,8,9,1,2,3,4,5,6],
      [2,3,4,5,6,7,8,9,1], [5,6,7,8,9,1,2,3,4], [8,9,1,2,3,4,5,6,7],
      [3,4,5,6,7,8,9,1,2], [6,7,8,9,1,2,3,4,5], [9,1,2,3,4,5,6,7,8],
    ];
    expect(countSudokuSolutions(solved, 2)).toBe(1);
  });

  it("marks repeated Wordle letters only as many times as they occur", () => {
    expect(getWordleFeedback("APPLE", "ALLEY")).toEqual([
      "correct", "present", "absent", "present", "absent",
    ]);
    expect(getWordleFeedback("LEVEL", "LEEEE")).toEqual([
      "correct", "correct", "absent", "correct", "absent",
    ]);
  });

  it("uses minimax to take a king when a winning capture is available", () => {
    const board = Array.from({ length: 8 }, () => Array(8).fill(null));
    board[0][0] = { type: "king", color: "black" };
    board[7][7] = { type: "king", color: "white" };
    board[6][7] = { type: "rook", color: "black" };
    const moves = (position, row, col) => {
      const piece = position[row][col];
      if (piece.type === "rook") return [[7, 7], [5, col], [4, col]];
      return [];
    };
    const best = findBestChessMove(board, moves, 2);
    expect(best).toMatchObject({ from: [6, 7], to: [7, 7] });
  });
});
