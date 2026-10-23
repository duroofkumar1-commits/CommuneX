# Architecture

## Request flow
React/Vite → Axios (`/api`) → Spring Security JWT filter → controller → service/business rules → Spring Data JPA repository → PostgreSQL/Neon.

The frontend is not an authorization boundary. Sensitive operations must be authorized by backend roles and ownership checks. Public response paths use DTOs; `passwordHash` also uses `@JsonIgnore` as defense in depth.

## Backend domains
- `users`: authentication, profile, account action tokens, blocks and roles
- `volunteers`: volunteer profiles and availability
- `requests`: request lifecycle, updates, state rules and priority ordering
- `community`: posts, comments, groups/discussions, polls and announcements
- `events`: events and registration
- `facilities`: directory and verified status
- `resources`: inventory and transactions
- `messaging`: direct messages/conversations/read markers
- `notifications`: in-app inbox/read state
- `documents`: saved editor documents
- `games`: scores and leaderboard
- `common`: health, search, analytics, activity, admin and assistant
- `dsa`: reusable data structures/algorithms

## Frontend
`ProtectedLayout` provides shared navigation, theme/language toggles and notification unread count. Pages live under `frontend/src/pages`, common HTTP/expiry handling in `frontend/src/services/api.js` and styles in `frontend/src/index.css`.

## Data principles
- Do not seed fictional facility/stock information as verified fact.
- Facility submissions remain unverified until authorized review.
- Inventory changes create transaction history and are transactional.
- Help-request transitions have ownership/state checks and status history.
- Game points are recreational and never affect assistance priority.
- Selected local audio and Focus Mode notes stay in the browser.

## External dependencies
Neon/PostgreSQL for persistence; SMTP for real verification/reset mail; optional local Ollama-compatible endpoint for general language responses; OpenStreetMap and public OSRM demo endpoint for map/route demonstration.
