package com.careermind.service.impl;

import com.careermind.client.KnowledgeBaseClient;
import com.careermind.domain.*;
import com.careermind.dto.*;
import com.careermind.enums.*;
import com.careermind.repository.*;
import com.careermind.service.DiscussionEngine;
import com.careermind.websocket.DiscussionWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionEngineImpl implements DiscussionEngine {

    private final DiscussionRepository discussionRepository;
    private final TaskRepository taskRepository;
    private final RoundRepository roundRepository;
    private final MessageRepository messageRepository;
    private final LLMGatewayImpl llmGateway;
    private final DiscussionWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final KnowledgeBaseClient knowledgeBaseClient;

    @Override
    @Transactional
    public Discussion createDiscussion(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task不存在"));

        Discussion discussion = Discussion.builder()
                .task(task)
                .currentRound(0)
                .isActive(false)
                .isPaused(false)
                .build();

        return discussionRepository.save(discussion);
    }

    @Override
    public DiscussionDto startDiscussion(Long taskId) {
        DiscussionDto dto = transactionTemplate.execute(status -> {
            Discussion discussion = discussionRepository.findByTaskId(taskId)
                    .orElseGet(() -> createDiscussion(taskId));

            if (Boolean.TRUE.equals(discussion.getIsActive())) {
                throw new RuntimeException("讨论正在进行中");
            }

            discussion.setIsActive(true);
            discussion.setIsPaused(false);

            if (discussion.getCurrentRound() == null || discussion.getCurrentRound() == 0) {
                discussion.setCurrentRound(1);
                discussion = discussionRepository.save(discussion);
                createRoundIfNotExists(discussion, 1, RoundType.INDEPENDENT);
            } else {
                discussion = discussionRepository.save(discussion);
            }

            // 同步 task.status → DISCUSSING
            Task task = taskRepository.findById(taskId).orElse(null);
            if (task != null && task.getStatus() != com.careermind.enums.TaskStatus.COMPLETED) {
                task.setStatus(com.careermind.enums.TaskStatus.DISCUSSING);
                taskRepository.save(task);
            }

            discussionRepository.flush();
            roundRepository.flush();

            return convertToDto(discussion);
        });

        Discussion savedDiscussion = discussionRepository.findById(dto.getId()).orElseThrow();
        startAgentDiscussion(savedDiscussion);

        return dto;
    }

    @Override
    public DiscussionDto getDiscussion(Long taskId) {
        Optional<Discussion> discussionOpt = discussionRepository.findByTaskId(taskId);
        if (discussionOpt.isEmpty()) {
            return null;
        }
        return convertToDto(discussionOpt.get());
    }

    @Override
    @Transactional
    public DiscussionDto pauseDiscussion(Long taskId) {
        Discussion discussion = discussionRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("讨论不存在"));
        discussion.setIsPaused(true);
        return convertToDto(discussionRepository.save(discussion));
    }

    @Override
    @Transactional
    public DiscussionDto resumeDiscussion(Long taskId) {
        Discussion discussion = discussionRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("讨论不存在"));
        discussion.setIsPaused(false);
        return convertToDto(discussionRepository.save(discussion));
    }

    @Override
    @Transactional
    public DiscussionDto stopDiscussion(Long taskId) {
        Discussion discussion = discussionRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("讨论不存在"));
        discussion.setIsActive(false);
        discussion.setIsPaused(false);
        return convertToDto(discussionRepository.save(discussion));
    }

    @Override
    public DiscussionDto nextRound(Long taskId) {
        DiscussionDto dto = transactionTemplate.execute(status -> {
            Discussion discussion = discussionRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new RuntimeException("讨论不存在"));

            int nextRoundNum = discussion.getCurrentRound() + 1;
            if (nextRoundNum > 4) {
                throw new RuntimeException("已达到最大轮次");
            }

            RoundType roundType = getRoundType(nextRoundNum);
            createRoundIfNotExists(discussion, nextRoundNum, roundType);
            discussion.setCurrentRound(nextRoundNum);
            discussion.setIsPaused(false);
            discussion = discussionRepository.save(discussion);

            discussionRepository.flush();
            roundRepository.flush();

            return convertToDto(discussion);
        });

        Discussion savedDiscussion = discussionRepository.findById(dto.getId()).orElseThrow();
        startAgentDiscussion(savedDiscussion);

        return dto;
    }

    private void createRound(Discussion discussion, int roundNumber, RoundType roundType) {
        Round round = Round.builder()
                .discussion(discussion)
                .roundNumber(roundNumber)
                .roundType(roundType)
                .isCompleted(false)
                .build();
        roundRepository.save(round);
    }

    private void createRoundIfNotExists(Discussion discussion, int roundNumber, RoundType roundType) {
        boolean exists = roundRepository.findByDiscussionIdAndRoundNumber(
                discussion.getId(), roundNumber).isPresent();
        if (!exists) {
            createRound(discussion, roundNumber, roundType);
        }
    }

    private RoundType getRoundType(int roundNum) {
        return switch (roundNum) {
            case 1 -> RoundType.INDEPENDENT;
            case 2 -> RoundType.CHALLENGE;
            case 3 -> RoundType.REVISION;
            case 4 -> RoundType.FINAL;
            default -> throw new RuntimeException("无效轮次");
        };
    }

    private void startAgentDiscussion(Discussion discussion) {
        Long discussionId = discussion.getId();
        int currentRound = discussion.getCurrentRound();

        Task task = taskRepository.findById(discussion.getTask().getId()).orElse(null);
        if (task == null) {
            log.error("无法加载Task数据");
            return;
        }

        List<Agent> agents = new ArrayList<>(task.getAgents());
        Long taskId = task.getId();

        boolean isActive = discussion.getIsActive() != null ? discussion.getIsActive() : false;
        log.info("准备启动讨论: Task ID={}, Agents数量={}, 第{}轮, isActive={}", taskId, agents.size(), currentRound, isActive);

        if (agents.isEmpty()) {
            log.error("没有Agent参与讨论，无法启动");
            return;
        }

        if (!isActive) {
            log.error("讨论状态为未激活，不启动异步线程");
            return;
        }

        for (Agent agent : agents) {
            log.info("参与讨论的Agent: {} (ID: {}, Type: {})", agent.getName(), agent.getId(), agent.getType());
        }

        final boolean activeState = isActive;
        final RoundType roundTypeState = getRoundType(currentRound);
        CompletableFuture.runAsync(() -> {
            log.info("异步讨论线程启动 - Discussion ID: {}, Task ID: {}, isActive={}, roundType={}", discussionId, taskId, activeState, roundTypeState);
            runAgentDiscussion(discussionId, taskId, currentRound, agents, activeState, roundTypeState);
        }).exceptionally(e -> {
            log.error("异步讨论执行失败", e);
            return null;
        });
    }

    private void runAgentDiscussion(Long discussionId, Long taskId, int currentRound, List<Agent> agents, boolean isActive, RoundType roundType) {
        log.info("开始第{}轮讨论, Task ID: {}, Discussion ID: {}, 参与Agent数: {}, isActive={}, type={}",
                currentRound, taskId, discussionId, agents.size(), isActive, roundType);

        if (!isActive) {
            log.warn("Discussion {} 未激活，不处理消息", discussionId);
            return;
        }

        for (int i = 0; i < agents.size(); i++) {
            Agent agent = agents.get(i);
            log.info("处理第 {}/{} 个Agent: {}", i + 1, agents.size(), agent.getName());

            try {
                processAgentMessageStream(agent, discussionId, taskId, currentRound, roundType, agents);

                if (i < agents.size() - 1) {
                    log.info("等待2秒后处理下一个Agent...");
                    Thread.sleep(2000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("讨论被中断", e);
                break;
            }
        }

        try {
            transactionTemplate.execute(status -> {
                Round round = roundRepository.findByDiscussionIdAndRoundNumber(
                        discussionId, currentRound).orElse(null);
                if (round != null) {
                    round.setIsCompleted(true);
                    // P2: 计算并持久化 divergence
                    java.util.List<Message> rmsgs = messageRepository.findByRoundIdOrderByCreatedAtAsc(round.getId());
                    java.math.BigDecimal divergence = com.careermind.util.DivergenceCalculator.compute(rmsgs);
                    round.setDivergence(divergence);
                    roundRepository.save(round);
                    log.info("第{}轮讨论完成，divergence={}", currentRound, divergence);

                    // 推送图增量
                    try {
                        webSocketHandler.sendGraphDelta(taskId, currentRound, divergence.doubleValue());
                    } catch (Exception ex) {
                        log.warn("sendGraphDelta 失败: {}", ex.getMessage());
                    }

                    // 第 4 轮（FINAL）结束 → task.status = COMPLETED
                    if (currentRound >= 4) {
                        Task task = taskRepository.findById(taskId).orElse(null);
                        if (task != null) {
                            task.setStatus(com.careermind.enums.TaskStatus.COMPLETED);
                            taskRepository.save(task);
                            log.info("Task {} 已标记为 COMPLETED", taskId);
                        }
                        // 同时把 discussion.isActive 置为 false（讨论自然结束）
                        Discussion d = discussionRepository.findById(discussionId).orElse(null);
                        if (d != null) {
                            d.setIsActive(false);
                            discussionRepository.save(d);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.error("标记轮次完成失败: {}", e.getMessage(), e);
        }

        // 本轮结束后清空用户插话标记
        try {
            transactionTemplate.execute(status -> {
                Discussion discussion = discussionRepository.findById(discussionId).orElse(null);
                if (discussion != null && Boolean.TRUE.equals(discussion.getHasUserInterjection())) {
                    discussion.setHasUserInterjection(false);
                    discussion.setInterjectionContent(null);
                    discussionRepository.save(discussion);
                    log.info("用户插话标记已清空: Discussion ID={}", discussionId);
                }
                return null;
            });
        } catch (Exception e) {
            log.error("清空用户插话标记失败: {}", e.getMessage(), e);
        }
    }

    private void processAgentMessageStream(Agent agent, Long discussionId, Long taskId, int currentRound, RoundType roundType, List<Agent> allAgents) {
        log.info("[processAgentMessageStream] Agent: {}, Discussion ID: {}, Task ID: {}, Round: {}, Type: {}",
                agent.getName(), discussionId, taskId, currentRound, roundType);

        Discussion discussion = discussionRepository.findById(discussionId).orElse(null);
        if (discussion == null) {
            log.error("[processAgentMessageStream] Discussion {} 不存在", discussionId);
            return;
        }

        if (Boolean.TRUE.equals(discussion.getIsPaused())) {
            log.info("[processAgentMessageStream] 讨论已暂停，跳过Agent {}", agent.getName());
            return;
        }

        Round round = roundRepository.findByDiscussionIdAndRoundNumber(
                discussionId, currentRound).orElse(null);
        if (round == null) {
            log.warn("[processAgentMessageStream] Round不存在，尝试创建: Discussion ID={}, Round={}", discussionId, currentRound);
            round = Round.builder()
                    .discussion(discussion)
                    .roundNumber(currentRound)
                    .roundType(roundType)
                    .isCompleted(false)
                    .build();
            round = roundRepository.save(round);
            log.info("[processAgentMessageStream] Round创建成功: ID={}, Type={}", round.getId(), round.getRoundType());
        }

        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("[processAgentMessageStream] Task不存在: {}", taskId);
            return;
        }

        String prompt = buildPrompt(agent, task, round, allAgents, currentRound, discussionId);
        log.info("[processAgentMessageStream] Agent {} 开始流式生成内容", agent.getName());

        StringBuilder contentBuilder = new StringBuilder();
        final Round finalRound = round;

        webSocketHandler.sendStreamStart(taskId, agent.getId(), agent.getName(), agent.getType().name(), agent.getAvatarUrl());

        llmGateway.generateAgentResponseStream(agent, prompt, chunk -> {
            contentBuilder.append(chunk);
            webSocketHandler.sendStreamChunk(taskId, chunk);
        }, () -> {
            String fullContent = contentBuilder.toString();
            log.info("[processAgentMessageStream] Agent {} 流式生成完成，内容长度: {}", agent.getName(), fullContent.length());

            // P2: 解析 confidence 与 edgeType
            java.math.BigDecimal confidence = com.careermind.util.MessageMetaParser.parseConfidence(fullContent)
                    .orElse(java.math.BigDecimal.valueOf(0.6));
            String edgeType = com.careermind.util.MessageMetaParser.inferEdgeType(currentRound, fullContent);

            Message message = Message.builder()
                    .round(finalRound)
                    .agent(agent)
                    .content(fullContent)
                    .isFinal(finalRound.getRoundType() == RoundType.FINAL)
                    .confidence(confidence)
                    .edgeType(edgeType)
                    .build();
            Message savedMessage = messageRepository.save(message);
            log.info("[processAgentMessageStream] Agent {} 消息已保存, ID: {}, confidence: {}, edgeType: {}",
                    agent.getName(), savedMessage.getId(), confidence, edgeType);

            webSocketHandler.sendStreamEnd(taskId, savedMessage.getId());
        });
    }


    private String buildPrompt(Agent agent, Task task, Round round, List<Agent> allAgents, int currentRound, Long discussionId) {
        StringBuilder prompt = new StringBuilder();

        Discussion discussion = discussionRepository.findById(discussionId).orElse(null);
        boolean hasInterjection = discussion != null && Boolean.TRUE.equals(discussion.getHasUserInterjection());
        String interjectionContent = hasInterjection ? discussion.getInterjectionContent() : null;

        // 基础信息
        prompt.append("你是").append(agent.getName()).append("。\n\n");
        prompt.append("=== 用户信息 ===\n");
        prompt.append("背景：").append(task.getBackground()).append("\n");
        prompt.append("目标：").append(task.getGoal()).append("\n");
        if (task.getConstraints() != null && !task.getConstraints().isEmpty()) {
            prompt.append("约束条件：").append(task.getConstraints()).append("\n");
        }
        prompt.append("\n");

        if (hasInterjection && interjectionContent != null) {
            prompt.append("=== 用户插话 ===\n");
            prompt.append("用户在讨论中补充了以下内容，请在发言中适当参考：\n");
            prompt.append(interjectionContent).append("\n\n");
        }

        // 注入知识库检索结果
        if (task.getKbId() != null) {
            String ragContext = knowledgeBaseClient.queryKnowledgeBase(task.getKbId(), task.getGoal());
            if (!ragContext.isEmpty()) {
                prompt.append(ragContext);
                prompt.append("\n");
            }
        }

        RoundType roundType = round.getRoundType();

        switch (roundType) {
            case INDEPENDENT -> {
                // Round 1: 独立诊断
                prompt.append("=== 当前轮次：独立诊断 ===\n\n");
                prompt.append("这是第一轮讨论。请基于用户提供的背景和目标，独立输出你的初步观点。\n\n");
                prompt.append("请按以下格式输出：\n");
                prompt.append("1. 我看到的关键问题：...\n");
                prompt.append("2. 我的初步建议方向：...\n");
                prompt.append("3. 需要进一步确认的信息：...\n");
            }
            case CHALLENGE -> {
                // Round 2: 质疑挑战
                prompt.append("=== 当前轮次：质疑挑战 ===\n\n");
                prompt.append("上一轮（独立诊断）中，各位专家已经发表了初步观点。\n\n");

                // 获取上一轮的消息
                List<Message> previousMessages = getPreviousRoundMessages(discussionId, currentRound - 1);
                if (!previousMessages.isEmpty()) {
                    prompt.append("=== 上一轮各位专家的观点 ===\n");
                    for (Message msg : previousMessages) {
                        if (msg.getAgent() != null && !msg.getAgent().getId().equals(agent.getId())) {
                            prompt.append("【").append(msg.getAgent().getName()).append("】\n");
                            prompt.append(msg.getContent()).append("\n\n");
                        }
                    }
                }

                prompt.append("=== 你的任务 ===\n");
                prompt.append("请针对至少2位其他专家的观点进行回应：\n");
                prompt.append("1. 对其他专家观点的质疑或补充（选择与你立场不同的观点）\n");
                prompt.append("2. 明确表达你的不同意见和支持论据\n");
                prompt.append("3. 如果有证据支撑，可以直接反驳\n\n");
                prompt.append("请按以下格式输出：\n");
                prompt.append("对 @[专家姓名] 的质疑/补充：...\n");
                prompt.append("我的反驳/支持理由：...\n");
                prompt.append("进一步阐述我的观点：...\n");
            }
            case REVISION -> {
                // Round 3: 修正观点
                prompt.append("=== 当前轮次：修正完善 ===\n\n");
                prompt.append("经过上一轮的观点碰撞，现在需要基于收到的反馈修正或坚持你的观点。\n\n");

                // 获取Round 1和Round 2的消息
                List<Message> round1Messages = getPreviousRoundMessages(discussionId, 1);
                List<Message> round2Messages = getPreviousRoundMessages(discussionId, 2);

                // 找到针对当前Agent的质疑
                List<String> challengesAgainstMe = new ArrayList<>();
                for (Message msg : round2Messages) {
                    if (msg.getAgent() != null &&
                        (msg.getContent().contains(agent.getName()) ||
                        msg.getContent().contains("@" + agent.getName()))) {
                        challengesAgainstMe.add(msg.getAgent().getName() + ": " + msg.getContent());
                    }
                }

                if (!challengesAgainstMe.isEmpty()) {
                    prompt.append("=== 针对你的质疑 ===\n");
                    for (String challenge : challengesAgainstMe) {
                        prompt.append(challenge).append("\n\n");
                    }
                }

                // 获取当前Agent在Round 1的发言
                String myRound1Content = round1Messages.stream()
                    .filter(m -> m.getAgent().getId().equals(agent.getId()))
                    .map(Message::getContent)
                    .findFirst()
                    .orElse("");

                if (!myRound1Content.isEmpty()) {
                    prompt.append("=== 你之前（Round 1）的观点 ===\n");
                    prompt.append(myRound1Content).append("\n\n");
                }

                prompt.append("=== 你的任务 ===\n");
                prompt.append("请基于质疑和讨论，修正或坚持你的观点：\n");
                prompt.append("1. 明确回应质疑：接受/部分接受/不接受\n");
                prompt.append("2. 如果接受，说明修正后的观点\n");
                prompt.append("3. 如果不接受，解释为什么坚持原有立场\n\n");
                prompt.append("请按以下格式输出：\n");
                prompt.append("1. 我对质疑的回应：\n");
                prompt.append("   - 接受 @XXX 的观点，因为...\n");
                prompt.append("   - 不接受 @XXX 的质疑，因为...\n");
                prompt.append("2. 修正后的观点（或坚持的观点）：...\n");
                prompt.append("3. 我坚持的核心立场：...\n");
            }
            case FINAL -> {
                // Round 4: 最终陈述
                prompt.append("=== 当前轮次：最终陈述 ===\n\n");
                prompt.append("这是最后一轮讨论。请综合前面的讨论，给出你的最终建议。\n\n");

                // 获取所有历史消息
                List<Message> allMessages = new ArrayList<>();
                for (int i = 1; i < currentRound; i++) {
                    allMessages.addAll(getPreviousRoundMessages(discussionId, i));
                }

                if (!allMessages.isEmpty()) {
                    prompt.append("=== 讨论摘要 ===\n");
                    prompt.append("前面的讨论中：\n");
                    // 简单总结每个Agent的最终立场
                    Map<String, String> agentPositions = new HashMap<>();
                    for (Message msg : allMessages) {
                        if (msg.getAgent() != null) {
                            String agentName = msg.getAgent().getName();
                            // 只保留每个Agent最新的观点
                            agentPositions.put(agentName, msg.getContent().substring(0, Math.min(200, msg.getContent().length())));
                        }
                    }
                    prompt.append("\n");
                }

                prompt.append("=== 你的任务 ===\n");
                prompt.append("简洁总结你的最终建议：\n\n");
                prompt.append("请按以下格式输出：\n");
                prompt.append("我的最终建议：...\n");
                prompt.append("关键理由（最多3条）：\n");
                prompt.append("1. ...\n");
                prompt.append("2. ...\n");
                prompt.append("3. ...\n");
                prompt.append("风险提示：...\n");
            }
        }

        // P2: 自报置信度，便于辩论可视化
        prompt.append("\n=== 置信度要求（重要） ===\n");
        prompt.append("请在你的回复最后一行单独输出 [confidence: X.XX]，X.XX 是 0.00-1.00 的小数，表示你对本轮观点的信心。\n");
        if (roundType == RoundType.CHALLENGE) {
            prompt.append("如果你明确质疑某位同事，请在开头使用 \"我对 @某某 的观点有异议：…\" 这样的格式；如果你支持/同意/补充某位同事的观点，请在开头使用 \"我同意/支持/补充 @某某：…\"。\n");
        }

        return prompt.toString();
    }

    private List<Message> getPreviousRoundMessages(Long discussionId, int roundNumber) {
        Optional<Round> roundOpt = roundRepository.findByDiscussionIdAndRoundNumber(discussionId, roundNumber);
        if (roundOpt.isPresent()) {
            return messageRepository.findByRoundIdOrderByCreatedAtAsc(roundOpt.get().getId());
        }
        return new ArrayList<>();
    }

    @Override
    public DiscussionDto addUserMessage(Long taskId, String content) {
        DiscussionDto dto = transactionTemplate.execute(status -> {
            Discussion discussion = discussionRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new RuntimeException("讨论不存在"));

            Round round = roundRepository.findByDiscussionIdAndRoundNumber(
                    discussion.getId(), discussion.getCurrentRound())
                    .orElseThrow(() -> new RuntimeException("当前轮次不存在"));

            Message userMessage = Message.builder()
                    .round(round)
                    .agent(null)
                    .content("【用户提问】" + content)
                    .messageType(MessageType.USER)
                    .isFinal(false)
                    .build();
            messageRepository.save(userMessage);

            discussion.setHasUserInterjection(true);
            discussion.setInterjectionContent(content);
            discussionRepository.save(discussion);

            webSocketHandler.sendMessageToTask(taskId, MessageDto.builder()
                    .id(userMessage.getId())
                    .agentId(-1L)
                    .agentName("用户")
                    .agentType("USER")
                    .agentAvatar(null)
                    .content("【用户提问】" + content)
                    .messageType(MessageType.USER.name())
                    .isFinal(false)
                    .createdAt(userMessage.getCreatedAt())
                    .build());

            log.info("用户消息已添加到讨论: Task ID={}, Round={}, Content={}",
                    taskId, round.getRoundNumber(), content.substring(0, Math.min(50, content.length())));

            return convertToDto(discussion);
        });

        return dto;
    }

    private DiscussionDto convertToDto(Discussion discussion) {
        List<Round> rounds = roundRepository.findByDiscussionIdOrderByRoundNumberAsc(discussion.getId());

        return DiscussionDto.builder()
                .id(discussion.getId())
                .taskId(discussion.getTask().getId())
                .currentRound(discussion.getCurrentRound())
                .isActive(discussion.getIsActive())
                .isPaused(discussion.getIsPaused())
                .hasUserInterjection(discussion.getHasUserInterjection())
                .interjectionContent(discussion.getInterjectionContent())
                .rounds(rounds.stream().map(this::convertToRoundDto).collect(Collectors.toList()))
                .build();
    }

    private RoundDto convertToRoundDto(Round round) {
        List<Message> messages = messageRepository.findByRoundIdOrderByCreatedAtAsc(round.getId());

        return RoundDto.builder()
                .id(round.getId())
                .roundNumber(round.getRoundNumber())
                .roundType(round.getRoundType().name())
                .isCompleted(round.getIsCompleted())
                .messages(messages.stream().map(this::convertToMessageDto).collect(Collectors.toList()))
                .createdAt(round.getCreatedAt())
                .build();
    }

    private MessageDto convertToMessageDto(Message message) {
        return MessageDto.builder()
                .id(message.getId())
                .agentId(message.getAgent() != null ? message.getAgent().getId() : -1L)
                .agentName(message.getAgent() != null ? message.getAgent().getName() : "用户")
                .agentAvatar(message.getAgent() != null ? message.getAgent().getAvatarUrl() : null)
                .agentType(message.getAgent() != null ? message.getAgent().getType().name() : "USER")
                .content(message.getContent())
                .replyToMessageId(message.getReplyToMessageId())
                .messageType(message.getMessageType() != null ? message.getMessageType().name() : MessageType.AGENT.name())
                .isFinal(message.getIsFinal())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
