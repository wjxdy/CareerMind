package com.careermind.controller;

import com.careermind.dto.AgentCreateRequest;
import com.careermind.dto.ApiResponse;
import com.careermind.service.AgentService;
import com.careermind.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;
    private final RequestUtil requestUtil;

    @GetMapping("/preset")
    public ApiResponse<Object> getPresetAgents() {
        return ApiResponse.success(agentService.getPresetAgents());
    }

    @GetMapping
    public ApiResponse<Object> getAvailableAgents(HttpServletRequest request) {
        Long userId = requestUtil.getCurrentUserId(request);
        return ApiResponse.success(agentService.getAvailableAgents(userId));
    }

    @PostMapping
    public ApiResponse<Object> createAgent(
            HttpServletRequest request,
            @Valid @RequestBody AgentCreateRequest requestBody) {
        Long userId = requestUtil.getCurrentUserId(request);
        return ApiResponse.success(agentService.createAgent(userId, requestBody));
    }

    @PutMapping("/{agentId}")
    public ApiResponse<Object> updateAgent(
            @PathVariable Long agentId,
            @Valid @RequestBody AgentCreateRequest request) {
        return ApiResponse.success(agentService.updateAgent(agentId, request));
    }

    @DeleteMapping("/{agentId}")
    public ApiResponse<Void> deleteAgent(@PathVariable Long agentId) {
        agentService.deleteAgent(agentId);
        return ApiResponse.success();
    }

    @PostMapping("/init")
    public ApiResponse<Void> initPresetAgents() {
        agentService.initPresetAgents();
        return ApiResponse.success();
    }
}
