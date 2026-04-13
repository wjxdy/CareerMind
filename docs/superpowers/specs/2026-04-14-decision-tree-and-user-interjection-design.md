# 决策树可视化与用户插话功能设计文档

> 日期：2026-04-14
> 范围：careermind-frontend + careermind-backend

---

## 一、设计目标

1. **决策树可视化**：在任务详情页（TaskView）展示从职业困惑到 4 轮专家讨论再到最终候选方案的完整决策链路，作为导航入口。
2. **用户插话**：允许用户在讨论进行中随时发言；系统以「轮内回环」方式让 Agent 回应插话，随后无缝恢复主讨论，避免认知断层。

---

## 二、决策树可视化

### 2.1 交互与布局

- **常驻缩略图**：在 TaskView 的"参与专家"区块下方显示一个高度约 260px 的横向 ECharts Tree 缩略图。
- **展开全屏**：缩略图底部居中放置"展开查看完整决策树"按钮，点击后以 Element Plus `el-dialog` 全屏展示可缩放、可拖拽的完整树图。
- **点击导航**：
  - 根节点/轮次节点 → 跳转到 `/discussions/${taskId}`
  - 方案叶子节点 → 跳转到 `/results/${taskId}`
- **颜色编码**：复用 `agentStore.getAgentColor()`，不同专家分支使用其专属颜色；轮次节点使用统一主题色；方案节点使用成功绿色。

### 2.2 数据映射

ECharts Tree 的数据结构如下：

```
根节点: task.goal (职业困惑)
  ├─ 第1轮 - 独立诊断
  │   ├─ Agent A 摘要
  │   ├─ Agent B 摘要
  │   └─ ...
  ├─ 第2轮 - 质疑挑战
  │   ├─ Agent A 摘要
  │   └─ ...
  ├─ 第3轮 - 修正完善
  │   └─ ...
  ├─ 第4轮 - 最终陈述
  │   └─ ...
  └─ [候选方案] (仅当 mergeResult 存在)
      ├─ 方案1: title
      ├─ 方案2: title
      └─ 方案3: title
```

- **摘要生成规则**：对每条 Agent 消息取前 18 个字符 + "..."，避免节点文字过长压垮布局。
- **空状态**：若讨论尚未开始，树图区域显示占位文案："讨论启动后将生成决策树"，并引导用户点击"查看讨论"。

### 2.3 组件拆分

- 新建 `DecisionTree.vue`（`src/components/task/DecisionTree.vue`）
  - Props: `task: Task`, `discussion: Discussion | null`, `mergeResult: MergeResult | null`
  - 内部初始化 ECharts 实例，监听窗口 resize，提供 `expand` / `collapse` 状态。
- TaskView 修改：
  - 引入 `DecisionTree`
  - `onMounted` 时并行加载 `discussionApi.getDiscussion(taskId)` 与 `mergeApi.getMergeResult(taskId)`

---

## 三、用户插话（轮内回环方案）

### 3.1 核心思想

用户插话不会粗暴截断正在流式输出的 Agent，而是：
1. 暂停主讨论线程；
2. 启动一个**独立的"插话回应线程"**，让所有 Agent 针对用户插话做简短回应；
3. 回应完成后自动恢复主讨论。

这样所有 Agent 都"看到"了用户插话，已发言的 Agent 通过简短回应弥补认知，未发言的 Agent 在后续正常发言时 prompt 里也会注入插话内容。

### 3.2 完整流程

```
用户发送插话
    ↓
discussionApi.sendMessage() 保存用户消息到当前 round
    ↓
discussionApi.pauseDiscussion() 暂停主讨论 (isPaused = true)
    ↓
后端启动 InterjectionHandler（独立 CompletableFuture）
    ↓
对每个 Agent：
  - 构造短 prompt，要求针对用户插话做 1-2 句话回应
  - 流式输出到前端（复用现有 WebSocket 协议，新增 type = interjection_stream_start/chunk/end）
  - 消息以 `messageType = INTERJECTION` 标记存入当前 round
    ↓
全部 Agent 回应完成后
    ↓
后端自动调用 resumeDiscussion() (isPaused = false)
    ↓
主讨论线程从 pause 点继续执行
```

### 3.3 Prompt 设计（插话回应）

```text
=== 用户插话 ===
用户在讨论过程中发表了以下观点或提问：
"{userContent}"

=== 你的任务 ===
你是 {agentName}。请基于你的角色定位，用 1-2 句话简要回应用户的插话。
要求：
1. 直接称呼"用户"或"你"；
2. 观点明确，不展开长篇论述；
3. 如果用户的插话与你之前的观点冲突，简要说明你的立场。
```

### 3.4 Prompt 设计（未发言 Agent 的后续正常发言）

在 `buildPrompt()` 中，对当前轮次（即被插话的轮次）增加一段：

```text
=== 用户插话 ===
用户在讨论中补充了以下内容，请在发言中适当参考：
"{userContent}"
```

这段内容只注入到**用户插话之后才发言的 Agent** 的 prompt 中；已发言并已做过插话回应的 Agent 不再重复注入（避免冗余）。

### 3.5 后端状态管理

- `Discussion` 实体新增字段 `hasUserInterjection: boolean`（标记当前轮是否有插话）。
- `Discussion` 实体新增字段 `interjectionContent: String`（记录插话内容，供后续 prompt 注入使用）。
- `Message` 实体新增字段 `messageType`（枚举：`AGENT` / `USER` / `INTERJECTION`，默认 `AGENT`），用于区分普通 Agent 发言、用户插话和插话回应。
- `MessageDto` 同步新增 `messageType` 字段。

### 3.6 WebSocket 消息扩展

前端需要区分"主讨论流式"与"插话回应流式"，以便 UI 做不同渲染（例如插话回应用更紧凑的卡片）。新增消息类型：

- `interjection_stream_start`
- `interjection_stream_chunk`
- `interjection_stream_end`

字段结构与现有 `stream_start` / `stream_chunk` / `stream_end` 保持一致。

### 3.7 前端 UI 变化

- `DiscussionPanel.vue`：
  - 用户消息使用特殊样式（左侧用户头像 + 淡蓝色背景气泡），与 Agent 消息区分。
  - 收到 `interjection_stream_*` 时，以紧凑行内卡片形式渲染在消息流中，顶部带小标签"回应用户插话"。
  - 输入框发送成功后，自动清空并短暂显示"插话已发送，专家正在回应..."的提示条；收到 `interjection_stream_end` 且所有 Agent 回应完毕后提示条消失。
- `AgentMessage.vue`：
  - 支持渲染 `agentType === 'USER'` 的消息样式。
  - 支持 `isInterjection` 属性，渲染紧凑版式。

---

## 四、文件改动清单

### 前端

- `src/components/task/DecisionTree.vue` — 新建决策树组件
- `src/views/TaskView.vue` — 引入 DecisionTree，并行加载 discussion / mergeResult
- `src/components/discussion/DiscussionPanel.vue` — 插话流程控制、WebSocket 新类型处理
- `src/components/discussion/AgentMessage.vue` — 用户消息与插话回应样式
- `src/types/index.ts` — 如有需要，扩展 Message 相关类型（可选）

### 后端

- `com.careermind.domain.Discussion` — 新增 `hasUserInterjection`、`interjectionContent`
- `com.careermind.domain.Message` — 新增 `messageType` 字段
- `com.careermind.dto.DiscussionDto` — 同步新增字段
- `com.careermind.dto.MessageDto` — 同步新增 `messageType`
- `com.careermind.service.DiscussionEngineImpl` —
  - `addUserMessage()`：保存消息后触发 pause + 启动插话线程
  - 新增 `handleUserInterjection()`：调度所有 Agent 做简短回应
  - `buildPrompt()`：对未发言 Agent 注入插话内容
  - `runAgentDiscussion()`：pause/resume 机制保持现有逻辑即可
- `com.careermind.websocket.DiscussionWebSocketHandler` — 新增插话流式推送方法
- `application.yml` 或 flyway 脚本 — 为 Discussion / Message 表新增对应列（如使用 JPA `ddl-auto` 则无需手动脚本）

---

## 五、测试策略

1. **决策树**：
   - 讨论未开始时显示占位文案；
   - 讨论完成后树图包含 4 轮分支 + 方案叶子；
   - 点击节点正确跳转路由。
2. **用户插话**：
   - 讨论中发送插话，Discussion 状态变为 paused；
   - 插话回应消息以 `INTERJECTION` 样式出现在消息流；
   - 全部回应完成后状态自动恢复为未暂停；
   - 未发言 Agent 的后续消息内容中包含对用户插话的引用。

---

## 六、范围与边界

**本期必做**：
- 决策树组件（缩略图 + 弹窗全屏）
- 轮内回环插话（暂停 → 插话回应 → 自动恢复）

**本期不做**：
- 决策树节点与讨论消息的精确滚动定位（高亮定位放在后续迭代）
- 结果页的方案对比表
- 讨论历史的时间轴视图
