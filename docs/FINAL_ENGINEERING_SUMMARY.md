# Final Engineering Summary

## Assignment outcome
This repository extends the existing React + Spring Boot URL shortener with a controlled SDLC workflow API. The implementation targets the assignment's greenfield, brownfield and ambiguous scenarios and adds durable workflow persistence.

## Implemented
- URL shortening, redirects, click analytics and authenticated user-owned link management remain available.
- HTTP(S)-only URL validation rejects relative URLs, unsupported schemes, embedded credentials and destinations over 2048 characters.
- Atomic click-count updates reduce lost increments under concurrent redirects.
- JWT secret validation fails application startup for missing/invalid signing secrets.
- Consistent API error payloads avoid exposing unexpected exception messages or stack traces.
- Workflow runs, tasks and audit records are stored in MySQL through Flyway migrations.
- DAG dependencies are checked before task execution.
- Clarification re-runs upstream artifacts; approval hashes bind the decision to the current requirement and upstream outputs.
- Bounded task attempts and safe-stop states are persisted.
- Workflow reads and decisions are scoped to the authenticated owner.
- Owner-scoped run counters, success rate, retry events and safe-stop events are exposed.
- Backend container uses Java 21 and a non-root runtime user.

## Assignment scenario mapping
| Scenario | Demonstrated flow |
|---|---|
| Greenfield | Normalize requirement → plan → codebase analysis → design → approval → implementation → validation/docs → release readiness |
| Brownfield | Existing modules are identified by the deterministic codebase-analysis adapter; generated output calls for backward-compatibility and migration checks |
| Ambiguous | Workflow pauses for clarification; clarified requirement regenerates upstream artifacts before approval |

## Key assumptions
- MySQL 8.0 is the supported database for the included Flyway migrations.
- Demo agents are deterministic so the assignment can be run without an external model key.
- Approval is performed by the authenticated workflow owner; independent reviewer roles are not wired to enterprise IAM.
- Release readiness is advisory and never deploys.

## Validation status
- Maven POM XML and OpenAPI YAML parse successfully.
- URL validation unit tests have been added.
- Full Maven compilation/test execution was not verified in this environment because the Maven Wrapper could not download Maven 3.9.9 from Maven Central (network fetch failed).
- Docker was unavailable in this environment, so image build and container startup were not verified.

## Not production-complete
The current implementation is a production-oriented assignment prototype, not a claim of production readiness. Before deployment, implement durable asynchronous workers and leases, independent reviewer RBAC, idempotency keys, true parallel branch execution, LLM provider security controls, SAST/SCA/SBOM, distributed telemetry, MTTR and latency measurements, rate limiting, tenant isolation, privacy/retention controls, backup/DR and load/security testing.
