# CommuneX feature inventory and delivery status

This is an implementation inventory for the supplied source bundle. Source presence is not equivalent to production verification; see `testing.md` and `deployment.md` before a live release.

## Application areas included in source

| Area | Included work | Important condition |
|---|---|---|
| Authentication | Register, login, JWT, password hashing, protected routes, email verification/password-action token flows | SMTP and a full security review are required for production email flows |
| Profiles and roles | User profile, volunteer profile, role-aware API endpoints and admin role paths | Server-side authorization remains authoritative |
| Community | Posts, comments/replies, reactions, groups, polls, announcements, bookmarks and reports | Moderation and abuse controls need security/E2E coverage |
| Assistance | Create/list/update help requests, status history and priority ordering | Platform coordination only; not an official emergency service |
| Facilities and maps | Facility directory/search and map explorer using available facility coordinates | Do not claim facilities are nearby/verified unless records are present and maintained; public demo routing/tiles have no production SLA |
| Resources/inventory | Resource endpoints, inventory adjustments and transaction-history schema | Stock accuracy depends on authorized, current updates |
| Events | Events and RSVP endpoints/screens | Calendar/email reminders need additional provider setup if enabled |
| Messaging and notifications | Private-message and in-app notification modules | Production abuse/rate-limit and privacy testing still required |
| Search and activity | Search, activity history and analytics endpoints/screens | Analytics must be read as observed platform data, not demo guarantees |
| Assistant | Controlled backend assistant paths and optional local model configuration | Local model is optional; perform grounding/prompt-injection/security evaluation |
| Music player | Browser-local audio selection, playback controls, queue navigation/repeat/shuffle | Audio remains user-provided/local; no copyrighted catalogue is bundled |
| Text editor and focus mode | Draft editing, word/character counts, autosave and undo/redo behavior | Browser-local draft persistence is not cloud backup |
| Games | Tic-Tac-Toe, Snake, Memory Match, quiz, Tower of Hanoi, sliding puzzle, Sudoku, maze, Connect Four and word puzzle | Scores may be saved through API; run browser tests on a clean install |
| Gamification | Activity/impact view, badges/progress/challenge UI | Only server-backed records should be described as verified contributions |
| DSA | Nine sorting algorithms plus searching, linked lists, stacks/queues, trees/heaps, graph algorithms, trie/KMP and advanced structures | Automated tests are included for key algorithm groups; full algorithm test coverage is still required |
| Engineering | Flyway migration scripts, sample env files, Dockerfiles, Compose and GitHub Actions workflow | Existing Neon schema must be inspected/baselined before migrations are enabled there |

## Not yet verified for production

- Clean dependency installation and frontend production build on the target Windows machine.
- Maven compile and tests for the complete backend.
- Full API/integration test suite, security tests and Playwright journeys.
- Migration parity against the user's current Neon database.
- Live SMTP, optional AI model and deployment-provider configuration.
- Live map/routing provider limits and deployment CORS configuration.
- Production secrets, backups, monitoring, HTTPS and operational runbook.

Do not describe this source package as already deployed or fully production-certified. Complete the checks above before real users depend on it.
