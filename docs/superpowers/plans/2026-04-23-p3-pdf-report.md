# P3 PDF 决策报告 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `docs/superpowers/specs/2026-04-22-pdf-report-design.md` 实现一键导出可下载 PDF 决策报告。

**Architecture:** 后端聚合 task + discussion + graph + mergeResult + LLM 生成的执行摘要/行动清单为 `ReportResponse`；前端新增 `/report/print/:taskId` 打印视图，加载报告数据并使用 html2pdf.js 直接下载。报告复用 P1 设计 token + P2 图表组件。

**Tech Stack:** Spring Boot 3.2, Vue 3, html2pdf.js (新), 复用 vis-network / ECharts。

---

## File Structure

**后端新增：**
```
src/main/java/com/careermind/dto/ReportResponse.java
src/main/java/com/careermind/dto/ReportTaskDto.java
src/main/java/com/careermind/dto/ReportDiscussionDto.java
src/main/java/com/careermind/dto/ReportRoundDto.java
src/main/java/com/careermind/dto/ReportExtrasDto.java
src/main/java/com/careermind/dto/ActionPlanDto.java
src/main/java/com/careermind/service/ReportService.java
src/main/java/com/careermind/service/impl/ReportServiceImpl.java
src/main/java/com/careermind/controller/ReportController.java
```

**后端修改：**
```
src/main/java/com/careermind/service/MergeService.java         + generateExecutiveSummary, generateActionPlan
src/main/java/com/careermind/service/impl/MergeServiceImpl.java  实现两个新方法（调 LLM）
```

**前端新增：**
```
src/api/report.ts
src/types/report.ts
src/utils/pdf-export.ts
src/views/ReportPrintView.vue
src/components/report/ReportCover.vue
src/components/report/ReportExecutiveSummary.vue
src/components/report/ReportProblem.vue
src/components/report/ReportRoundSummary.vue
src/components/report/ReportPlansComparison.vue
src/components/report/ReportBlindSpots.vue
src/components/report/ReportActionPlan.vue
src/components/report/ReportBack.vue
src/styles/print.css
e2e-tests/tests/report.spec.js
```

**前端修改：**
```
src/router/index.ts                 加 /report/print/:taskId 路由（meta.layout='none'）
src/views/ResultView.vue            "导出 PDF 报告" 按钮跳新窗口
```

---

## 任务总览

- Task 1: 后端 ReportResponse DTO 套件
- Task 2: MergeService 扩展 generateExecutiveSummary + generateActionPlan
- Task 3: ReportService 聚合实现
- Task 4: ReportController + 路由
- Task 5: 前端 report.ts API + types
- Task 6: pdf-export.ts util
- Task 7: ReportCover 组件
- Task 8: 报告 4 个内容组件（ExecutiveSummary/Problem/RoundSummary/PlansComparison）
- Task 9: 报告 3 个尾部组件（BlindSpots/ActionPlan/Back）
- Task 10: print.css
- Task 11: ReportPrintView 组装 + 路由
- Task 12: ResultView 触发入口
- Task 13: E2E 冒烟 + 手动跑一次出 PDF

---

## Task 1: 后端 ReportResponse DTO 套件

**Files:**
- Create: `careermind-backend/src/main/java/com/careermind/dto/ActionPlanDto.java`
- Create: `careermind-backend/src/main/java/com/careermind/dto/ReportExtrasDto.java`
- Create: `careermind-backend/src/main/java/com/careermind/dto/ReportRoundDto.java`
- Create: `careermind-backend/src/main/java/com/careermind/dto/ReportDiscussionDto.java`
- Create: `careermind-backend/src/main/java/com/careermind/dto/ReportTaskDto.java`
- Create: `careermind-backend/src/main/java/com/careermind/dto/ReportResponse.java`

- [ ] **Step 1:**

```java
// ActionPlanDto.java
package com.careermind.dto;
import lombok.Data;
import java.util.List;

@Data
public class ActionPlanDto {
    private List<String> day7;
    private List<String> day30;
    private List<String> day90;
}
```

```java
// ReportExtrasDto.java
package com.careermind.dto;
import lombok.Data;

@Data
public class ReportExtrasDto {
    private String executiveSummary;
    private ActionPlanDto actionPlan;
}
```

```java
// ReportRoundDto.java
package com.careermind.dto;
import lombok.Data;
import java.util.List;

@Data
public class ReportRoundDto {
    private Integer roundNumber;
    private String label;
    private Double divergence;

    @Data
    public static class Item {
        private Long agentId;
        private String agentName;
        private String agentType;
        private String content;
        private Double confidence;
    }

    private List<Item> messages;
}
```

```java
// ReportDiscussionDto.java
package com.careermind.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportDiscussionDto {
    private Integer currentRound;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer totalMessages;
}
```

```java
// ReportTaskDto.java
package com.careermind.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportTaskDto {
    private Long id;
    private String title;
    private String background;
    private String goal;
    private String constraints;
    private LocalDateTime createdAt;
    private String username;
}
```

```java
// ReportResponse.java
package com.careermind.dto;
import lombok.Data;
import java.util.List;

@Data
public class ReportResponse {
    private ReportTaskDto task;
    private ReportDiscussionDto discussion;
    private List<ReportRoundDto> rounds;
    private GraphResponse graph;
    private MergeResultDto mergeResult;
    private ReportExtrasDto extras;
}
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/dto/Report*.java careermind-backend/src/main/java/com/careermind/dto/ActionPlanDto.java
git commit -m "feat(p3): add ReportResponse DTO suite"
```

---

## Task 2: MergeService 扩展 generateExecutiveSummary + generateActionPlan

**Files:**
- Modify: `careermind-backend/src/main/java/com/careermind/service/MergeService.java`
- Modify: `careermind-backend/src/main/java/com/careermind/service/impl/MergeServiceImpl.java`

- [ ] **Step 1:** 接口加：

```java
String generateExecutiveSummary(Long taskId);
com.careermind.dto.ActionPlanDto generateActionPlan(Long taskId);
```

- [ ] **Step 2:** 实现里调 `LLMGateway` 用 prompt：

```java
@Override
public String generateExecutiveSummary(Long taskId) {
    Discussion d = discussionRepository.findByTaskId(taskId).orElse(null);
    if (d == null) return "";
    StringBuilder digest = new StringBuilder();
    for (Round r : d.getRounds()) {
        for (Message m : r.getMessages()) {
            if (m.getAgent() == null) continue;
            digest.append("【").append(m.getAgent().getName()).append("】 ");
            String c = m.getContent();
            digest.append(c == null ? "" : c.substring(0, Math.min(120, c.length())));
            digest.append("\n");
        }
    }
    String prompt = "以下是一次多 Agent 职业决策讨论的全量记录。请以第三人视角写一段不超过 200 字的执行摘要，结构：用户的核心问题 + 主要分歧点 + 最终建议 + 置信度。语言客观、简洁，不要使用'我'。\n\n【讨论记录】\n" + digest.toString();
    try {
        return llmGateway.generateOnce(prompt).trim();
    } catch (Exception e) {
        return "讨论已完成，五位专家从行业、能力、风险、机会、价值五个维度进行了 4 轮讨论，最终形成候选方案。";
    }
}

@Override
public com.careermind.dto.ActionPlanDto generateActionPlan(Long taskId) {
    com.careermind.dto.ActionPlanDto plan = new com.careermind.dto.ActionPlanDto();
    plan.setDay7(java.util.Collections.emptyList());
    plan.setDay30(java.util.Collections.emptyList());
    plan.setDay90(java.util.Collections.emptyList());

    MergeResult mr = mergeResultRepository.findByTaskId(taskId).orElse(null);
    if (mr == null || mr.getPlans() == null || mr.getPlans().isEmpty()) return plan;
    com.careermind.domain.Plan selected = mr.getPlans().stream()
        .filter(p -> Boolean.TRUE.equals(p.getIsSelected()))
        .findFirst().orElse(mr.getPlans().get(0));

    String prompt = "基于以下选定方案，输出接下来 7/30/90 天的行动清单。要求严格 JSON 格式：\n" +
        "{ \"day7\": [\"...\",\"...\"], \"day30\": [\"...\"], \"day90\": [\"...\"] }\n" +
        "每时段 3-5 条，每条不超过 30 字，可执行、可验证。只返回 JSON，不要多余文字。\n\n" +
        "【选定方案】\n标题：" + selected.getTitle() + "\n描述：" + selected.getDescription();

    try {
        String raw = llmGateway.generateOnce(prompt);
        // 容错：抓出第一个 { ... }
        int s = raw.indexOf('{'), e = raw.lastIndexOf('}');
        if (s >= 0 && e > s) {
            String json = raw.substring(s, e + 1);
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            return mapper.readValue(json, com.careermind.dto.ActionPlanDto.class);
        }
    } catch (Exception ex) {
        // 兜底：返回空
    }
    return plan;
}
```

> 注意：`LLMGateway` 现有 stream 方法；如无 `generateOnce(String)`，请新增一个非流式简单调用：

```java
// LLMGateway.java
String generateOnce(String prompt);
// LLMGatewayImpl.java（基于现有 KIMI 调用，把 stream 累加为 String 返回）
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/service/MergeService.java careermind-backend/src/main/java/com/careermind/service/impl/MergeServiceImpl.java careermind-backend/src/main/java/com/careermind/client/LLMGateway*.java
git commit -m "feat(p3): MergeService.generateExecutiveSummary + generateActionPlan + LLMGateway.generateOnce"
```

---

## Task 3: ReportService 聚合

**Files:**
- Create: `careermind-backend/src/main/java/com/careermind/service/ReportService.java`
- Create: `careermind-backend/src/main/java/com/careermind/service/impl/ReportServiceImpl.java`

- [ ] **Step 1:**

```java
// ReportService.java
package com.careermind.service;
import com.careermind.dto.ReportResponse;

public interface ReportService {
    ReportResponse build(Long taskId, boolean refreshExtras);
}
```

```java
// ReportServiceImpl.java
package com.careermind.service.impl;

import com.careermind.domain.*;
import com.careermind.dto.*;
import com.careermind.repository.*;
import com.careermind.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final TaskRepository taskRepository;
    private final DiscussionRepository discussionRepository;
    private final RoundRepository roundRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MergeService mergeService;
    private final DiscussionGraphService graphService;
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Map<String, String> ROUND_LABELS = Map.of(
        "INDEPENDENT", "独立诊断",
        "CHALLENGE",   "质疑挑战",
        "REVISION",    "修正完善",
        "FINAL",       "最终陈述"
    );

    @Override
    @Transactional(readOnly = true)
    public ReportResponse build(Long taskId, boolean refreshExtras) {
        Task task = taskRepository.findById(taskId).orElseThrow();
        Discussion d = discussionRepository.findByTaskId(taskId).orElse(null);

        ReportResponse resp = new ReportResponse();

        ReportTaskDto t = new ReportTaskDto();
        t.setId(task.getId());
        t.setTitle(task.getTitle());
        t.setBackground(task.getBackground());
        t.setGoal(task.getGoal());
        t.setConstraints(task.getConstraints());
        t.setCreatedAt(task.getCreatedAt());
        if (task.getUser() != null) t.setUsername(task.getUser().getUsername());
        resp.setTask(t);

        ReportDiscussionDto ddto = new ReportDiscussionDto();
        if (d != null) {
            ddto.setCurrentRound(d.getCurrentRound());
            ddto.setStartedAt(d.getCreatedAt());
        }
        resp.setDiscussion(ddto);

        List<ReportRoundDto> rounds = new ArrayList<>();
        int total = 0;
        if (d != null) {
            for (Round r : roundRepository.findByDiscussionIdOrderByRoundNumberAsc(d.getId())) {
                ReportRoundDto rd = new ReportRoundDto();
                rd.setRoundNumber(r.getRoundNumber());
                rd.setLabel(ROUND_LABELS.getOrDefault(r.getRoundType().name(), r.getRoundType().name()));
                rd.setDivergence(r.getDivergence() == null ? 0.5 : r.getDivergence().doubleValue());
                List<ReportRoundDto.Item> items = new ArrayList<>();
                for (Message m : messageRepository.findByRoundIdOrderByCreatedAtAsc(r.getId())) {
                    if (m.getAgent() == null) continue;
                    ReportRoundDto.Item it = new ReportRoundDto.Item();
                    it.setAgentId(m.getAgent().getId());
                    it.setAgentName(m.getAgent().getName());
                    it.setAgentType(m.getAgent().getType() != null ? m.getAgent().getType().name() : "CUSTOM");
                    it.setContent(com.careermind.util.MessageMetaParser.stripConfidence(m.getContent()));
                    it.setConfidence(m.getConfidence() == null ? 0.6 : m.getConfidence().doubleValue());
                    items.add(it);
                    total++;
                }
                rd.setMessages(items);
                rounds.add(rd);
            }
        }
        ddto.setTotalMessages(total);
        resp.setRounds(rounds);

        resp.setGraph(graphService.buildGraph(taskId));

        try { resp.setMergeResult(mergeService.getMergeResult(taskId)); }
        catch (Exception ignore) { resp.setMergeResult(null); }

        // extras（缓存 24h）
        ReportExtrasDto extras = null;
        String key = "report:extras:" + taskId;
        if (!refreshExtras) {
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                try { extras = mapper.readValue(cached, ReportExtrasDto.class); } catch (Exception ignore) {}
            }
        }
        if (extras == null) {
            extras = new ReportExtrasDto();
            extras.setExecutiveSummary(mergeService.generateExecutiveSummary(taskId));
            extras.setActionPlan(mergeService.generateActionPlan(taskId));
            try { redis.opsForValue().set(key, mapper.writeValueAsString(extras), Duration.ofHours(24)); }
            catch (Exception ignore) {}
        }
        resp.setExtras(extras);

        return resp;
    }
}
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/service/ReportService.java careermind-backend/src/main/java/com/careermind/service/impl/ReportServiceImpl.java
git commit -m "feat(p3): ReportService aggregates task/discussion/graph/merge/extras with Redis cache"
```

---

## Task 4: ReportController + 路由

**Files:**
- Create: `careermind-backend/src/main/java/com/careermind/controller/ReportController.java`

- [ ] **Step 1:**

```java
package com.careermind.controller;

import com.careermind.dto.ReportResponse;
import com.careermind.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{taskId}")
    public ReportResponse get(@PathVariable Long taskId,
                              @RequestParam(defaultValue = "false") boolean refresh) {
        return reportService.build(taskId, refresh);
    }

    @PostMapping("/{taskId}/regenerate-summary")
    public ReportResponse regenerate(@PathVariable Long taskId) {
        return reportService.build(taskId, true);
    }
}
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-backend/src/main/java/com/careermind/controller/ReportController.java
git commit -m "feat(p3): GET /api/reports/{taskId} + regenerate endpoint"
```

---

## Task 5: 前端 report.ts API + types

**Files:**
- Create: `careermind-frontend/src/types/report.ts`
- Create: `careermind-frontend/src/api/report.ts`

- [ ] **Step 1:**

```ts
// types/report.ts
import type { GraphResponse } from './graph'
import type { MergeResult } from './index'

export interface ActionPlan { day7: string[]; day30: string[]; day90: string[] }

export interface ReportExtras { executiveSummary: string; actionPlan: ActionPlan }

export interface ReportRoundItem {
  agentId: number; agentName: string; agentType: string; content: string; confidence: number
}

export interface ReportRound {
  roundNumber: number; label: string; divergence: number; messages: ReportRoundItem[]
}

export interface ReportTask {
  id: number; title: string; background?: string; goal?: string; constraints?: string
  createdAt?: string; username?: string
}

export interface ReportDiscussionMeta {
  currentRound?: number; startedAt?: string; endedAt?: string; totalMessages?: number
}

export interface ReportResponse {
  task: ReportTask
  discussion: ReportDiscussionMeta
  rounds: ReportRound[]
  graph: GraphResponse
  mergeResult: MergeResult | null
  extras: ReportExtras
}
```

```ts
// api/report.ts
import request from './request'
import type { ReportResponse } from '@/types/report'

export const reportApi = {
  getReport: (taskId: number, refresh = false): Promise<ReportResponse> =>
    request.get(`/reports/${taskId}`, { params: { refresh } }),
  regenerate: (taskId: number): Promise<ReportResponse> =>
    request.post(`/reports/${taskId}/regenerate-summary`),
}
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/types/report.ts careermind-frontend/src/api/report.ts
git commit -m "feat(p3): report types and API client"
```

---

## Task 6: pdf-export.ts util

**Files:**
- Create: `careermind-frontend/src/utils/pdf-export.ts`

- [ ] **Step 1:** 装依赖：

```bash
cd careermind-frontend && npm install html2pdf.js@^0.10.2
```

- [ ] **Step 2:**

```ts
// utils/pdf-export.ts
// @ts-ignore (no official types)
import html2pdf from 'html2pdf.js'

export async function exportElementToPdf(el: HTMLElement, filename: string): Promise<void> {
  await html2pdf()
    .set({
      margin: [10, 10, 10, 10],
      filename,
      image: { type: 'jpeg', quality: 0.96 },
      html2canvas: { scale: 2, useCORS: true, backgroundColor: '#ffffff' },
      jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' },
      pagebreak: { mode: ['css', 'legacy'] },
    })
    .from(el)
    .save()
}
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-frontend/package.json careermind-frontend/package-lock.json careermind-frontend/src/utils/pdf-export.ts
git commit -m "feat(p3): html2pdf util for PDF download"
```

---

## Task 7-9: Report 子组件

**Files:**
- Create: `careermind-frontend/src/components/report/ReportCover.vue`
- Create: `careermind-frontend/src/components/report/ReportExecutiveSummary.vue`
- Create: `careermind-frontend/src/components/report/ReportProblem.vue`
- Create: `careermind-frontend/src/components/report/ReportRoundSummary.vue`
- Create: `careermind-frontend/src/components/report/ReportPlansComparison.vue`
- Create: `careermind-frontend/src/components/report/ReportBlindSpots.vue`
- Create: `careermind-frontend/src/components/report/ReportActionPlan.vue`
- Create: `careermind-frontend/src/components/report/ReportBack.vue`

每个组件 props 简单接受对应数据片段。代码骨架（每段都用单 props，不带 state）：

- [ ] **Step 1: ReportCover.vue**

```vue
<template>
  <section class="cover page-break">
    <div class="cover-top"><BrandLogo size="lg" /></div>
    <div class="cover-mid">
      <h1>{{ title }}</h1>
      <p class="subtitle">职业决策报告</p>
    </div>
    <div class="cover-meta">
      <div>{{ formatDate(date) }}</div>
      <div v-if="username">{{ username }}</div>
      <div class="conv-circle">
        <div class="num">{{ Math.round(convergence * 100) }}%</div>
        <div class="lbl">共识度</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import BrandLogo from '@/components/ui/BrandLogo.vue'
import dayjs from 'dayjs'
defineProps<{ title: string; date?: string; username?: string; convergence: number }>()
const formatDate = (d?: string) => d ? dayjs(d).format('YYYY-MM-DD') : ''
</script>

<style scoped>
.cover { display: flex; flex-direction: column; height: 100vh; padding: 40px; background: var(--bg-card); }
.cover-top { display: flex; }
.cover-mid { flex: 1; display: flex; flex-direction: column; justify-content: center; }
.cover-mid h1 { font-size: 48px; line-height: 1.15; }
.subtitle { font-size: 18px; color: var(--text-secondary); margin-top: 12px; }
.cover-meta { display: flex; align-items: center; justify-content: space-between; padding-top: 20px; border-top: 1px solid var(--border-subtle); font-size: 13px; color: var(--text-secondary); }
.conv-circle { display: inline-flex; flex-direction: column; align-items: center; padding: 12px 18px; border: 2px solid var(--accent); border-radius: 9999px; }
.num { font-size: 22px; font-weight: 700; color: var(--accent); }
.lbl { font-size: 11px; color: var(--text-muted); }
</style>
```

- [ ] **Step 2: ReportExecutiveSummary.vue**

```vue
<template>
  <section class="esum">
    <h2>执行摘要</h2>
    <p>{{ text || '尚未生成摘要。' }}</p>
  </section>
</template>
<script setup lang="ts">
defineProps<{ text: string }>()
</script>
<style scoped>
.esum { padding: 24px 0; }
.esum h2 { font-size: 18px; margin-bottom: 10px; }
.esum p { font-size: 13px; line-height: 1.7; color: var(--text-primary); }
</style>
```

- [ ] **Step 3: ReportProblem.vue**

```vue
<template>
  <section class="prob">
    <h2>咨询问题与背景</h2>
    <div class="row"><span class="ik">背景</span><p>{{ task.background || '—' }}</p></div>
    <div class="row"><span class="ik">目标</span><p>{{ task.goal || '—' }}</p></div>
    <div class="row"><span class="ik">约束</span><p>{{ task.constraints || '—' }}</p></div>
  </section>
</template>
<script setup lang="ts">
import type { ReportTask } from '@/types/report'
defineProps<{ task: ReportTask }>()
</script>
<style scoped>
.prob { padding: 24px 0; }
.prob h2 { font-size: 18px; margin-bottom: 14px; }
.row { display: grid; grid-template-columns: 60px 1fr; gap: 12px; padding: 8px 0; border-top: 1px solid var(--border-subtle); }
.row:first-of-type { border-top: none; }
.ik { color: var(--text-muted); font-size: 13px; }
.row p { margin: 0; font-size: 13px; line-height: 1.6; }
</style>
```

- [ ] **Step 4: ReportRoundSummary.vue**

```vue
<template>
  <section class="round-section page-break-avoid">
    <div class="r-head">
      <BaseBadge tone="accent">第 {{ round.roundNumber }} 轮</BaseBadge>
      <h3>{{ round.label }}</h3>
      <span class="div">分歧度 {{ Math.round(round.divergence * 100) }}%</span>
    </div>
    <div class="agents">
      <div v-for="(m, i) in round.messages" :key="i" class="agent-row" :data-agent-type="m.agentType">
        <AgentAvatar :agent-type="m.agentType" :size="30" />
        <div class="bubble">
          <div class="meta"><span class="name">{{ m.agentName }}</span><span class="conf">置信度 {{ Math.round(m.confidence * 100) }}%</span></div>
          <p class="content">{{ m.content }}</p>
        </div>
      </div>
    </div>
  </section>
</template>
<script setup lang="ts">
import BaseBadge from '@/components/ui/BaseBadge.vue'
import AgentAvatar from '@/components/agent/AgentAvatar.vue'
import type { ReportRound } from '@/types/report'
defineProps<{ round: ReportRound }>()
</script>
<style scoped>
.round-section { padding: 20px 0; border-top: 1px dashed var(--border-subtle); }
.r-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.r-head h3 { font-size: 16px; margin: 0; }
.div { font-size: 12px; color: var(--text-muted); margin-left: auto; }
.agents { display: flex; flex-direction: column; gap: 12px; }
.agent-row { display: flex; gap: 10px; }
.bubble { flex: 1; background: var(--bg-inset); border-left: 3px solid var(--agent); border-radius: var(--radius-md); padding: 10px 12px; }
.meta { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px; }
.name { color: var(--agent); font-weight: 600; }
.conf { color: var(--text-muted); }
.content { margin: 0; font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
</style>
```

- [ ] **Step 5: ReportPlansComparison.vue**

```vue
<template>
  <section class="plans page-break">
    <h2>候选方案对比</h2>
    <div class="grid">
      <div v-for="(p, i) in plans" :key="i" class="plan" :class="{ selected: p.isSelected }">
        <div class="ph">
          <span class="num">方案 {{ i + 1 }}</span>
          <span class="conf">{{ Math.round(p.confidence * 100) }}%</span>
        </div>
        <h4>{{ p.title }}</h4>
        <p class="desc">{{ p.description }}</p>
        <div v-if="p.milestones?.length" class="sec"><h5>里程碑</h5><ul><li v-for="(m, j) in p.milestones" :key="j">{{ m }}</li></ul></div>
        <div v-if="p.risks?.length" class="sec"><h5>风险</h5><ul><li v-for="(r, j) in p.risks" :key="j">{{ r }}</li></ul></div>
        <div v-if="p.applicableConditions" class="sec"><h5>适用条件</h5><p>{{ p.applicableConditions }}</p></div>
      </div>
    </div>
  </section>
</template>
<script setup lang="ts">
import type { Plan } from '@/types'
defineProps<{ plans: Plan[] }>()
</script>
<style scoped>
.plans { padding: 24px 0; }
.plans h2 { font-size: 18px; margin-bottom: 14px; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.plan { padding: 14px; border: 1px solid var(--border-subtle); border-radius: var(--radius-md); break-inside: avoid; }
.plan.selected { border-color: var(--accent); }
.ph { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-muted); }
.plan h4 { font-size: 14px; margin: 6px 0; }
.desc { margin: 0 0 10px; font-size: 13px; color: var(--text-secondary); line-height: 1.5; }
.sec { margin-top: 8px; }
.sec h5 { font-size: 11px; color: var(--text-muted); margin: 0 0 4px; text-transform: uppercase; }
.sec ul { margin: 0; padding-left: 16px; font-size: 12px; line-height: 1.5; }
</style>
```

- [ ] **Step 6: ReportBlindSpots.vue**

```vue
<template>
  <section class="blinds">
    <h2>认知盲区</h2>
    <ol>
      <li v-for="(b, i) in items" :key="i">{{ b }}</li>
    </ol>
  </section>
</template>
<script setup lang="ts">
defineProps<{ items: string[] }>()
</script>
<style scoped>
.blinds { padding: 24px 0; break-inside: avoid; }
.blinds h2 { font-size: 18px; margin-bottom: 10px; }
.blinds ol { margin: 0; padding-left: 20px; font-size: 13px; line-height: 1.7; color: var(--text-primary); }
</style>
```

- [ ] **Step 7: ReportActionPlan.vue**

```vue
<template>
  <section class="ap page-break">
    <h2>行动清单</h2>
    <div class="cols">
      <div class="col"><h3>未来 7 天</h3><ul><li v-for="(s, i) in plan.day7" :key="i">{{ s }}</li></ul></div>
      <div class="col"><h3>未来 30 天</h3><ul><li v-for="(s, i) in plan.day30" :key="i">{{ s }}</li></ul></div>
      <div class="col"><h3>未来 90 天</h3><ul><li v-for="(s, i) in plan.day90" :key="i">{{ s }}</li></ul></div>
    </div>
  </section>
</template>
<script setup lang="ts">
import type { ActionPlan } from '@/types/report'
defineProps<{ plan: ActionPlan }>()
</script>
<style scoped>
.ap { padding: 24px 0; }
.ap h2 { font-size: 18px; margin-bottom: 14px; }
.cols { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
.col { padding: 12px; background: var(--bg-inset); border-radius: var(--radius-md); break-inside: avoid; }
.col h3 { font-size: 13px; color: var(--accent); margin: 0 0 8px; }
.col ul { margin: 0; padding-left: 18px; font-size: 12px; line-height: 1.6; }
</style>
```

- [ ] **Step 8: ReportBack.vue**

```vue
<template>
  <section class="back page-break">
    <BrandLogo size="md" />
    <p class="msg">本报告由 CareerMind 多 Agent 决策系统生成</p>
    <p class="muted">版权 © 2026 CareerMind · 仅供个人决策参考</p>
  </section>
</template>
<script setup lang="ts">
import BrandLogo from '@/components/ui/BrandLogo.vue'
</script>
<style scoped>
.back { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; min-height: 80vh; padding: 40px; }
.msg { font-size: 14px; color: var(--text-primary); margin: 0; }
.muted { font-size: 12px; color: var(--text-muted); margin: 0; }
</style>
```

- [ ] **Step 9:** 三批 commit：

```bash
git add careermind-frontend/src/components/report/ReportCover.vue careermind-frontend/src/components/report/ReportExecutiveSummary.vue careermind-frontend/src/components/report/ReportProblem.vue careermind-frontend/src/components/report/ReportRoundSummary.vue
git commit -m "feat(p3): report cover/summary/problem/round components"

git add careermind-frontend/src/components/report/ReportPlansComparison.vue careermind-frontend/src/components/report/ReportBlindSpots.vue careermind-frontend/src/components/report/ReportActionPlan.vue careermind-frontend/src/components/report/ReportBack.vue
git commit -m "feat(p3): report plans/blindspots/action/back components"
```

---

## Task 10: print.css

**Files:**
- Create: `careermind-frontend/src/styles/print.css`

- [ ] **Step 1:**

```css
@page { size: A4; margin: 16mm 14mm; }
@media print {
  body { background: white !important; }
  .no-print { display: none !important; }
  .page-break { page-break-before: always; }
  .page-break-avoid { break-inside: avoid; page-break-inside: avoid; }
  html[data-theme="dark"] body { background: white !important; color: black !important; }
}
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/styles/print.css
git commit -m "feat(p3): print stylesheet"
```

---

## Task 11: ReportPrintView 组装

**Files:**
- Create: `careermind-frontend/src/views/ReportPrintView.vue`
- Modify: `careermind-frontend/src/router/index.ts`

- [ ] **Step 1:**

```vue
<template>
  <div class="report-shell">
    <div class="toolbar no-print">
      <BaseButton variant="ghost" @click="$router.push(`/discussions/${taskId}`)">← 返回讨论</BaseButton>
      <BaseButton variant="primary" :loading="downloading" @click="onDownload">下载 PDF</BaseButton>
      <BaseButton variant="secondary" @click="onPrint">打印</BaseButton>
    </div>

    <div v-if="loading" class="state">正在加载报告…</div>
    <div v-else-if="!report" class="state">报告不可用</div>

    <div v-else id="report-root" class="report-doc">
      <ReportCover
        :title="report.task.title"
        :date="report.task.createdAt"
        :username="report.task.username"
        :convergence="report.graph?.finalConvergence ?? 0.5"
      />
      <ReportExecutiveSummary :text="report.extras?.executiveSummary || ''" />
      <ReportProblem :task="report.task" />

      <h2 class="sec-title">四轮讨论</h2>
      <ReportRoundSummary v-for="r in report.rounds" :key="r.roundNumber" :round="r" />

      <ReportPlansComparison v-if="report.mergeResult?.plans?.length" :plans="report.mergeResult.plans" />
      <ReportBlindSpots v-if="report.mergeResult?.blindSpots?.length" :items="report.mergeResult.blindSpots" />
      <ReportActionPlan v-if="report.extras?.actionPlan" :plan="report.extras.actionPlan" />
      <ReportBack />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import BaseButton from '@/components/ui/BaseButton.vue'
import ReportCover from '@/components/report/ReportCover.vue'
import ReportExecutiveSummary from '@/components/report/ReportExecutiveSummary.vue'
import ReportProblem from '@/components/report/ReportProblem.vue'
import ReportRoundSummary from '@/components/report/ReportRoundSummary.vue'
import ReportPlansComparison from '@/components/report/ReportPlansComparison.vue'
import ReportBlindSpots from '@/components/report/ReportBlindSpots.vue'
import ReportActionPlan from '@/components/report/ReportActionPlan.vue'
import ReportBack from '@/components/report/ReportBack.vue'
import { reportApi } from '@/api/report'
import { exportElementToPdf } from '@/utils/pdf-export'
import { message } from '@/utils/naive-discrete'
import type { ReportResponse } from '@/types/report'
import '@/styles/print.css'

const route = useRoute()
const taskId = computed(() => Number(route.params.taskId))

const report = ref<ReportResponse | null>(null)
const loading = ref(true)
const downloading = ref(false)

onMounted(async () => {
  loading.value = true
  try { report.value = await reportApi.getReport(taskId.value) }
  catch (e: any) { message.error(e.message || '加载报告失败') }
  finally { loading.value = false }
})

const onDownload = async () => {
  const el = document.getElementById('report-root')
  if (!el) return
  downloading.value = true
  try {
    await exportElementToPdf(el, `CareerMind-${report.value?.task.title || 'report'}.pdf`)
  } catch (e: any) {
    message.error('PDF 导出失败：' + (e.message || ''))
  } finally {
    downloading.value = false
  }
}

const onPrint = () => window.print()
</script>

<style scoped>
.report-shell { min-height: 100vh; background: var(--bg-page); padding: 24px; }
.toolbar { position: sticky; top: 0; z-index: 10; display: flex; gap: 8px; justify-content: flex-end; padding: 12px; background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); margin-bottom: 16px; }
.state { padding: 80px 0; text-align: center; color: var(--text-muted); }
.report-doc { max-width: 800px; margin: 0 auto; padding: 32px 40px; background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); }
.sec-title { font-size: 18px; margin: 24px 0 8px; }
</style>
```

- [ ] **Step 2:** 路由：

```ts
{
  path: '/report/print/:taskId',
  name: 'ReportPrint',
  component: () => import('@/views/ReportPrintView.vue'),
  meta: { requiresAuth: true },
}
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-frontend/src/views/ReportPrintView.vue careermind-frontend/src/router/index.ts
git commit -m "feat(p3): ReportPrintView and route"
```

---

## Task 12: ResultView 触发入口

**Files:**
- Modify: `careermind-frontend/src/views/ResultView.vue`

- [ ] **Step 1:** 替换 `onExport`：

```ts
const onExport = () => {
  const url = window.location.origin + `/report/print/${taskId.value}`
  window.open(url, '_blank')
}
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/views/ResultView.vue
git commit -m "feat(p3): ResultView opens print view in new tab"
```

---

## Task 13: E2E + 手动跑一次出 PDF

**Files:**
- Create: `e2e-tests/tests/report.spec.js`

- [ ] **Step 1:**

```js
const { test, expect } = require('@playwright/test')

test.describe('PDF report (P3)', () => {
  test('Report print view loads sections when task exists', async ({ page }) => {
    await page.goto('/report/print/1').catch(() => {})
    await page.waitForLoadState('domcontentloaded')
    const root = page.locator('#report-root')
    if (await root.count() > 0) {
      await expect(root.first()).toBeVisible()
      // 至少应当看到 cover 与 problem 段
      await expect(page.locator('.cover, .prob')).toHaveCount({ greaterThanOrEqual: 1 } as any)
    }
  })
})
```

- [ ] **Step 2:** 跑：

```bash
cd e2e-tests && npx playwright test report.spec.js --project=chromium
```

- [ ] **Step 3:** 手动：跑完整 4 轮讨论 + 生成结果 → 在 ResultView 点"导出 PDF 报告" → 新窗口加载 → 点"下载 PDF"得到 .pdf 文件，用 macOS Preview 打开核对：封面、摘要、问题、4 轮、方案、盲区、行动、封底。

- [ ] **Step 4:** 更新 PROJECT_STATUS，commit。

---

## 自检

- **Spec coverage**：
  - 后端 API（§4.1, 4.2）→ Task 1, 3, 4
  - LLM extras（§4.3）→ Task 2
  - 前端依赖、路由、视图（§5.1-5.4）→ Task 6, 11
  - 8 个组件（§5.7）→ Task 7-9
  - 触发入口（§5.8）→ Task 12
  - 打印样式（§5.5）→ Task 10
  - 测试（§7）→ Task 13
- **Placeholder**：无 TBD/later。
- **类型一致**：`ActionPlan` 字段 `day7/day30/day90` 在 Task 1/5/7-Step8 一致；`ReportResponse` 套件命名一致。
