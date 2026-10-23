# Feature matrix and release status

This document separates code surfaces from release verification. The frontend pages and backend domain layers are included for the features listed below, but production readiness requires clean builds, API integration tests, schema reconciliation and a live staging smoke test.

| Area | Included source | Release notes |
|---|---|---|
| Auth/profile/roles | registration, login, JWT, profile, account action-token endpoints, role checks | SMTP required for actual verification/recovery mail; rotate old secrets |
| Volunteers | register, profile, update, availability/discovery | verify role transition and ownership against DB |
| Assistance | open/mine/create, accept/start/complete/cancel/status/history, priority queue and notifications | coordination only, not emergency dispatch |
| Community | feed, posts, comments/replies, likes/bookmarks/reports/moderation | security review of blocks and abuse controls still required |
| Groups/polls/announcements/events | create/list/membership/discussions/votes/registration | review privacy, authorization and capacity/concurrency edges |
| Messaging | conversations, send, read state | run access/privacy/blocking security tests |
| Facilities/map | directory, filters, verified state, OSM embed and public OSRM routing | add only verified facility records; external service limits apply |
| Resources/inventory | inventory records, low-stock filter, quantity updates and history | test transaction concurrency and audit roles |
| Notifications/activity | inbox, unread/read state and personal activity | verify privacy and transactional event creation |
| Search/analytics/admin | global search, stats, admin overview and role management | verify query scope and admin provisioning |
| Assistant | controlled lookup and optional Ollama | general generation requires configured model and safety evaluation |
| Music/editor | local audio controls and browser playlist metadata; documents with local autosave/undo/redo and optional cloud sync | audio file bytes stay local; playlists currently save metadata locally |
| Games | ten playable games, score and leaderboard endpoints, personal points | points are entertainment-only, not verified community impact |
| Impact/focus | activity stats, daily local challenge, browser-local notes/audio | local challenge completion is not a verified contribution record |
| DSA | required sorting/search/graph/data-structure source and tests | Maven unavailable in this build environment; run `mvn verify` |
| UI/engineering | responsive nav, dark/light styles, partial Urdu navigation, PWA shell, CI workflow | full Urdu localization, accessibility audit and release checks remain |

## Release tasks
1. Fresh frontend install, lint, tests and build on supported Node 22.
2. Maven `verify` on Java 21.
3. Review existing Neon schema and migration baseline; back up before applying changes.
4. Configure and verify SMTP, optional AI, CORS and routing services.
5. Extend integration/E2E and security tests; deploy to staging and perform smoke tests.
