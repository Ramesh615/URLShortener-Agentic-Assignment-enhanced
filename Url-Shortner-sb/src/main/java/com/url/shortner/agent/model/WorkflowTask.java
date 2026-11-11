package com.url.shortner.agent.model;

import java.time.Instant;
import java.util.List;

public record WorkflowTask(
        String id, String name, WorkflowStage stage, List<String> dependsOn,
        TaskState state, String output, int attempts, Instant startedAt, Instant completedAt
) {}
