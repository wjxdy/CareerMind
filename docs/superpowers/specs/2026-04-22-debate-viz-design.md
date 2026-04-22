# 辩论可视化 设计文档（P2）

- 创建日期：2026-04-22
- 依赖：P1（品牌重做）已完成；使用其设计 token 与 AgentAvatar

## 1. 目标

在不改变现有讨论流程的前提下，为讨论产出**三张可视化图**，使评委能在 10 秒内"看见"AI 辩论的结构与收敛过程。

具体产物：
1. **Discussion 页小地图温度条**（实时）
2. **观点演化图**（力导向网络图，Task 详情页）— PPT 海报级素材
3. **决策收敛图**（轮次-分歧度折线，Result 页）

## 2. 非目标

- 不做语音/多模态
- 不对接真实数据源
- 不实现交互式"拖拽节点重算"——节点布局由力导向自动生成后允许用户拖动但不反馈到模型
- 不做历史对比（跨 task 的观点对比）

## 3. 数据模型

### 3.1 需要的数据维度

| 维度 | 含义 | 来源 |
|---|---|---|
| 节点 Node | (agentId, roundNumber) | 现有 `Message.agentId` + `Round.roundNumber` |
| 边 Edge | messageA → messageB 的关系 | 需要"关系类型" |
| 关系类型 | support / challenge / revise | 新增 |
| Agent 信心值 | 每轮每 Agent 的 confidence [0,1] | 新增 |
| 轮次分歧度 | 一轮整体的 divergence [0,1] | 新增（派生） |

### 3.2 现有字段盘点

现有 `Message` 已有：
- `agentId`、`roundId` (通过 Round 反查 roundNumber)
- `replyToMessageId`（指向被质疑/支持的消息）
- `messageType` (AGENT / USER / INTERJECTION)

**缺少**：边的"类型"（support vs challenge vs revise）与"信心值"。

### 3.3 数据库变更

对 `messages` 表增加 2 列（Flyway migration）：

```sql
ALTER TABLE messages
  ADD COLUMN edge_type VARCHAR(16) NULL COMMENT 'SUPPORT/CHALLENGE/REVISE/NONE',
  ADD COLUMN confidence DECIMAL(3,2) NULL COMMENT '0.00-1.00';
```

- `edge_type`：仅在 `replyToMessageId` 非空时有意义；Round 2 默认 CHALLENGE，Round 3 默认 REVISE
- `confidence`：所有 Agent 消息都有值；从消息正文尾部正则提取 Agent 自报的 `[confidence: 0.82]` 标签，找不到时回落到默认 0.6

对 `rounds` 表增加 1 列：

```sql
ALTER TABLE rounds
  ADD COLUMN divergence DECIMAL(3,2) NULL COMMENT '整轮分歧度 0.00-1.00';
```

此列在每轮结束时由后端计算写入。

## 4. 后端变更

### 4.1 Prompt 修改

`DiscussionEngineImpl` 在组装各轮 system prompt 时追加：

> 请在回复的最后一行单独输出 `[confidence: X.XX]`，其中 X.XX 是 0.00 到 1.00 的小数，表示你对本轮观点的信心。

Round 2 Prompt 同时要求：

> 如果你明确在质疑某位同事的观点，请在开头引用："> 我对 @某某 的观点有异议：…"。

**替代路径（若 Agent 偶发漏输出信心标签）**：由后端消息保存时用默认值兜底，不强依赖。

### 4.2 服务层

新增 `DiscussionGraphService`：
- `GraphData buildGraph(Long taskId)` 聚合节点/边/信心/分歧度
- `BigDecimal computeDivergence(List<Message> roundMessages)` — 基于信心值方差、质疑密度的归一化打分

新增方法在 `MessageServiceImpl` 或 `DiscussionEngineImpl`：
- `parseConfidence(String content)` — 正则 `\[confidence:\s*(\d\.\d{1,2})\]`；返回 Optional
- `parseEdgeType(Round round, String content)` — 按轮次与引用关键词判定

### 4.3 API

`GET /api/discussions/tasks/{taskId}/graph` → `GraphResponse`

```json
{
  "nodes": [
    { "id": "a1-r1", "agentId": 1, "agentType": "INDUSTRY_ANALYST",
      "agentName": "行业分析师", "roundNumber": 1,
      "messageId": 123, "snippet": "我认为该行业正在转型…",
      "confidence": 0.78, "wordCount": 420 }
  ],
  "edges": [
    { "id": "e1", "from": "a2-r2", "to": "a1-r1", "type": "CHALLENGE" }
  ],
  "rounds": [
    { "roundNumber": 1, "divergence": 0.65 },
    { "roundNumber": 2, "divergence": 0.58 },
    { "roundNumber": 3, "divergence": 0.32 },
    { "roundNumber": 4, "divergence": 0.12 }
  ],
  "finalConvergence": 0.88
}
```

- `id` 节点键采用 `aN-rM`
- 边只在 Round 2/3 有，Round 1/4 节点是孤岛
- Round 4 节点表示"最终陈述"，用于力导向图中作为终态高亮
- `finalConvergence = 1 - divergence(round4)`

### 4.4 实时推送

WebSocket 在 `stream_end` 之后额外下发 `graph_delta`：

```json
{ "type": "graph_delta", "roundNumber": 2, "divergence": 0.58 }
```

前端据此在讨论页小地图实时更新温度条。

### 4.5 计算策略

**信心值 confidence（每消息）**：
1. 从消息尾部 `[confidence: 0.XX]` 正则提取
2. 找不到 → 按 Agent type + roundNumber 查默认表（独立诊断 0.7，质疑 0.6，修正 0.75，最终 0.85）

**分歧度 divergence（每轮）**：
- `v = stddev(confidences) * 0.5 + challengeDensity * 0.5`
  - `stddev` 按 [0,1] 归一化到 [0,0.5]（理论 max 0.5）
  - `challengeDensity = challengeCount / totalMessages` 归一化到 [0,1]
- 最终 `divergence = min(1.0, max(0.0, v))`

写入 `rounds.divergence`，每轮结束时计算。

## 5. 前端变更

### 5.1 新依赖

```
"vis-network": "^9.1.9",
"vis-data":    "^7.1.9"
```

### 5.2 组件

| 组件 | 位置 | 职责 |
|---|---|---|
| `ThermoBar` | `src/components/discussion/ThermoBar.vue` | 温度条 |
| `OpinionGraph` | `src/components/viz/OpinionGraph.vue` | vis-network 力导向图 |
| `ConvergenceChart` | `src/components/viz/ConvergenceChart.vue` | ECharts 折线图 |
| `VizLegend` | `src/components/viz/VizLegend.vue` | 图例（Agent 色块 + 边类型） |

#### 5.2.1 ThermoBar
- 横条 240×6，从左（红 `#EF4444`）到右（绿 `#10B981`）线性渐变
- 一根白色小指针根据 `1 - latestDivergence` 百分比定位
- 轮次结束时自动滑动 400ms；并冒出 "+12% 收敛" 小浮字 1.2s

#### 5.2.2 OpinionGraph
- vis-network `Network` 实例，节点 20（5×4）
- 节点：
  - `shape: "dot"`，大小 = 10 + wordCount/50 范围 [14, 36]
  - 颜色 = Agent 主色（背景 + 描边同色）
  - label 显示 Agent 名短写（"行业"/"能力"/"风险"/"机会"/"价值"）+ 小字 `R{roundNumber}`
  - 悬停 tooltip：完整 snippet（前 100 字）
- 边：
  - `CHALLENGE` 红虚线 1.5px 箭头；`SUPPORT` 绿实线 1px；`REVISE` 蓝实线 1px
- 布局：
  - `solver: "forceAtlas2Based"`，默认力导向
  - 按轮次分层的**可选模式**：切换后 y 轴按 roundNumber，x 轴均匀分布
  - 有"时间线回放"按钮：依次高亮 Round 1→2→3→4 的节点与边（每轮 1.5s 驻留）
- 物理稳定后自动关闭 physics（防止性能问题）

#### 5.2.3 ConvergenceChart
- ECharts `line` 系列
- X：Round 1 / 2 / 3 / 4
- Y 左轴：分歧度（降序感更好：反转为"共识度"= 1 - divergence）
- 叠加 5 条细线：每个 Agent 的 confidence 随轮次变化
- 主线（共识度）粗 3px，Agent 线 1.5px
- 末尾端 Round 4 位置标 `final: 88%` 大字

### 5.3 挂点

- **讨论页**：底部圆桌区下方添加 `ThermoBar`（贴在底边或浮在圆桌中心下方），宽 240px
- **TaskView**：在现有决策树下方新增"观点演化"Tab，默认显示 `OpinionGraph`
- **ResultView**：在顶部横幅与方案卡片之间插入 `ConvergenceChart`

## 6. 文件变更

**后端新增**：
```
domain/DiscussionGraphEdge.java  (可选, 若直接用 Message 字段无需)
dto/GraphResponse.java
dto/GraphNodeDto.java
dto/GraphEdgeDto.java
dto/GraphRoundStatDto.java
service/DiscussionGraphService.java
service/impl/DiscussionGraphServiceImpl.java
controller/DiscussionGraphController.java
resources/db/migration/V2__add_edge_confidence.sql
```

**后端修改**：
```
service/impl/DiscussionEngineImpl.java   追加 confidence prompt、边类型推断、每轮结束计算 divergence
domain/Message.java                       增 edgeType + confidence 字段
domain/Round.java                         增 divergence 字段
websocket/DiscussionWebSocketHandler.java 新增 sendGraphDelta
```

**前端新增**：
```
src/api/graph.ts
src/types/graph.ts
src/components/discussion/ThermoBar.vue
src/components/viz/OpinionGraph.vue
src/components/viz/ConvergenceChart.vue
src/components/viz/VizLegend.vue
```

**前端修改**：
```
src/components/discussion/DiscussionPanel.vue  监听 graph_delta + 挂载 ThermoBar
src/components/discussion/RoundtableStage.vue  容器布局
src/views/TaskView.vue                         新增 OpinionGraph Tab
src/views/ResultView.vue                       挂 ConvergenceChart
```

## 7. 交互细节

- **演化图节点拖拽**：用户可手动拖拽微调布局；`Ctrl+S` 保存布局到 localStorage
- **节点点击**：弹 Drawer 显示完整消息与质疑关系
- **时间线回放**：顶部按钮"▶ 回放辩论"，点击后 vis-network 按轮次逐帧高亮
- **图例始终可见**：右上角 VizLegend，可折叠

## 8. 测试

- 后端 JUnit：
  - `DiscussionGraphServiceTest` 覆盖 5 Agent × 4 轮 mock 数据的节点/边/分歧度计算
  - `parseConfidence` 各种边界（有/无/异常格式/多个）
- Playwright E2E：
  - `viz-task.spec.js` 访问 TaskView 能看到 `.opinion-graph canvas`
  - 完成 4 轮讨论后 Result 页能看到 `.convergence-chart` SVG
  - ThermoBar 指针位置随 graph_delta 改变（通过注入模拟 WebSocket 消息断言）

## 9. 风险与缓解

| 风险 | 缓解 |
|---|---|
| Agent 不按 prompt 输出信心标签 | 默认兜底 + monitor 日志；必要时 Round 4 末尾再走一次"自评"小调用 |
| vis-network gz 大 | Tree-shake 子模块；接受 ~80KB gz 代价 |
| Challenge 类型误判 | Round 2 默认 CHALLENGE，Round 3 REVISE，文本"同意/补充"关键词反转 |
| divergence 公式与主观感受不符 | 提供一个"调参接口"（配置文件里 weights），比赛前人工校准 |

## 10. 完成定义

- [ ] Flyway migration V2 成功应用；字段默认值迁移合理
- [ ] 跑完一次完整 4 轮讨论后，数据库 messages 表 60-80% 消息有 confidence
- [ ] `GET /api/discussions/tasks/{id}/graph` 返回 20 节点、≥8 边、4 轮统计
- [ ] TaskView 能渲染出力导向图且节点按 Agent 色区分
- [ ] ResultView 折线图在 Round 4 显示 finalConvergence
- [ ] ThermoBar 温度条在讨论过程中可见流动
- [ ] E2E viz-task.spec.js 通过
- [ ] 对比赛 PPT 输出一张 1920×1080 的观点演化图高清 PNG
