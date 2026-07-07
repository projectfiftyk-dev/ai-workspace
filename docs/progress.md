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

## Week 2 — AI pipeline, staged confirmation flow

**Status: Partially verified live** (as of 2026-07-07) — the core
paste → analysis loop has been confirmed working against the real stack
(backend, real `ai-service`/Gemini, frontend, all running for real, not
stubbed). Confirming an action item into a `Task` is still only verified by
an automated test, not by hand against the live stack yet.

The plan's data model was amended before this work started: action items
are staged (`confirmed: false`) rather than auto-created as `Task`s — a
`Task` is only created when a user explicitly confirms one. What's below
implements that version, not the earlier "auto-created tasks" phrasing in
the Week 2 plan bullet.

### Delivered
- [x] FastAPI `/analyze` endpoint (`ai-service`) backed by a `GeminiProvider`
      (Gemini 2.5 Flash) behind the `LLMProvider` interface
- [x] `AnalysisService` (backend): loads a source's messages, calls
      `/analyze` with a bounded timeout, maps the response into a staged
      `AiResult`
- [x] `AiResult.actionItems` is now `List<ActionItem>` (`text`, `assignee`,
      `deadline`, `confirmed`), defaulting to `confirmed: false`
- [x] `POST /api/sources/{sourceId}/analyze` — trigger/retry analysis for a
      source
- [x] `GET /api/ai-results/{id}` — fetch an `AiResult` including per-item
      `confirmed` state
- [x] `POST /api/ai-results/{id}/action-items/{index}/confirm` — confirms
      one action item and creates the corresponding `Task` (`status:
      backlog`)
- [x] `PasteIngestService` auto-triggers analysis right after a paste is
      saved; if the AI service is unreachable or times out, the paste still
      succeeds and the failure comes back as a separate `analysisError`
      rather than failing the request
- [x] Paste Input page extended: shows summary/decisions/deadlines
      read-only, lists action items each with a "Confirm as Task" button
      that flips to a confirmed state once clicked

### Confirmed working live (real backend, real ai-service, real UI)
- Paste text on the Paste Input page → real `/analyze` call to Gemini via
  `ai-service` → summary, decisions, deadlines, and action items render
  correctly on the page

### Not yet confirmed working live
- Clicking "Confirm as Task" against the real running backend — the
  endpoint and UI are implemented and pass an automated test (below), but
  nobody has clicked it against the live stack and checked MongoDB for the
  resulting `Task` yet. This is the next thing to check by hand.

### Also verified this session (automated, backend-only)
- Backend compiles clean; frontend typechecks clean
- A `MockMvc` integration test drove paste → analyze → confirm against a
  **real MongoDB** and a **real HTTP call** to a stub `/analyze` server (not
  real Gemini) — confirmed a `Task` document gets created and
  `ai_results.actionItems[].confirmed` flips correctly for just the
  confirmed item

### Note on this session's dev environment
This session's own sandboxed terminal couldn't bind Tomcat at all
(`Selector.open()` failed trying to use an AF_UNIX loopback pipe) — a
restriction specific to that sandboxed shell, not the backend code; running
the backend directly (e.g. via IntelliJ, as done for the live check above)
works fine.

### Not yet built (correctly — later weeks)
- Dashboard/Kanban/Discussion still don't surface `ai_results`/`tasks`
  anywhere — the only place to see an analysis is right after pasting
  (Week 4)
- Per-action-item `assignee`/`deadline` are always `null` — the LLM prompt
  only extracts a flat action item string, not structured fields per item
- Sync scheduling, mock/real Teams data flowing through this pipeline
  (Week 3, Week 5)

### Dev environment notes (new this session)
- `ai-service` needs `GEMINI_API_KEY` loaded into the shell manually —
  `main.py` doesn't call `load_dotenv()`, so the root `.env` isn't picked up
  automatically outside `docker compose`
- New backend env var: `AI_SERVICE_URL` (defaults to `http://localhost:8000`
  if unset)

### Next step
Click "Confirm as Task" against the live stack and verify the `Task`
document in MongoDB — once that's done, this section can move to
**Complete**.
