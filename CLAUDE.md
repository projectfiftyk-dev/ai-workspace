# AI Communication Workspace — Project Context

## What this is
A source-agnostic tool that ingests conversations (pasted text now, Teams/Slack
later), extracts structure via an LLM, and manages the result as tasks. Core
loop: `messages` → AI analysis → `ai_results` → `tasks`, all referencing a
`sources` document so the pipeline doesn't care where a message came from.

See [docs/project_plan_v2.md](docs/project_plan_v2.md) for the full 7–8 week
plan and the reasoning behind the `ConversationSourceProvider` abstraction.

## Tech stack
- **backend/** — Spring Boot 3.3.4, Java 17, MongoDB (Spring Data), stateless
  JWT auth (jjwt), Spring Security
- **ai-service/** — FastAPI (Python), will host the LLM abstraction
  (`LLMProvider` interface, Gemini Flash implementation)
- **frontend/** — React 19 + TypeScript + Vite, react-router-dom, plain React
  Context for state (no Redux/Zustand)
- **Infra** — `docker-compose.yml` runs mongodb + backend + frontend together;
  MongoDB is also runnable standalone via `docker compose up -d mongodb`

## Conventions
- Backend package layout: `controller` / `service` / `repository` / `model` /
  `dto` / `security` / `source` (provider abstraction lives here)
- New ingestion sources implement `ConversationSourceProvider`
  (`source/ConversationSourceProvider.java`) — paste is a special case
  (`PasteIngestService`) since there's nothing to pull
- Frontend: `pages/` (routed views), `components/` (shared UI), `context/`
  (AuthContext), `lib/api.ts` (fetch wrapper with Bearer token + `/api` proxy
  to backend in `vite.config.ts`)
- Auth is stateless Bearer JWT, not cookies — CORS is intentionally permissive
  because there's no cookie-based CSRF surface
- Mongo collections: `sources`, `messages`, `ai_results`, `tasks`, `users`,
  `organizations` — schema documented in the project plan

## Local dev setup
- MongoDB: `docker compose up -d mongodb` → exposed on host port **27018**
  (not 27017, to avoid clashing with any locally installed Mongo)
- Backend: needs `MONGO_URI` and `JWT_SECRET` as real environment variables
  when run from an IDE/`mvn spring-boot:run` — the root `.env` file is only
  auto-loaded by `docker compose`, not by Spring Boot directly. Set both in
  the IntelliJ Run Configuration (or export them in your shell) using the
  values from `.env`, with `MONGO_URI` pointed at port 27018.
- Frontend: `npm run dev` in `frontend/` (proxies `/api` to `localhost:8080`)

## Current status
Week 1 (foundation) is complete: scaffolds for all three services, real
email/password auth, Mongo collections v2, `PasteIngestService` +
`MockTeamsProvider`, and the paste endpoint verified end-to-end (UI → API →
Mongo). Frontend also has early page stubs (Dashboard/Kanban/Discussion) that
are actually Week 4 scope — Dashboard content itself is still a placeholder.
Next up per the plan: Week 2, wiring the FastAPI `/analyze` endpoint into the
backend so pasted messages produce real `ai_results` and `tasks`.

## Important rules for Claude
- Always TypeScript on the frontend, never plain JS; no `any`
- Keep the `ConversationSourceProvider` abstraction intact — adding a new
  source (Slack, Messenger) should mean writing one new class, not touching
  existing pipeline code
- Keep the LLM behind an interface in `ai-service` for the same reason
- Sticky notes / note-to-task mapping is explicitly out of MVP scope — don't
  build it unless asked
