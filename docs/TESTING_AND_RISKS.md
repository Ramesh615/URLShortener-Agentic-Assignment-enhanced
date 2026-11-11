# Testing, Validation, Risks and Limitations

## Automated tests present
- Legacy `AgenticWorkflowServiceTest`: greenfield approval flow, rejection, ambiguity clarification, duplicate approval rejection and replanning.
- `UrlMappingServiceValidationTest`: rejects javascript destinations, relative destinations, embedded URL credentials and oversized destinations.

Run: `cd Url-Shortner-sb && ./mvnw clean test`.

## Validation still required before release
- Build and run the full Maven test suite in CI with dependency access.
- Add MySQL Testcontainers integration tests for Flyway migrations, JPA mappings, transaction boundaries and URL click concurrency.
- Add v2 workflow integration tests for owner isolation, approval hash invalidation, clarification re-execution, bounded retries, audit durability and optimistic-lock conflicts.
- Add API security tests for JWT expiry/signature failures, role authorization, CORS and rate limiting.
- Add load tests for redirect throughput, analytics queries and workflow creation.
- Add dependency/SAST scanning, secret scanning and SBOM generation.

## Manual end-to-end validation
1. Generate `JWT_SECRET` with `openssl rand -base64 48`.
2. Start MySQL and backend using Docker Compose.
3. Register/login using `/api/auth/public/register` and `/api/auth/public/login`.
4. Create a short URL; verify redirect, click count and analytics.
5. Create GREENFIELD and BROWNFIELD v2 workflows; verify the approval gate and owner-only access.
6. Create an AMBIGUOUS workflow; clarify it and verify the upstream artifacts are regenerated before approval.
7. Approve and reject workflows; inspect task states and audit events.
8. Verify `/api/v2/workflows/metrics` returns only the authenticated owner's aggregate counts.

## Risks and mitigations
- **Malicious redirect destinations:** enforce absolute HTTP(S) and reject embedded credentials. Do not fetch destination URLs server-side; add reputation/deny lists and abuse controls before public launch.
- **Short-code collision:** cryptographic random generation and a database unique constraint protect integrity. Concurrent duplicate-key collisions may still return a conflict; implement bounded insert retry with transaction isolation before high-volume launch.
- **Click counter contention:** use an atomic database increment to avoid lost updates. Validate throughput and counter overflow under load.
- **Analytics privacy:** current click events store timestamps only. Define consent, retention, deletion and anonymization before collecting IP/referrer/device.
- **Agent hallucination/tool misuse:** demo agents are deterministic and have no unrestricted tools. A live LLM adapter must validate structured outputs and use allow-listed tools, budgets and human gates.
- **Workflow availability:** persistence survives restart, but execution is synchronous and request-bound. A process crash during a task needs a durable worker/lease recovery design.
- **Authorization:** workflow access is owner-scoped. Independent reviewer RBAC and separation of duties are not implemented.
- **Rollback:** safe-stop prevents downstream orchestration; it does not reverse arbitrary external side effects.
- **Metrics:** current success rate is a run-count ratio, not an SLO. MTTR and end-to-end latency are not currently measured.

## Release decision
Do not deploy this prototype to a shared production environment until the outstanding validation and production gaps in `PRODUCTION_UPGRADE.md` are addressed.
