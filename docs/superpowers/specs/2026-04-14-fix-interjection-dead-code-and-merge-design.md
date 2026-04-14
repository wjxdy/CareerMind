# 修复插话死代码并合并决策树功能设计文档

> 日期：2026-04-14
> 范围：careermind-frontend + careermind-backend

---

## 一、设计目标

1. **接受简化版用户插话**：保留后端当前实现（用户插话保存为 USER 消息，注入下一轮 Prompt，轮次结束后清空标记），不再恢复被删除的"轮内回环专门回应"逻辑。
2. **清理前端死代码**：移除 `DiscussionPanel.vue` 中不会再被触发的 `interjection_stream_*` WebSocket 处理逻辑和 `interjectionPending` UI。
3. **合并功能到 main**：将 `feature/decision-tree-and-interjection` 分支完整合并到 `main`，包含已完成的决策树组件。

---

## 二、后端（保持不变）

- `addUserMessage()`：保存 `MessageType.USER` 消息，设置 `hasUserInterjection=true` 和 `interjectionContent`。
- `buildPrompt()`：若当前轮有插话标记，则在 Prompt 中注入用户插话内容。
- `markRoundCompleted()`：轮次结束后清空插话标记。

---

## 三、前端清理

### DiscussionPanel.vue

**删除内容：**
1. `interjectionPending` ref 及模板中的 `.interjection-banner` 提示条。
2. `interjectionStreamingMessage` / `interjectionStreamingContent` ref。
3. WebSocket `case 'interjection_stream_start'`、`'interjection_stream_chunk'`、`'interjection_stream_end'`、`'discussion_resumed'` 处理逻辑。
4. 模板中用于渲染插话回应流式消息的 `<!-- 插话回应流式消息 -->` 区块。
5. `.interjection-banner` 和 `.streaming-message` 相关 CSS（如 `.streaming-message` 已被主讨论流式复用则保留）。

**保留内容：**
- `sendMessage()` 正常发送用户消息并刷新讨论。
- `AgentMessage.vue` 中 `messageType === 'USER'` 的用户消息样式（绿色边框气泡）。

---

## 四、决策树（保持不变）

- `DecisionTree.vue` 组件完整保留。
- `TaskView.vue` 中对 `DecisionTree` 的引用和数据加载逻辑完整保留。

---

## 五、测试验证

1. 后端编译通过：`mvn clean compile -DskipTests`
2. 前端类型检查通过：`npx vue-tsc --noEmit`
3. 浏览器验证：
   - TaskView 显示决策树，包含 4 轮分支 + 候选方案叶子
   - 点击节点正确跳转
   - 讨论中发送用户消息，以 USER 样式显示
   - 下一轮 Agent 回复中隐含对用户插话的引用
   - 无"插话已发送，专家正在回应"的残留提示条

---

## 六、范围与边界

**本期必做：**
- 清理前端插话死代码
- 合并 feature 分支到 main

**本期不做：**
- 恢复轮内回环专门回应逻辑
- 修改决策树交互
