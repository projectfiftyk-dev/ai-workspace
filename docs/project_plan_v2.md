# AI Communication Workspace — MVP Plan v2

**Change from v1:** scope expanded to be source-agnostic from day one (paste + Teams, architected for Slack/Messenger later), with manual/auto sync control. Timeline extended from 5 to 7–8 weeks to fit this properly instead of cramming it in.
**Sticky notes / AI note-to-task feature: explicitly deferred to post-MVP.** Reasoning below.

---

## 1. Why sticky notes are out of MVP scope

Everything else in this plan is one product: ingest a conversation → extract structure → manage it as tasks. Sticky notes are a second interaction paradigm (freeform spatial notes, two-way AI mapping between unstructured notes and structured tasks) with their own data model and UI. Bolting it on late would hurt both the MVP demo and the feature itself. It's kept as a named, real roadmap item — not lost, just sequenced after the core loop is proven and you've seen how real extracted tasks actually look in practice, which will make the note-mapping design better anyway.

---

## 2. Core architectural decision: `ConversationSource` abstraction

Two different ingestion patterns exist, and the design needs to accommodate both without forcing one into the other's shape:

- **Pull-based sources** (Teams, Slack, Messenger, future connectors): backend periodically fetches new messages via an API, on a schedule or manual trigger, per-channel.
- **Push-based source** (copy-paste): user submits text directly; there's nothing to "sync," it becomes a message immediately and flows straight into analysis.

Both converge on the same downstream pipeline (`messages` → `AnalysisService` → `ai_results` → `tasks`). The abstraction:

```
interface ConversationSourceProvider {
  String getProviderType();               // "teams", "slack", "paste", etc.
  List<MessageDTO> fetchNewMessages(SourceConfig config);  // pull-based only
  // paste doesn't implement fetch — it goes through a separate PasteIngestService
  // that wraps input into a Source + Message and calls AnalysisService directly
}
```

Implementations for MVP: `PasteIngestService` (not a fetch-based provider, a direct ingest path) and `MockTeamsProvider` → swapped for `GraphTeamsProvider` once a real tenant is available. Adding Slack later means writing one new class implementing this interface — nothing else in the app changes.

### Same pattern applied to the AI service

The FastAPI service abstracts the LLM behind an `LLMProvider` interface (`analyze(messages) -> AnalysisResult`), with a `GeminiProvider` implementation for now (Gemini Flash, free tier — Pro was removed from Gemini's free tier in April 2026, and Flash is sufficient for structured extraction). Swapping providers later, or comparing quality across them, means writing one new class. Free-tier note: Gemini's free tier may use inputs to improve their models, so this is fine for mock/synthetic test data, but should be reconsidered before real Teams data ever flows through it.

---

## 3. Updated data model

### `sources` (generalizes the old `channels` collection)
```json
{
  "_id": ObjectId,
  "orgId": ObjectId,
  "provider": "paste | teams | slack | messenger",
  "externalId": "string | null",       // null for paste (ephemeral, no external channel)
  "displayName": "string",
  "syncMode": "manual | auto",
  "enabled": true,
  "lastSyncedAt": "ISODate | null"
}
```

### `messages`
```json
{
  "_id": ObjectId,
  "sourceId": ObjectId,
  "externalMessageId": "string | null",  // null for paste
  "threadId": "string | null",
  "authorName": "string | null",         // null allowed for paste (no author metadata)
  "content": "string",
  "sentAt": ISODate,
  "syncedAt": ISODate
}
```

### `ai_results` and `tasks`
Unchanged in shape from v1 — they reference `sourceId` instead of `channelId`, but the structure (summary, action items, decisions, deadlines / status, assignee, source references) stays the same. This is the payoff of the abstraction: the AI pipeline and task system don't care where a message came from.

---

## 4. Revised 7–8 Week Plan

### Week 1 — Foundation, source abstraction, paste working end-to-end
- Repo scaffold: Spring Boot + MongoDB, FastAPI skeleton, React + TS
- Real email login (skip Teams OAuth for now — not a blocker for anything else)
- MongoDB collections v2 (`sources`, `messages`, `ai_results`, `tasks`) with the schema above
- `ConversationSourceProvider` interface + `PasteIngestService` + `MockTeamsProvider`
- Paste endpoint: submit text → stored as a `source` (type `paste`) + `message` → confirm it lands in Mongo correctly
- **Checkpoint:** paste some text via API, see it stored correctly. No AI yet — that's next.

### Week 2 — AI pipeline, proven against paste first
- FastAPI `/analyze` endpoint per the contract (unchanged from v1)
- Backend wiring: message(s) → AI service → `ai_results` → auto-created `tasks`
- Prove the **entire core loop** end-to-end using paste input only: paste text → summary/actions/decisions appear → tasks created and linked back to source
- This is deliberately Azure-independent — you get a fully working demo of the core value prop this week regardless of tenant status

### Week 3 — Sync mechanics + mock Teams
- Scheduled job for pull-based sources, respecting `syncMode` (auto runs on schedule, manual waits for a trigger)
- Manual "sync now" endpoint per source
- `MockTeamsProvider` returns realistic seeded fake channel data through the same pipeline proven in Week 2
- Settings groundwork: data model support for enabling/disabling sources and choosing sync mode (UI comes Week 6)

### Week 4 — Frontend core
- Dashboard: today's activity, open action items, recent summaries (source-agnostic — shows paste and Teams items uniformly)
- Kanban board, drag-and-drop, link back to source
- Discussion view: raw messages + AI output, works identically regardless of source type
- Paste input UI (a simple "paste a conversation" screen hitting the Week 1–2 pipeline)

### Week 5 — Real Teams integration (whenever tenant is ready)
- Implement `GraphTeamsProvider` against the interface — if your tenant access (Developer Program / Business Basic trial / work IT) has landed by now, swap it in
- If tenant access is still blocked, this week's checkpoint becomes "confirm `GraphTeamsProvider` is code-complete and unit-tested against mocked Graph responses," and the live swap happens whenever access clears — the rest of the plan doesn't wait on it
- Because everything downstream was already validated against `MockTeamsProvider`, this is a narrow, low-risk task by this point

### Week 6 — Source management UI + search
- Settings screen: add/enable/disable sources, toggle manual vs. auto sync, select which channels to include
- Basic keyword search (Mongo text index) over messages and summaries
- Extensibility check: write a short internal doc showing what a hypothetical `SlackProvider` would need to implement — proves the architecture actually holds up, without spending the time building Slack itself

### Week 7 — Polish
- Responsive pass, empty/loading states across all views
- Error handling pass on the sync jobs and AI pipeline (partial failures, API timeouts)
- Deploy to a basic cloud setup

### Week 8 — Buffer + demo prep
- Absorb whatever slipped from earlier weeks (Teams tenant access delays are the most likely culprit)
- README with architecture diagram, screenshots, demo GIF, and a clearly scoped **Roadmap** section listing: additional connectors (Slack, Messenger, Discord), semantic search, reminders/weekly reports, and the sticky notes + AI mapping feature with a short paragraph on the intended concept

---

## 5. Definition of done for MVP

- [ ] Paste text → AI extraction → tasks created, linked back to source
- [ ] Teams (real or mock, depending on tenant timing) → same pipeline, same result
- [ ] Manual and auto sync both functional, user can choose per source
- [ ] Dashboard, Kanban, Discussion view all source-agnostic
- [ ] Basic keyword search works
- [ ] Architecture demonstrably supports adding a new source type without touching existing code
- [ ] Sticky notes feature is *not* built, but is documented as a scoped, intentional next phase