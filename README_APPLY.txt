COMMUNEX — DSA FIXES + UNUSED FILE CLEANUP
==========================================

This is a PATCH BUNDLE, not a replacement for the whole CommuneX project.
It contains only files that were changed or added for the DSA corrections,
DSA documentation, tests, and unused-file cleanup.

1) APPLY THE UPDATED FILES
--------------------------
Extract this ZIP directly into the root of your existing CommuneX project
(the folder that contains frontend/, backend/, docs/, and package files).
Allow the existing Games.jsx and games.css files to be overwritten.

2) RUN THE UNUSED-FILE CLEANUP (WINDOWS / POWERSHELL)
-----------------------------------------------------
Open PowerShell in the CommuneX project root and run:

  powershell -ExecutionPolicy Bypass -File .\CLEANUP_UNUSED.ps1 -ProjectRoot (Get-Location).Path

This removes only the exact unused source files and empty placeholders listed
in DELETE_MANIFEST.txt. It deliberately preserves .git, .env files,
frontend/vercel.json, and the historical Flyway backup.

Optional: remove generated folders when preparing a smaller source archive:

  powershell -ExecutionPolicy Bypass -File .\CLEANUP_UNUSED.ps1 -ProjectRoot (Get-Location).Path -RemoveGeneratedFolders

That optional switch removes frontend/node_modules, frontend/dist, and
backend/target. Rebuildable dependencies/artifacts can be restored; do not
use it if you need the currently installed dependencies or build output.

3) INSTALL / VERIFY FRONTEND
-----------------------------
From the frontend folder:

  npm ci
  npm run lint
  npm test
  npm run build

The supplied environment's node_modules had a missing platform-specific
Rolldown binding, so the full Vitest run and production build could not be
verified in that environment. The updated source passed ESLint and JSX
syntax validation, and the algorithm checks / 100-level pathfinding sweeps
were run separately.

4) FILES IN THIS PATCH
----------------------
- frontend/src/pages/Games.jsx
  Corrected Arcade DSA integrations and accurate game descriptions.
- frontend/src/utils/dsa/arcadeAlgorithms.js
  Shared graph traversal, pathfinding, heap, Sudoku backtracking,
  duplicate-aware Wordle feedback, and chess minimax implementations.
- frontend/src/pages/games.css
  Maze, portal, and tower route/exploration visual states.
- frontend/src/test/arcadeAlgorithms.test.js
  Unit tests for the new algorithms.
- docs/dsa.md
  Documentation revised to match actual application usage.
- CLEANUP_UNUSED.ps1 / DELETE_MANIFEST.txt
  Conservative cleanup instructions and exact deletion list.

Do not upload local .env or frontend/.env.local files to a public repository.
