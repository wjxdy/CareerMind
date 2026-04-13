package com.careermind.service.impl;

import com.careermind.domain.Agent;
import com.careermind.dto.AgentCreateRequest;
import com.careermind.enums.AgentType;
import com.careermind.repository.AgentRepository;
import com.careermind.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentServiceImpl implements AgentService {

    private final AgentRepository agentRepository;

    @Override
    public List<Agent> getPresetAgents() {
        return agentRepository.findByIsPresetTrue();
    }

    @Override
    public List<Agent> getUserAgents(Long userId) {
        return agentRepository.findByUserId(userId);
    }

    @Override
    public List<Agent> getAvailableAgents(Long userId) {
        return agentRepository.findByIsPresetTrueOrUserId(userId);
    }

    @Override
    @Transactional
    public Agent createAgent(Long userId, AgentCreateRequest request) {
        AgentType agentType = request.getType() != null ?
                AgentType.valueOf(request.getType()) : AgentType.CUSTOM;

        Agent agent = Agent.builder()
                .name(request.getName())
                .type(agentType)
                .systemPrompt(request.getSystemPrompt())
                .modelType(request.getModelType())
                .description(request.getDescription())
                .userId(userId)
                .isPreset(false)
                .build();

        return agentRepository.save(agent);
    }

    @Override
    @Transactional
    public Agent updateAgent(Long agentId, AgentCreateRequest request) {
        Agent agent = getAgentById(agentId);

        if (request.getName() != null) agent.setName(request.getName());
        if (request.getSystemPrompt() != null) agent.setSystemPrompt(request.getSystemPrompt());
        if (request.getModelType() != null) agent.setModelType(request.getModelType());
        if (request.getDescription() != null) agent.setDescription(request.getDescription());

        return agentRepository.save(agent);
    }

    @Override
    @Transactional
    public void deleteAgent(Long agentId) {
        Agent agent = getAgentById(agentId);
        if (Boolean.TRUE.equals(agent.getIsPreset())) {
            throw new RuntimeException("预设Agent不能删除");
        }
        agentRepository.delete(agent);
    }

    @Override
    public Agent getAgentById(Long agentId) {
        return agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent不存在"));
    }

    @Override
    @Transactional
    public void initPresetAgents() {
        if (!agentRepository.findByIsPresetTrue().isEmpty()) {
            return;
        }

        List<Agent> presetAgents = new ArrayList<>();

        // 行业分析师
        presetAgents.add(Agent.builder()
                .name(AgentType.INDUSTRY_ANALYST.getChineseName())
                .type(AgentType.INDUSTRY_ANALYST)
                .systemPrompt("你是一位资深的行业分析师，专注于外部市场视角。你的核心关注点是行业趋势、市场规模、竞争格局和5年预测。\n\n你的发言风格：\n- 基于数据说话，引用行业趋势和增长率\n- 关注市场机会和威胁\n- 评估目标行业/岗位的发展前景\n- 提醒用户注意行业周期和结构性变化\n\n输出格式要求：\n1. 关键行业洞察\n2. 市场机会分析\n3. 潜在风险提示")
                .modelType("kimi")
                .description("外部市场视角 - 行业趋势、市场规模、竞争格局、5年预测")
                .isPreset(true)
                .build());

        // 能力评估师
        presetAgents.add(Agent.builder()
                .name(AgentType.SKILL_ASSESSOR.getChineseName())
                .type(AgentType.SKILL_ASSESSOR)
                .systemPrompt("你是一位专业的能力评估师，专注于现实能力视角。你的核心关注点是技能匹配度、学习成本、转型可行性。\n\n你的发言风格：\n- 客观分析当前能力与目标岗位的差距\n- 提供具体的学习路径和时间估算\n- 评估转型的可行性和难度\n- 关注实际可执行性而非理想状态\n\n输出格式要求：\n1. 技能匹配度分析\n2. 能力缺口识别\n3. 学习路径建议")
                .modelType("kimi")
                .description("现实能力视角 - 技能匹配度、学习成本、转型可行性")
                .isPreset(true)
                .build());

        // 风险警示者
        presetAgents.add(Agent.builder()
                .name(AgentType.RISK_WATCHER.getChineseName())
                .type(AgentType.RISK_WATCHER)
                .systemPrompt("你是一位保守的风险警示者，专注于防御视角。你的核心关注点是最坏情况、Plan B、隐性成本、市场波动。\n\n你的发言风格：\n- 总是考虑最坏情况\n- 质疑乐观预期\n- 强调备选方案的重要性\n- 提醒用户考虑机会成本\n\n输出格式要求：\n1. 主要风险识别\n2. 最坏情况推演\n3. Plan B 建议")
                .modelType("claude")
                .description("保守防御视角 - 最坏情况、Plan B、隐性成本、市场波动")
                .isPreset(true)
                .build());

        // 机会挖掘者
        presetAgents.add(Agent.builder()
                .name(AgentType.OPPORTUNITY_HUNTER.getChineseName())
                .type(AgentType.OPPORTUNITY_HUNTER)
                .systemPrompt("你是一位积极的机会挖掘者，专注于进攻视角。你的核心关注点是蓝海市场、非对称机会、被忽视的路径。\n\n你的发言风格：\n- 善于发现隐藏的机会\n- 鼓励大胆但有策略的行动\n- 从困境中看到转机\n- 提出创新的解决方案\n\n输出格式要求：\n1. 隐藏机会识别\n2. 非对称优势分析\n3. 大胆建议")
                .modelType("claude")
                .description("积极进攻视角 - 蓝海市场、非对称机会、被忽视的路径")
                .isPreset(true)
                .build());

        // 价值观拷问者
        presetAgents.add(Agent.builder()
                .name(AgentType.VALUE_EXAMINER.getChineseName())
                .type(AgentType.VALUE_EXAMINER)
                .systemPrompt("你是一位深入的价值观拷问者，专注于内在动机视角。你的核心关注点是真实需求、内在动机、长期满足感。\n\n你的发言风格：\n- 追问用户真正想要什么\n- 揭示表面目标与深层需求的矛盾\n- 关注长期幸福感和满足感\n- 挑战用户的核心假设\n\n输出格式要求：\n1. 真实需求挖掘\n2. 潜在矛盾识别\n3. 内在动机分析")
                .modelType("kimi")
                .description("内在动机视角 - 真实需求、内在动机、长期满足感")
                .isPreset(true)
                .build());

        agentRepository.saveAll(presetAgents);
    }
}
