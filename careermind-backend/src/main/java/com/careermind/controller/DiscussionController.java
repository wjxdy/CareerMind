package com.careermind.controller;

import com.careermind.dto.ApiResponse;
import com.careermind.dto.DiscussionDto;
import com.careermind.dto.UserMessageRequest;
import com.careermind.service.DiscussionEngine;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discussions")
@RequiredArgsConstructor
public class DiscussionController {

    private final DiscussionEngine discussionEngine;

    @PostMapping("/tasks/{taskId}/start")
    public ApiResponse<Object> startDiscussion(@PathVariable Long taskId) {
        return ApiResponse.success(discussionEngine.startDiscussion(taskId));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<ApiResponse<Object>> getDiscussion(@PathVariable Long taskId) {
        DiscussionDto discussion = discussionEngine.getDiscussion(taskId);
        if (discussion == null) {
            return ResponseEntity.status(404).body(ApiResponse.error(404, "讨论不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(discussion));
    }

    @PostMapping("/tasks/{taskId}/pause")
    public ApiResponse<Object> pauseDiscussion(@PathVariable Long taskId) {
        return ApiResponse.success(discussionEngine.pauseDiscussion(taskId));
    }

    @PostMapping("/tasks/{taskId}/resume")
    public ApiResponse<Object> resumeDiscussion(@PathVariable Long taskId) {
        return ApiResponse.success(discussionEngine.resumeDiscussion(taskId));
    }

    @PostMapping("/tasks/{taskId}/stop")
    public ApiResponse<Object> stopDiscussion(@PathVariable Long taskId) {
        return ApiResponse.success(discussionEngine.stopDiscussion(taskId));
    }

    @PostMapping("/tasks/{taskId}/next-round")
    public ApiResponse<Object> nextRound(@PathVariable Long taskId) {
        return ApiResponse.success(discussionEngine.nextRound(taskId));
    }

    @PostMapping("/tasks/{taskId}/messages")
    public ApiResponse<Object> sendUserMessage(
            @PathVariable Long taskId,
            @RequestBody UserMessageRequest request) {
        return ApiResponse.success(discussionEngine.addUserMessage(taskId, request.getContent()));
    }
}
