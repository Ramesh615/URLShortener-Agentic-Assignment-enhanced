package com.url.shortner.agent.service;

import com.url.shortner.agent.api.ApprovalRequest;
import com.url.shortner.agent.api.CreateRunRequest;
import com.url.shortner.agent.api.DecisionRequest;
import com.url.shortner.agent.model.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Deterministic agentic orchestration prototype. Agent adapters are intentionally
 * bounded and replaceable; no generated code is auto-deployed.
 */
@Service
public class AgenticWorkflowService {
    private final Map<String, MutableRun> runs = new ConcurrentHashMap<>();
    private final AtomicInteger totalRetries = new AtomicInteger();
    private final AtomicInteger totalRollbacks = new AtomicInteger();

    public WorkflowRun create(CreateRunRequest request) {
        String scenario = request.scenario().toUpperCase(Locale.ROOT);
        MutableRun run = new MutableRun(UUID.randomUUID().toString(), scenario, request.requirement());
        runs.put(run.id, run);
        event(run, "system", "RUN_CREATED", "Scenario=" + scenario);
        executeUntilGate(run);
        return snapshot(run);
    }

    public WorkflowRun get(String id) { return snapshot(find(id)); }

    public WorkflowRun approve(String id, String reviewer, ApprovalRequest request) {
        MutableRun run = find(id);
        synchronized (run) {
            if (run.status != RunStatus.WAITING_FOR_APPROVAL)
                throw new IllegalStateException("This workflow is not waiting for approval");
            event(run, reviewer, "HUMAN_APPROVED", request.comment() == null ? "" : request.comment());
            run.decisions.put("implementationApproval", reviewer + ": APPROVED");
            run.approvalRequired = false;
            run.status = RunStatus.RUNNING;
            executeAfterImplementationGate(run);
            return snapshot(run);
        }
    }

    public WorkflowRun clarify(String id, DecisionRequest request) {
        MutableRun run = find(id);
        synchronized (run) {
            if (run.status != RunStatus.WAITING_FOR_CLARIFICATION)
                throw new IllegalStateException("This workflow is not waiting for clarification");
            run.decisions.put("clarificationResolved", request.decision());
            run.decisions.put("clarificationRationale", request.rationale());
            event(run, "requester", "CLARIFICATION_PROVIDED", request.decision() + " | " + request.rationale());
            run.status = RunStatus.RUNNING;
            executeUntilGate(run);
            return snapshot(run);
        }
    }

    public WorkflowRun reject(String id, String reviewer, ApprovalRequest request) {
        MutableRun run = find(id);
        synchronized (run) {
            if (run.status != RunStatus.WAITING_FOR_APPROVAL)
                throw new IllegalStateException("This workflow is not waiting for approval");
            run.status = RunStatus.REJECTED;
            run.approvalRequired = false;
            run.decisions.put("implementationApproval", reviewer + ": REJECTED");
            event(run, reviewer, "HUMAN_REJECTED", request.comment() == null ? "No reason supplied" : request.comment());
            skipPending(run);
            touch(run);
            return snapshot(run);
        }
    }

    public WorkflowRun replan(String id, DecisionRequest request) {
        MutableRun run = find(id);
        synchronized (run) {
            if (run.status == RunStatus.COMPLETED || run.status == RunStatus.REJECTED)
                throw new IllegalStateException("Completed/rejected runs cannot be replanned");
            run.decisions.put("replan", request.decision() + " | " + request.rationale());
            event(run, "human", "REPLAN_REQUESTED", request.rationale());
            // Upstream requirement changes invalidate downstream outputs and require fresh review.
            run.tasks.values().forEach(t -> {
                if (t.state == TaskState.SUCCEEDED) { t.state = TaskState.PENDING; t.output = null; t.completedAt = null; }
            });
            run.status = RunStatus.RUNNING;
            run.approvalRequired = false;
            executeUntilGate(run);
            return snapshot(run);
        }
    }

    public Map<String, Object> metrics() {
        long completed = runs.values().stream().filter(r -> r.status == RunStatus.COMPLETED).count();
        long failed = runs.values().stream().filter(r -> r.status == RunStatus.FAILED || r.status == RunStatus.SAFE_STOP).count();
        long total = runs.size();
        double successRate = total == 0 ? 0 : (completed * 100.0 / total);
        double avgLatency = runs.values().stream().filter(r -> r.status == RunStatus.COMPLETED)
                .mapToLong(r -> Duration.between(r.createdAt, r.updatedAt).toMillis()).average().orElse(0);
        return Map.of("totalRuns", total, "completedRuns", completed, "failedOrSafeStoppedRuns", failed,
                "successRatePercent", successRate, "retryCount", totalRetries.get(),
                "rollbackCount", totalRollbacks.get(), "averageCompletedLatencyMs", avgLatency,
                "mttrMs", 0.0, "mttrNote", "No recovery events measured in this prototype");
    }

    private void executeUntilGate(MutableRun r) {
        try {
            runTask(r, "requirements");
            runTask(r, "planning");
            runTask(r, "codebase");
            runTask(r, "design");
            // Ambiguous requests must be clarified rather than silently completed with assumptions.
            if ("AMBIGUOUS".equals(r.scenario) && !r.decisions.containsKey("clarificationResolved")) {
                r.decisions.put("clarification", "Clarification required: confirm expiry duration, whether expiry is configurable per link, analytics dimensions, and retention policy.");
                r.status = RunStatus.WAITING_FOR_CLARIFICATION;
                r.approvalRequired = false;
                r.approvalReason = "Business ambiguity must be resolved before implementation approval.";
                event(r, "policy-engine", "CLARIFICATION_GATE", r.approvalReason);
                touch(r);
                return;
            }
            // High-impact implementation is held for an explicit human approval.
            r.status = RunStatus.WAITING_FOR_APPROVAL;
            r.approvalRequired = true;
            r.approvalReason = "Implementation changes require reviewer approval before execution.";
            event(r, "policy-engine", "APPROVAL_GATE", r.approvalReason);
            touch(r);
        } catch (Exception e) { failSafe(r, e); }
    }

    private void executeAfterImplementationGate(MutableRun r) {
        try {
            runTask(r, "implementation");
            // Independent validation and documentation agents run concurrently and join at release.
            runParallelTasks(r, "testing", "documentation");
            runTask(r, "release");
            r.status = RunStatus.COMPLETED;
            r.currentStage = WorkflowStage.COMPLETED;
            event(r, "orchestrator", "RUN_COMPLETED", "All required gates passed; no deployment performed.");
            touch(r);
        } catch (Exception e) { failSafe(r, e); }
    }

    private void runTask(MutableRun r, String id) {
        MutableTask task = r.tasks.get(id);
        for (String dependency : task.dependencies) {
            if (r.tasks.get(dependency).state != TaskState.SUCCEEDED)
                throw new IllegalStateException("Dependency " + dependency + " is not complete for " + id);
        }
        task.state = TaskState.RUNNING;
        task.startedAt = Instant.now();
        r.currentStage = task.stage;
        event(r, "agent:" + id, "TASK_STARTED", task.name);
        try {
            // Bounded retry policy (2 attempts). This prototype's built-in agents are deterministic.
            task.output = produceOutput(r, id);
            task.state = TaskState.SUCCEEDED;
            task.completedAt = Instant.now();
            event(r, "agent:" + id, "TASK_SUCCEEDED", "Output generated and recorded.");
        } catch (RuntimeException ex) {
            task.attempts++;
            if (task.attempts < 2) { totalRetries.incrementAndGet(); task.output = produceOutput(r, id); task.state = TaskState.SUCCEEDED; }
            else throw ex;
        }
        task.attempts++;
        touch(r);
    }

    private void runParallelTasks(MutableRun r, String first, String second) {
        MutableTask a = r.tasks.get(first), b = r.tasks.get(second);
        for (String dep : a.dependencies) if (r.tasks.get(dep).state != TaskState.SUCCEEDED)
            throw new IllegalStateException("Dependency " + dep + " is not complete for " + first);
        for (String dep : b.dependencies) if (r.tasks.get(dep).state != TaskState.SUCCEEDED)
            throw new IllegalStateException("Dependency " + dep + " is not complete for " + second);
        Instant started = Instant.now();
        a.state = TaskState.RUNNING; a.startedAt = started;
        b.state = TaskState.RUNNING; b.startedAt = started;
        event(r, "orchestrator", "PARALLEL_FAN_OUT", first + " and " + second);
        CompletableFuture<String> firstOutput = CompletableFuture.supplyAsync(() -> produceOutput(r, first));
        CompletableFuture<String> secondOutput = CompletableFuture.supplyAsync(() -> produceOutput(r, second));
        CompletableFuture.allOf(firstOutput, secondOutput).join();
        Instant completed = Instant.now();
        a.output = firstOutput.join(); a.state = TaskState.SUCCEEDED; a.completedAt = completed; a.attempts++;
        b.output = secondOutput.join(); b.state = TaskState.SUCCEEDED; b.completedAt = completed; b.attempts++;
        event(r, "agent:" + first, "TASK_SUCCEEDED", "Parallel output generated.");
        event(r, "agent:" + second, "TASK_SUCCEEDED", "Parallel output generated.");
        event(r, "orchestrator", "PARALLEL_JOIN", "Both branches synchronized.");
        touch(r);
    }

    private String produceOutput(MutableRun r, String id) {
        return switch (id) {
            case "requirements" -> "Normalized requirement: " + r.requirement
                    + "\nAcceptance: valid HTTP(S) URL; unique short code; redirect; click event; owner-scoped management.";
            case "planning" -> "DAG: requirements -> planning -> codebase -> design -> implementation -> {testing, documentation} -> release. "
                    + "Human approval before implementation and release-readiness gate.";
            case "codebase" -> "Brownfield impact: Spring Boot REST controllers, UrlMappingService, UrlMapping/ClickEvent entities, repositories, JWT security, React client. "
                    + "Preserve existing /api/urls and redirect contracts.";
            case "design" -> "Design: validate URL at boundary; generate collision-checked codes; persist mapping and click events transactionally; "
                    + "owner-check mutations; append audit events; never deploy generated changes automatically.";
            case "implementation" -> "Implementation plan/artifact: URL CRUD, redirect analytics, validation, secure configuration, orchestration API. "
                    + "Code changes are reviewed by a human; this agent records the proposed implementation scope.";
            case "testing" -> "Validation plan: unit tests for code generation/validation, integration tests for REST + database, auth/ownership, redirect and analytics; "
                    + "security checks for secrets, open redirects and input validation.";
            case "documentation" -> "Documentation artifact: architecture, API contract, run instructions, scenario walkthroughs, assumptions, risks and rollback procedure.";
            case "release" -> "Release readiness: tests and documentation tasks complete; approval required before any real deployment. Deployment is disabled in this prototype.";
            default -> throw new IllegalArgumentException("Unknown task " + id);
        };
    }

    private void failSafe(MutableRun r, Exception e) {
        r.status = RunStatus.SAFE_STOP;
        r.rollbackCount++;
        totalRollbacks.incrementAndGet();
        event(r, "policy-engine", "FALLBACK_TO_HUMAN_REVIEW", "Automated execution stopped; manual engineering review is required.");
        event(r, "policy-engine", "SAFE_STOP_ROLLBACK", e.getMessage());
        skipPending(r);
        touch(r);
    }

    private void skipPending(MutableRun r) {
        r.tasks.values().stream().filter(t -> t.state == TaskState.PENDING).forEach(t -> t.state = TaskState.SKIPPED);
        r.tasks.values().stream().filter(t -> t.state == TaskState.RUNNING).forEach(t -> t.state = TaskState.FAILED);
    }
    private MutableRun find(String id) {
        MutableRun run = runs.get(id);
        if (run == null) throw new NoSuchElementException("Workflow run not found: " + id);
        return run;
    }
    private void event(MutableRun r, String actor, String action, String details) {
        r.audit.add(new AuditEvent(Instant.now(), actor, action, details));
    }
    private void touch(MutableRun r) { r.updatedAt = Instant.now(); }
    private WorkflowRun snapshot(MutableRun r) {
        synchronized (r) {
            Map<String, WorkflowTask> tasks = new LinkedHashMap<>();
            r.tasks.forEach((k,t) -> tasks.put(k, new WorkflowTask(t.id,t.name,t.stage,List.copyOf(t.dependencies),t.state,t.output,t.attempts,t.startedAt,t.completedAt)));
            return new WorkflowRun(r.id,r.scenario,r.requirement,r.status,r.currentStage,r.approvalRequired,r.approvalReason,
                    Collections.unmodifiableMap(tasks),List.copyOf(r.audit),Map.copyOf(r.decisions),r.createdAt,r.updatedAt,
                    Duration.between(r.createdAt,r.updatedAt).toMillis(),totalRetries.get(),r.rollbackCount);
        }
    }

    private static class MutableRun {
        final String id, scenario, requirement;
        final Instant createdAt = Instant.now();
        Instant updatedAt = createdAt;
        RunStatus status = RunStatus.RUNNING;
        WorkflowStage currentStage = WorkflowStage.REQUIREMENTS;
        boolean approvalRequired;
        String approvalReason;
        int rollbackCount;
        final Map<String,String> decisions = new LinkedHashMap<>();
        final List<AuditEvent> audit = new ArrayList<>();
        final Map<String,MutableTask> tasks = new LinkedHashMap<>();
        MutableRun(String id,String scenario,String requirement) {
            this.id=id; this.scenario=scenario; this.requirement=requirement;
            add("requirements","Understand requirement",WorkflowStage.REQUIREMENTS);
            add("planning","Decompose tasks and dependencies",WorkflowStage.PLANNING,"requirements");
            add("codebase","Analyze impacted codebase",WorkflowStage.CODEBASE_ANALYSIS,"planning");
            add("design","Produce architecture and design",WorkflowStage.DESIGN,"codebase");
            add("implementation","Implement approved changes",WorkflowStage.IMPLEMENTATION,"design");
            add("testing","Run validation and tests",WorkflowStage.TESTING,"implementation");
            add("documentation","Generate engineering documentation",WorkflowStage.DOCUMENTATION,"implementation");
            add("release","Evaluate release readiness",WorkflowStage.RELEASE_READINESS,"testing","documentation");
        }
        void add(String id,String name,WorkflowStage stage,String... deps) {
            tasks.put(id,new MutableTask(id,name,stage,List.of(deps)));
        }
    }
    private static class MutableTask {
        final String id,name; final WorkflowStage stage; final List<String> dependencies;
        TaskState state=TaskState.PENDING; String output; int attempts; Instant startedAt,completedAt;
        MutableTask(String id,String name,WorkflowStage stage,List<String> dependencies) {
            this.id=id;this.name=name;this.stage=stage;this.dependencies=dependencies;
        }
    }
}
