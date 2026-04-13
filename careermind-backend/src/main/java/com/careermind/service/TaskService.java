package com.careermind.service;

import com.careermind.domain.Task;
import com.careermind.dto.TaskCreateRequest;
import com.careermind.dto.TaskDto;

import java.util.List;

public interface TaskService {
    Task createTask(Long userId, TaskCreateRequest request);
    Task getTaskById(Long taskId);
    List<TaskDto> getUserTasks(Long userId);
    List<TaskDto> getUserTasksByStatus(Long userId, String status);
    Task updateTaskStatus(Long taskId, String status);
    void deleteTask(Long taskId);
}
