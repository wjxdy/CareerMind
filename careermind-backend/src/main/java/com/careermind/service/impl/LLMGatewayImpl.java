package com.careermind.service.impl;

import com.careermind.config.LLMConfig;
import com.careermind.domain.Agent;
import com.careermind.domain.Message;
import com.careermind.domain.Task;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class LLMGatewayImpl {

    private final LLMConfig llmConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 生成Agent发言内容（同步版本）
     */
    public String generateAgentResponse(Agent agent, String context) {
        String provider = getProviderForAgent(agent);

        if (hasApiKey(provider)) {
            return callLLMAPI(provider, buildSystemPrompt(agent), context);
        }

        log.warn("未配置 {} 的 API Key，使用模拟数据", provider);
        return generateMockResponse(agent);
    }

    /**
     * 生成Agent发言内容（流式版本）
     */
    public void generateAgentResponseStream(Agent agent, String context, Consumer<String> onChunk, Runnable onComplete) {
        String provider = getProviderForAgent(agent);

        if (hasApiKey(provider)) {
            callLLMAPIStream(provider, buildSystemPrompt(agent), context, onChunk, onComplete);
        } else {
            log.warn("未配置 {} 的 API Key，使用模拟数据", provider);
            String mockResponse = generateMockResponse(agent);
            // 模拟流式输出
            String[] chunks = mockResponse.split("");
            for (String chunk : chunks) {
                onChunk.accept(chunk);
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            onComplete.run();
        }
    }

    /**
     * 生成Merge整合结果（同步版本）
     */
    public String generateMergeResult(Task task, List<Message> messages) {
        String provider = llmConfig.getDefaultProvider();

        if (hasApiKey(provider)) {
            StringBuilder conversation = new StringBuilder();
            conversation.append("用户背景：").append(task.getBackground()).append("\n");
            conversation.append("用户目标：").append(task.getGoal()).append("\n\n");
            conversation.append("=== 专家讨论记录 ===\n\n");

            for (Message msg : messages) {
                String agentName = msg.getAgent() != null ? msg.getAgent().getName() : "用户";
                conversation.append("【").append(agentName).append("】\n");
                conversation.append(msg.getContent()).append("\n\n");
            }

            String systemPrompt = "你是一位专业的职业规划顾问。基于以下专家讨论记录，整合各方观点，生成一份结构化的分析报告。" +
                    "报告应包括：1.共识总结 2.分歧点 3.2-3个候选方案 4.认知盲区";

            return callLLMAPI(provider, systemPrompt, conversation.toString());
        }

        log.warn("未配置 {} 的 API Key，使用模拟数据", provider);
        return generateMockMergeResult();
    }

    /**
     * 生成Merge整合结果（流式版本）
     */
    public void generateMergeResultStream(Task task, List<Message> messages, Consumer<String> onChunk, Runnable onComplete) {
        String provider = llmConfig.getDefaultProvider();

        if (hasApiKey(provider)) {
            StringBuilder conversation = new StringBuilder();
            conversation.append("用户背景：").append(task.getBackground()).append("\n");
            conversation.append("用户目标：").append(task.getGoal()).append("\n\n");
            conversation.append("=== 专家讨论记录 ===\n\n");

            for (Message msg : messages) {
                String agentName = msg.getAgent() != null ? msg.getAgent().getName() : "用户";
                conversation.append("【").append(agentName).append("】\n");
                conversation.append(msg.getContent()).append("\n\n");
            }

            String systemPrompt = "你是一位专业的职业规划顾问。基于以下专家讨论记录，整合各方观点，生成一份结构化的分析报告。" +
                    "报告应包括：1.共识总结 2.分歧点 3.2-3个候选方案 4.认知盲区";

            callLLMAPIStream(provider, systemPrompt, conversation.toString(), onChunk, onComplete);
        } else {
            log.warn("未配置 {} 的 API Key，使用模拟数据", provider);
            String mockResponse = generateMockMergeResult();
            // 模拟流式输出
            String[] chunks = mockResponse.split("");
            for (String chunk : chunks) {
                onChunk.accept(chunk);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            onComplete.run();
        }
    }

    private String getProviderForAgent(Agent agent) {
        return llmConfig.getDefaultProvider();
    }

    private boolean hasApiKey(String provider) {
        return switch (provider) {
            case "kimi" -> llmConfig.getKimi() != null &&
                    llmConfig.getKimi().getApiKey() != null &&
                    !llmConfig.getKimi().getApiKey().contains("your-");
            case "openai" -> llmConfig.getOpenai() != null &&
                    llmConfig.getOpenai().getApiKey() != null &&
                    !llmConfig.getOpenai().getApiKey().contains("your-");
            case "claude" -> llmConfig.getClaude() != null &&
                    llmConfig.getClaude().getApiKey() != null &&
                    !llmConfig.getClaude().getApiKey().contains("your-");
            default -> false;
        };
    }

    private String callLLMAPI(String provider, String systemPrompt, String userContent) {
        return switch (provider) {
            case "kimi" -> callKimi(systemPrompt, userContent);
            case "openai" -> callOpenAI(systemPrompt, userContent);
            case "claude" -> callClaude(systemPrompt, userContent);
            default -> generateMockResponse(null);
        };
    }

    private void callLLMAPIStream(String provider, String systemPrompt, String userContent, Consumer<String> onChunk, Runnable onComplete) {
        switch (provider) {
            case "kimi" -> callKimiStream(systemPrompt, userContent, onChunk, onComplete);
            case "openai" -> callOpenAIStream(systemPrompt, userContent, onChunk, onComplete);
            default -> {
                String mockResponse = generateMockResponse(null);
                onChunk.accept(mockResponse);
                onComplete.run();
            }
        }
    }

    private String callKimi(String systemPrompt, String userContent) {
        try {
            LLMConfig.KimiConfig config = llmConfig.getKimi();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            ArrayNode messages = requestBody.putArray("messages");

            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userContent);

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody),
                    headers
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    config.getBaseUrl() + "/chat/completions",
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return jsonResponse.path("choices").get(0)
                        .path("message").path("content").asText();
            }

            log.error("Kimi API 调用失败: {}", response.getStatusCode());
            return generateMockResponse(null);

        } catch (Exception e) {
            log.error("调用 Kimi API 失败", e);
            return generateMockResponse(null);
        }
    }

    private void callKimiStream(String systemPrompt, String userContent, Consumer<String> onChunk, Runnable onComplete) {
        try {
            LLMConfig.KimiConfig config = llmConfig.getKimi();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("stream", true);

            ArrayNode messages = requestBody.putArray("messages");

            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userContent);

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody),
                    headers
            );

            // 配置支持流的RestTemplate
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setBufferRequestBody(false);
            RestTemplate streamRestTemplate = new RestTemplate(factory);

            ResponseExtractor<Void> extractor = (ClientHttpResponse response) -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                break;
                            }
                            try {
                                JsonNode jsonNode = objectMapper.readTree(data);
                                JsonNode choices = jsonNode.path("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).path("delta");
                                    String content = delta.path("content").asText();
                                    if (content != null && !content.isEmpty()) {
                                        onChunk.accept(content);
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("解析流数据失败: {}", line);
                            }
                        }
                    }
                }
                return null;
            };

            streamRestTemplate.execute(
                    config.getBaseUrl() + "/chat/completions",
                    HttpMethod.POST,
                    request -> {
                        request.getHeaders().putAll(headers);
                        byte[] body = objectMapper.writeValueAsString(requestBody).getBytes(StandardCharsets.UTF_8);
                        request.getBody().write(body);
                    },
                    extractor
            );

            onComplete.run();

        } catch (Exception e) {
            log.error("调用 Kimi 流式 API 失败", e);
            onChunk.accept(generateMockResponse(null));
            onComplete.run();
        }
    }

    private String callOpenAI(String systemPrompt, String userContent) {
        try {
            LLMConfig.OpenAIConfig config = llmConfig.getOpenai();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());

            ArrayNode messages = requestBody.putArray("messages");

            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userContent);

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody),
                    headers
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    config.getBaseUrl() + "/chat/completions",
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return jsonResponse.path("choices").get(0)
                        .path("message").path("content").asText();
            }

            log.error("OpenAI API 调用失败: {}", response.getStatusCode());
            return generateMockResponse(null);

        } catch (Exception e) {
            log.error("调用 OpenAI API 失败", e);
            return generateMockResponse(null);
        }
    }

    private void callOpenAIStream(String systemPrompt, String userContent, Consumer<String> onChunk, Runnable onComplete) {
        try {
            LLMConfig.OpenAIConfig config = llmConfig.getOpenai();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(config.getApiKey());

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("stream", true);

            ArrayNode messages = requestBody.putArray("messages");

            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);

            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userContent);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setBufferRequestBody(false);
            RestTemplate streamRestTemplate = new RestTemplate(factory);

            ResponseExtractor<Void> extractor = (ClientHttpResponse response) -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6);
                            if ("[DONE]".equals(data)) {
                                break;
                            }
                            try {
                                JsonNode jsonNode = objectMapper.readTree(data);
                                JsonNode choices = jsonNode.path("choices");
                                if (choices.isArray() && choices.size() > 0) {
                                    JsonNode delta = choices.get(0).path("delta");
                                    String content = delta.path("content").asText();
                                    if (content != null && !content.isEmpty()) {
                                        onChunk.accept(content);
                                    }
                                }
                            } catch (Exception e) {
                                log.debug("解析流数据失败: {}", line);
                            }
                        }
                    }
                }
                return null;
            };

            streamRestTemplate.execute(
                    config.getBaseUrl() + "/chat/completions",
                    HttpMethod.POST,
                    request -> {
                        request.getHeaders().putAll(headers);
                        byte[] body = objectMapper.writeValueAsString(requestBody).getBytes(StandardCharsets.UTF_8);
                        request.getBody().write(body);
                    },
                    extractor
            );

            onComplete.run();

        } catch (Exception e) {
            log.error("调用 OpenAI 流式 API 失败", e);
            onChunk.accept(generateMockResponse(null));
            onComplete.run();
        }
    }

    private String callClaude(String systemPrompt, String userContent) {
        try {
            LLMConfig.ClaudeConfig config = llmConfig.getClaude();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", config.getApiKey());
            headers.set("anthropic-version", "2023-06-01");

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("system", systemPrompt);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userContent);

            HttpEntity<String> entity = new HttpEntity<>(
                    objectMapper.writeValueAsString(requestBody),
                    headers
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    config.getBaseUrl() + "/messages",
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                return jsonResponse.path("content").get(0).path("text").asText();
            }

            log.error("Claude API 调用失败: {}", response.getStatusCode());
            return generateMockResponse(null);

        } catch (Exception e) {
            log.error("调用 Claude API 失败", e);
            return generateMockResponse(null);
        }
    }

    private String buildSystemPrompt(Agent agent) {
        String basePrompt = agent.getSystemPrompt();
        if (basePrompt != null && !basePrompt.isEmpty()) {
            return basePrompt;
        }

        return switch (agent.getType()) {
            case INDUSTRY_ANALYST ->
                    "你是一位资深的行业分析师，专注于外部市场视角。分析行业趋势、市场规模、竞争格局。";
            case SKILL_ASSESSOR ->
                    "你是一位专业的能力评估师，专注于现实能力视角。分析技能匹配度、学习成本、转型可行性。";
            case RISK_WATCHER ->
                    "你是一位保守的风险警示者，专注于防御视角。识别最坏情况、Plan B、隐性成本。";
            case OPPORTUNITY_HUNTER ->
                    "你是一位积极的机会挖掘者，专注于进攻视角。发现蓝海市场、非对称机会。";
            case VALUE_EXAMINER ->
                    "你是一位深入的价值观拷问者，专注于内在动机视角。挖掘真实需求、内在动机。";
            default -> "你是一位职业规划顾问，请基于用户的情况给出专业建议。";
        };
    }

    private String generateMockResponse(Agent agent) {
        if (agent == null) {
            return "基于当前情况，建议综合考虑各方面因素后再做决定。";
        }

        return switch (agent.getType()) {
            case INDUSTRY_ANALYST ->
                    "从行业趋势来看，这个方向的复合年增长率为25%，但头部效应明显。\n\n" +
                    "1. 市场规模在未来5年将扩大3倍\n" +
                    "2. 但竞争也在加剧，需要评估自己的差异化优势\n" +
                    "3. 建议关注细分赛道的机会";
            case SKILL_ASSESSOR ->
                    "从能力匹配度分析，你需要关注以下几个关键点：\n\n" +
                    "1. 当前技能栈与目标岗位有约60%的重叠度\n" +
                    "2. 需要补足：A技术、B框架、C方法论\n" +
                    "3. 预计学习周期：6-12个月，取决于投入时间";
            case RISK_WATCHER ->
                    "我需要提醒你这个选择的风险：\n\n" +
                    "1. 最坏情况：转型失败，损失时间和金钱\n" +
                    "2. Plan B是什么？退路是否明确？\n" +
                    "3. 机会成本：当前岗位也有发展空间，值得放弃吗？";
            case OPPORTUNITY_HUNTER ->
                    "我看到了几个被忽视的机会：\n\n" +
                    "1. 你提到的困境其实是一个信号，说明你处在一个细分赛道的前夜\n" +
                    "2. 现在入场可以获得先发优势\n" +
                    "3. 建议大胆行动，不要等到完全准备好";
            case VALUE_EXAMINER ->
                    "我想追问几个核心问题：\n\n" +
                    "1. 你说想要稳定，但目标都是高风险方向，这之间的张力在哪里？\n" +
                    "2. 你真正追求的是收入、成长，还是自我实现？\n" +
                    "3. 5年后你想成为什么样的人？";
            default ->
                    "作为" + agent.getName() + "，我认为这个决策需要综合考虑多个因素...";
        };
    }

    /**
     * 使用AI生成候选方案（结构化输出）
     */
    public String generatePlansWithAI(Task task, List<Message> messages) {
        String provider = llmConfig.getDefaultProvider();

        if (hasApiKey(provider)) {
            StringBuilder conversation = new StringBuilder();
            conversation.append("用户背景：").append(task.getBackground()).append("\n");
            conversation.append("用户目标：").append(task.getGoal()).append("\n");
            if (task.getConstraints() != null && !task.getConstraints().isEmpty()) {
                conversation.append("约束条件：").append(task.getConstraints()).append("\n");
            }
            conversation.append("\n=== 专家讨论记录 ===\n\n");

            // 按Agent分组整理观点
            Map<String, List<String>> agentOpinions = new HashMap<>();
            for (Message msg : messages) {
                String agentName = msg.getAgent() != null ? msg.getAgent().getName() : "未知";
                String agentType = msg.getAgent() != null && msg.getAgent().getType() != null
                        ? msg.getAgent().getType().name() : "UNKNOWN";
                String key = agentName + "(" + agentType + ")";

                agentOpinions.computeIfAbsent(key, k -> new ArrayList<>()).add(msg.getContent());
            }

            for (Map.Entry<String, List<String>> entry : agentOpinions.entrySet()) {
                conversation.append("【").append(entry.getKey()).append("】\n");
                for (String content : entry.getValue()) {
                    conversation.append(content).append("\n");
                }
                conversation.append("\n");
            }

            String systemPrompt = "你是一位专业的职业规划顾问（Merge Agent）。基于专家讨论记录，生成2-3个具体的候选方案。\n\n" +
                    "你必须按以下JSON格式输出（不要包含任何其他文字）：\n\n" +
                    "{\n" +
                    "  \"plans\": [\n" +
                    "    {\n" +
                    "      \"title\": \"方案标题（如：渐进平衡型）\",\n" +
                    "      \"description\": \"方案描述（50-100字）\",\n" +
                    "      \"confidence\": 80,\n" +
                    "      \"supporters\": [\"支持者Agent名称\"],\n" +
                    "      \"opponents\": [\"反对者Agent名称\"],\n" +
                    "      \"milestones\": [\"里程碑1\", \"里程碑2\"],\n" +
                    "      \"risks\": [\"风险1\", \"风险2\"],\n" +
                    "      \"applicableConditions\": \"适用条件描述\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"blindSpots\": [\"认知盲区1\", \"认知盲区2\", \"认知盲区3\"]\n" +
                    "}\n\n" +
                    "要求：\n" +
                    "1. 每个方案必须有明确的支持者和反对者（从参与讨论的Agent中选择）\n" +
                    "2. 置信度基于支持Agent的数量和观点一致性（60-95之间）\n" +
                    "3. 方案应该有明显差异（激进vs保守、短期vs长期等）\n" +
                    "4. 认知盲区要指出用户潜在的矛盾或未考虑的因素";

            return callLLMAPI(provider, systemPrompt, conversation.toString());
        }

        log.warn("未配置 {} 的 API Key，使用模拟方案数据", provider);
        return generateMockPlans();
    }

    private String generateMockPlans() {
        return "{\n" +
               "  \"plans\": [\n" +
               "    {\n" +
               "      \"title\": \"激进冲刺型\",\n" +
               "      \"description\": \"全力转型到目标方向，6个月内完成技能储备并跳槽\",\n" +
               "      \"confidence\": 65,\n" +
               "      \"supporters\": [\"机会挖掘者\", \"行业分析师\"],\n" +
               "      \"opponents\": [\"风险警示者\"],\n" +
               "      \"milestones\": [\"3个月: 完成核心技能学习\", \"6个月: 拿到目标岗位offer\"],\n" +
               "      \"risks\": [\"转型期收入下降\", \"学习压力大\", \"失败退路有限\"],\n" +
               "      \"applicableConditions\": \"年轻、无家庭负担、有一定积蓄\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"title\": \"渐进平衡型\",\n" +
               "      \"description\": \"保持当前工作，业余时间学习新技能，1年后寻求内部转岗或跳槽\",\n" +
               "      \"confidence\": 80,\n" +
               "      \"supporters\": [\"能力评估师\", \"价值观拷问者\"],\n" +
               "      \"opponents\": [\"机会挖掘者\"],\n" +
               "      \"milestones\": [\"6个月: 完成基础技能学习\", \"12个月: 内部转岗或跳槽\"],\n" +
               "      \"risks\": [\"学习进度慢\", \"可能错过风口\", \"工作学习双重压力\"],\n" +
               "      \"applicableConditions\": \"有稳定工作、需要维持收入、家庭责任\"\n" +
               "    },\n" +
               "    {\n" +
               "      \"title\": \"探索验证型\",\n" +
               "      \"description\": \"先用3个月时间低成本探索，验证兴趣和能力匹配度后再决定\",\n" +
               "      \"confidence\": 75,\n" +
               "      \"supporters\": [\"风险警示者\", \"能力评估师\"],\n" +
               "      \"opponents\": [\"行业分析师\"],\n" +
               "      \"milestones\": [\"1个月: 完成行业调研\", \"3个月: 完成一个小项目验证\"],\n" +
               "      \"risks\": [\"时间成本\", \"验证不够充分\", \"可能错过时机\"],\n" +
               "      \"applicableConditions\": \"不确定方向、需要验证、风险偏好低\"\n" +
               "    }\n" +
               "  ],\n" +
               "  \"blindSpots\": [\n" +
               "    \"你说想要稳定，但目标都是高风险方向，可能存在内在矛盾\",\n" +
               "    \"对转型成本的估计可能过于乐观\",\n" +
               "    \"忽略了家庭因素对决策的约束\"\n" +
               "  ]\n" +
               "}";
    }

    private String generateMockMergeResult() {
        return "基于各位专家的深入讨论，我们梳理出了以下核心观点：\n\n" +
               "【共识】\n" +
               "- 转型方向具有发展前景，但需要充分的准备\n" +
               "- 当前技能有一定基础，但存在明显缺口\n" +
               "- 风险评估显示这是可控但非零风险的决策\n\n" +
               "【分歧】\n" +
               "- 激进派vs保守派：对时机的判断存在分歧\n" +
               "- 内在需求vs外在机会：价值观层面需要进一步厘清\n\n" +
               "【建议】\n" +
               "建议采用渐进式策略，在保持当前稳定的同时，有计划地积累新领域的能力。";
    }
}
