package com.careermind.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Component
public class KnowledgeBaseClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rag.service.url:http://localhost:3000}")
    private String ragServiceUrl;

    /**
     * 查询知识库，获取与用户问题相关的文档片段
     */
    public String queryKnowledgeBase(Long kbId, String query) {
        try {
            String url = ragServiceUrl + "/api/kb/" + kbId + "/query";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("query", query);
            requestBody.put("top_k", 5);
            requestBody.put("score_threshold", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return extractContextFromResponse(response.getBody());
            }

            log.warn("RAG服务查询失败: status={}, body={}", response.getStatusCode(), response.getBody());
            return "";
        } catch (Exception e) {
            log.error("调用RAG服务失败, kbId={}, query={}", kbId, query, e);
            return "";
        }
    }

    private String extractContextFromResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode results = root.path("results");

            if (!results.isArray() || results.isEmpty()) {
                return "";
            }

            StringBuilder context = new StringBuilder();
            context.append("=== 相关知识库资料 ===\n\n");

            int index = 1;
            for (JsonNode result : results) {
                String content = result.path("content").asText("");
                float score = result.path("score").floatValue();
                String filename = result.path("document").path("filename").asText("未知文档");

                if (!content.isEmpty()) {
                    context.append("[").append(index).append("] 来源: ").append(filename)
                           .append(" (相关度: ").append(String.format("%.2f", score)).append(")\n");
                    context.append(content).append("\n\n");
                    index++;
                }
            }

            return context.toString();
        } catch (Exception e) {
            log.error("解析RAG响应失败", e);
            return "";
        }
    }
}
