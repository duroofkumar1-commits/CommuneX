# CommuneX AI integration

The platform assistant is designed around controlled backend features rather than arbitrary database/SQL access. The backend can search platform data and explain how to use CommuneX; optional general-model chat can be configured with an Ollama-compatible local API.

## Optional local model

Set these environment variables in `backend/.env`:

- `COMMUNEX_AI_BASE_URL` — base URL of a locally running Ollama-compatible service (leave blank to disable model calls).
- `COMMUNEX_AI_MODEL` — local model name, such as `llama3.2` when that model is actually installed.

Model availability depends on the user's machine and local model installation. No hosted paid provider is configured by default.

## Safety boundary

- Platform search must use backend services and current database results.
- Do not let the model execute arbitrary SQL or modify records directly.
- Check authorization on the backend for every private or consequential tool.
- Never present missing facility/resource information as verified fact.
- Assistance tools are coordination features, not a replacement for official emergency services.
- Do not place API keys, credentials, or tokens in frontend code or source control.

AI integrations need separate evaluation for factual grounding, authorization, prompt injection, unavailable data and model errors before production use.
