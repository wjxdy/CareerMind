package com.careermind.service.impl;

import com.careermind.domain.Agent;
import com.careermind.domain.Task;
import com.careermind.domain.User;
import com.careermind.dto.TaskCreateRequest;
import com.careermind.dto.TaskDto;
import com.careermind.enums.AgentType;
import com.careermind.enums.TaskStatus;
import com.careermind.repository.AgentRepository;
import com.careermind.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TaskServiceImpl 单元测试
 *
 * 测试覆盖:
 * 1. 创建任务 - 正常流程
 * 2. 创建任务 - Agent不存在的情况
 * 3. 查询任务 - 根据ID查询
 * 4. 查询任务 - 查询不存在的情况
 * 5. 获取用户任务列表
 * 6. 更新任务状态
 * 7. 删除任务
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private TaskServiceImpl taskService;

    private TaskCreateRequest validRequest;
    private Agent testAgent;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        validRequest = new TaskCreateRequest();
        validRequest.setTitle("测试职业规划");
        validRequest.setBackground("5年Java开发经验");
        validRequest.setGoal("转向AI领域");
        validRequest.setConstraints("薪资不低于当前");
        validRequest.setAgentIds(Arrays.asList(1L, 2L));

        testAgent = Agent.builder()
                .id(1L)
                .name("行业分析师")
                .type(AgentType.INDUSTRY_ANALYST)
                .description("分析行业发展趋势")
                .build();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Test
    void createTask_WithValidRequest_ShouldCreateTask() {
        // Given
        Long userId = 1L;
        when(agentRepository.findById(1L)).thenReturn(Optional.of(testAgent));
        when(agentRepository.findById(2L)).thenReturn(Optional.of(
                Agent.builder().id(2L).name("技能评估师").type(AgentType.INDUSTRY_ANALYST).build()
        ));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task task = invocation.getArgument(0);
            task.setId(1L);
            return task;
        });

        // When
        Task result = taskService.createTask(userId, validRequest);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试职业规划", result.getTitle());
        assertEquals("5年Java开发经验", result.getBackground());
        assertEquals("转向AI领域", result.getGoal());
        assertEquals(TaskStatus.PENDING, result.getStatus());
        assertNotNull(result.getAgents());
        assertEquals(2, result.getAgents().size());
        assertEquals(userId, result.getUser().getId());

        verify(agentRepository, times(2)).findById(anyLong());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void createTask_WithNonExistentAgent_ShouldThrowException() {
        // Given
        Long userId = 1L;
        when(agentRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.createTask(userId, validRequest);
        });

        assertTrue(exception.getMessage().contains("Agent不存在"));
        verify(agentRepository, times(1)).findById(1L);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void getTaskById_WithExistingTask_ShouldReturnTask() {
        // Given
        Long taskId = 1L;
        Task expectedTask = Task.builder()
                .id(taskId)
                .title("测试任务")
                .status(TaskStatus.PENDING)
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(expectedTask));

        // When
        Task result = taskService.getTaskById(taskId);

        // Then
        assertNotNull(result);
        assertEquals(taskId, result.getId());
        assertEquals("测试任务", result.getTitle());
    }

    @Test
    void getTaskById_WithNonExistentTask_ShouldThrowException() {
        // Given
        Long taskId = 999L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.getTaskById(taskId);
        });

        assertTrue(exception.getMessage().contains("Task不存在"));
    }

    @Test
    void getUserTasks_ShouldReturnTaskList() {
        // Given
        Long userId = 1L;
        List<Task> tasks = Arrays.asList(
                Task.builder().id(1L).title("任务1").status(TaskStatus.PENDING).build(),
                Task.builder().id(2L).title("任务2").status(TaskStatus.COMPLETED).build()
        );
        when(taskRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(tasks);

        // When
        List<TaskDto> result = taskService.getUserTasks(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("任务1", result.get(0).getTitle());
        assertEquals("任务2", result.get(1).getTitle());
    }

    @Test
    void updateTaskStatus_ShouldUpdateStatus() {
        // Given
        Long taskId = 1L;
        Task existingTask = Task.builder()
                .id(taskId)
                .title("测试任务")
                .status(TaskStatus.PENDING)
                .build();
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(existingTask));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        Task result = taskService.updateTaskStatus(taskId, "COMPLETED");

        // Then
        assertNotNull(result);
        assertEquals(TaskStatus.COMPLETED, result.getStatus());
        verify(taskRepository, times(1)).save(existingTask);
    }

    @Test
    void deleteTask_ShouldCallRepository() {
        // Given
        Long taskId = 1L;

        // When
        taskService.deleteTask(taskId);

        // Then
        verify(taskRepository, times(1)).deleteById(taskId);
    }
}
