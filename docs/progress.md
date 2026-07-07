# Progress Log

Tracks what's actually been built and verified against the plan in
[project_plan_v2.md](project_plan_v2.md). Update at the end of each week.

---

## Week 1 — Foundation, source abstraction, paste working end to end

**Status: Complete** (verified 2026-07-07)

### Delivered
- [x] Repo scaffold: Spring Boot + MongoDB backend, FastAPI `ai-service`
      skeleton, React + TS frontend
- [x] Real email/password login — register + login, bcrypt password hashing,
      stateless JWT auth, org auto-created for the first user to register
      under a given organization name
- [x] MongoDB collections v2 (`sources`, `messages`, `ai_results`, `tasks`)
      matching the schema in the project plan
- [x] `ConversationSourceProvider` interface, `PasteIngestService`
      (push-based, doesn't implement the interface — nothing to pull),
      `MockTeamsProvider` (pull-based stub)
- [x] Paste endpoint (`POST /api/paste`) — submit text → stored as a `Source`
      (`provider: "paste"`) + `Message`
- [x] **Checkpoint met:** pasted text via the actual UI (not just curl),
      confirmed the `Source` and `Message` documents in MongoDB directly

### Verified this session
- Registered a user through the UI, logged in, session persisted across
  reload
- Submitted text on the Paste Input page → `POST /api/paste` → `201 Created`
  → confirmed matching documents in `sources`/`messages` via `mongosh`
- Backend runs both via `docker compose` and directly from IntelliJ

### Ahead of schedule
Frontend already has routed, auth-gated page stubs for Dashboard, Kanban
Board, and Discussion View — these are Week 4 scope. Content is still
placeholder text; not a Week 1 requirement.

### Not yet built (correctly — these are later weeks)
- AI extraction / `/analyze` endpoint / `ai_results` / auto-created `tasks`
  (Week 2)
- Sync scheduling, manual "sync now", real `MockTeamsProvider` data flowing
  through the pipeline (Week 3)
- Real dashboard/kanban/discussion functionality (Week 4)
- Real Teams integration via `GraphTeamsProvider` (Week 5)

### Dev environment notes (for future reference)
- MongoDB via `docker compose up -d mongodb` — exposed on host port
  **27018**, not the default 27017 (a separate, unrelated local `mongod`
  install already occupies 27017 on this machine)
- Running the backend from IntelliJ requires `MONGO_URI` and `JWT_SECRET` to
  be set as real environment variables on the Run Configuration — the root
  `.env` file is only auto-loaded by `docker compose`, not by Spring Boot
  when launched directly
- Frontend dev server proxies `/api` → `localhost:8080` (see
  `frontend/vite.config.ts`)

---

## Week 2 — AI pipeline (next up)

Not started. Per the plan: FastAPI `/analyze` endpoint, backend wiring from
`Message` → AI service → `ai_results` → auto-created `Task`s, proven
end-to-end using paste input only.
