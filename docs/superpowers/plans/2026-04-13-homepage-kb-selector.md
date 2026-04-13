# 首页添加知识库选择器 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在首页输入区域添加一个可选的知识库选择器，使创建咨询时可以直接关联知识库。

**Architecture:** 在 `HomeView.vue` 的 `.input-box` 内部，紧邻 `input-actions` 上方插入 `el-select`。登录后加载知识库列表，用户选择后随 `taskApi.createTask` 提交 `kbId`。

**Tech Stack:** Vue 3, Element Plus, TypeScript

---

## File Structure

- **Modify:** `careermind-frontend/src/views/HomeView.vue`
  - 添加 `selectedKbId` 响应式变量
  - 添加知识库列表 `kbList` 及加载逻辑
  - 在输入框底部插入 `el-select` 选择器
  - `handleSubmit` 时把 `selectedKbId` 传入 `taskData.kbId`

---

### Task 1: 添加状态与数据加载逻辑

**Files:**
- Modify: `careermind-frontend/src/views/HomeView.vue`

- [ ] **Step 1: 引入 `kbApi` 和 `KnowledgeBase` 类型**

在 `<script setup>` 的 import 区域添加：

```typescript
import { kbApi } from '@/api/kb'
import type { KnowledgeBase } from '@/types/kb'
```

- [ ] **Step 2: 声明响应式变量**

在 `selectedAgents` 等变量附近添加：

```typescript
const kbList = ref<KnowledgeBase[]>([])
const selectedKbId = ref<number | undefined>(undefined)
```

- [ ] **Step 3: 在 `onMounted` 中加载知识库列表**

在 `onMounted` 内部追加（放在 `agentStore.fetchPresetAgents()` 之后）：

```typescript
  // 加载知识库列表
  if (userStore.isLoggedIn) {
    kbApi.getKbs({ page: 1, size: 100 }).then(res => {
      kbList.value = res.items
    }).catch(() => {
      // 静默失败，不影响首页主流程
    })
  }
```

- [ ] **Step 4: 提交变更**

```bash
git add careermind-frontend/src/views/HomeView.vue
git commit -m "feat(home): add kb list loading for homepage selector"
```

---

### Task 2: 添加知识库选择器 UI

**Files:**
- Modify: `careermind-frontend/src/views/HomeView.vue`

- [ ] **Step 1: 在模板中插入选择器**

在 `.input-actions` div 之前，添加以下代码：

```vue
            <!-- 知识库选择 -->
            <div v-if="userStore.isLoggedIn" class="kb-selector">
              <el-select
                v-model="selectedKbId"
                placeholder="关联知识库（可选）"
                clearable
                style="width: 220px"
                size="default"
              >
                <el-option
                  v-for="kb in kbList"
                  :key="kb.id"
                  :label="kb.name"
                  :value="kb.id"
                />
              </el-select>
            </div>
```

- [ ] **Step 2: 添加基础样式**

在 `<style scoped>` 底部添加：

```css
.kb-selector {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}
```

- [ ] **Step 3: 提交变更**

```bash
git add careermind-frontend/src/views/HomeView.vue
git commit -m "feat(home): add kb selector UI in homepage"
```

---

### Task 3: 提交时携带 kbId

**Files:**
- Modify: `careermind-frontend/src/views/HomeView.vue`

- [ ] **Step 1: 修改 `handleSubmit` 中的 `taskData`**

找到 `taskData` 对象，在 `agentIds` 之后添加 `kbId`：

```typescript
    const taskData = {
      title: inputText.value.slice(0, 6),
      background: userBio,
      goal: inputText.value,
      constraints: '',
      agentIds: selectedAgents.value,
      kbId: selectedKbId.value
    }
```

- [ ] **Step 2: 提交变更**

```bash
git add careermind-frontend/src/views/HomeView.vue
git commit -m "feat(home): pass selected kbId when creating task from homepage"
```

---

### Task 4: 本地验证

**Files:**
- 无需修改文件

- [ ] **Step 1: 启动前端开发服务器并访问首页**

确认 Vite dev server 已运行：`lsof -i :5173`
如未运行，在前端目录执行 `npm run dev`。

- [ ] **Step 2: 登录后检查首页**

打开 http://localhost:5173/，登录后观察输入框下方是否出现"关联知识库（可选）"下拉框。

- [ ] **Step 3: 功能测试**

1. 不选知识库，输入问题，点击"开始咨询" → 咨询创建成功
2. 选择一个知识库，输入问题，点击"开始咨询" → 咨询创建成功，且后端日志/RAG 查询能体现关联了知识库

---

## Self-Review

1. **Spec coverage:** 所有设计点（可选、登录后显示、加载列表、提交 kbId）均已覆盖。
2. **Placeholder scan:** 无 TBD/TODO/模糊描述，每步均有具体代码。
3. **Type consistency:** `kbId` 为 `number | undefined`，与 `CreateTaskData` 中的 `kbId?: number` 一致。
