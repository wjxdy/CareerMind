# 修复插话死代码并合并决策树功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 清理 `feature/decision-tree-and-interjection` 分支上前端遗留的插话死代码，验证编译通过后将该分支合并到 `main`。

**Architecture:** 后端已采用简化版插话实现（保存 USER 消息并注入下一轮 Prompt），前端需移除不会再触发的 `interjection_stream_*` WebSocket 处理、`interjectionPending` UI 及关联响应式状态。决策树组件保持不变。

**Tech Stack:** Vue 3 + TypeScript, Spring Boot, Git

---

## 文件结构映射

| 文件 | 操作 | 职责 |
|---|---|---|
| `careermind-frontend/src/components/discussion/DiscussionPanel.vue` | 修改 | 移除插话死代码：模板中的插话流式消息和 pending 横幅、script 中的相关 ref 和 WebSocket case、未使用的 `Loading` import、`.interjection-banner` CSS |

---

### Task 1: 切换到 feature 分支

- [ ] **Step 1: 检出 feature 分支**

```bash
git checkout feature/decision-tree-and-interjection
```

Expected: 成功切换到 `feature/decision-tree-and-interjection`

---

### Task 2: 清理 DiscussionPanel.vue 模板中的插话死代码

**Files:**
- Modify: `careermind-frontend/src/components/discussion/DiscussionPanel.vue`

- [ ] **Step 1: 删除插话回应流式消息区块**

删除第 63–74 行：

```vue
        <!-- 插话回应流式消息 -->
        <div v-if="interjectionStreamingMessage" class="streaming-message">
          <div class="round-divider">
            <el-divider>
              <el-tag size="small" type="warning">回应用户插话</el-tag>
            </el-divider>
          </div>
          <AgentMessage
            :message="interjectionStreamingMessage"
            :is-streaming="true"
          />
        </div>
```

- [ ] **Step 2: 删除插话状态提示横幅**

删除第 78–82 行：

```vue
    <!-- 插话状态提示 -->
    <div v-if="interjectionPending" class="interjection-banner">
      <el-icon><Loading /></el-icon>
      <span>插话已发送，专家正在回应...</span>
    </div>
```

---

### Task 3: 清理 DiscussionPanel.vue 脚本中的插话死代码

**Files:**
- Modify: `careermind-frontend/src/components/discussion/DiscussionPanel.vue`

- [ ] **Step 1: 移除未使用的 `Loading` import**

将第 117 行：

```typescript
import { Loading } from '@element-plus/icons-vue'
```

删除。

- [ ] **Step 2: 移除插话相关的 ref 定义**

删除第 140–143 行：

```typescript
// 插话回应相关
const interjectionPending = ref(false)
const interjectionStreamingMessage = ref<Message | null>(null)
const interjectionStreamingContent = ref('')
```

- [ ] **Step 3: 移除 WebSocket 中插话相关的 case 处理**

删除第 248–282 行的四个 case：

```typescript
      case 'interjection_stream_start':
        interjectionStreamingMessage.value = {
          id: Date.now(),
          agentId: data.data.agentId,
          agentName: data.data.agentName,
          agentType: data.data.agentType,
          agentAvatar: data.data.agentAvatar,
          content: '',
          messageType: 'INTERJECTION',
          isFinal: false,
          createdAt: new Date().toISOString()
        }
        interjectionStreamingContent.value = ''
        scrollToBottom()
        break

      case 'interjection_stream_chunk':
        if (interjectionStreamingMessage.value) {
          interjectionStreamingContent.value += data.content
          interjectionStreamingMessage.value.content = interjectionStreamingContent.value
          scrollToBottom()
        }
        break

      case 'interjection_stream_end':
        interjectionStreamingMessage.value = null
        interjectionStreamingContent.value = ''
        loadDiscussion()
        scrollToBottom()
        break

      case 'discussion_resumed':
        interjectionPending.value = false
        loadDiscussion()
        break
```

注意：删除后 `case 'stream_end':` 和 `case 'message':` 之间应直接相邻。

---

### Task 4: 清理 DiscussionPanel.vue 样式中的插话死代码

**Files:**
- Modify: `careermind-frontend/src/components/discussion/DiscussionPanel.vue`

- [ ] **Step 1: 删除 `.interjection-banner` CSS**

删除第 500–510 行：

```css
.interjection-banner {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  background: #fffbeb;
  color: #92400e;
  font-size: 13px;
  border-top: 1px solid #fcd34d;
}
```

- [ ] **Step 2: Commit 前端清理**

```bash
git add careermind-frontend/src/components/discussion/DiscussionPanel.vue
git commit -m "refactor(frontend): remove dead interjection streaming UI code"
```

---

### Task 5: 编译与类型检查验证

- [ ] **Step 1: 后端编译**

```bash
cd careermind-backend && mvn clean compile -DskipTests
```

Expected: `BUILD SUCCESS`

- [ ] **Step 2: 前端类型检查**

```bash
cd careermind-frontend && npx vue-tsc --noEmit
```

Expected: 无 TypeScript 错误

- [ ] **Step 3: Commit 编译通过标记（如有配置文件变更则 commit，否则只记录）**

若编译无额外变更需要提交，跳过 commit。

---

### Task 6: 合并 feature 分支到 main

- [ ] **Step 1: 切回 main 并合并**

```bash
git checkout main
git merge feature/decision-tree-and-interjection --no-ff -m "feat: merge decision tree and simplified user interjection"
```

Expected: 合并成功，无冲突

- [ ] **Step 2: 推送 main 到远程**

```bash
git push origin main
```

Expected: 推送成功

- [ ] **Step 3: 推送更新后的 feature 分支（含清理 commit）**

```bash
git push origin feature/decision-tree-and-interjection
```

Expected: 推送成功

---

### Task 7: 更新项目进度文档

**Files:**
- Modify: `PROJECT_STATUS.md`

- [ ] **Step 1: 更新决策树和插话功能状态为已完成**

在 `PROJECT_STATUS.md` 的"前端功能进度"或"下一步工作计划"区域，添加或更新：

```markdown
- [x] 决策树可视化 ✅ 已完成 - TaskView 展示完整决策链路
- [x] 用户插话功能 ✅ 已完成 - 用户消息保存并注入下一轮 Prompt
```

并更新最后更新时间为 `2026-04-14`。

- [ ] **Step 2: Commit 进度更新**

```bash
git add PROJECT_STATUS.md
git commit -m "docs: update PROJECT_STATUS for decision tree and interjection merge"
```

---

## Self-Review

**1. Spec coverage：**
- 移除 `interjectionStreamingMessage` / `interjectionStreamingContent` / `interjectionPending` → Task 3 Step 2
- 移除模板中的插话流式消息和 pending 横幅 → Task 2
- 移除 WebSocket `interjection_stream_*` 和 `discussion_resumed` case → Task 3 Step 3
- 移除未使用的 `Loading` import → Task 3 Step 1
- 移除 `.interjection-banner` CSS → Task 4 Step 1
- 编译验证 → Task 5
- 合并到 main → Task 6
- 更新进度文档 → Task 7

**2. Placeholder scan：** 无 TBD、TODO，所有步骤含具体行号和代码片段。

**3. Type consistency：** 删除的是本地 ref 和 case 分支，不影响现有 `Message` 类型定义；`AgentMessage.vue` 中 `messageType === 'USER'` 样式保留不动。

---

## 执行方式选择

**Plan complete and saved to `docs/superpowers/plans/2026-04-14-fix-interjection-dead-code-and-merge.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints for review.

Which approach do you prefer?
