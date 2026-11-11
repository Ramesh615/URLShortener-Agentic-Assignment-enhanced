package com.url.shortner.agent.v2.api;
import jakarta.validation.constraints.*;
public record CreateWorkflowV2Request(@NotBlank @Size(max=12000) String requirement,@NotBlank @Pattern(regexp="(?i)GREENFIELD|BROWNFIELD|AMBIGUOUS") String scenario) {}
