package com.careermind.service;

import com.careermind.domain.Agent;
import com.careermind.dto.AgentCreateRequest;

import java.util.List;

public interface AgentService {
    List<Agent> getPresetAgents();
    List<Agent> getUserAgents(Long userId);
    List<Agent> getAvailableAgents(Long userId);
    Agent createAgent(Long userId, AgentCreateRequest request);
    Agent updateAgent(Long agentId, AgentCreateRequest request);
    void deleteAgent(Long agentId);
    Agent getAgentById(Long agentId);
    void initPresetAgents();
}
