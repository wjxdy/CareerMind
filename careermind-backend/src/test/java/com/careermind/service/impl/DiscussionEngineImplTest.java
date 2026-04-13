package com.careermind.service.impl;

import com.careermind.domain.*;
import com.careermind.dto.DiscussionDto;
import com.careermind.enums.AgentType;
import com.careermind.enums.RoundType;
import com.careermind.repository.*;
import com.careermind.websocket.DiscussionWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DiscussionEngineImpl 单元测试
 *
 * 核心测试:
 * 1. 创建讨论
 * 2. 开始讨论 - 启动第一轮
 * 3. 下一论次 - 进入第二轮(Challenge)
 * 4. 暂停/恢复讨论
 * 5. 停止讨论
 */
@ExtendWith(MockitoExtension.class)
class DiscussionEngineImplTest {

    @Mock
    private DiscussionRepository discussionRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private LLMGatewayImpl llmGateway;

    @Mock
    private DiscussionWebSocketHandler webSocketHandler;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private DiscussionEngineImpl discussionEngine;

    private Task testTask;
    private Discussion testDiscussion;
    private Agent testAgent;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        testTask = Task.builder()
                .id(1L)
                .title("测试任务")
                .agents(Arrays.asList(
                        Agent.builder().id(1L).name("行业分析师").type(AgentType.INDUSTRY_ANALYST).build(),
                        Agent.builder().id(2L).name("技能评估师").type(AgentType.INDUSTRY_ANALYST).build()
                ))
                .build();

        testDiscussion = Discussion.builder()
                .id(1L)
                .task(testTask)
                .currentRound(0)
                .isActive(false)
                .isPaused(false)
                .build();

        testAgent = Agent.builder()
                .id(1L)
                .name("测试Agent")
                .type(AgentType.INDUSTRY_ANALYST)
                .build();
    }

    @Test
    void createDiscussion_WithValidTask_ShouldCreateDiscussion() {
        // Given
        Long taskId = 1L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.of(testTask));
        when(discussionRepository.save(any(Discussion.class))).thenAnswer(invocation -> {
            Discussion d = invocation.getArgument(0);
            d.setId(1L);
            return d;
        });

        // When
        Discussion result = discussionEngine.createDiscussion(taskId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(0, result.getCurrentRound());
        assertFalse(result.getIsActive());
        assertEquals(testTask, result.getTask());
    }

    @Test
    void createDiscussion_WithNonExistentTask_ShouldThrowException() {
        // Given
        Long taskId = 999L;
        when(taskRepository.findById(taskId)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            discussionEngine.createDiscussion(taskId);
        });

        assertTrue(exception.getMessage().contains("Task不存在"));
    }

    @Test
    void startDiscussion_WhenNotActive_ShouldStartDiscussion() {
        // Given
        Long taskId = 1L;

        Discussion inactiveDiscussion = Discussion.builder()
                .id(1L)
                .task(testTask)
                .currentRound(0)
                .isActive(false)
                .isPaused(false)
                .build();

        Discussion savedDiscussion = Discussion.builder()
                .id(1L)
                .task(testTask)
                .currentRound(1)
                .isActive(true)
                .isPaused(false)
                .build();

        // Mock transaction template behavior
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            // 模拟事务执行，返回DTO
            return DiscussionDto.builder()
                    .id(1L)
                    .taskId(taskId)
                    .currentRound(1)
                    .isActive(true)
                    .isPaused(false)
                    .rounds(Collections.emptyList())
                    .build();
        });

        when(discussionRepository.findById(1L)).thenReturn(Optional.of(savedDiscussion));

        // When
        DiscussionDto result = discussionEngine.startDiscussion(taskId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCurrentRound());
        assertTrue(result.getIsActive());
        assertFalse(result.getIsPaused());
    }

    @Test
    void startDiscussion_WhenAlreadyActive_ShouldThrowException() {
        // Given
        Long taskId = 1L;

        // When transaction template executes, it should throw
        when(transactionTemplate.execute(any())).thenThrow(
                new RuntimeException("讨论正在进行中")
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            discussionEngine.startDiscussion(taskId);
        });

        assertTrue(exception.getMessage().contains("讨论正在进行中"));
    }

    @Test
    void getDiscussion_WithExistingDiscussion_ShouldReturnDto() {
        // Given
        Long taskId = 1L;
        Discussion discussion = Discussion.builder()
                .id(1L)
                .task(testTask)
                .currentRound(2)
                .isActive(true)
                .isPaused(false)
                .build();

        when(discussionRepository.findByTaskId(taskId)).thenReturn(Optional.of(discussion));
        when(roundRepository.findByDiscussionIdOrderByRoundNumberAsc(1L))
                .thenReturn(Collections.emptyList());

        // When
        DiscussionDto result = discussionEngine.getDiscussion(taskId);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(taskId, result.getTaskId());
        assertEquals(2, result.getCurrentRound());
        assertTrue(result.getIsActive());
    }

    @Test
    void pauseDiscussion_ShouldSetPaused() {
        // Given
        Long taskId = 1L;
        Discussion discussion = Discussion.builder()
                .id(1L)
                .task(testTask)
                .currentRound(1)
                .isActive(true)
                .isPaused(false)
                .build();

        when(discussionRepository.findByTaskId(taskId)).thenReturn(Optional.of(discussion));
        when(discussionRepository.save(any(Discussion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roundRepository.findByDiscussionIdOrderByRoundNumberAsc(any())).thenReturn(Collections.emptyList());

        // When
        DiscussionDto result = discussionEngine.pauseDiscussion(taskId);

        // Then
        assertNotNull(result);
        assertTrue(result.getIsPaused());
    }

    @Test
    void resumeDiscussion_ShouldClearPaused() {
        // Given
        Long taskId = 1L;
        Discussion discussion = Discussion.builder()
                .id(1L)
                .task(testTask)
                .currentRound(1)
                .isActive(true)
                .isPaused(true)
                .build();

        when(discussionRepository.findByTaskId(taskId)).thenReturn(Optional.of(discussion));
        when(discussionRepository.save(any(Discussion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roundRepository.findByDiscussionIdOrderByRoundNumberAsc(any())).thenReturn(Collections.emptyList());

        // When
        DiscussionDto result = discussionEngine.resumeDiscussion(taskId);

        // Then
        assertNotNull(result);
        assertFalse(result.getIsPaused());
    }

    @Test
    void stopDiscussion_ShouldDeactivate() {
        // Given
        Long taskId = 1L;
        Discussion discussion = Discussion.builder()
                .id(1L)
                .task(testTask)
                .currentRound(2)
                .isActive(true)
                .isPaused(false)
                .build();

        when(discussionRepository.findByTaskId(taskId)).thenReturn(Optional.of(discussion));
        when(discussionRepository.save(any(Discussion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(roundRepository.findByDiscussionIdOrderByRoundNumberAsc(any())).thenReturn(Collections.emptyList());

        // When
        DiscussionDto result = discussionEngine.stopDiscussion(taskId);

        // Then
        assertNotNull(result);
        assertFalse(result.getIsActive());
        assertFalse(result.getIsPaused());
    }

    @Test
    void nextRound_FromRound1To2_ShouldProgressToChallenge() {
        // Given
        Long taskId = 1L;

        // When transaction template executes
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            return DiscussionDto.builder()
                    .id(1L)
                    .taskId(taskId)
                    .currentRound(2)
                    .isActive(true)
                    .isPaused(false)
                    .rounds(Collections.emptyList())
                    .build();
        });

        Discussion updatedDiscussion = Discussion.builder()
                .id(1L)
                .task(testTask)
                .currentRound(2)
                .isActive(true)
                .build();

        when(discussionRepository.findById(1L)).thenReturn(Optional.of(updatedDiscussion));

        // When
        DiscussionDto result = discussionEngine.nextRound(taskId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getCurrentRound());
    }

    @Test
    void nextRound_WhenExceedMaxRounds_ShouldThrowException() {
        // Given
        Long taskId = 1L;

        when(transactionTemplate.execute(any())).thenThrow(
                new RuntimeException("已达到最大轮次")
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            discussionEngine.nextRound(taskId);
        });

        assertTrue(exception.getMessage().contains("已达到最大轮次"));
    }

}
