package com.url.shortner.agent.v2.agent;
import java.util.Map;
public record AgentRequest(String workflowId,String requirement,long requirementVersion,String taskKey,Map<String,String> upstreamArtifacts) {}
