/**
 * Reusable data-structure and algorithm implementations used by CommuneX Arcade.
 * Coordinates are { x, y }; grid rows are indexed as grid[y][x].
 */

export class MinPriorityQueue {
  constructor(compare = (a, b) => a.priority - b.priority) {
    this.items = [];
    this.compare = compare;
  }

  get size() {
    return this.items.length;
  }

  push(value, priority = 0) {
    const node = { value, priority, sequence: MinPriorityQueue.sequence++ };
    this.items.push(node);
    this.#bubbleUp(this.items.length - 1);
  }

  pop() {
    if (!this.items.length) return null;
    const first = this.items[0];
    const last = this.items.pop();
    if (this.items.length) {
      this.items[0] = last;
      this.#sinkDown(0);
    }
    return first;
  }

  #less(a, b) {
    const order = this.compare(a, b);
    return order === 0 ? a.sequence < b.sequence : order < 0;
  }

  #bubbleUp(index) {
    while (index > 0) {
      const parent = Math.floor((index - 1) / 2);
      if (!this.#less(this.items[index], this.items[parent])) break;
      [this.items[index], this.items[parent]] = [this.items[parent], this.items[index]];
      index = parent;
    }
  }

  #sinkDown(index) {
    while (true) {
      const left = index * 2 + 1;
      const right = left + 1;
      let smallest = index;
      if (left < this.items.length && this.#less(this.items[left], this.items[smallest])) smallest = left;
      if (right < this.items.length && this.#less(this.items[right], this.items[smallest])) smallest = right;
      if (smallest === index) break;
      [this.items[index], this.items[smallest]] = [this.items[smallest], this.items[index]];
      index = smallest;
    }
  }
}
MinPriorityQueue.sequence = 0;

const keyOf = ({ x, y }) => `${x},${y}`;
const manhattan = (a, b) => Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
const DEFAULT_DIRECTIONS = [
  { x: 0, y: -1 }, // up
  { x: 1, y: 0 },  // right
  { x: 0, y: 1 },  // down
  { x: -1, y: 0 }, // left
];

function reconstructPath(parent, start, end) {
  const path = [];
  let key = keyOf(end);
  const startKey = keyOf(start);
  if (key !== startKey && !parent.has(key)) return [];
  while (key !== null) {
    const [x, y] = key.split(',').map(Number);
    path.push({ x, y });
    if (key === startKey) break;
    key = parent.get(key);
  }
  path.reverse();
  return path[0] && keyOf(path[0]) === startKey ? path : [];
}

/**
 * Run BFS, left-first DFS, Dijkstra, or A* on a four-neighbour grid.
 * BFS minimizes edge count; Dijkstra/A* minimize the supplied non-negative entry cost.
 */
export function solveGridPath(
  grid,
  start,
  end,
  algorithm = 'BFS',
  { costAt = () => 1, minStepCost = 1, blockedValue = 1, directions } = {},
) {
  if (!Array.isArray(grid) || !grid.length || !grid[0]?.length) {
    return { path: [], visited: [], cost: 0, explored: 0 };
  }
  const rows = grid.length;
  const cols = grid[0].length;
  const inside = ({ x, y }) => x >= 0 && y >= 0 && x < cols && y < rows;
  const open = (point) => inside(point) && grid[point.y][point.x] !== blockedValue;
  if (!open(start) || !open(end)) return { path: [], visited: [], cost: 0, explored: 0 };

  const dirs = directions || (algorithm === 'DFS'
    // DFS pushes in reverse so the stack pops LEFT first, then DOWN, RIGHT, UP.
    ? [{ x: -1, y: 0 }, { x: 0, y: 1 }, { x: 1, y: 0 }, { x: 0, y: -1 }]
    : DEFAULT_DIRECTIONS);
  const startKey = keyOf(start);
  const endKey = keyOf(end);
  const parent = new Map([[startKey, null]]);
  const visited = [];

  if (algorithm === 'BFS' || algorithm === 'DFS') {
    const frontier = [start];
    let head = 0;
    while (algorithm === 'DFS' ? frontier.length > 0 : head < frontier.length) {
      const current = algorithm === 'DFS' ? frontier.pop() : frontier[head++];
      visited.push(current);
      const currentKey = keyOf(current);
      if (currentKey === endKey) break;
      const neighbours = dirs
        .map((direction) => ({ x: current.x + direction.x, y: current.y + direction.y }))
        .filter((next) => open(next) && !parent.has(keyOf(next)));
      const ordered = algorithm === 'DFS' ? neighbours.slice().reverse() : neighbours;
      ordered.forEach((next) => {
        parent.set(keyOf(next), currentKey);
        frontier.push(next);
      });
    }
  } else if (algorithm === 'Dijkstra' || algorithm === 'A*') {
    const useHeuristic = algorithm === 'A*';
    const distances = new Map([[startKey, 0]]);
    const closed = new Set();
    let sequence = 0;
    const frontier = new MinPriorityQueue((a, b) => (
      a.priority - b.priority
    ));
    const startH = useHeuristic ? manhattan(start, end) * minStepCost : 0;
    frontier.push({ point: start, cost: 0, heuristic: startH }, startH);

    while (frontier.size) {
      const entry = frontier.pop();
      const current = entry.value.point;
      const currentKey = keyOf(current);
      if (closed.has(currentKey) || entry.value.cost !== distances.get(currentKey)) continue;
      closed.add(currentKey);
      visited.push(current);
      if (currentKey === endKey) break;

      dirs.forEach((direction) => {
        const next = { x: current.x + direction.x, y: current.y + direction.y };
        if (!open(next)) return;
        const nextKey = keyOf(next);
        if (closed.has(nextKey)) return;
        const stepCost = Number(costAt(next.x, next.y));
        if (!Number.isFinite(stepCost) || stepCost < 0) {
          throw new RangeError('Pathfinding edge costs must be finite and non-negative.');
        }
        const nextCost = entry.value.cost + stepCost;
        if (nextCost >= (distances.get(nextKey) ?? Infinity)) return;
        distances.set(nextKey, nextCost);
        parent.set(nextKey, currentKey);
        const h = useHeuristic ? manhattan(next, end) * minStepCost : 0;
        frontier.push({ point: next, cost: nextCost, heuristic: h, sequence: sequence++ }, nextCost + h);
      });
    }
  } else {
    throw new Error(`Unknown pathfinding algorithm: ${algorithm}`);
  }

  const path = reconstructPath(parent, start, end);
  let cost = 0;
  if (path.length > 1) {
    for (const point of path.slice(1)) {
      cost += algorithm === 'BFS' ? 1 : Number(costAt(point.x, point.y));
    }
  }
  return { path, visited, cost, explored: visited.length };
}

/** BFS where stepping onto one portal immediately places the player at its pair. */
export function solvePortalPath(grid, start, goal, portals = []) {
  const portalMap = new Map();
  if (portals.length === 2) {
    portalMap.set(keyOf(portals[0]), portals[1]);
    portalMap.set(keyOf(portals[1]), portals[0]);
  }
  const startKey = keyOf(start);
  const goalKey = keyOf(goal);
  const queue = [start];
  let head = 0;
  const parent = new Map([[startKey, null]]);
  const visited = [];

  while (head < queue.length) {
    const current = queue[head++];
    const currentKey = keyOf(current);
    visited.push(current);
    if (currentKey === goalKey) break;

    DEFAULT_DIRECTIONS.forEach((direction) => {
      const entry = { x: current.x + direction.x, y: current.y + direction.y };
      if (grid[entry.y]?.[entry.x] !== 0) return;
      const next = portalMap.get(keyOf(entry)) || entry;
      const nextKey = keyOf(next);
      if (parent.has(nextKey)) return;
      parent.set(nextKey, currentKey);
      queue.push(next);
    });
  }

  const path = reconstructPath(parent, start, goal);
  return { path, visited, moves: Math.max(0, path.length - 1), explored: visited.length };
}

/** Solve a 9x9 Sudoku using bit masks and a minimum-remaining-values cell choice. */
export function solveSudoku(board, { solutionLimit = 1 } = {}) {
  if (!Array.isArray(board) || board.length !== 9 || board.some((row) => !Array.isArray(row) || row.length !== 9)) {
    throw new TypeError('Sudoku board must be a 9x9 matrix.');
  }

  const work = board.map((row) => row.slice());
  const rowMask = Array(9).fill(0);
  const colMask = Array(9).fill(0);
  const boxMask = Array(9).fill(0);
  const fullMask = 0b1111111110;
  const boxIndex = (r, c) => Math.floor(r / 3) * 3 + Math.floor(c / 3);

  for (let r = 0; r < 9; r += 1) {
    for (let c = 0; c < 9; c += 1) {
      const value = work[r][c];
      if (!Number.isInteger(value) || value < 0 || value > 9) return { solutions: 0, solution: null };
      if (!value) continue;
      const bit = 1 << value;
      const box = boxIndex(r, c);
      if ((rowMask[r] & bit) || (colMask[c] & bit) || (boxMask[box] & bit)) {
        return { solutions: 0, solution: null };
      }
      rowMask[r] |= bit;
      colMask[c] |= bit;
      boxMask[box] |= bit;
    }
  }

  let solutions = 0;
  let firstSolution = null;
  function search() {
    if (solutions >= solutionLimit) return;
    let best = null;
    let bestCandidates = 0;
    let bestCount = 10;

    for (let r = 0; r < 9; r += 1) {
      for (let c = 0; c < 9; c += 1) {
        if (work[r][c] !== 0) continue;
        const available = fullMask & ~(rowMask[r] | colMask[c] | boxMask[boxIndex(r, c)]);
        let count = 0;
        for (let bits = available; bits; bits &= bits - 1) count += 1;
        if (count < bestCount) {
          best = [r, c];
          bestCandidates = available;
          bestCount = count;
          if (count <= 1) break;
        }
      }
      if (bestCount <= 1) break;
    }

    if (!best) {
      solutions += 1;
      if (!firstSolution) firstSolution = work.map((row) => row.slice());
      return;
    }
    if (!bestCandidates) return;

    const [r, c] = best;
    const box = boxIndex(r, c);
    for (let bits = bestCandidates; bits && solutions < solutionLimit; bits &= bits - 1) {
      const bit = bits & -bits;
      const value = 31 - Math.clz32(bit);
      work[r][c] = value;
      rowMask[r] |= bit;
      colMask[c] |= bit;
      boxMask[box] |= bit;
      search();
      work[r][c] = 0;
      rowMask[r] &= ~bit;
      colMask[c] &= ~bit;
      boxMask[box] &= ~bit;
    }
  }

  search();
  return { solutions, solution: firstSolution };
}

export function countSudokuSolutions(board, limit = 2) {
  return solveSudoku(board, { solutionLimit: limit }).solutions;
}

/** Correct Wordle feedback: exact matches are consumed before misplaced letters. */
export function getWordleFeedback(answer, guess) {
  const feedback = Array(guess.length).fill('absent');
  const remaining = new Map();
  for (let index = 0; index < answer.length; index += 1) {
    if (guess[index] === answer[index]) {
      feedback[index] = 'correct';
    } else {
      remaining.set(answer[index], (remaining.get(answer[index]) || 0) + 1);
    }
  }
  for (let index = 0; index < guess.length; index += 1) {
    if (feedback[index] === 'correct') continue;
    const available = remaining.get(guess[index]) || 0;
    if (available > 0) {
      feedback[index] = 'present';
      remaining.set(guess[index], available - 1);
    }
  }
  return feedback;
}

function applyChessMove(board, move) {
  const next = board.map((row) => row.slice());
  const [fromRow, fromCol] = move.from;
  const [toRow, toCol] = move.to;
  const moving = next[fromRow][fromCol];
  next[toRow][toCol] = moving.type === 'pawn' && (toRow === 0 || toRow === 7)
    ? { ...moving, type: 'queen' }
    : moving;
  next[fromRow][fromCol] = null;
  return next;
}

const CHESS_VALUES = { pawn: 100, knight: 320, bishop: 335, rook: 500, queen: 900, king: 20000 };
function evaluateChessBoard(board) {
  let score = 0;
  let blackKing = false;
  let whiteKing = false;
  for (let r = 0; r < 8; r += 1) {
    for (let c = 0; c < 8; c += 1) {
      const piece = board[r][c];
      if (!piece) continue;
      if (piece.type === 'king') {
        if (piece.color === 'black') blackKing = true;
        else whiteKing = true;
      }
      const material = CHESS_VALUES[piece.type] || 0;
      const centerBonus = (3.5 - (Math.abs(3.5 - r) + Math.abs(3.5 - c) / 2)) * (piece.type === 'knight' || piece.type === 'bishop' ? 4 : 1);
      score += (piece.color === 'black' ? 1 : -1) * (material + centerBonus);
    }
  }
  if (!blackKing) return -100000;
  if (!whiteKing) return 100000;
  return score;
}

/**
 * Alpha-beta minimax for the simplified arcade chess ruleset.
 * movesForPiece(board, row, col) must return [[row, col], ...].
 */
export function findBestChessMove(board, movesForPiece, depth = 2) {
  const collectMoves = (position, color) => {
    const moves = [];
    for (let r = 0; r < 8; r += 1) {
      for (let c = 0; c < 8; c += 1) {
        const piece = position[r][c];
        if (!piece || piece.color !== color) continue;
        movesForPiece(position, r, c).forEach(([toRow, toCol]) => {
          const captured = position[toRow][toCol];
          moves.push({ from: [r, c], to: [toRow, toCol], captureValue: captured ? CHESS_VALUES[captured.type] : 0 });
        });
      }
    }
    moves.sort((a, b) => b.captureValue - a.captureValue);
    return moves;
  };

  function minimax(position, remainingDepth, alpha, beta, blackToMove) {
    const score = evaluateChessBoard(position);
    if (remainingDepth === 0 || Math.abs(score) >= 100000) return score;
    const moves = collectMoves(position, blackToMove ? 'black' : 'white');
    if (!moves.length) return score;

    if (blackToMove) {
      let best = -Infinity;
      for (const move of moves) {
        best = Math.max(best, minimax(applyChessMove(position, move), remainingDepth - 1, alpha, beta, false));
        alpha = Math.max(alpha, best);
        if (beta <= alpha) break;
      }
      return best;
    }

    let best = Infinity;
    for (const move of moves) {
      best = Math.min(best, minimax(applyChessMove(position, move), remainingDepth - 1, alpha, beta, true));
      beta = Math.min(beta, best);
      if (beta <= alpha) break;
    }
    return best;
  }

  const candidates = collectMoves(board, 'black');
  let bestMove = null;
  let bestScore = -Infinity;
  let alpha = -Infinity;
  for (const move of candidates) {
    const score = minimax(applyChessMove(board, move), Math.max(0, depth - 1), alpha, Infinity, false);
    if (score > bestScore) {
      bestScore = score;
      bestMove = move;
    }
    alpha = Math.max(alpha, bestScore);
  }
  return bestMove;
}
