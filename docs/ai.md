# CommuneX Assistant

The route is `POST /api/assistant/chat`. The backend performs deterministic platform lookups for facility/resource questions and provides help-request, volunteer and routing guidance. It does not give a model direct database access or accept arbitrary SQL.

For optional general-language answers, configure `COMMUNEX_AI_BASE_URL` and `COMMUNEX_AI_MODEL` for a compatible local Ollama deployment. Leave the base URL blank to disable model generation. If the local endpoint fails, the app returns a clear fallback rather than inventing a result.

Before public use, evaluate prompt injection, private-data leakage, permissions, rate limits, output grounding, timeouts and error handling. Consequential operations should require explicit UI confirmation and backend authorization; the current assistant provides lookup/guidance rather than arbitrary state-changing tools. It must never invent facility, inventory, user-record, route or emergency facts.
