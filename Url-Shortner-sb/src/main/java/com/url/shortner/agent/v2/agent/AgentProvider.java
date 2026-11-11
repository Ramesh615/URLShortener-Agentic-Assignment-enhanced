package com.url.shortner.agent.v2.agent;
import java.util.List;
public interface AgentProvider { AgentResult invoke(AgentType type, AgentRequest request); }
