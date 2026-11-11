package com.url.shortner.agent.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkflowRun(
        String id, String scenario, String requirement, RunStatus status,
        WorkflowStage currentStage, boolean approvalRequired, String approvalReason,
        Map<String, WorkflowTask> tasks, List<AuditEvent> auditTrail,
        Map<String, String> decisions, Instant createdAt, Instant updatedAt,
        long durationMillis, int retryCount, int rollbackCount
) {}
