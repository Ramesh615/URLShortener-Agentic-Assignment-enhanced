# Production-oriented upgrade (v2)

This upgrade preserves the existing URL shortener and legacy `/api/agentic/**` API, and adds a durable workflow foundation under `/api/v2/workflows`.

## Implemented in this repository
- Flyway-managed schema for URL shortening and workflow persistence.
- Durable workflow run, task and audit tables; optimistic locking on run/task rows.
- Owner-scoped workflow read/decision operations using the authenticated principal.
- Explicit task dependency graph and dependency-success checks.
- Versioned requirements and SHA-256 approval scope covering the requirement plus pre-approval artifacts.
- Human clarification and approval gates; deployment is not exposed.
- Provider-neutral `AgentProvider` interface with a deterministic demo implementation.
- Bounded in-process retries and persisted attempt/audit events.
- Replanning invalidates stale outputs; clarification regenerates upstream artifacts before approval.
- Owner-scoped metrics: total, completed, safe-stopped, rejected and active/awaiting counts, success rate, retry events and safe-stop events.
- URL destination validation (absolute HTTP(S), no embedded credentials), 2048-character database alignment, and atomic click-count increments.
- JWT configuration fails fast for missing/invalid secrets; invalid tokens do not create partial authentication.
- Consistent API error responses without leaking unexpected exception details.
- Java 21 non-root backend container image.

## Run locally
1. Install Java 21, Docker and Node.js 20+.
2. Generate a local secret: `export JWT_SECRET=$(openssl rand -base64 48)`.
3. Start backend and MySQL: `cd Url-Shortner-sb && docker compose up --build`.
4. Run frontend separately: `cd Url-Shortner-Frontend && npm ci && npm run dev`.
5. Authenticate using the existing login flow and call `POST /api/v2/workflows` with:
   `{"scenario":"GREENFIELD","requirement":"Build a URL shortener with analytics"}`.

## v2 endpoints
- `POST /api/v2/workflows`
- `GET /api/v2/workflows/{id}`
- `GET /api/v2/workflows/metrics`
- `POST /api/v2/workflows/{id}/approve`
- `POST /api/v2/workflows/{id}/reject`
- `POST /api/v2/workflows/{id}/clarifications`
- `POST /api/v2/workflows/{id}/replans`

All v2 workflow endpoints require JWT authentication and workflow ownership.

## Explicit production gaps — do not represent these as implemented
This is a production-oriented assignment prototype, not a production deployment certification.

- `DemoAgentProvider` is deterministic and does not call an LLM. A live provider still needs strict structured output schemas, tool allow-lists, prompt-injection-resistant context handling, redaction, token/time budgets and provider timeout/circuit-breaker controls.
- Task execution is synchronous inside the request transaction. For multi-instance production, move execution to a durable queue/worker model with short DB transactions, atomic task claiming, lease renewal, stale-lease recovery, cancellation, outbox publishing and dead-letter handling.
- Testing, security and documentation are separate DAG branches but currently run sequentially. Parallel fan-out/fan-in requires durable worker coordination.
- Reviewer identity is the authenticated workflow owner. Enterprise reviewer RBAC, separation of duties and approval delegation must be integrated with trusted IAM.
- Replanning invalidates all artifacts; semantic impact analysis is not implemented.
- No generated code is executed or automatically applied. There is no shell execution, Git patching, container sandbox, SAST/SCA, SBOM signing or deployment.
- Rollback is a safe-stop of orchestration work, not compensation for already committed external side effects.
- Metrics include owner-scoped run counters, retry events and safe-stop events. True rollback frequency, measured end-to-end latency, task histograms, MTTR and distributed traces are not yet implemented.
- Add rate limiting, abuse controls, redirect reputation checks, analytics retention/privacy controls, tenant isolation, secret-manager integration, backups, disaster recovery and operational SLOs before shared production use.
- Add database-backed integration tests, concurrency/load tests, migration tests and security scanning in CI before release.

## Security notes
- Set a unique Base64 JWT secret of at least 256 bits using `JWT_SECRET`; do not use a committed/default production secret.
- Hibernate DDL is `validate`; Flyway owns schema evolution.
- Workflow records are scoped by authenticated owner. Do not treat owner-scoped access as reviewer authorization.
- Never execute LLM-produced shell commands on the application host.
- Restrict CORS, configure TLS at the ingress, protect Actuator exposure, and use managed secrets in deployment.
