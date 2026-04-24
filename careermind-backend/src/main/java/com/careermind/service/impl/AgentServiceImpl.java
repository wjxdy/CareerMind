package com.careermind.service.impl;

import com.careermind.domain.Agent;
import com.careermind.dto.AgentCreateRequest;
import com.careermind.enums.AgentType;
import com.careermind.repository.AgentRepository;
import com.careermind.service.AgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
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
        // 按 type upsert：已存在的预设 Agent 跳过，缺失的补上
        // 这样新增类型（如法律团）可在不清库的情况下自动 seed
        java.util.Set<AgentType> existingPresetTypes = agentRepository.findByIsPresetTrue().stream()
                .map(Agent::getType)
                .collect(java.util.stream.Collectors.toSet());

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

        // ==================== 法律团 ====================

        // 合同审查师
        presetAgents.add(Agent.builder()
                .name(AgentType.CONTRACT_REVIEWER.getChineseName())
                .type(AgentType.CONTRACT_REVIEWER)
                .systemPrompt("你是一位严谨的合同审查师，专注于合同条款与书面约定的审视视角。你的核心关注点是条款漏洞、违约后果、隐含义务、模糊表述。\n\n你的发言风格：\n- 逐条审视相关条款，指出不利表述\n- 提醒隐含风险与未约定事项\n- 给出条款修改建议\n- 用法律术语简明阐述\n\n输出格式要求：\n1. 关键条款审查\n2. 风险与漏洞提示\n3. 修改/补充建议")
                .modelType("kimi")
                .description("合同文本视角 - 条款漏洞、违约后果、模糊表述、修改建议")
                .isPreset(true)
                .build());

        // 诉讼风险师
        presetAgents.add(Agent.builder()
                .name(AgentType.LITIGATION_ANALYST.getChineseName())
                .type(AgentType.LITIGATION_ANALYST)
                .systemPrompt("你是一位资深的诉讼风险师，专注于打官司的可行性与成本视角。你的核心关注点是胜诉概率、举证难度、时间成本、赔偿额预期。\n\n你的发言风格：\n- 客观评估起诉的利弊\n- 引用类似判例推演结果\n- 计算时间与经济代价\n- 区分理论上赢与实际能拿到的差距\n\n输出格式要求：\n1. 胜诉概率评估\n2. 主要证据清单与举证难度\n3. 时间与金钱成本估算")
                .modelType("kimi")
                .description("诉讼可行性视角 - 胜诉概率、举证、时间与赔偿")
                .isPreset(true)
                .build());

        // 权益维护者
        presetAgents.add(Agent.builder()
                .name(AgentType.RIGHTS_DEFENDER.getChineseName())
                .type(AgentType.RIGHTS_DEFENDER)
                .systemPrompt("你是一位坚定的权益维护者，专注于站在当事人一边主张权利的视角。你的核心关注点是法律依据、可主张的权利、维权路径。\n\n你的发言风格：\n- 先找到可以主张的法律依据\n- 鼓励合理维权，不接受不合理让步\n- 指出相关法律条文与判例支持\n- 提醒时效与程序要求\n\n输出格式要求：\n1. 可主张的权利清单\n2. 法律依据（法条/判例）\n3. 维权路径与时效")
                .modelType("claude")
                .description("权利主张视角 - 法律依据、可主张权利、维权路径")
                .isPreset(true)
                .build());

        // 实务执行官
        presetAgents.add(Agent.builder()
                .name(AgentType.PRACTICAL_COUNSEL.getChineseName())
                .type(AgentType.PRACTICAL_COUNSEL)
                .systemPrompt("你是一位务实的实务执行官，专注于下一步具体该怎么做的视角。你的核心关注点是当下最重要的动作、证据固定、沟通策略、可行方案。\n\n你的发言风格：\n- 不讲理论，只讲可执行步骤\n- 给出近期（24h / 1 周 / 1 月）具体动作\n- 教如何保留证据、如何沟通\n- 推荐是否咨询专业律师、何种类型\n\n输出格式要求：\n1. 今天就该做的事\n2. 近 1 周的步骤\n3. 需要寻求专业协助的节点")
                .modelType("claude")
                .description("执行落地视角 - 动作清单、证据固定、沟通策略")
                .isPreset(true)
                .build());

        // 调解智者
        presetAgents.add(Agent.builder()
                .name(AgentType.MEDIATION_ADVISOR.getChineseName())
                .type(AgentType.MEDIATION_ADVISOR)
                .systemPrompt("你是一位经验丰富的调解智者，专注于避免对抗、达成双赢的视角。你的核心关注点是对方立场、共同利益、可协商空间、和解方案。\n\n你的发言风格：\n- 站在双方立场理解分歧\n- 提出可能的让步交换\n- 强调时间成本与关系维护\n- 设计分层和解方案（从低让步到高让步）\n\n输出格式要求：\n1. 对方可能的真实诉求\n2. 可协商的让步点\n3. 建议的和解方案（上中下三档）")
                .modelType("kimi")
                .description("和解协商视角 - 双方立场、让步点、分层方案")
                .isPreset(true)
                .build());

        // 过滤掉已存在的类型
        List<Agent> toInsert = presetAgents.stream()
                .filter(a -> !existingPresetTypes.contains(a.getType()))
                .toList();
        if (!toInsert.isEmpty()) {
            agentRepository.saveAll(toInsert);
            log.info("[initPresetAgents] 新增 {} 个预设 Agent: {}",
                    toInsert.size(),
                    toInsert.stream().map(a -> a.getType().name()).toList());
        } else {
            log.info("[initPresetAgents] 所有预设 Agent 已存在，跳过");
        }
    }
}
