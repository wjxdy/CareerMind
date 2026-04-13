package com.careermind.controller;

import com.careermind.domain.Agent;
import com.careermind.domain.Task;
import com.careermind.domain.User;
import com.careermind.dto.TaskCreateRequest;
import com.careermind.enums.AgentType;
import com.careermind.enums.TaskStatus;
import com.careermind.repository.AgentRepository;
import com.careermind.repository.TaskRepository;
import com.careermind.repository.UserRepository;
import com.careermind.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TaskController 集成测试
 *
 * 测试 API 接口:
 * 1. 创建任务
 * 2. 获取任务列表
 * 3. 获取单个任务
 * 4. 更新任务状态
 * 5. 删除任务
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String authToken;
    private User testUser;
    private Agent testAgent;

    @BeforeEach
    void setUp() {
        // 清理数据
        taskRepository.deleteAll();
        userRepository.deleteAll();
        agentRepository.deleteAll();

        // 创建测试用户
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .build();
        testUser = userRepository.save(testUser);

        // 创建测试 Agent
        testAgent = Agent.builder()
                .name("测试Agent")
                .type(AgentType.INDUSTRY_ANALYST)
                .description("用于测试的Agent")
                .systemPrompt("测试Prompt")
                .modelType("KIMI")
                .build();
        testAgent = agentRepository.save(testAgent);

        // 生成 JWT Token
        authToken = jwtUtil.generateToken(testUser.getId());
    }

    @Test
    void createTask_WithValidRequest_ShouldCreateTask() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("测试职业规划");
        request.setBackground("5年开发经验");
        request.setGoal("转向AI领域");
        request.setAgentIds(Collections.singletonList(testAgent.getId()));

        mockMvc.perform(post("/api/tasks")
                        .header("X-User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("测试职业规划"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.agents").isArray());
    }

    @Test
    void createTask_WithoutAgentIds_ShouldReturnError() throws Exception {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setTitle("测试职业规划");
        request.setBackground("5年开发经验");
        request.setGoal("转向AI领域");
        request.setAgentIds(Collections.emptyList());

        mockMvc.perform(post("/api/tasks")
                        .header("X-User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserTasks_ShouldReturnTaskList() throws Exception {
        // 先创建几个任务
        createTestTask("任务1", TaskStatus.PENDING);
        createTestTask("任务2", TaskStatus.COMPLETED);

        mockMvc.perform(get("/api/tasks")
                        .header("X-User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getTaskById_WithExistingTask_ShouldReturnTask() throws Exception {
        Task task = createTestTask("单个任务", TaskStatus.PENDING);

        mockMvc.perform(get("/api/tasks/" + task.getId())
                        .header("X-User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(task.getId()))
                .andExpect(jsonPath("$.data.title").value("单个任务"));
    }

    @Test
    void updateTaskStatus_ShouldUpdateStatus() throws Exception {
        Task task = createTestTask("待更新任务", TaskStatus.PENDING);

        mockMvc.perform(put("/api/tasks/" + task.getId() + "/status")
                        .header("X-User-Id", testUser.getId())
                        .param("status", "IN_PROGRESS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void deleteTask_ShouldRemoveTask() throws Exception {
        Task task = createTestTask("待删除任务", TaskStatus.PENDING);

        mockMvc.perform(delete("/api/tasks/" + task.getId())
                        .header("X-User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getTasksByStatus_ShouldFilterByStatus() throws Exception {
        createTestTask("进行中任务1", TaskStatus.DISCUSSING);
        createTestTask("进行中任务2", TaskStatus.DISCUSSING);
        createTestTask("已完成任务", TaskStatus.COMPLETED);

        mockMvc.perform(get("/api/tasks/status/IN_PROGRESS")
                        .header("X-User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    private Task createTestTask(String title, TaskStatus status) {
        Task task = Task.builder()
                .title(title)
                .background("测试背景")
                .goal("测试目标")
                .status(status)
                .user(testUser)
                .agents(Arrays.asList(testAgent))
                .build();
        return taskRepository.save(task);
    }
}
