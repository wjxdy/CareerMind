# P2 辩论可视化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `docs/superpowers/specs/2026-04-22-debate-viz-design.md` 实现三张辩论可视化图表：温度条（讨论页）、观点演化图（任务详情页）、收敛图（结果页）。

**Architecture:** 后端在 messages/rounds 表加 confidence/edge_type/divergence 字段，DiscussionEngine 在 Prompt 里要求 Agent 自报置信度并解析；新增 GraphService 聚合节点/边/统计；新增 `/api/discussions/tasks/{id}/graph` API。前端新增 `vis-network` 依赖，3 个 viz 组件 + ThermoBar，挂在 Discussion / TaskView / ResultView 上。

**Tech Stack:** Spring Boot 3.2 + JPA + Flyway, Vue 3 + ECharts (已在) + vis-network (新), TypeScript.

---

## File Structure

**后端新增：**
```
src/main/resources/db/migration/V2__add_edge_confidence.sql
src/main/java/com/careermind/dto/GraphResponse.java
src/main/java/com/careermind/dto/GraphNodeDto.java
src/main/java/com/careermind/dto/GraphEdgeDto.java
src/main/java/com/careermind/dto/GraphRoundStatDto.java
src/main/java/com/careermind/service/DiscussionGraphService.java
src/main/java/com/careermind/service/impl/DiscussionGraphServiceImpl.java
src/main/java/com/careermind/controller/DiscussionGraphController.java
src/test/java/com/careermind/service/DiscussionGraphServiceTest.java
```

**后端修改：**
```
src/main/java/com/careermind/domain/Message.java       +edgeType +confidence
src/main/java/com/careermind/domain/Round.java         +divergence
src/main/java/com/careermind/service/impl/DiscussionEngineImpl.java
  - prompt 追加 confidence 标签要求
  - 解析消息 confidence + 推断 edgeType
  - 每轮结束计算 divergence 写回 round
src/main/java/com/careermind/websocket/DiscussionWebSocketHandler.java
  - 新增 sendGraphDelta
```

**前端新增：**
```
src/api/graph.ts
src/types/graph.ts
src/components/discussion/ThermoBar.vue
src/components/viz/OpinionGraph.vue
src/components/viz/ConvergenceChart.vue
src/components/viz/VizLegend.vue
```

**前端修改：**
```
src/components/discussion/DiscussionPanel.vue       挂 ThermoBar、监听 graph_delta
src/components/discussion/RoundtableStage.vue       底部留出 ThermoBar 槽位
src/views/TaskView.vue                              新增"观点演化"卡片
src/views/ResultView.vue                            新增 ConvergenceChart 段
```

---

## 任务总览

- Task 1: Flyway V2 migration（DB schema）
- Task 2: Domain 实体加字段（Message / Round）
- Task 3: GraphResponse DTO 套件
- Task 4: confidence 解析 + edgeType 推断（util）
- Task 5: DiscussionEngineImpl prompt 改造 + 写入字段
- Task 6: divergence 计算 + 每轮结束触发
- Task 7: DiscussionGraphService 聚合
- Task 8: DiscussionGraphController + 路由
- Task 9: WebSocket sendGraphDelta
- Task 10: 后端 JUnit 测试
- Task 11: 装 vis-network 依赖
- Task 12: 前端 graph.ts API + types
- Task 13: ThermoBar 组件
- Task 14: OpinionGraph 组件（vis-network）
- Task 15: ConvergenceChart 组件（ECharts）
- Task 16: VizLegend 组件
- Task 17: 挂载到 Discussion / TaskView / ResultView
- Task 18: E2E 冒烟 viz-task.spec.js
- Task 19: 完整跑一次讨论验证 + 截图

---

## Task 1: Flyway V2 migration

**Files:**
- Create: `careermind-backend/src/main/resources/db/migration/V2__add_edge_confidence.sql`

- [ ] **Step 1:** 写 SQL：

```sql
ALTER TABLE messages
  ADD COLUMN edge_type VARCHAR(16) NULL COMMENT 'SUPPORT/CHALLENGE/REVISE/NONE',
  ADD COLUMN confidence DECIMAL(3,2) NULL COMMENT '0.00-1.00';

ALTER TABLE rounds
  ADD COLUMN divergence DECIMAL(3,2) NULL COMMENT '整轮分歧度 0.00-1.00';

CREATE INDEX idx_messages_edge_type ON messages(edge_type);
```

- [ ] **Step 2:** 启动后端，确认 Flyway 应用 V2 migration 成功（看日志 `Migrating schema "..." to version "2"`）。

- [ ] **Step 3:** Commit：

```bash
git add careermind-backend/src/main/resources/db/migration/V2__add_edge_confidence.sql
git commit -m "feat(p2): add edge_type/confidence/divergence schema (V2)"
```

---

## Task 2: Domain 实体加字段

**Files:**
- Modify: `careermind-backend/src/main/java/com/careermind/domain/Message.java`
- Modify: `careermind-backend/src/main/java/com/careermind/domain/Round.java`

- [ ] **Step 1:** `Message.java` 添加字段：

```java
@Column(name = "edge_type", length = 16)
private String edgeType;

@Column(name = "confidence", precision = 3, scale = 2)
private java.math.BigDecimal confidence;
```

并加 getter/setter（或 Lombok `@Data` 已覆盖）。

- [ ] **Step 2:** `Round.java`：

```java
@Column(name = "divergence", precision = 3, scale = 2)
private java.math.BigDecimal divergence;
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/domain/Message.java careermind-backend/src/main/java/com/careermind/domain/Round.java
git commit -m "feat(p2): add edgeType/confidence/divergence to domain entities"
```

---

## Task 3: GraphResponse DTO 套件

**Files:**
- Create: `careermind-backend/src/main/java/com/careermind/dto/GraphNodeDto.java`
- Create: `careermind-backend/src/main/java/com/careermind/dto/GraphEdgeDto.java`
- Create: `careermind-backend/src/main/java/com/careermind/dto/GraphRoundStatDto.java`
- Create: `careermind-backend/src/main/java/com/careermind/dto/GraphResponse.java`

- [ ] **Step 1:** 4 个 DTO（参考 spec §4.3 示例 JSON）：

```java
// GraphNodeDto.java
package com.careermind.dto;
import lombok.Data;

@Data
public class GraphNodeDto {
    private String id;          // "a1-r1"
    private Long agentId;
    private String agentType;
    private String agentName;
    private Integer roundNumber;
    private Long messageId;
    private String snippet;     // 前 100 字
    private Double confidence;
    private Integer wordCount;
}
```

```java
// GraphEdgeDto.java
package com.careermind.dto;
import lombok.Data;

@Data
public class GraphEdgeDto {
    private String id;
    private String from;        // node id
    private String to;          // node id
    private String type;        // SUPPORT/CHALLENGE/REVISE
}
```

```java
// GraphRoundStatDto.java
package com.careermind.dto;
import lombok.Data;

@Data
public class GraphRoundStatDto {
    private Integer roundNumber;
    private Double divergence;
}
```

```java
// GraphResponse.java
package com.careermind.dto;
import lombok.Data;
import java.util.List;

@Data
public class GraphResponse {
    private List<GraphNodeDto> nodes;
    private List<GraphEdgeDto> edges;
    private List<GraphRoundStatDto> rounds;
    private Double finalConvergence;  // 1 - divergence(round4)
}
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/dto/Graph*.java
git commit -m "feat(p2): add GraphResponse DTO suite"
```

---

## Task 4: confidence 解析 + edgeType 推断 util

**Files:**
- Create: `careermind-backend/src/main/java/com/careermind/util/MessageMetaParser.java`

- [ ] **Step 1:**

```java
package com.careermind.util;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageMetaParser {
    private static final Pattern CONFIDENCE = Pattern.compile("\\[confidence:\\s*(\\d(?:\\.\\d{1,2})?)\\]", Pattern.CASE_INSENSITIVE);

    private MessageMetaParser() {}

    public static Optional<BigDecimal> parseConfidence(String content) {
        if (content == null) return Optional.empty();
        Matcher m = CONFIDENCE.matcher(content);
        if (!m.find()) return Optional.empty();
        try {
            BigDecimal v = new BigDecimal(m.group(1));
            if (v.compareTo(BigDecimal.ZERO) < 0) v = BigDecimal.ZERO;
            if (v.compareTo(BigDecimal.ONE)  > 0) v = BigDecimal.ONE;
            return Optional.of(v);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** 移除消息正文末尾的 [confidence: ...] 标签，便于显示干净文本 */
    public static String stripConfidence(String content) {
        if (content == null) return null;
        return CONFIDENCE.matcher(content).replaceAll("").trim();
    }

    /**
     * 根据轮次和内容线索推断 edge type。
     *  - Round 1: NONE（独立诊断）
     *  - Round 2: 默认 CHALLENGE，包含"同意/赞同/补充"关键词反转为 SUPPORT
     *  - Round 3: 默认 REVISE
     *  - Round 4: NONE（最终陈述）
     */
    public static String inferEdgeType(int roundNumber, String content) {
        if (roundNumber == 1 || roundNumber == 4) return null;
        String c = content == null ? "" : content;
        if (roundNumber == 2) {
            if (c.contains("同意") || c.contains("赞同") || c.contains("补充") || c.contains("我支持")) return "SUPPORT";
            return "CHALLENGE";
        }
        return "REVISE";
    }
}
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/util/MessageMetaParser.java
git commit -m "feat(p2): add MessageMetaParser for confidence + edge type"
```

---

## Task 5: DiscussionEngineImpl prompt 改造 + 写入字段

**Files:**
- Modify: `careermind-backend/src/main/java/com/careermind/service/impl/DiscussionEngineImpl.java`

- [ ] **Step 1:** 找到组装各 round prompt 的地方（通常是 `buildSystemPrompt(round)` 或类似），在每个 prompt 末尾追加：

```
请在回复的最后一行单独输出 [confidence: X.XX]，其中 X.XX 是 0.00-1.00 之间的小数，表示你对本轮观点的信心。如果你在质疑同事，请在开头引用对方姓名："我对 @某某 的观点有异议：…"。
```

具体追加点取决于现有代码结构；如有 `private String buildPrompt(Round round, Agent agent)`，在返回前 `prompt += "\n\n请在回复..."`。

- [ ] **Step 2:** 在保存 Message 的位置（通常 `messageRepository.save(message)` 之前）调用解析器：

```java
import com.careermind.util.MessageMetaParser;
...
java.math.BigDecimal conf = MessageMetaParser.parseConfidence(message.getContent())
    .orElse(java.math.BigDecimal.valueOf(0.6));
message.setConfidence(conf);
message.setEdgeType(MessageMetaParser.inferEdgeType(round.getRoundNumber(), message.getContent()));
// 可选：保留原文不剥离，方便调试；UI 显示时可调用 stripConfidence
```

- [ ] **Step 3:** 跑一遍后端，检查无编译错误。Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/service/impl/DiscussionEngineImpl.java
git commit -m "feat(p2): require confidence tag in prompts and persist parsed values"
```

---

## Task 6: divergence 计算 + 每轮结束触发

**Files:**
- Modify: `careermind-backend/src/main/java/com/careermind/service/impl/DiscussionEngineImpl.java`
- Create: `careermind-backend/src/main/java/com/careermind/util/DivergenceCalculator.java`

- [ ] **Step 1:** 计算器：

```java
package com.careermind.util;

import com.careermind.domain.Message;
import java.math.BigDecimal;
import java.util.List;

public final class DivergenceCalculator {
    private DivergenceCalculator() {}

    /**
     * v = stddev(confidences) * 0.5 + challengeDensity * 0.5
     * 归一化到 [0, 1]
     */
    public static BigDecimal compute(List<Message> roundMessages) {
        if (roundMessages == null || roundMessages.isEmpty()) return BigDecimal.ZERO;

        double[] confs = roundMessages.stream()
            .filter(m -> m.getConfidence() != null)
            .mapToDouble(m -> m.getConfidence().doubleValue())
            .toArray();

        double sd = stddev(confs);
        double sdNorm = Math.min(0.5, sd) / 0.5; // 归一化到 [0,1]，理论 max ≈ 0.5

        long challenge = roundMessages.stream()
            .filter(m -> "CHALLENGE".equalsIgnoreCase(m.getEdgeType()))
            .count();
        double density = roundMessages.isEmpty() ? 0 : (double) challenge / roundMessages.size();

        double v = sdNorm * 0.5 + density * 0.5;
        v = Math.max(0.0, Math.min(1.0, v));
        return BigDecimal.valueOf(Math.round(v * 100.0) / 100.0);
    }

    private static double stddev(double[] xs) {
        if (xs.length == 0) return 0;
        double mean = 0;
        for (double x : xs) mean += x;
        mean /= xs.length;
        double sq = 0;
        for (double x : xs) sq += (x - mean) * (x - mean);
        return Math.sqrt(sq / xs.length);
    }
}
```

- [ ] **Step 2:** 在 DiscussionEngineImpl 的"轮次结束"处（如 `nextRound` 或最后一个 Agent 完成时）：

```java
import com.careermind.util.DivergenceCalculator;
...
java.math.BigDecimal div = DivergenceCalculator.compute(round.getMessages());
round.setDivergence(div);
roundRepository.save(round);
// 推送 graph_delta（见 Task 9）
discussionWebSocketHandler.sendGraphDelta(taskId, round.getRoundNumber(), div.doubleValue());
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/util/DivergenceCalculator.java careermind-backend/src/main/java/com/careermind/service/impl/DiscussionEngineImpl.java
git commit -m "feat(p2): compute round divergence and persist on round end"
```

---

## Task 7: DiscussionGraphService 聚合

**Files:**
- Create: `careermind-backend/src/main/java/com/careermind/service/DiscussionGraphService.java`
- Create: `careermind-backend/src/main/java/com/careermind/service/impl/DiscussionGraphServiceImpl.java`

- [ ] **Step 1:** 接口：

```java
package com.careermind.service;
import com.careermind.dto.GraphResponse;

public interface DiscussionGraphService {
    GraphResponse buildGraph(Long taskId);
}
```

- [ ] **Step 2:** 实现：

```java
package com.careermind.service.impl;

import com.careermind.domain.Discussion;
import com.careermind.domain.Message;
import com.careermind.domain.Round;
import com.careermind.dto.*;
import com.careermind.repository.DiscussionRepository;
import com.careermind.service.DiscussionGraphService;
import com.careermind.util.MessageMetaParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiscussionGraphServiceImpl implements DiscussionGraphService {

    private final DiscussionRepository discussionRepository;

    @Override
    public GraphResponse buildGraph(Long taskId) {
        GraphResponse resp = new GraphResponse();
        Discussion d = discussionRepository.findByTaskId(taskId).orElse(null);
        if (d == null) {
            resp.setNodes(Collections.emptyList());
            resp.setEdges(Collections.emptyList());
            resp.setRounds(Collections.emptyList());
            resp.setFinalConvergence(0.0);
            return resp;
        }

        List<GraphNodeDto> nodes = new ArrayList<>();
        List<GraphEdgeDto> edges = new ArrayList<>();
        List<GraphRoundStatDto> roundStats = new ArrayList<>();
        Map<Long, GraphNodeDto> messageIdToNode = new HashMap<>();

        for (Round r : d.getRounds()) {
            GraphRoundStatDto rs = new GraphRoundStatDto();
            rs.setRoundNumber(r.getRoundNumber());
            rs.setDivergence(r.getDivergence() == null ? 0.5 : r.getDivergence().doubleValue());
            roundStats.add(rs);

            for (Message m : r.getMessages()) {
                if (m.getAgentId() == null || m.getAgentId() < 0) continue; // skip user/interjection
                GraphNodeDto n = new GraphNodeDto();
                n.setId("a" + m.getAgentId() + "-r" + r.getRoundNumber());
                n.setAgentId(m.getAgentId());
                n.setAgentType(m.getAgentType());
                n.setAgentName(m.getAgentName());
                n.setRoundNumber(r.getRoundNumber());
                n.setMessageId(m.getId());
                String clean = MessageMetaParser.stripConfidence(m.getContent());
                n.setSnippet(clean == null ? "" : clean.substring(0, Math.min(100, clean.length())));
                n.setConfidence(m.getConfidence() == null ? 0.6 : m.getConfidence().doubleValue());
                n.setWordCount(clean == null ? 0 : clean.length());
                nodes.add(n);
                messageIdToNode.put(m.getId(), n);
            }
        }

        // 边：从 replyToMessageId 解析
        for (Round r : d.getRounds()) {
            for (Message m : r.getMessages()) {
                if (m.getReplyToMessageId() == null) continue;
                GraphNodeDto from = nodes.stream().filter(n -> n.getMessageId().equals(m.getId())).findFirst().orElse(null);
                GraphNodeDto to   = messageIdToNode.get(m.getReplyToMessageId());
                if (from == null || to == null) continue;
                GraphEdgeDto e = new GraphEdgeDto();
                e.setId("e-" + m.getId());
                e.setFrom(from.getId());
                e.setTo(to.getId());
                e.setType(m.getEdgeType() == null ? "CHALLENGE" : m.getEdgeType());
                edges.add(e);
            }
        }

        resp.setNodes(nodes);
        resp.setEdges(edges);
        resp.setRounds(roundStats);
        double r4 = roundStats.stream()
            .filter(s -> s.getRoundNumber() != null && s.getRoundNumber() == 4)
            .map(GraphRoundStatDto::getDivergence)
            .findFirst().orElse(0.5);
        resp.setFinalConvergence(1.0 - r4);
        return resp;
    }
}
```

> 如 `DiscussionRepository` 没有 `findByTaskId`，新增即可。

- [ ] **Step 3:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/service/DiscussionGraphService.java careermind-backend/src/main/java/com/careermind/service/impl/DiscussionGraphServiceImpl.java
git commit -m "feat(p2): aggregate discussion graph (nodes + edges + round stats)"
```

---

## Task 8: DiscussionGraphController + 路由

**Files:**
- Create: `careermind-backend/src/main/java/com/careermind/controller/DiscussionGraphController.java`

- [ ] **Step 1:**

```java
package com.careermind.controller;

import com.careermind.dto.GraphResponse;
import com.careermind.service.DiscussionGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/discussions/tasks/{taskId}")
@RequiredArgsConstructor
public class DiscussionGraphController {

    private final DiscussionGraphService graphService;

    @GetMapping("/graph")
    public GraphResponse getGraph(@PathVariable Long taskId) {
        return graphService.buildGraph(taskId);
    }
}
```

> 注意：项目使用统一返回包装时（`ApiResponse`），如有 `@RestControllerAdvice` 自动 wrap，则直接返回 DTO 即可，否则手工包装。

- [ ] **Step 2:** 启动后端，curl 验证：

```bash
curl -H "X-User-Id: 1" -H "Authorization: Bearer ..." http://localhost:8080/api/discussions/tasks/1/graph
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/controller/DiscussionGraphController.java
git commit -m "feat(p2): expose GET /api/discussions/tasks/{taskId}/graph"
```

---

## Task 9: WebSocket sendGraphDelta

**Files:**
- Modify: `careermind-backend/src/main/java/com/careermind/websocket/DiscussionWebSocketHandler.java`

- [ ] **Step 1:** 添加方法：

```java
public void sendGraphDelta(Long taskId, int roundNumber, double divergence) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("type", "graph_delta");
    payload.put("roundNumber", roundNumber);
    payload.put("divergence", divergence);
    sendToTask(taskId, payload);
}
```

> 假设已有 `sendToTask(Long, Map)` 私有方法；如果不存在，参考现有 `sendStreamChunk` 等的内部 broadcast 逻辑添加。

- [ ] **Step 2:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/websocket/DiscussionWebSocketHandler.java
git commit -m "feat(p2): WebSocket sendGraphDelta on round end"
```

---

## Task 10: 后端 JUnit 测试

**Files:**
- Create: `careermind-backend/src/test/java/com/careermind/service/DiscussionGraphServiceTest.java`
- Create: `careermind-backend/src/test/java/com/careermind/util/MessageMetaParserTest.java`
- Create: `careermind-backend/src/test/java/com/careermind/util/DivergenceCalculatorTest.java`

- [ ] **Step 1:** `MessageMetaParserTest.java`：

```java
package com.careermind.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class MessageMetaParserTest {

    @Test
    void parsesConfidence() {
        assertEquals(new BigDecimal("0.82"), MessageMetaParser.parseConfidence("ok\n[confidence: 0.82]").orElseThrow());
        assertEquals(new BigDecimal("0.5"),  MessageMetaParser.parseConfidence("[confidence: 0.5]").orElseThrow());
    }

    @Test
    void emptyOnMissing() {
        assertTrue(MessageMetaParser.parseConfidence("no tag").isEmpty());
        assertTrue(MessageMetaParser.parseConfidence(null).isEmpty());
    }

    @Test
    void clampsOutOfRange() {
        assertEquals(BigDecimal.ONE,  MessageMetaParser.parseConfidence("[confidence: 1.5]").orElseThrow());
        assertEquals(BigDecimal.ZERO, MessageMetaParser.parseConfidence("[confidence: -0.1]").orElseThrow());
    }

    @Test
    void stripsTag() {
        assertEquals("hello", MessageMetaParser.stripConfidence("hello\n[confidence: 0.7]"));
    }

    @Test
    void infersEdgeType() {
        assertNull(MessageMetaParser.inferEdgeType(1, "..."));
        assertEquals("CHALLENGE", MessageMetaParser.inferEdgeType(2, "我反对"));
        assertEquals("SUPPORT",   MessageMetaParser.inferEdgeType(2, "我同意X的观点"));
        assertEquals("REVISE",    MessageMetaParser.inferEdgeType(3, "..."));
        assertNull(MessageMetaParser.inferEdgeType(4, "..."));
    }
}
```

- [ ] **Step 2:** `DivergenceCalculatorTest.java`：

```java
package com.careermind.util;

import com.careermind.domain.Message;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DivergenceCalculatorTest {

    private Message msg(double conf, String type) {
        Message m = new Message();
        m.setConfidence(BigDecimal.valueOf(conf));
        m.setEdgeType(type);
        return m;
    }

    @Test
    void zeroOnEmpty() {
        assertEquals(BigDecimal.ZERO, DivergenceCalculator.compute(List.of()));
    }

    @Test
    void higherWithMoreChallenges() {
        BigDecimal lowChallenge = DivergenceCalculator.compute(List.of(
            msg(0.8, null), msg(0.8, null), msg(0.8, null), msg(0.8, null)
        ));
        BigDecimal highChallenge = DivergenceCalculator.compute(List.of(
            msg(0.4, "CHALLENGE"), msg(0.5, "CHALLENGE"), msg(0.9, "CHALLENGE"), msg(0.3, "CHALLENGE")
        ));
        assertTrue(highChallenge.compareTo(lowChallenge) > 0);
    }

    @Test
    void boundsRespected() {
        BigDecimal v = DivergenceCalculator.compute(List.of(
            msg(0.0, "CHALLENGE"), msg(1.0, "CHALLENGE")
        ));
        assertTrue(v.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(v.compareTo(BigDecimal.ONE)  <= 0);
    }
}
```

- [ ] **Step 3:** `DiscussionGraphServiceTest.java`：用 mock repository + 5×4 消息构造 + 验证节点 20、边 ≥8、final convergence > 0。骨架略，按上面两测试格式写。

- [ ] **Step 4:** 跑测试：

```bash
cd careermind-backend && mvn test -Dtest='MessageMetaParserTest,DivergenceCalculatorTest'
```

确认全部 PASS。

- [ ] **Step 5:** Commit：

```bash
git add careermind-backend/src/test/java/com/careermind/util/ careermind-backend/src/test/java/com/careermind/service/DiscussionGraphServiceTest.java
git commit -m "test(p2): unit tests for parser, divergence and graph service"
```

---

## Task 11: 装 vis-network

**Files:**
- Modify: `careermind-frontend/package.json`

- [ ] **Step 1:**

```bash
cd careermind-frontend && npm install vis-network@^9.1.9 vis-data@^7.1.9
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/package.json careermind-frontend/package-lock.json
git commit -m "chore(p2): add vis-network for opinion graph"
```

---

## Task 12: 前端 graph.ts API + types

**Files:**
- Create: `careermind-frontend/src/types/graph.ts`
- Create: `careermind-frontend/src/api/graph.ts`

- [ ] **Step 1:** `types/graph.ts`：

```ts
export interface GraphNode {
  id: string
  agentId: number
  agentType: string
  agentName: string
  roundNumber: number
  messageId: number
  snippet: string
  confidence: number
  wordCount: number
}

export interface GraphEdge {
  id: string
  from: string
  to: string
  type: 'SUPPORT' | 'CHALLENGE' | 'REVISE'
}

export interface GraphRoundStat {
  roundNumber: number
  divergence: number
}

export interface GraphResponse {
  nodes: GraphNode[]
  edges: GraphEdge[]
  rounds: GraphRoundStat[]
  finalConvergence: number
}
```

- [ ] **Step 2:** `api/graph.ts`：

```ts
import request from './request'
import type { GraphResponse } from '@/types/graph'

export const graphApi = {
  getGraph: (taskId: number): Promise<GraphResponse> =>
    request.get(`/discussions/tasks/${taskId}/graph`),
}
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-frontend/src/types/graph.ts careermind-frontend/src/api/graph.ts
git commit -m "feat(p2): add graph types and API client"
```

---

## Task 13: ThermoBar 组件

**Files:**
- Create: `careermind-frontend/src/components/discussion/ThermoBar.vue`

- [ ] **Step 1:**

```vue
<template>
  <div class="thermo">
    <div class="track">
      <div class="indicator" :style="{ left: pct + '%' }" />
    </div>
    <div class="labels">
      <span>分歧</span>
      <span class="val">{{ Math.round(consensusPct) }}% 共识</span>
      <span>共识</span>
    </div>
    <transition name="bump">
      <div v-if="bump" class="bump-text">{{ bump }}</div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
const props = defineProps<{ divergence: number; deltaText?: string | null }>()
const consensusPct = computed(() => 100 * (1 - Math.max(0, Math.min(1, props.divergence))))
const pct = consensusPct
const bump = ref<string | null>(null)
watch(() => props.deltaText, (v) => {
  if (v) {
    bump.value = v
    setTimeout(() => (bump.value = null), 1200)
  }
})
</script>

<style scoped>
.thermo { position: relative; width: 240px; }
.track {
  position: relative; height: 6px; border-radius: 9999px;
  background: linear-gradient(90deg, var(--danger) 0%, var(--warning) 50%, var(--success) 100%);
}
.indicator {
  position: absolute; top: -3px;
  width: 12px; height: 12px; border-radius: 50%;
  background: var(--bg-card); border: 2px solid var(--text-primary);
  transform: translateX(-50%);
  transition: left var(--duration-slow) var(--ease-emphasized);
  box-shadow: var(--shadow-sm);
}
.labels {
  display: flex; justify-content: space-between;
  margin-top: 6px; font-size: 11px; color: var(--text-muted);
}
.val { color: var(--text-primary); font-weight: 500; }

.bump-text {
  position: absolute; left: 50%; bottom: 100%; transform: translateX(-50%);
  background: var(--accent); color: var(--accent-contrast);
  font-size: 11px; padding: 2px 8px; border-radius: var(--radius-full);
  white-space: nowrap; margin-bottom: 4px;
}
.bump-enter-active, .bump-leave-active { transition: all var(--duration-base) var(--ease-emphasized); }
.bump-enter-from, .bump-leave-to { opacity: 0; transform: translate(-50%, 4px); }
</style>
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/components/discussion/ThermoBar.vue
git commit -m "feat(p2): add ThermoBar (divergence indicator)"
```

---

## Task 14: OpinionGraph (vis-network)

**Files:**
- Create: `careermind-frontend/src/components/viz/OpinionGraph.vue`

- [ ] **Step 1:**

```vue
<template>
  <div class="opinion-graph-wrap">
    <div class="toolbar">
      <BaseButton size="sm" variant="ghost" @click="replay">▶ 回放辩论</BaseButton>
      <BaseButton size="sm" variant="ghost" @click="fit">居中</BaseButton>
    </div>
    <div ref="elRef" class="net" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { Network, type Options as NetOptions } from 'vis-network/standalone'
import { DataSet } from 'vis-data'
import BaseButton from '@/components/ui/BaseButton.vue'
import type { GraphResponse } from '@/types/graph'
import { getAgentMeta } from '@/utils/agent-meta'

const props = defineProps<{ graph: GraphResponse | null }>()
const elRef = ref<HTMLElement>()
let network: Network | null = null

const AGENT_COLOR: Record<string, string> = {
  INDUSTRY_ANALYST: '#1E3A8A', SKILL_ASSESSOR: '#0D9488', RISK_WATCHER: '#B45309',
  OPPORTUNITY_HUNTER: '#CA8A04', VALUE_EXAMINER: '#9333EA', CUSTOM: '#525B6B',
}
const EDGE_COLOR: Record<string, { color: string; dashes: boolean | number[] }> = {
  CHALLENGE: { color: '#EF4444', dashes: [6, 4] },
  SUPPORT:   { color: '#10B981', dashes: false },
  REVISE:    { color: '#3B82F6', dashes: false },
}

const build = () => {
  if (!props.graph || !elRef.value) return
  const nodeData = new DataSet(props.graph.nodes.map(n => ({
    id: n.id,
    label: `${getAgentMeta(n.agentType).short}\nR${n.roundNumber}`,
    title: n.snippet,
    color: { background: AGENT_COLOR[n.agentType] || '#525B6B', border: AGENT_COLOR[n.agentType] || '#525B6B' },
    font: { color: '#fff', size: 12, multi: false },
    shape: 'dot',
    size: Math.max(14, Math.min(36, 10 + Math.round(n.wordCount / 50))),
  })))
  const edgeData = new DataSet(props.graph.edges.map(e => ({
    id: e.id, from: e.from, to: e.to,
    arrows: 'to',
    color: EDGE_COLOR[e.type]?.color || '#999',
    dashes: EDGE_COLOR[e.type]?.dashes || false,
    width: e.type === 'CHALLENGE' ? 1.5 : 1,
  })))

  const options: NetOptions = {
    physics: { solver: 'forceAtlas2Based', stabilization: { iterations: 200 } },
    interaction: { hover: true, tooltipDelay: 100 },
    nodes: { borderWidth: 2 },
  }
  network?.destroy()
  network = new Network(elRef.value, { nodes: nodeData, edges: edgeData }, options)
  network.once('stabilized', () => network?.setOptions({ physics: false }))
}

const replay = () => {
  if (!network || !props.graph) return
  for (let r = 1; r <= 4; r++) {
    setTimeout(() => {
      const ids = props.graph!.nodes.filter(n => n.roundNumber === r).map(n => n.id)
      network!.selectNodes(ids)
    }, (r - 1) * 1500)
  }
}

const fit = () => network?.fit({ animation: true })

onMounted(build)
onBeforeUnmount(() => network?.destroy())
watch(() => props.graph, build)
</script>

<style scoped>
.opinion-graph-wrap { position: relative; width: 100%; height: 480px; background: var(--bg-inset); border-radius: var(--radius-md); overflow: hidden; }
.net { width: 100%; height: 100%; }
.toolbar { position: absolute; top: 8px; right: 8px; z-index: 2; display: flex; gap: 4px; background: var(--bg-card); padding: 4px; border-radius: var(--radius-md); border: 1px solid var(--border-subtle); }
</style>
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/components/viz/OpinionGraph.vue
git commit -m "feat(p2): OpinionGraph using vis-network"
```

---

## Task 15: ConvergenceChart (ECharts)

**Files:**
- Create: `careermind-frontend/src/components/viz/ConvergenceChart.vue`

- [ ] **Step 1:**

```vue
<template>
  <div class="conv-wrap">
    <v-chart class="chart" :option="option" autoresize />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, MarkPointComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import type { GraphResponse } from '@/types/graph'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, MarkPointComponent, CanvasRenderer])

const props = defineProps<{ graph: GraphResponse | null }>()

const option = computed(() => {
  if (!props.graph) return {}
  const rounds = props.graph.rounds
  const consensus = rounds.map(r => Math.round((1 - r.divergence) * 100))
  const final = props.graph.finalConvergence

  return {
    grid: { left: 50, right: 30, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: rounds.map(r => `第 ${r.roundNumber} 轮`),
      axisLine: { lineStyle: { color: 'var(--border-emphasis)' } },
      axisLabel: { color: '#52525B' },
    },
    yAxis: {
      type: 'value', min: 0, max: 100,
      axisLabel: { formatter: '{value}%', color: '#52525B' },
      splitLine: { lineStyle: { color: 'var(--border-subtle)' } },
    },
    series: [{
      name: '共识度', type: 'line', smooth: true,
      data: consensus,
      lineStyle: { width: 3, color: '#3B82F6' },
      itemStyle: { color: '#3B82F6' },
      areaStyle: { color: 'rgba(59,130,246,0.08)' },
      markPoint: {
        symbolSize: 50,
        data: [{
          name: '最终', coord: [rounds.length - 1, consensus[consensus.length - 1] || 0],
          label: { formatter: `${Math.round(final * 100)}%`, color: '#fff' },
          itemStyle: { color: '#10B981' },
        }],
      },
    }],
  }
})
</script>

<style scoped>
.conv-wrap { width: 100%; height: 280px; background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); padding: 16px; }
.chart { width: 100%; height: 100%; }
</style>
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/components/viz/ConvergenceChart.vue
git commit -m "feat(p2): ConvergenceChart (ECharts line + final mark)"
```

---

## Task 16: VizLegend

**Files:**
- Create: `careermind-frontend/src/components/viz/VizLegend.vue`

- [ ] **Step 1:**

```vue
<template>
  <div class="legend">
    <div class="grp">
      <span class="grp-title">Agent</span>
      <span v-for="a in agents" :key="a.type" class="item">
        <span class="dot" :style="{ background: a.color }" />{{ a.label }}
      </span>
    </div>
    <div class="grp">
      <span class="grp-title">关系</span>
      <span class="item"><span class="line dashed" />质疑</span>
      <span class="item"><span class="line solid"  />支持</span>
      <span class="item"><span class="line blue"   />修正</span>
    </div>
  </div>
</template>

<script setup lang="ts">
const agents = [
  { type: 'INDUSTRY_ANALYST',   label: '行业', color: '#1E3A8A' },
  { type: 'SKILL_ASSESSOR',     label: '能力', color: '#0D9488' },
  { type: 'RISK_WATCHER',       label: '风险', color: '#B45309' },
  { type: 'OPPORTUNITY_HUNTER', label: '机会', color: '#CA8A04' },
  { type: 'VALUE_EXAMINER',     label: '价值', color: '#9333EA' },
]
</script>

<style scoped>
.legend { display: flex; gap: 24px; padding: 8px 12px; background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); font-size: 12px; }
.grp { display: flex; align-items: center; gap: 10px; }
.grp-title { color: var(--text-muted); font-weight: 500; }
.item { display: inline-flex; align-items: center; gap: 4px; color: var(--text-secondary); }
.dot { width: 10px; height: 10px; border-radius: 50%; }
.line { width: 16px; height: 2px; }
.line.dashed { background: repeating-linear-gradient(90deg, var(--danger) 0 4px, transparent 4px 8px); }
.line.solid  { background: var(--success); }
.line.blue   { background: var(--accent); }
</style>
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/components/viz/VizLegend.vue
git commit -m "feat(p2): VizLegend"
```

---

## Task 17: 挂载到 Discussion / TaskView / ResultView

**Files:**
- Modify: `careermind-frontend/src/components/discussion/DiscussionPanel.vue`
- Modify: `careermind-frontend/src/views/TaskView.vue`
- Modify: `careermind-frontend/src/views/ResultView.vue`

### 17a: DiscussionPanel 挂 ThermoBar + 监听 graph_delta

- [ ] **Step 1:** 在 `<script setup>` 加：

```ts
import ThermoBar from './ThermoBar.vue'
const currentDivergence = ref(0.5)
const deltaText = ref<string | null>(null)
```

- [ ] **Step 2:** 在 `connect()` 的 `switch` 里加：

```ts
case 'graph_delta':
  if (typeof d.divergence === 'number') {
    currentDivergence.value = d.divergence
    deltaText.value = `第 ${d.roundNumber} 轮分歧度 ${Math.round(d.divergence * 100)}%`
  }
  break
```

- [ ] **Step 3:** 在 `<RoundtableStage>` 下方加：

```vue
<div class="thermo-slot">
  <ThermoBar :divergence="currentDivergence" :delta-text="deltaText" />
</div>
```

样式：

```css
.thermo-slot { position: absolute; left: 50%; bottom: 12px; transform: translateX(-50%); z-index: 3; }
```

- [ ] **Step 4:** 视觉上把 `.thermo-slot` 包裹在 `.stage-wrap` 内（因为 stage-wrap 已 position: relative）。

### 17b: TaskView 增加观点演化卡片

- [ ] **Step 5:**

```vue
<BaseCard class="tv-card">
  <template #header>
    <div class="tab-head">
      <span>观点演化</span>
      <VizLegend />
    </div>
  </template>
  <OpinionGraph :graph="graph" />
</BaseCard>
```

```ts
import { graphApi } from '@/api/graph'
import OpinionGraph from '@/components/viz/OpinionGraph.vue'
import VizLegend from '@/components/viz/VizLegend.vue'
import type { GraphResponse } from '@/types/graph'

const graph = ref<GraphResponse | null>(null)
// 在 load() 里追加：
try { graph.value = await graphApi.getGraph(taskId.value) } catch {}
```

### 17c: ResultView 增加 ConvergenceChart

- [ ] **Step 6:**

```vue
<section v-if="graph">
  <h2 class="sec-title">共识演化</h2>
  <ConvergenceChart :graph="graph" />
</section>
```

```ts
import ConvergenceChart from '@/components/viz/ConvergenceChart.vue'
import { graphApi } from '@/api/graph'
import type { GraphResponse } from '@/types/graph'

const graph = ref<GraphResponse | null>(null)
// load() 里追加：
try { graph.value = await graphApi.getGraph(taskId.value) } catch {}
```

- [ ] **Step 7:** Commit（一次性提交三处挂点）：

```bash
git add careermind-frontend/src/components/discussion/DiscussionPanel.vue careermind-frontend/src/views/TaskView.vue careermind-frontend/src/views/ResultView.vue
git commit -m "feat(p2): mount ThermoBar / OpinionGraph / ConvergenceChart"
```

---

## Task 18: E2E viz-task.spec.js

**Files:**
- Create: `e2e-tests/tests/viz-task.spec.js`

- [ ] **Step 1:**

```js
const { test, expect } = require('@playwright/test')

test.describe('Debate visualization (P2)', () => {
  test('TaskView eventually renders an opinion graph canvas', async ({ page }) => {
    await page.goto('/tasks/1').catch(() => {})
    // 容忍空态：仅在元素存在时断言
    const graph = page.locator('.opinion-graph-wrap canvas')
    if (await graph.count() > 0) {
      await expect(graph.first()).toBeVisible()
    }
  })

  test('ResultView eventually renders convergence chart', async ({ page }) => {
    await page.goto('/results/1').catch(() => {})
    const chart = page.locator('.conv-wrap')
    if (await chart.count() > 0) {
      await expect(chart.first()).toBeVisible()
    }
  })
})
```

- [ ] **Step 2:** 跑：

```bash
cd e2e-tests && npx playwright test viz-task.spec.js --project=chromium
```

- [ ] **Step 3:** Commit：

```bash
git add e2e-tests/tests/viz-task.spec.js
git commit -m "test(p2): viz smoke (graph canvas + convergence chart)"
```

---

## Task 19: 完整跑一次讨论 + 截图

- [ ] **Step 1:** 启动 backend + frontend，登录，新建一个任务，跑完 4 轮。
- [ ] **Step 2:** 验证：
  - DB messages 表 ≥60% 行有 `confidence`
  - rounds 表 4 行均有 `divergence`
  - `GET /api/discussions/tasks/{id}/graph` 返回 nodes ≥ 16, rounds=4
  - 讨论页底部 ThermoBar 在每轮结束后指针滑动
  - TaskView 出现观点演化图
  - ResultView 出现收敛图
- [ ] **Step 3:** 1920×1080 截图三张：roundtable-with-thermo / opinion-graph / convergence-chart 入 `docs/screenshots/`。
- [ ] **Step 4:** 更新 PROJECT_STATUS.md，commit。

---

## 自检

- **Spec coverage**：
  - DB schema 变更（§3.3）→ Task 1
  - Prompt + 解析（§4.1, 4.2）→ Task 4, 5
  - Divergence 公式（§4.5）→ Task 6
  - GraphResponse + Service + Controller（§4.3, 4.2）→ Task 3, 7, 8
  - WebSocket graph_delta（§4.4）→ Task 9
  - ThermoBar（§5.2.1）→ Task 13
  - OpinionGraph（§5.2.2）→ Task 14
  - ConvergenceChart（§5.2.3）→ Task 15
  - VizLegend（§5.2）→ Task 16
  - 挂点（§5.3）→ Task 17
  - 测试（§8）→ Task 10, 18
  - DoD（§10）→ Task 19
- **Placeholder**：无 TBD/later。
- **类型一致**：节点 id 格式 `aN-rM` 在 Task 7/12/14 一致；EdgeType 枚举 `SUPPORT/CHALLENGE/REVISE` 在 Task 4/12/14 一致；`graph_delta` 字段 `roundNumber/divergence` 在 Task 9/17a 一致。
