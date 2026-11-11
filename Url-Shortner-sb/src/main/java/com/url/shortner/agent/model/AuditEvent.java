package com.url.shortner.agent.model;

import java.time.Instant;

public record AuditEvent(Instant timestamp, String actor, String action, String details) {}
