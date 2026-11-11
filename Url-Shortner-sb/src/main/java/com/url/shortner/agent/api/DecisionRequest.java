package com.url.shortner.agent.api;

import jakarta.validation.constraints.NotBlank;

public record DecisionRequest(@NotBlank String decision, @NotBlank String rationale) {}
