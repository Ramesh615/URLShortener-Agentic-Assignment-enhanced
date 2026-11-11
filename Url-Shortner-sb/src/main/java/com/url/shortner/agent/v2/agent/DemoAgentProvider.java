package com.url.shortner.agent.v2.agent;
import org.springframework.context.annotation.Profile; import org.springframework.stereotype.Component;
/** Deterministic provider for reproducible demos/tests. Replace with a secured LLM adapter for live inference. */
@Component @Profile("!live-agents") public class DemoAgentProvider implements AgentProvider {
 public AgentResult invoke(AgentType type,AgentRequest r) {
  String content=switch(type){
   case REQUIREMENTS -> "Normalized requirement: "+r.requirement()+"\nAcceptance criteria must be confirmed and testable.";
   case PLANNER -> "Dependency-aware plan generated for requirement version "+r.requirementVersion()+".";
   case CODEBASE_ANALYSIS -> "Impacted areas: URL mapping domain, repository, redirect API, analytics, security and React client. Validate against repository scan.";
   case ARCHITECTURE -> "Proposed design includes versioned schema migration, backward-compatible API, validation, ownership checks and observability.";
   case IMPLEMENTATION -> "Implementation task prepared. No shell execution or deployment is permitted by this demo provider.";
   case TESTING -> "Test plan: unit, integration, authorization, expiry/redirect and concurrency tests.";
   case SECURITY -> "Security review required: SSRF/open redirect, secret scanning, dependency scan and generated-code isolation.";
   case DOCUMENTATION -> "Engineering documentation generated from approved artifacts.";
   case RELEASE_READINESS -> "Release readiness is advisory only; deployment remains disabled.";
  };
  return new AgentResult(content,false,null);
 }
}
