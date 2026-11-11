package com.url.shortner.agent.v2.agent;
public record AgentResult(String content, boolean needsHumanClarification, String clarificationQuestion) { }
