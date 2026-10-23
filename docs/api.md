# API route guide

All application endpoints start with `/api`. Except health and auth/recovery entry points, API requests require `Authorization: Bearer <JWT>`.

| Domain | Representative routes | Access notes |
|---|---|---|
| Health | `GET /api/health` | Public |
| Auth | `POST /api/auth/register`, `/login`, `/forgot-password`, `/reset-password`, `/verify-email`, `/resend-verification` | Email delivery needs SMTP |
| User | `GET/PUT /api/users/me`, `/api/users/blocks` | Signed-in user; role comes from backend |
| Volunteer | `/api/volunteers/register`, `/me`, `/available` | Signed-in; profile routes role restricted |
| Assistance | `/api/requests`, `/open`, `/my`, `/{id}/accept`, `/{id}/in-progress`, `/{id}/complete`, `/{id}/cancel`, `/{id}/history`, `/{id}/status` | Role, ownership and state checked in backend |
| Community | `/api/community/feed`, `/posts`, `/posts/{id}/comments`, post like/bookmark/report and moderation routes | Moderation is role restricted |
| Groups | `/api/groups`, `/{id}/join`, `/{id}/membership`, `/api/groups/{groupId}/discussions` | Signed-in membership/discussion actions |
| Polls | `/api/polls`, `/{id}/vote` | One vote per user/poll enforced |
| Announcements | `/api/announcements` | Publishing role restricted |
| Events | `/api/events`, `/{id}/register`, `/{id}/registration` | Registration/capacity checked in service |
| Facilities | `GET /api/facilities?q=&category=`; create/update/archive routes | Write needs privileged facility roles |
| Resources | `GET /api/resources?q=&lowStock=`; POST; `PATCH /{id}/quantity`; `GET /{id}/history` | Quantity updates role restricted and recorded |
| Messages | `/api/messages`, `/conversations`, `/read` | Participant scope must be enforced by backend |
| Notifications | `/api/notifications`, `/unread-count`, `/{id}/read` | Own inbox |
| Search | `GET /api/search?q=` | Signed-in; supported platform records |
| Assistant | `POST /api/assistant/chat` | Backend-controlled lookup/guidance |
| Documents | `/api/documents` | Own documents only |
| Games | `POST /api/games/scores`, `GET /scores/me`, `GET /leaderboard?game=` | Score validation; unrelated to assistance priority |
| Activity | `GET /api/activity` | Own activity |
| Analytics | `GET /api/analytics` | Verify response scope before production release |
| Admin | `/api/admin/overview`, `/users`, `/users/{id}/role` | ADMIN only |

See controllers and DTOs for exact request/response shapes. This table is a route guide, not a generated OpenAPI contract.
