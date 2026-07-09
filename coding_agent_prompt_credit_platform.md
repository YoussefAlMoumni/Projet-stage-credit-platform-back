# Coding Agent Prompt — Plateforme Intelligente d'Octroi de Crédit

Copy everything below the line into the coding agent (Claude Code, Copilot, Continue.dev) with the project root as working directory.

---

## Role

You are a senior Java backend/desktop engineer. Build a complete, working, locally-runnable credit evaluation platform from scratch. Follow every specification below exactly. Do not ask clarifying questions — where a decision is unspecified, choose the most standard, production-conventional option and document the choice in a `DECISIONS.md` file at the project root.

## Project Summary

A local intelligent credit-granting platform. A banker enters a SIREN (or physical-person identifier), the system runs a five-stage AI evaluation pipeline against locally-hosted LLMs (via Ollama), and returns a structured credit decision report. The platform has:

1. A Spring Boot backend (REST API, security, persistence, orchestration).
2. A JavaFX desktop client consuming that backend.
3. A PostgreSQL database.
4. Ollama as the local LLM inference engine (models already available locally: `deepseek-r1:8b`, `deepseek-r1:14b`).

This replaces an existing Python/LangGraph/LangChain-Ollama prototype. The prototype's pipeline logic (sequential agents: solvabilité → historique → garanties → conformité → superviseur) must be reproduced faithfully in Java — do not invent a different pipeline shape.

## Hard Requirements

- Language: Java 21.
- Build tool: Maven (multi-module if it simplifies backend/desktop separation; single module acceptable if justified in `DECISIONS.md`).
- IDE target: Eclipse (Enterprise Java Developers) — project must import cleanly via Maven → Import Existing Maven Project.
- Backend framework: Spring Boot (latest stable 3.x), Spring Security, Spring Data JPA.
- Database: PostgreSQL. Provide `schema.sql` or rely on `ddl-auto=update` — pick one, document in `DECISIONS.md`.
- Desktop UI: JavaFX (latest LTS-compatible version for JDK 21), FXML-based views, MVC controller separation.
- LLM access: raw HTTP calls to Ollama's REST API (`http://localhost:11434/api/generate`). No LangChain, no Python, no external AI SDK.
- No hardcoded secrets. Use `application.properties` / `application-dev.properties` with placeholder values and a `.env.example` or documented properties template.
- All code must compile and run via `mvn clean package` and standard Eclipse "Run As → Java Application" / "Spring Boot App" without manual classpath edits.

## Functional Requirements

### 1. Dossier management
- CRUD for `Dossier` (personne physique/morale): SIREN or ID, name, raw financial/identity data blob (JSON or structured fields — your choice, document it).
- Persisted in PostgreSQL via Spring Data JPA repositories.

### 2. Evaluation pipeline (core logic)
Reproduce this exact sequential flow as a Spring-managed orchestrator class:

```
solvabilite -> historique -> garanties -> conformite -> superviseur
```

- Each of the first four stages is an independent "agent" class that:
  - Builds a role-specific prompt from the dossier + prior context available to it.
  - Calls Ollama with model `deepseek-r1:8b`.
  - Uses `num_predict=256`.
- The `superviseur` stage:
  - Receives the outputs of all four prior stages concatenated into its prompt.
  - Calls Ollama with model `deepseek-r1:14b`.
  - Uses `num_predict=768`.
  - Produces the final decision report (accepted/rejected/conditional + justification).

### 3. Run profiles
Implement two configurable execution profiles, selectable per request:

| Profile | worker_ctx | worker_keep_alive | supervisor_ctx | supervisor_keep_alive |
|---|---|---|---|---|
| FAST | 2048 | 0 | 4096 | 0 |
| FULL | 4096 | 300 | 8192 | 300 |

`keep_alive` and `num_ctx` map directly to Ollama's `/api/generate` request options (`options.num_ctx`, top-level `keep_alive`).

### 4. REST API
Expose at minimum:
- `POST /api/credit-requests/{siren}/evaluate?mode=FAST|FULL` → runs full pipeline, persists result, returns final report + all intermediate agent outputs.
- `GET /api/credit-requests/{siren}/history` → past evaluations for that SIREN.
- `GET /api/dossiers/{siren}` → dossier detail.
- `POST /api/dossiers` → create dossier.

### 5. Security
- Spring Security with role-based access: `ROLE_BANQUIER` (can submit evaluations, view reports), `ROLE_ADMIN` (full access, user management).
- Authentication mechanism: JWT (stateless, since the desktop client is a separate process from the backend). Document token issuance endpoint (`POST /api/auth/login`).
- Passwords hashed with BCrypt.

### 6. Desktop client (JavaFX)
Screens required:
- Login screen (calls `/api/auth/login`, stores JWT in memory for session).
- SIREN input / dossier lookup screen.
- Evaluation trigger screen (choose FAST/FULL, show progress while pipeline runs).
- Report view screen (final decision + expandable per-agent detail).
- History view (past evaluations for a SIREN).
- Admin view (visible only to `ROLE_ADMIN`): user/role management.

The desktop client must talk to the backend exclusively over HTTP (loopback `localhost:8080` in dev), never call Ollama or the database directly.

## Non-Functional Requirements

- Clean package structure separating `domain`, `repository`, `service`, `controller`, `agents`, `workflow`, `config`, `security`, and `desktop` (client-side FXML/controllers).
- Meaningful logging at INFO level for each pipeline stage (stage name, model used, duration).
- Basic error handling: Ollama unreachable, malformed model response, DB connection failure — all must return clear errors, not stack traces, to the API caller.
- Unit tests for: each agent's prompt-building logic, the orchestrator's sequential execution order, and the REST controller's request/response mapping. Use JUnit 5 + Mockito.
- README.md at project root covering: prerequisites (JDK 21, Maven, PostgreSQL, Ollama with both models pulled), setup steps, how to run backend, how to run desktop client, how to run tests.
- A `DECISIONS.md` documenting every point left to your discretion above and why you chose it.

## Deliverables Checklist

Produce, in order, and confirm each compiles/runs before moving to the next:

1. Maven project skeleton importable into Eclipse.
2. `application.properties` + PostgreSQL connection wiring, verified with a trivial JPA entity round-trip.
3. `OllamaClient` HTTP wrapper, verified with a manual smoke-test call against a running local Ollama instance.
4. All five agent classes + orchestrator, verified with a scripted end-to-end dossier evaluation producing a final report.
5. REST controllers + Spring Security + JWT auth, verified with `curl`/Postman-style manual calls (document the exact commands used in README).
6. JavaFX desktop client wired to the backend, verified by running the full user flow: login → SIREN input → trigger evaluation → view report → view history.
7. Unit test suite, all green.
8. Final README.md and DECISIONS.md.

## Constraints

- Do not introduce Python, LangChain, or LangGraph anywhere in this codebase — this is a full rewrite, not a wrapper around the existing prototype.
- Do not skip the FAST/FULL profile distinction — both must be functional and independently testable.
- Do not deviate from the five-stage sequential pipeline order.
- Do not use any hosted/cloud LLM API — Ollama only, local inference only.
- Keep the desktop client and backend as separate deployable units even if run on the same machine during development.

## Start Here

Begin with deliverable 1 (Maven skeleton). After each deliverable, report what was built, how it was verified, and any deviation from this spec with justification recorded in `DECISIONS.md` before proceeding to the next deliverable.
