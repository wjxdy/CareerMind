package com.careermind.service.impl;

import com.careermind.domain.Agent;
import com.careermind.domain.Task;
import com.careermind.dto.TaskCreateRequest;
import com.careermind.dto.TaskDto;
import com.careermind.enums.TaskStatus;
import com.careermind.repository.AgentRepository;
import com.careermind.repository.TaskRepository;
import com.careermind.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final AgentRepository agentRepository;

    @Override
    @Transactional
    public Task createTask(Long userId, TaskCreateRequest request) {
        List<Agent> agents = request.getAgentIds().stream()
                .map(agentId -> agentRepository.findById(agentId)
                        .orElseThrow(() -> new RuntimeException("Agent不存在: " + agentId)))
                .collect(Collectors.toList());

        Task task = Task.builder()
                .title(request.getTitle())
                .background(request.getBackground())
                .goal(request.getGoal())
                .constraints(request.getConstraints())
                .status(TaskStatus.PENDING)
                .user(new com.careermind.domain.User() {{ setId(userId); }})
                .agents(agents)
                .kbId(request.getKbId())
                .build();

        return taskRepository.save(task);
    }

    @Override
    public Task getTaskById(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task不存在"));
    }

    @Override
    public List<TaskDto> getUserTasks(Long userId) {
        List<Task> tasks = taskRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return tasks.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<TaskDto> getUserTasksByStatus(Long userId, String status) {
        TaskStatus taskStatus = TaskStatus.valueOf(status);
        List<Task> tasks = taskRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, taskStatus);
        return tasks.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private TaskDto convertToDto(Task task) {
        // 强制加载agents集合
        List<Agent> agents = task.getAgents();
        List<TaskDto.AgentDto> agentDtos = agents != null ? agents.stream()
                .map(agent -> TaskDto.AgentDto.builder()
                        .id(agent.getId())
                        .name(agent.getName())
                        .type(agent.getType() != null ? agent.getType().name() : null)
                        .description(agent.getDescription())
                        .build())
                .collect(Collectors.toList()) : null;

        return TaskDto.builder()
                .id(task.getId())
                .title(task.getTitle())
                .background(task.getBackground())
                .goal(task.getGoal())
                .constraints(task.getConstraints())
                .status(task.getStatus())
                .agents(agentDtos)
                .kbId(task.getKbId())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public Task updateTaskStatus(Long taskId, String status) {
        Task task = getTaskById(taskId);
        TaskStatus taskStatus = TaskStatus.valueOf(status);
        task.setStatus(taskStatus);
        return taskRepository.save(task);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }
}
