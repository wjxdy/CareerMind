package com.careermind.service.impl;

import com.careermind.domain.*;
import com.careermind.dto.*;
import com.careermind.repository.*;
import com.careermind.service.MergeService;
import com.careermind.websocket.DiscussionWebSocketHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MergeServiceImpl implements MergeService {

    private final MergeResultRepository mergeResultRepository;
    private final PlanRepository planRepository;
    private final TaskRepository taskRepository;
    private final DiscussionRepository discussionRepository;
    private final RoundRepository roundRepository;
    private final MessageRepository messageRepository;
    private final LLMGatewayImpl llmGateway;
    private final DiscussionWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MergeResult generateMergeResult(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task不存在"));

        Discussion discussion = discussionRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("讨论不存在"));

        // 获取所有轮次的消息
        List<Round> rounds = roundRepository.findByDiscussionIdOrderByRoundNumberAsc(discussion.getId());
        List<Message> allMessages = new ArrayList<>();
        for (Round round : rounds) {
            allMessages.addAll(messageRepository.findByRoundIdOrderByCreatedAtAsc(round.getId()));
        }

        // 使用LLM生成整合结果（同步版本）
        String mergeContent = llmGateway.generateMergeResult(task, allMessages);

        MergeResult mergeResult = MergeResult.builder()
                .task(task)
                .summary(mergeContent)
                .convergenceRate(calculateConvergenceRate(allMessages))
                .build();
        mergeResult = mergeResultRepository.save(mergeResult);

        // 使用AI生成候选方案
        generatePlansWithAI(mergeResult, task, allMessages);

        return mergeResult;
    }

    /**
     * 流式生成最终结果
     */
    public void generateMergeResultStream(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task不存在"));

        Discussion discussion = discussionRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("讨论不存在"));

        // 获取所有轮次的消息
        List<Round> rounds = roundRepository.findByDiscussionIdOrderByRoundNumberAsc(discussion.getId());
        List<Message> allMessages = new ArrayList<>();
        for (Round round : rounds) {
            allMessages.addAll(messageRepository.findByRoundIdOrderByCreatedAtAsc(round.getId()));
        }

        // 发送流式开始事件
        webSocketHandler.sendResultStreamStart(taskId);

        StringBuilder contentBuilder = new StringBuilder();

        llmGateway.generateMergeResultStream(task, allMessages, chunk -> {
            contentBuilder.append(chunk);
            webSocketHandler.sendResultStreamChunk(taskId, chunk);
        }, () -> {
            // 流式结束，保存结果
            String fullContent = contentBuilder.toString();

            MergeResult mergeResult = MergeResult.builder()
                    .task(task)
                    .summary(fullContent)
                    .convergenceRate(calculateConvergenceRate(allMessages))
                    .build();
            mergeResult = mergeResultRepository.save(mergeResult);

            // 使用AI生成候选方案
            generatePlansWithAI(mergeResult, task, allMessages);

            webSocketHandler.sendResultStreamEnd(taskId, mergeResult.getId());
            log.info("流式结果生成完成，Task ID: {}, Result ID: {}", taskId, mergeResult.getId());
        });
    }

    @Override
    public MergeResultDto getMergeResult(Long taskId) {
        MergeResult mergeResult = mergeResultRepository.findByTaskId(taskId)
                .orElseThrow(() -> new RuntimeException("结果不存在"));
        return convertToDto(mergeResult);
    }

    @Override
    @Transactional
    public MergeResult selectPlan(Long mergeResultId, Long planId) {
        MergeResult mergeResult = mergeResultRepository.findById(mergeResultId)
                .orElseThrow(() -> new RuntimeException("结果不存在"));

        // 重置所有方案选择状态
        for (Plan plan : mergeResult.getPlans()) {
            plan.setIsSelected(false);
        }

        // 设置选中的方案
        Plan selectedPlan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("方案不存在"));
        selectedPlan.setIsSelected(true);
        planRepository.save(selectedPlan);

        return mergeResult;
    }

    /**
     * 使用AI生成候选方案
     */
    private void generatePlansWithAI(MergeResult mergeResult, Task task, List<Message> messages) {
        try {
            // 调用AI生成方案
            String aiResponse = llmGateway.generatePlansWithAI(task, messages);
            log.info("AI生成方案响应: {}", aiResponse.substring(0, Math.min(500, aiResponse.length())));

            // 解析JSON响应
            JsonNode rootNode = objectMapper.readTree(aiResponse);

            // 解析方案
            JsonNode plansNode = rootNode.path("plans");
            List<Plan> plans = new ArrayList<>();

            if (plansNode.isArray()) {
                for (JsonNode planNode : plansNode) {
                    Plan plan = Plan.builder()
                            .mergeResult(mergeResult)
                            .title(planNode.path("title").asText("未命名方案"))
                            .description(planNode.path("description").asText(""))
                            .confidence(planNode.path("confidence").asInt(70))
                            .supporters(jsonArrayToString(planNode.path("supporters")))
                            .opponents(jsonArrayToString(planNode.path("opponents")))
                            .milestones(jsonArrayToString(planNode.path("milestones")))
                            .risks(jsonArrayToString(planNode.path("risks")))
                            .applicableConditions(planNode.path("applicableConditions").asText(""))
                            .build();
                    plans.add(plan);
                }
            }

            if (plans.isEmpty()) {
                // 如果AI没有生成方案，使用默认方案
                log.warn("AI未生成方案，使用默认方案");
                plans = generateDefaultPlans(mergeResult);
            }

            planRepository.saveAll(plans);

            // 解析并保存认知盲区
            JsonNode blindSpotsNode = rootNode.path("blindSpots");
            List<String> blindSpots = new ArrayList<>();
            if (blindSpotsNode.isArray()) {
                for (JsonNode spot : blindSpotsNode) {
                    blindSpots.add(spot.asText());
                }
            }

            // 保存认知盲区到MergeResult
            try {
                String blindSpotsJson = objectMapper.writeValueAsString(blindSpots);
                mergeResult.setBlindSpots(blindSpotsJson);
                mergeResultRepository.save(mergeResult);
                log.info("认知盲区已保存: {}", blindSpots);
            } catch (Exception ex) {
                log.error("保存认知盲区失败", ex);
            }

        } catch (Exception e) {
            log.error("AI生成方案失败，使用默认方案", e);
            List<Plan> defaultPlans = generateDefaultPlans(mergeResult);
            planRepository.saveAll(defaultPlans);
        }
    }

    private List<Plan> generateDefaultPlans(MergeResult mergeResult) {
        List<Plan> plans = new ArrayList<>();

        plans.add(Plan.builder()
                .mergeResult(mergeResult)
                .title("激进冲刺型")
                .description("全力转型到目标方向，6个月内完成技能储备并跳槽")
                .confidence(65)
                .supporters("[\"机会挖掘者\", \"行业分析师\"]")
                .opponents("[\"风险警示者\"]")
                .milestones("[\"3个月: 完成核心技能学习\", \"6个月: 拿到目标岗位offer\"]")
                .risks("[\"转型期收入下降\", \"学习压力大\", \"失败退路有限\"]")
                .applicableConditions("年轻、无家庭负担、有一定积蓄")
                .build());

        plans.add(Plan.builder()
                .mergeResult(mergeResult)
                .title("渐进平衡型")
                .description("保持当前工作，业余时间学习新技能，1年后寻求内部转岗或跳槽")
                .confidence(80)
                .supporters("[\"能力评估师\", \"价值观拷问者\"]")
                .opponents("[\"机会挖掘者\"]")
                .milestones("[\"6个月: 完成基础技能学习\", \"12个月: 内部转岗或跳槽\"]")
                .risks("[\"学习进度慢\", \"可能错过风口\", \"工作学习双重压力\"]")
                .applicableConditions("有稳定工作、需要维持收入、家庭责任")
                .build());

        plans.add(Plan.builder()
                .mergeResult(mergeResult)
                .title("探索验证型")
                .description("先用3个月时间低成本探索，验证兴趣和能力匹配度后再决定")
                .confidence(75)
                .supporters("[\"风险警示者\", \"能力评估师\"]")
                .opponents("[\"行业分析师\"]")
                .milestones("[\"1个月: 完成行业调研\", \"3个月: 完成一个小项目验证\"]")
                .risks("[\"时间成本\", \"验证不够充分\", \"可能错过时机\"]")
                .applicableConditions("不确定方向、需要验证、风险偏好低")
                .build());

        return plans;
    }

    private String jsonArrayToString(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arrayNode.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(arrayNode.get(i).asText()).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private Double calculateConvergenceRate(List<Message> messages) {
        // 模拟计算观点收敛度
        if (messages.size() < 4) return 0.3;
        if (messages.size() < 8) return 0.5;
        if (messages.size() < 12) return 0.7;
        return 0.85;
    }

    private MergeResultDto convertToDto(MergeResult mergeResult) {
        List<Plan> plans = planRepository.findByMergeResultIdOrderByConfidenceDesc(mergeResult.getId());

        // 从 MergeResult 获取认知盲区，如果没有则使用默认值
        List<String> blindSpots = parseBlindSpots(mergeResult.getBlindSpots());

        return MergeResultDto.builder()
                .id(mergeResult.getId())
                .summary(mergeResult.getSummary())
                .plans(plans.stream().map(this::convertToPlanDto).collect(Collectors.toList()))
                .blindSpots(blindSpots)
                .convergenceRate(mergeResult.getConvergenceRate())
                .build();
    }

    private List<String> parseBlindSpots(String blindSpotsJson) {
        if (blindSpotsJson == null || blindSpotsJson.isEmpty()) {
            return Arrays.asList(
                    "暂无认知盲区分析",
                    "请等待讨论完成后重新生成结果"
            );
        }
        try {
            return objectMapper.readValue(blindSpotsJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception e) {
            log.error("解析认知盲区失败: {}", blindSpotsJson);
            return Arrays.asList("认知盲区解析失败");
        }
    }

    private PlanDto convertToPlanDto(Plan plan) {
        return PlanDto.builder()
                .id(plan.getId())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .confidence(plan.getConfidence())
                .supporters(parseJsonArray(plan.getSupporters()))
                .opponents(parseJsonArray(plan.getOpponents()))
                .milestones(parseJsonArray(plan.getMilestones()))
                .risks(parseJsonArray(plan.getRisks()))
                .applicableConditions(plan.getApplicableConditions())
                .isSelected(plan.getIsSelected())
                .build();
    }

    private List<String> parseJsonArray(String json) {
        if (json == null) return new ArrayList<>();
        // 简单解析JSON数组
        return Arrays.asList(json.replace("[", "").replace("]", "")
                .replace("\"", "").split(","));
    }
}
