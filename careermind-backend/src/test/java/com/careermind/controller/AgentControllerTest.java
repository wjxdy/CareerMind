package com.careermind.controller;

import com.careermind.domain.Agent;
import com.careermind.enums.AgentType;
import com.careermind.repository.AgentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AgentController 集成测试
 *
 * 测试 API 接口:
 * 1. 获取所有 Agent 列表
 * 2. 响应格式验证
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AgentRepository agentRepository;

    @BeforeEach
    void setUp() {
        // 清理并准备测试数据
        agentRepository.deleteAll();

        // 创建系统预设 Agent
        agentRepository.save(Agent.builder()
                .name("行业分析师")
                .type(AgentType.INDUSTRY_ANALYST)
                .description("分析行业发展趋势、未来前景")
                .systemPrompt("你是行业分析师，擅长分析行业趋势...")
                .modelType("KIMI")
                .avatarUrl("/avatars/analyst.png")
                .build());

        agentRepository.save(Agent.builder()
                .name("技能评估师")
                .type(AgentType.INDUSTRY_ANALYST)
                .description("评估技能匹配度和转型难度")
                .systemPrompt("你是技能评估师，擅长技能分析...")
                .modelType("KIMI")
                .avatarUrl("/avatars/assessor.png")
                .build());
    }

    @Test
    void getAllAgents_ShouldReturnAgentList() throws Exception {
        mockMvc.perform(get("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("success"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("行业分析师"))
                .andExpect(jsonPath("$.data[1].name").value("技能评估师"));
    }

    @Test
    void getAllAgents_ResponseStructure_ShouldBeCorrect() throws Exception {
        mockMvc.perform(get("/api/agents")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].name").exists())
                .andExpect(jsonPath("$.data[0].type").exists())
                .andExpect(jsonPath("$.data[0].description").exists())
                .andExpect(jsonPath("$.data[0].avatarUrl").exists());
    }
}
