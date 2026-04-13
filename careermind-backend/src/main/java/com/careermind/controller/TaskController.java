package com.careermind.controller;

import com.careermind.dto.ApiResponse;
import com.careermind.dto.TaskCreateRequest;
import com.careermind.service.TaskService;
import com.careermind.util.RequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final RequestUtil requestUtil;

    @PostMapping
    public ApiResponse<Object> createTask(
            HttpServletRequest request,
            @Valid @RequestBody TaskCreateRequest requestBody) {
        Long userId = requestUtil.getCurrentUserId(request);
        return ApiResponse.success(taskService.createTask(userId, requestBody));
    }

    @GetMapping
    public ApiResponse<Object> getUserTasks(HttpServletRequest request) {
        Long userId = requestUtil.getCurrentUserId(request);
        return ApiResponse.success(taskService.getUserTasks(userId));
    }

    @GetMapping("/{taskId}")
    public ApiResponse<Object> getTaskById(@PathVariable Long taskId) {
        return ApiResponse.success(taskService.getTaskById(taskId));
    }

    @PutMapping("/{taskId}/status")
    public ApiResponse<Object> updateTaskStatus(
            @PathVariable Long taskId,
            @RequestParam String status) {
        return ApiResponse.success(taskService.updateTaskStatus(taskId, status));
    }

    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable Long taskId) {
        taskService.deleteTask(taskId);
        return ApiResponse.success();
    }
}
