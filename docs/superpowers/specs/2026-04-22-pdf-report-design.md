# PDF 决策报告 设计文档（P3）

- 创建日期：2026-04-22
- 依赖：P1（设计体系）、P2（观点演化图、收敛图）

## 1. 目标

提供**一键生成可下载 PDF**的专业决策报告，让用户"把讨论结果带走"，同时作为比赛评委的实物产出（打印版/PDF 版均可）。

## 2. 非目标

- 不做报告模板的多样化（v1 只有一份模板）
- 不做 Word/PPT 导出
- 不做在线分享短链/协作评论
- 不做定时生成/邮件推送

## 3. 技术路线

### 3.1 决策与权衡

| 方案 | 优 | 劣 |
|---|---|---|
| **前端 html2pdf.js（选）** | 零服务端成本、所见即所得、样式与设计体系一致 | 大图截图受 DOM 限制；中文字体需内嵌 |
| 前端 jsPDF（手写绘图） | 轻量 | 开发量大，排版复杂 |
| 后端 openhtmltopdf | 服务端渲染稳定 | CSS 支持有限，flex/grid 支持差 |
| 后端 Puppeteer 微服务 | 最高保真 | 新部署单元，Chrome 重 |

**选 html2pdf.js**：配合专用 `/report/print/:taskId` 打印视图，最快落地 + 外观可控。

### 3.2 数据流

```
Result 页 → 点"导出 PDF 报告" → 新窗口打开 /report/print/:taskId
  ↓
打印视图挂载 → 调 GET /api/reports/{taskId} 聚合数据 → 全量渲染
  ↓
所有 ECharts/vis-network 图在 mounted 后等 1 帧，截为 PNG base64 inline
  ↓
用户点"下载 PDF" 按钮 → html2pdf 将整页分页生成 PDF → 自动触发 download
```

## 4. 后端

### 4.1 新增 API

`GET /api/reports/{taskId}` → `ReportResponse`

```json
{
  "task": {
    "id": 123, "title": "要不要裸辞考研", "background": "…",
    "goal": "…", "constraints": "…", "createdAt": "…", "user": "…"
  },
  "discussion": {
    "currentRound": 4,
    "startedAt": "…", "endedAt": "…",
    "totalMessages": 24
  },
  "rounds": [
    { "roundNumber": 1, "label": "独立诊断", "divergence": 0.65,
      "messages": [ { "agentId":1, "agentName":"…", "content":"…", "confidence":0.8 } ] },
    ...
  ],
  "graph": {  "... 与 P2 graph 相同结构" },
  "mergeResult": {
    "summary": "…",
    "plans": [ { "title":"…", "description":"…", "confidence":0.85, "supporters":[], "opponents":[], "milestones":[], "risks":[], "applicableConditions":"…" } ],
    "blindSpots": [ "…" ],
    "convergenceRate": 0.88
  },
  "extras": {
    "executiveSummary": "AI 生成的 200 字摘要",
    "actionPlan": {
      "day7":  [ "…" ],
      "day30": [ "…" ],
      "day90": [ "…" ]
    }
  }
}
```

### 4.2 服务层

扩展 `MergeService`：
- `String generateExecutiveSummary(Task, MergeResult)` — 调 LLM 生成 200 字摘要
- `ActionPlan generateActionPlan(Task, Plan selectedPlan)` — 调 LLM 生成 7/30/90 天行动项（结构化 JSON），带 prompt 约束输出格式

新增 `ReportService`：
- `ReportResponse build(Long taskId)` — 聚合 task/discussion/graph/mergeResult/extras
- 对 `extras` 字段做缓存：
  - 用 Redis key `report:extras:{taskId}`，TTL 24h
  - 首次生成后缓存；若用户重新生成则带 `?refresh=true` 旁路

新增 `ReportController`：
- `GET /api/reports/{taskId}` → `ReportResponse`
- `POST /api/reports/{taskId}/regenerate-summary` — 强制重算 extras

### 4.3 Prompt 模板

**executiveSummary** prompt：

> 以下是一次多 Agent 职业决策讨论的全量记录。请以第三人视角写一段不超过 200 字的执行摘要，结构：用户的核心问题 + 主要分歧点 + 最终建议 + 置信度。语言客观、简洁，不要使用"我"。
> 【讨论记录】…

**actionPlan** prompt：

> 基于以下选定方案，输出接下来 7/30/90 天的行动清单。要求 JSON 格式：
> ```json
> { "day7": ["…","…","…"], "day30": [], "day90": [] }
> ```
> 每时段 3-5 条，每条不超过 30 字，可执行、可验证。
> 【选定方案】…

### 4.4 文件变更（后端）

**新增**：
```
domain/ActionPlan.java                  （可选，或直接 POJO 在 DTO 内）
dto/ReportResponse.java
dto/ReportTaskDto.java
dto/ReportDiscussionDto.java
dto/ReportRoundDto.java
dto/ReportExtrasDto.java
dto/ActionPlanDto.java
service/ReportService.java
service/impl/ReportServiceImpl.java
controller/ReportController.java
```

**修改**：
```
service/MergeService.java           + generateExecutiveSummary, generateActionPlan
service/impl/MergeServiceImpl.java  同上实现
```

## 5. 前端

### 5.1 新依赖

```
"html2pdf.js": "^0.10.2"
```

### 5.2 新路由

```ts
{ path: '/report/print/:taskId', component: () => import('@/views/ReportPrintView.vue'),
  meta: { layout: 'none', requiresAuth: true } }
```

`meta.layout = 'none'` 由 `PageShell` 识别，不显示 Sidebar，仅渲染报告内容。

### 5.3 视图结构

`src/views/ReportPrintView.vue`：

```
<ReportCover />
<ReportExecutiveSummary />
<ReportProblem />           <!-- 咨询问题 + 背景 + 约束 -->
<ReportRoundSummary v-for="round" />
<ReportGraphSnapshot />     <!-- 观点演化图（静态 PNG 或重建） -->
<ReportConvergenceChart />  <!-- 收敛图 -->
<ReportPlansComparison />   <!-- 候选方案对比 -->
<ReportBlindSpots />
<ReportActionPlan />        <!-- 7/30/90 天 -->
<ReportBack />              <!-- 封底 -->
```

每个子组件由一个新建文件承载：`src/components/report/` 目录。

### 5.4 交互

页面右上角浮动工具栏（仅屏幕可见，打印/PDF 时隐藏）：
- 「← 返回讨论」
- 「下载 PDF」按钮：调用 `html2pdf().from(document.querySelector('#report-root')).save('CareerMind-报告-' + task.title + '.pdf')`
- 「打印」按钮（fallback）：`window.print()`

### 5.5 打印样式

`src/styles/print.css`（在 ReportPrintView 挂载时 import）：

```css
@page {
  size: A4;
  margin: 20mm 15mm;
}

@media print {
  body { background: white; }
  .no-print { display: none !important; }
  .page-break { page-break-before: always; }
  /* 强制 svg/canvas 可打印 */
  .opinion-graph canvas { max-width: 100%; }
}
```

### 5.6 图表导出

- `OpinionGraph` 组件对外暴露 `exportAsPng(): Promise<string>`（Base64）
- `ConvergenceChart` 同样（ECharts 原生 `getDataURL`）
- ReportPrintView `onMounted` 后：等 2 × `requestAnimationFrame` → 对两张图截图 → 替换为 `<img src="base64">`（避免 html2pdf 直接抓 canvas 失败）

### 5.7 新增组件

```
src/views/ReportPrintView.vue
src/components/report/ReportCover.vue
src/components/report/ReportExecutiveSummary.vue
src/components/report/ReportProblem.vue
src/components/report/ReportRoundSummary.vue
src/components/report/ReportGraphSnapshot.vue
src/components/report/ReportConvergenceChart.vue
src/components/report/ReportPlansComparison.vue
src/components/report/ReportBlindSpots.vue
src/components/report/ReportActionPlan.vue
src/components/report/ReportBack.vue
src/api/report.ts
src/types/report.ts
src/utils/pdf-export.ts
```

### 5.8 触发入口

`ResultView.vue` 新增按钮「导出 PDF 报告」：

```vue
<BaseButton variant="primary" @click="openReport">导出 PDF 报告 📄</BaseButton>
```

```ts
const openReport = () => {
  const url = router.resolve({ name: 'ReportPrint', params: { taskId } }).href
  window.open(url, '_blank')
}
```

## 6. 报告样式规范

- 封面 A4 纵向，顶部 40mm 为深色块 + Logo（白），中央大字标题（咨询主题），副文本为日期与用户名，底部居中 `收敛率 88%` 大圆盘
- 内容页标题 18pt、正文 10.5pt、行高 1.5
- Agent 观点区：每个 Agent 一行，左侧 Avatar + Agent 色条，右侧内容
- 图表占单页（page-break-before）
- 配色完全复用 P1 tokens，打印时 @media print 忽略暗色模式，强制日间配色

## 7. 测试

- 后端 JUnit：
  - `ReportServiceImplTest` 聚合逻辑 + extras 缓存命中/旁路
  - `generateActionPlan` mock LLM 返回 JSON，测试解析与兜底
- E2E：
  - `report.spec.js` 访问 `/report/print/:id` 成功渲染 11 个子 section
  - 点"下载 PDF" 后 5 秒内有 `.pdf` 文件到达 Playwright download 目录
- 手动：
  - 实际 Chrome 打印预览检查分页
  - 用 A4 打印机印一份比赛汇报用

## 8. 风险与缓解

| 风险 | 缓解 |
|---|---|
| 中文字体被截断（PDF 内嵌问题） | 使用 WebFont `Noto Sans SC`，`html2pdf` 配置 `image.type: 'jpeg', image.quality: 0.96`；必要时为打印视图强制使用系统字体栈 |
| html2pdf 分页切割图表 | 图表容器加 `.page-break` + `break-inside: avoid` |
| canvas/SVG 转图失败 | `exportAsPng` 失败时回落显示"图表已略"；不阻塞 PDF 生成 |
| LLM 生成 ActionPlan 格式破坏 | 用 Jackson 宽容模式解析 + 正则兜底；失败时返回空数组前端显示骨架并允许重试 |

## 9. 完成定义

- [ ] `GET /api/reports/{taskId}` 返回完整结构
- [ ] extras 缓存在 Redis 命中
- [ ] 前端 `/report/print/:id` 渲染 11 个 section 无错
- [ ] 两张图（观点演化 + 收敛图）在报告中以静态图形式出现
- [ ] "下载 PDF" 生成的 PDF 在 macOS Preview / Adobe Reader / 微信打开正常
- [ ] 打印预览分页合理（封面、摘要、四轮、图表、方案、盲区、行动、封底 ≥ 8 页）
- [ ] E2E report.spec.js 通过
- [ ] 至少打印一份实物样本作为比赛汇报备份
