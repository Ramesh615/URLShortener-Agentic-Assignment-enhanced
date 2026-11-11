package com.url.shortner.agent.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateRunRequest(
        @NotBlank(message = "Requirement is required") String requirement,
        @NotBlank(message = "Scenario is required")
        @Pattern(regexp = "GREENFIELD|BROWNFIELD|AMBIGUOUS", flags = Pattern.Flag.CASE_INSENSITIVE,
                message = "Scenario must be GREENFIELD, BROWNFIELD, or AMBIGUOUS")
        String scenario
) {}
