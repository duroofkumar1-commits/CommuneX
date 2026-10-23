import { useCallback, useEffect, useMemo, useState } from "react";
import api from "../services/api";
import "./logic-lab.css";

const LAB_GAMES = [
  { id: "logic-pinpoint", name: "Pinpoint", icon: "◉", type: "WORD ASSOCIATION", dsa: "Hash sets · clue filtering · candidate elimination", description: "Find the hidden category connecting the clues." },
  { id: "logic-crossclimb", name: "Crossclimb", icon: "↗", type: "WORD LADDER", dsa: "Graph search · Hamming distance · ordering", description: "Put words in a ladder where neighbours differ by one letter." },
  { id: "logic-queens", name: "Queens", icon: "♛", type: "CONSTRAINT GRID", dsa: "Backtracking · constraint satisfaction · pruning", description: "Place one queen in each row and column without touching diagonally." },
  { id: "logic-tango", name: "Tango", icon: "☯", type: "BINARY LOGIC", dsa: "Constraint propagation · row/column counts · pattern checks", description: "Balance suns and moons without runs of three." },
  { id: "logic-zip", name: "Zip", icon: "⌁", type: "PATHFINDING", dsa: "Grid graph · DFS-style path validation · visited sets", description: "Connect numbered checkpoints while visiting every cell." },
  { id: "logic-sudoku", name: "Mini Sudoku", icon: "▦", type: "NUMBER LOGIC", dsa: "Backtracking · bitsets · row/column/box constraints", description: "Complete a 6×6 Sudoku with 2×3 regions." },
  { id: "logic-patches", name: "Patches", icon: "▧", type: "SPATIAL LOGIC", dsa: "Connected components · flood fill · region-size constraints", description: "Partition the board into connected patches of five cells." },
  { id: "logic-wend", name: "Wend", icon: "⌗", type: "WORD SEARCH", dsa: "Trie-style prefix checks · graph traversal · visited cells", description: "Trace each hidden word through neighbouring letters." },
];

const PINPOINTS = [
  { clues: ["Mercury", "Venus", "Earth", "Mars", "Jupiter"], answer: "planets", aliases: ["planet", "planets", "planets in the solar system"] },
  { clues: ["Piano", "Violin", "Cello", "Harp", "Guitar"], answer: "string instruments", aliases: ["string instrument", "string instruments", "instruments with strings"] },
  { clues: ["Triangle", "Square", "Pentagon", "Hexagon", "Octagon"], answer: "polygons", aliases: ["polygon", "polygons", "shapes with straight sides"] },
  { clues: ["Ruby", "Python", "Java", "Kotlin", "Swift"], answer: "programming languages", aliases: ["programming language", "programming languages", "coding languages"] },
  { clues: ["Oak", "Maple", "Birch", "Cedar", "Willow"], answer: "trees", aliases: ["tree", "trees"] },
  { clues: ["Carbon", "Oxygen", "Hydrogen", "Nitrogen", "Helium"], answer: "chemical elements", aliases: ["element", "elements", "chemical elements"] },
  { clues: ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday"], answer: "weekdays", aliases: ["weekday", "weekdays", "working days"] },
  { clues: ["Denim", "Silk", "Wool", "Linen", "Velvet"], answer: "fabrics", aliases: ["fabric", "fabrics", "textiles"] },
];
const LADDERS = [
  ["COLD", "CORD", "CARD", "WARD", "WARM"],
  ["HEAD", "HEAL", "TEAL", "TELL", "TALL"],
  ["SAND", "SEND", "SENT", "WENT", "WEND"],
  ["FALL", "FILL", "FILE", "FIRE", "FIRM"],
  ["HARD", "HARE", "HIRE", "FIRE", "FIVE"],
];
const TANGO_SOLUTION = ["001101", "011010", "110100", "101001", "010011", "100110"].map((row) => row.split("").map(Number));
const QUEEN_SOLUTION = [1, 3, 5, 0, 2, 4];
const SUDOKU_SOLUTION = Array.from({ length: 6 }, (_, r) => Array.from({ length: 6 }, (_, c) => ((r * 3 + Math.floor(r / 2) + c) % 6) + 1));
const WEND_BANK = [
  { rows: ["CODE", "PATH", "NODE", "TREE"], words: ["CODE", "PATH", "NODE", "TREE"] },
  { rows: ["MATH", "LOOP", "DATA", "BYTE"], words: ["MATH", "LOOP", "DATA", "BYTE"] },
  { rows: ["JAVA", "TASK", "GRID", "SORT"], words: ["JAVA", "TASK", "GRID", "SORT"] },
  { rows: ["NODE", "HEAP", "HASH", "LINK"], words: ["NODE", "HEAP", "HASH", "LINK"] },
];

function rotate(array, amount) {
  const n = ((amount % array.length) + array.length) % array.length;
  return [...array.slice(n), ...array.slice(0, n)];
}
function shuffleBy(array, seed) {
  const result = [...array];
  let x = seed * 9301 + 49297;
  for (let i = result.length - 1; i > 0; i -= 1) {
    x = (x * 9301 + 49297) % 233280;
    const j = Math.floor((x / 233280) * (i + 1));
    [result[i], result[j]] = [result[j], result[i]];
  }
  return result;
}
function buildZip(level) {
  const path = [];
  for (let r = 0; r < 5; r += 1) {
    const cols = Array.from({ length: 5 }, (_, c) => c);
    if (r % 2) cols.reverse();
    cols.forEach((c) => path.push([r, c]));
  }
  const checkpointIndexes = [0, 4, 9, 14, 19, 24];
  return { path, checkpoints: Object.fromEntries(checkpointIndexes.map((idx, i) => [`${path[idx][0]}-${path[idx][1]}`, i + 1])), level };
}
function getSudoku(level) {
  return SUDOKU_SOLUTION.map((row, r) => row.map((value, c) => ((r * 7 + c * 3 + level) % (level < 5 ? 2 : 3) === 0 ? value : 0)));
}
function connected(cells) {
  if (!cells.length) return false;
  const all = new Set(cells.map(([r, c]) => `${r}-${c}`));
  const seen = new Set();
  const queue = [cells[0]];
  while (queue.length) {
    const [r, c] = queue.shift();
    const key = `${r}-${c}`;
    if (seen.has(key)) continue;
    seen.add(key);
    [[r - 1, c], [r + 1, c], [r, c - 1], [r, c + 1]].forEach(([nr, nc]) => {
      if (all.has(`${nr}-${nc}`) && !seen.has(`${nr}-${nc}`)) queue.push([nr, nc]);
    });
  }
  return seen.size === all.size;
}
function neighbours(a, b) { return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]) === 1; }

function PuzzleBoard({ gameId, level, onWin }) {
  const [guess, setGuess] = useState("");
  const [revealed, setRevealed] = useState(1);
  const [ladder, setLadder] = useState(() => shuffleBy(LADDERS[(level - 1) % LADDERS.length], level + 3));
  const [queens, setQueens] = useState([]);
  const [tango, setTango] = useState(() => Array.from({ length: 6 }, () => Array(6).fill(-1)));
  const [zip, setZip] = useState(() => ({ ...buildZip(level), path: [[0, 0]] }));
  const [sudoku, setSudoku] = useState(() => getSudoku(level));
  const [selectedCell, setSelectedCell] = useState(null);
  const [patches, setPatches] = useState(() => Array.from({ length: 5 }, () => Array(5).fill(-1)));
  const [activePatch, setActivePatch] = useState(0);
  const [wendFound, setWendFound] = useState([]);
  const [wendPath, setWendPath] = useState([]);
  const [feedback, setFeedback] = useState("");
  const [won, setWon] = useState(false);

  const pinpoint = PINPOINTS[(level - 1) % PINPOINTS.length];
  const ladderSolution = LADDERS[(level - 1) % LADDERS.length];
  const zipData = useMemo(() => buildZip(level), [level]);
  const wend = WEND_BANK[(level - 1) % WEND_BANK.length];
  const wendGrid = useMemo(() => shuffleBy(wend.rows, level + 11).map((word) => word.split("")), [wend, level]);



  const finish = useCallback((message = "Puzzle solved!") => {
    if (won) return;
    setWon(true); setFeedback(message); onWin();
  }, [won, onWin]);

  const submitPinpoint = () => {
    const normalized = guess.trim().toLowerCase().replace(/[^\p{L}\p{N} ]/gu, "").replace(/\s+/g, " ");
    if (pinpoint.aliases.includes(normalized)) finish(`Correct — the category is ${pinpoint.answer}.`);
    else { setFeedback("Not quite. Try another category or reveal another clue."); setRevealed((value) => Math.min(5, value + 1)); }
  };

  const moveWord = (index, delta) => {
    const target = index + delta;
    if (target < 0 || target >= ladder.length) return;
    const next = [...ladder]; [next[index], next[target]] = [next[target], next[index]];
    setLadder(next);
  };
  const checkLadder = () => {
    if (ladder.every((word, i) => word === ladderSolution[i])) finish("Word ladder ordered correctly.");
    else setFeedback("Not yet. Adjacent words must differ by exactly one letter.");
  };

  const toggleQueen = (r, c) => {
    setQueens((old) => {
      const existing = old.find(([row]) => row === r);
      if (existing && existing[1] === c) return old.filter(([row]) => row !== r);
      return [...old.filter(([row]) => row !== r), [r, c]];
    });
    setFeedback("");
  };
  const checkQueens = () => {
    if (queens.length !== 6) { setFeedback("Place exactly one queen in every row."); return; }
    const cols = queens.map((q) => q[1]);
    if (new Set(cols).size !== 6) { setFeedback("Two queens share a column."); return; }
    const ordered = [...queens].sort((a, b) => a[0] - b[0]);
    for (let i = 1; i < ordered.length; i += 1) {
      if (Math.abs(ordered[i][1] - ordered[i - 1][1]) <= 1) { setFeedback("Queens cannot touch diagonally."); return; }
    }
    finish("Board solved — all row, column and adjacency constraints hold.");
  };

  const toggleTango = (r, c) => setTango((old) => old.map((row, ri) => row.map((value, ci) => ri === r && ci === c ? (value === -1 ? 0 : value === 0 ? 1 : -1) : value)));
  const checkTango = () => {
    if (tango.some((row) => row.some((value) => value < 0))) { setFeedback("Fill every cell first."); return; }
    const rowsOk = tango.every((row) => row.reduce((sum, v) => sum + v, 0) === 3);
    const colsOk = Array.from({ length: 6 }, (_, c) => tango.reduce((sum, row) => sum + row[c], 0)).every((sum) => sum === 3);
    const noTriple = [...tango, ...Array.from({ length: 6 }, (_, c) => tango.map((row) => row[c]))].every((line) => !line.some((v, i) => i > 1 && v === line[i - 1] && v === line[i - 2]));
    if (rowsOk && colsOk && noTriple) finish("Balanced! Every row and column has three of each symbol.");
    else setFeedback("Check the balance: 3 suns and 3 moons per row/column, no run of three.");
  };

  const clickZipCell = (r, c) => {
    const current = zip.path;
    const key = `${r}-${c}`;
    const checkpoint = zipData.checkpoints[key];
    const visitedIndex = current.findIndex(([rr, cc]) => rr === r && cc === c);
    if (visitedIndex >= 0) {
      if (visitedIndex === current.length - 1 && current.length > 1) setZip({ ...zip, path: current.slice(0, -1) });
      return;
    }
    if (!neighbours(current[current.length - 1], [r, c])) { setFeedback("The path must move one cell at a time."); return; }
    const nextCheckpoint = Object.keys(zipData.checkpoints).filter((k) => current.some(([rr, cc]) => `${rr}-${cc}` === k)).length + 1;
    if (checkpoint && checkpoint !== nextCheckpoint) { setFeedback(`Reach checkpoint ${nextCheckpoint} next.`); return; }
    const nextPath = [...current, [r, c]];
    setZip({ ...zipData, path: nextPath });
    setFeedback("");
    if (nextPath.length === 25 && Object.keys(zipData.checkpoints).every((k) => nextPath.some(([rr, cc]) => `${rr}-${cc}` === k))) finish("Full route completed — every cell visited.");
  };

  const editSudoku = (value) => {
    if (!selectedCell) { setFeedback("Select an empty cell first."); return; }
    const [r, c] = selectedCell;
    if (getSudoku(level)[r][c] !== 0) { setFeedback("That cell is locked by a clue."); return; }
    setSudoku((old) => old.map((row, ri) => row.map((v, ci) => ri === r && ci === c ? value : v)));
    setFeedback("");
  };
  const checkSudoku = () => {
    if (sudoku.some((row) => row.some((v) => v === 0))) { setFeedback("Fill every empty cell."); return; }
    const validLines = [...sudoku, ...Array.from({ length: 6 }, (_, c) => sudoku.map((row) => row[c]))].every((line) => new Set(line).size === 6 && line.every((v) => v >= 1 && v <= 6));
    const validBoxes = [0, 2, 4].every((r0) => [0, 3].every((c0) => {
      const vals = [];
      for (let r = r0; r < r0 + 2; r += 1) for (let c = c0; c < c0 + 3; c += 1) vals.push(sudoku[r][c]);
      return new Set(vals).size === 6;
    }));
    if (validLines && validBoxes) finish("Sudoku solved — every row, column and 2×3 region is valid.");
    else setFeedback("A row, column or 2×3 region has a duplicate.");
  };

  const assignPatch = (r, c) => setPatches((old) => old.map((row, ri) => row.map((v, ci) => ri === r && ci === c ? (v === activePatch ? -1 : activePatch) : v)));
  const checkPatches = () => {
    for (let id = 0; id < 5; id += 1) {
      const cells = [];
      patches.forEach((row, r) => row.forEach((v, c) => { if (v === id) cells.push([r, c]); }));
      if (cells.length !== 5 || !connected(cells)) { setFeedback(`Patch ${String.fromCharCode(65 + id)} needs exactly five connected cells.`); return; }
    }
    finish("All five patches contain five connected cells.");
  };

  const chooseWendCell = (r, c) => {
    const cell = [r, c];
    if (wendPath.some(([rr, cc]) => rr === r && cc === c)) { setWendPath([]); return; }
    if (wendPath.length && !neighbours(wendPath[wendPath.length - 1], cell)) { setFeedback("Choose a neighbouring letter. Reset the path to start again."); return; }
    const next = [...wendPath, cell];
    const letters = next.map(([rr, cc]) => wendGrid[rr][cc]).join("");
    const target = wend.words.find((word) => word === letters);
    setWendPath(next); setFeedback("");
    if (target && !wendFound.includes(target)) {
      const found = [...wendFound, target]; setWendFound(found); setWendPath([]);
      if (found.length === wend.words.length) finish("Every hidden word found.");
    } else if (!wend.words.some((word) => word.startsWith(letters))) {
      setFeedback("That path doesn't begin a target word."); setWendPath([]);
    }
  };

  const rows = useMemo(() => {
    if (gameId === "logic-wend") return wendGrid;
    return [];
  }, [gameId, wendGrid]);

  return <div className="logic-board-wrap">
    <div className="logic-board-top"><span>LEVEL {String(level).padStart(2, "0")} / 50</span><span>{won ? "SOLVED ✓" : "PUZZLE ACTIVE"}</span></div>
    {gameId === "logic-pinpoint" && <div className="logic-puzzle-content">
      <div className="logic-clue-list">{pinpoint.clues.slice(0, revealed).map((clue, i) => <div key={clue}><span>CLUE 0{i + 1}</span><strong>{clue}</strong></div>)}</div>
      <p className="logic-hint">Find the shared category. Fewer clues revealed earns a better score.</p>
      <form className="logic-answer-form" onSubmit={(e) => { e.preventDefault(); submitPinpoint(); }}><input value={guess} onChange={(e) => setGuess(e.target.value)} placeholder="Guess the category…" aria-label="Guess the category" disabled={won} /><button className="logic-btn primary" type="submit" disabled={won}>CHECK ANSWER</button></form>
      <button className="logic-btn subtle" onClick={() => setRevealed((v) => Math.min(5, v + 1))} disabled={won || revealed === 5}>Reveal next clue ({revealed}/5)</button>
    </div>}
    {gameId === "logic-crossclimb" && <div className="logic-puzzle-content"><p className="logic-hint">Move each word up or down until every neighbouring pair differs by one letter.</p><div className="logic-ladder">{ladder.map((word, i) => <div className="logic-ladder-row" key={`${word}-${i}`}><span>{String(i + 1).padStart(2, "0")}</span><strong>{word}</strong><div><button aria-label="Move word up" disabled={i === 0 || won} onClick={() => moveWord(i, -1)}>↑</button><button aria-label="Move word down" disabled={i === ladder.length - 1 || won} onClick={() => moveWord(i, 1)}>↓</button></div></div>)}</div><button className="logic-btn primary" onClick={checkLadder} disabled={won}>CHECK LADDER</button></div>}
    {gameId === "logic-queens" && <div className="logic-puzzle-content"><p className="logic-hint">Place six queens. One per row and column; queens may not touch, even diagonally.</p><div className="logic-queens-grid">{Array.from({ length: 36 }, (_, i) => { const r = Math.floor(i / 6), c = i % 6, active = queens.some(([qr, qc]) => qr === r && qc === c); return <button key={i} aria-label={`Row ${r + 1}, column ${c + 1}`} className={`${(r + c) % 2 ? "shade" : ""} ${active ? "queen-active" : ""}`} onClick={() => toggleQueen(r, c)} disabled={won}>{active ? "♛" : ""}</button>; })}</div><div className="logic-action-row"><span>{queens.length}/6 queens placed</span><button className="logic-btn primary" onClick={checkQueens} disabled={won}>CHECK BOARD</button></div></div>}
    {gameId === "logic-tango" && <div className="logic-puzzle-content"><p className="logic-hint">Tap each cell to cycle blank → sun → moon. Every row and column needs three of each.</p><div className="logic-tango-grid">{tango.map((row, r) => row.map((v, c) => <button key={`${r}-${c}`} className={v === 0 ? "sun" : v === 1 ? "moon" : ""} onClick={() => toggleTango(r, c)} disabled={won}>{v === 0 ? "☀" : v === 1 ? "☾" : ""}</button>))}</div><button className="logic-btn primary" onClick={checkTango} disabled={won}>CHECK BALANCE</button></div>}
    {gameId === "logic-zip" && <div className="logic-puzzle-content"><p className="logic-hint">Start at 1 and trace an adjacent path through all 25 cells. Click the last cell to backtrack.</p><div className="logic-zip-grid">{Array.from({ length: 25 }, (_, i) => { const r = Math.floor(i / 5), c = i % 5, key = `${r}-${c}`, checkpoint = zipData.checkpoints[key], idx = zip.path.findIndex(([rr, cc]) => rr === r && cc === c); return <button key={key} className={`${idx >= 0 ? "path-cell" : ""} ${checkpoint ? "checkpoint" : ""}`} onClick={() => clickZipCell(r, c)} disabled={won}>{checkpoint || (idx >= 0 ? "•" : "")}</button>; })}</div><div className="logic-action-row"><span>{zip.path.length}/25 cells visited</span><button className="logic-btn subtle" onClick={() => { setZip({ ...zipData, path: [[0, 0]] }); setFeedback(""); }} disabled={won}>RESTART PATH</button></div></div>}
    {gameId === "logic-sudoku" && <div className="logic-puzzle-content"><p className="logic-hint">Select an empty cell, then choose a number. Each row, column and 2×3 box uses 1–6 once.</p><div className="logic-sudoku-grid">{sudoku.map((row, r) => row.map((value, c) => { const locked = getSudoku(level)[r][c] !== 0; const selected = selectedCell?.[0] === r && selectedCell?.[1] === c; return <button key={`${r}-${c}`} className={`${locked ? "locked" : ""} ${selected ? "selected" : ""} ${(Math.floor(r / 2) + Math.floor(c / 3)) % 2 ? "box-shade" : ""}`} onClick={() => { setSelectedCell([r, c]); setFeedback(""); }} disabled={won}>{value || ""}</button>; }))}</div><div className="logic-number-pad">{[1, 2, 3, 4, 5, 6].map((v) => <button key={v} onClick={() => editSudoku(v)} disabled={won}>{v}</button>)}<button onClick={() => editSudoku(0)} disabled={won}>⌫</button></div><button className="logic-btn primary" onClick={checkSudoku} disabled={won}>CHECK SUDOKU</button></div>}
    {gameId === "logic-patches" && <div className="logic-puzzle-content"><p className="logic-hint">Choose patch A–E, then assign cells. Every patch must be one connected shape of exactly five cells.</p><div className="logic-patch-select">{["A", "B", "C", "D", "E"].map((letter, i) => <button key={letter} className={activePatch === i ? "active" : ""} onClick={() => setActivePatch(i)} disabled={won}>{letter}<small>{patches.flat().filter((v) => v === i).length}/5</small></button>)}</div><div className="logic-patches-grid">{patches.map((row, r) => row.map((value, c) => <button key={`${r}-${c}`} className={value >= 0 ? `patch-${value}` : ""} onClick={() => assignPatch(r, c)} disabled={won}>{value >= 0 ? String.fromCharCode(65 + value) : "·"}</button>))}</div><button className="logic-btn primary" onClick={checkPatches} disabled={won}>CHECK PATCHES</button></div>}
    {gameId === "logic-wend" && <div className="logic-puzzle-content"><p className="logic-hint">Trace each word through adjacent letters. Each word found is marked below.</p><div className="logic-wend-grid">{rows.map((row, r) => row.map((letter, c) => <button key={`${r}-${c}`} className={wendPath.some(([rr, cc]) => rr === r && cc === c) ? "selected" : ""} onClick={() => chooseWendCell(r, c)} disabled={won}>{letter}</button>))}</div><div className="logic-word-list">{wend.words.map((word) => <span key={word} className={wendFound.includes(word) ? "found" : ""}>{wendFound.includes(word) ? "✓ " : "○ "}{word}</span>)}</div><button className="logic-btn subtle" onClick={() => { setWendPath([]); setFeedback(""); }} disabled={won}>RESET WORD PATH</button></div>}
    {feedback && <div className={`logic-feedback ${won ? "success" : ""}`} role="status">{feedback}</div>}
    {won && <div className="logic-win-panel"><strong>LEVEL CLEARED</strong><span>+100 XP · next level unlocked</span><button className="logic-btn primary" onClick={() => onWin(true)}>NEXT LEVEL →</button></div>}
  </div>;
}

function emptyProgress() {
  return Object.fromEntries(LAB_GAMES.map((game) => [game.id, { currentLevel: 1, highestUnlockedLevel: 1, totalScore: 0, victories: 0 }]));
}
function loadLocalProgress(email) {
  try { return { ...emptyProgress(), ...JSON.parse(localStorage.getItem(`communex.logic-lab.${email}`) || "{}") }; }
  catch { return emptyProgress(); }
}

export default function LinkedInInspiredGames() {
  const email = localStorage.getItem("communex.email") || "account";
  const [activeId, setActiveId] = useState(LAB_GAMES[0].id);
  const [progress, setProgress] = useState(() => loadLocalProgress(email));
  const [level, setLevel] = useState(1);
  const [toast, setToast] = useState("");
  const [showLevels, setShowLevels] = useState(false);
  const activeGame = LAB_GAMES.find((game) => game.id === activeId) || LAB_GAMES[0];
  const record = progress[activeId] || { currentLevel: 1, highestUnlockedLevel: 1, totalScore: 0, victories: 0 };

  useEffect(() => {
    let mounted = true;
    api.get("/games/progress/me").then(({ data }) => {
      if (!mounted || !Array.isArray(data)) return;
      const merged = { ...loadLocalProgress(email) };
      data.forEach((row) => {
        if (row.game && LAB_GAMES.some((game) => game.id === row.game)) merged[row.game] = { currentLevel: row.currentLevel, highestUnlockedLevel: row.highestUnlockedLevel, totalScore: row.totalScore, victories: row.victories };
      });
      setProgress(merged); localStorage.setItem(`communex.logic-lab.${email}`, JSON.stringify(merged));
      setLevel(merged[activeId]?.currentLevel || 1);
    }).catch(() => {});
    return () => { mounted = false; };
  }, [email]);

  const persist = useCallback(async (id, next) => {
    const merged = { ...loadLocalProgress(email), [id]: next };
    localStorage.setItem(`communex.logic-lab.${email}`, JSON.stringify(merged)); setProgress(merged);
    try { await api.put(`/games/progress/${id}`, next); }
    catch { /* local save remains available while backend is offline */ }
  }, [email]);

  const clearLevel = useCallback(async (nextLevel = false) => {
    const current = loadLocalProgress(email)[activeId] || record;
    if (nextLevel) {
      setToast("");
      const next = Math.min(50, level + 1);
      setLevel(next);
      await persist(activeId, { ...current, currentLevel: next, highestUnlockedLevel: Math.max(current.highestUnlockedLevel || 1, next) });
      return;
    }
    const unlocked = Math.min(50, Math.max(current.highestUnlockedLevel || 1, level + 1));
    const next = { currentLevel: Math.min(50, Math.max(level + 1, current.currentLevel || 1)), highestUnlockedLevel: unlocked, totalScore: (current.totalScore || 0) + 100, victories: (current.victories || 0) + 1 };
    setToast(`Level ${level} cleared — level ${unlocked} is unlocked.`);
    await persist(activeId, next);
  }, [activeId, email, level, persist, record]);

  const selectGame = (id) => { setActiveId(id); setLevel(progress[id]?.currentLevel || 1); setToast(""); setShowLevels(false); };
  const chooseLevel = (next) => { setLevel(next); setToast(""); setShowLevels(false); void persist(activeId, { ...record, currentLevel: next }); };
  const resetProgress = async () => {
    const blank = emptyProgress();
    localStorage.setItem(`communex.logic-lab.${email}`, JSON.stringify(blank)); setProgress(blank); setLevel(1); setToast("Logic Lab progress reset.");
    await Promise.all(LAB_GAMES.map((game) => api.put(`/games/progress/${game.id}`, blank[game.id]).catch(() => null)));
  };

  return <section className="logic-lab" id="logic-lab">
    <div className="logic-lab-hero">
      <div><span className="logic-eyebrow">COMMUNEX ARCADE / LOGIC LAB</span><h2>Daily puzzles.<br /><em>Algorithms underneath.</em></h2><p>Eight original, playable puzzle modes inspired by daily word and logic-game genres. DSA powers the checks, path validation, constraint solving and board logic — you just play.</p><div className="logic-lab-meta"><span>8 PUZZLE MODES</span><span>50 LEVELS EACH</span><span>ACCOUNT PROGRESS</span></div></div>
      <div className="logic-lab-hero-art" aria-hidden="true"><div className="logic-orbit orbit-a" /><div className="logic-orbit orbit-b" /><div className="logic-orbit orbit-c" /><span>⌘</span><small>THINK / TRACE / SOLVE</small></div>
    </div>
    <div className="logic-lab-heading"><div><span className="logic-eyebrow">THE PUZZLE COLLECTION</span><h3>Choose a challenge</h3><p>Each mode has 50 progressively unlocked levels. Progress saves to your account when the backend is available, with a browser fallback.</p></div><button className="logic-btn subtle" onClick={resetProgress}>↻ Reset Logic Lab</button></div>
    <div className="logic-game-tabs">{LAB_GAMES.map((game) => <button key={game.id} className={activeId === game.id ? "active" : ""} onClick={() => selectGame(game.id)}><span>{game.icon}</span><strong>{game.name}</strong><small>LVL {String(progress[game.id]?.currentLevel || 1).padStart(2, "0")}</small></button>)}</div>
    <div className="logic-lab-workspace">
      <div className="logic-lab-main">
        <div className="logic-game-title-row"><div><span className="logic-eyebrow">{activeGame.type} / {LAB_GAMES.findIndex((game) => game.id === activeId) + 1}/08</span><h3>{activeGame.name}</h3><p>{activeGame.description}</p></div><button className="logic-btn subtle" onClick={() => setShowLevels((v) => !v)}>{showLevels ? "Hide levels" : "Level select ▾"}</button></div>
        {showLevels && <div className="logic-level-select">{Array.from({ length: 50 }, (_, i) => i + 1).map((n) => <button key={n} disabled={n > record.highestUnlockedLevel} className={`${n === level ? "current" : ""} ${n < record.highestUnlockedLevel ? "cleared" : ""}`} onClick={() => chooseLevel(n)}>{n > record.highestUnlockedLevel ? "·" : n}</button>)}</div>}
        <div className="logic-dsa-note"><span>⌘</span><div><strong>DSA RUNNING IN THE BACKGROUND</strong><p>{activeGame.dsa}</p></div></div>
        <PuzzleBoard key={`${activeId}-${level}`} gameId={activeId} level={level} onWin={clearLevel} />
        {toast && <div className="logic-toast" role="status">{toast}</div>}
      </div>
      <aside className="logic-lab-aside"><div className="logic-record-card"><span className="logic-eyebrow">YOUR RECORD</span><h4>{activeGame.name} stats</h4><div><span>Current level</span><strong>{record.currentLevel}/50</strong></div><div><span>Highest unlocked</span><strong>{record.highestUnlockedLevel}/50</strong></div><div><span>Levels cleared</span><strong>{Math.max(0, record.highestUnlockedLevel - 1)}</strong></div><div><span>XP earned</span><strong>{(record.totalScore || 0).toLocaleString()}</strong></div><div className="logic-record-track"><i style={{ width: `${(Math.max(0, record.highestUnlockedLevel - 1) / 50) * 100}%` }} /></div><small>Saved for {email}. Browser saving works even if the API is offline.</small></div><div className="logic-record-card logic-dsa-card"><span className="logic-eyebrow">WHY IT COUNTS AS DSA</span><h4>Real algorithms, not DSA quizzes</h4><p>You solve the puzzle naturally while the implementation uses data structures and algorithms to validate moves, search paths, check constraints and track regions.</p><ul><li>Backtracking and constraint pruning</li><li>Grid graphs and visited sets</li><li>Flood fill / connected components</li><li>Hash-based clue matching</li></ul></div></aside>
    </div>
    <p className="logic-lab-footnote">Independent CommuneX implementations inspired by daily puzzle genres; not affiliated with LinkedIn and not using LinkedIn’s proprietary assets or daily puzzle content.</p>
  </section>;
}
