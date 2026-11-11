package com.url.shortner.agent.v2.api;
import jakarta.validation.constraints.*;
public record DecisionV2Request(@NotBlank @Size(max=4000) String decision,@Size(max=4000) String rationale) {}
