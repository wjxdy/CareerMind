# 决策树可视化与用户插话功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 TaskView 增加 ECharts 决策树导航，并在讨论中支持用户插话后由所有 Agent 简短回应，随后自动恢复讨论。

**Architecture:** 前端新增 DecisionTree 组件聚合 task/discussion/mergeResult 数据渲染 ECharts Tree；后端扩展 Discussion 和 Message 实体记录插话状态，DiscussionEngineImpl 新增独立的插话回应异步线程，通过扩展的 WebSocket 协议推送流式内容到前端。

**Tech Stack:** Vue 3 + TypeScript + vue-echarts, Spring Boot + JPA + WebSocket

---

## 文件结构映射

| 文件 | 操作 | 职责 |
|---|---|---|
| `careermind-backend/src/main/java/com/careermind/enums/MessageType.java` | 新建 | 消息类型枚举：AGENT / USER / INTERJECTION |
| `careermind-backend/src/main/java/com/careermind/domain/Message.java` | 修改 | 新增 `messageType` 字段 |
| `careermind-backend/src/main/java/com/careermind/domain/Discussion.java` | 修改 | 新增 `hasUserInterjection`、`interjectionContent` |
| `careermind-backend/src/main/java/com/careermind/dto/MessageDto.java` | 修改 | 同步新增 `messageType` |
| `careermind-backend/src/main/java/com/careermind/dto/DiscussionDto.java` | 修改 | 同步新增两个字段 |
| `careermind-backend/src/main/java/com/careermind/repository/MessageRepository.java` | 修改 | 新增按类型查询方法 |
| `careermind-backend/src/main/java/com/careermind/websocket/DiscussionWebSocketHandler.java` | 修改 | 新增插话流式推送 + discussion_resumed 事件 |
| `careermind-backend/src/main/java/com/careermind/service/impl/DiscussionEngineImpl.java` | 修改 | 插话调度、prompt 注入、自动恢复 |
| `careermind-frontend/src/types/index.ts` | 修改 | Message 类型扩展 `messageType` |
| `careermind-frontend/src/components/discussion/AgentMessage.vue` | 修改 | 用户消息与插话回应的样式区分 |
| `careermind-frontend/src/components/discussion/DiscussionPanel.vue` | 修改 | 插话流程 UI、WebSocket 新类型处理 |
| `careermind-frontend/src/components/task/DecisionTree.vue` | 新建 | ECharts 决策树组件 |
| `careermind-frontend/src/views/TaskView.vue` | 修改 | 引入 DecisionTree、并行加载 discussion/mergeResult |

---

### Task 1: 后端 - 创建 MessageType 枚举

**Files:**
- Create: `careermind-backend/src/main/java/com/careermind/enums/MessageType.java`

- [ ] **Step 1: 新建枚举文件**

```java
package com.careermind.enums;

public enum MessageType {
    AGENT,       // 普通Agent发言
    USER,        // 用户插话
    INTERJECTION // Agent对用户插话的回应
}
```

- [ ] **Step 2: Commit**

```bash
git add careermind-backend/src/main/java/com/careermind/enums/MessageType.java
git commit -m "feat(backend): add MessageType enum"
```

---

### Task 2: 后端 - Message 实体与 DTO 增加 messageType

**Files:**
- Modify: `careermind-backend/src/main/java/com/careermind/domain/Message.java`
- Modify: `careermind-backend/src/main/java/com/careermind/dto/MessageDto.java`

- [ ] **Step 1: Message.java 新增字段**

在 `isFinal` 字段上方插入：

```java
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    @Builder.Default
    private MessageType messageType = MessageType.AGENT;
```

同时确保文件顶部导入 `com.careermind.enums.MessageType`。

- [ ] **Step 2: MessageDto.java 新增字段**

在 `isFinal` 上方插入：

```java
    private String messageType;
```

- [ ] **Step 3: Commit**

```bash
git add careermind-backend/src/main/java/com/careermind/domain/Message.java careermind-backend/src/main/java/com/careermind/dto/MessageDto.java
git commit -m "feat(backend): add messageType to Message and MessageDto"
```

---

### Task 3: 后端 - Discussion 实体与 DTO 增加插话字段

**Files:**
- Modify: `careermind-backend/src/main/java/com/careermind/domain/Discussion.java`
- Modify: `careermind-backend/src/main/java/com/careermind/dto/DiscussionDto.java`

- [ ] **Step 1: Discussion.java 新增字段**

在 `isPaused` 字段下方、`rounds` 字段上方插入：

```java
    @Column(name = "has_user_interjection")
    @Builder.Default
    private Boolean hasUserInterjection = false;

    @Column(name = "interjection_content", length = 2000)
    private String interjectionContent;
```

- [ ] **Step 2: DiscussionDto.java 新增字段**

在 `isPaused` 下方插入：

```java
    private Boolean hasUserInterjection;
    private String interjectionContent;
```

- [ ] **Step 3: Commit**

```bash
git add careermind-backend/src/main/java/com/careermind/domain/Discussion.java careermind-backend/src/main/java/com/careermind/dto/DiscussionDto.java
git commit -m "feat(backend): add interjection fields to Discussion and DiscussionDto"
```

---

### Task 4: 后端 - MessageRepository 扩展查询方法

**Files:**
- Modify: `careermind-backend/src/main/java/com/careermind/repository/MessageRepository.java`

- [ ] **Step 1: 新增方法签名**

在接口内现有方法下方添加：

```java
    List<Message> findByRoundIdAndAgentIdAndMessageType(Long roundId, Long agentId, com.careermind.enums.MessageType messageType);
```

- [ ] **Step 2: Commit**

```bash
git add careermind-backend/src/main/java/com/careermind/repository/MessageRepository.java
git commit -m "feat(backend): add findByRoundIdAndAgentIdAndMessageType to MessageRepository"
```

---

### Task 5: 后端 - WebSocket Handler 扩展插话事件

**Files:**
- Modify: `careermind-backend/src/main/java/com/careermind/websocket/DiscussionWebSocketHandler.java`

- [ ] **Step 1: 在 sendStreamEnd 方法后插入三个新方法**

```java
    public void sendInterjectionStreamStart(Long taskId, Long agentId, String agentName, String agentType, String agentAvatar) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "interjection_stream_start");
                ObjectNode data = message.putObject("data");
                data.put("agentId", agentId);
                data.put("agentName", agentName);
                data.put("agentType", agentType);
                data.put("agentAvatar", agentAvatar);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("发送插话流式开始事件失败", e);
            }
        }
    }

    public void sendInterjectionStreamChunk(Long taskId, String content) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "interjection_stream_chunk");
                message.put("content", content);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("发送插话流式片段失败", e);
            }
        }
    }

    public void sendInterjectionStreamEnd(Long taskId, Long messageId) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "interjection_stream_end");
                message.put("messageId", messageId);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("发送插话流式结束事件失败", e);
            }
        }
    }

    public void sendDiscussionResumed(Long taskId) {
        WebSocketSession session = taskSessions.get(taskId);
        if (session != null && session.isOpen()) {
            try {
                ObjectNode message = objectMapper.createObjectNode();
                message.put("type", "discussion_resumed");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(message)));
            } catch (IOException e) {
                log.error("发送讨论恢复事件失败", e);
            }
        }
    }
```

- [ ] **Step 2: Commit**

```bash
git add careermind-backend/src/main/java/com/careermind/websocket/DiscussionWebSocketHandler.java
git commit -m "feat(backend): add interjection and resumed WebSocket events"
```

---

### Task 6: 后端 - DiscussionEngineImpl 实现插话调度

**Files:**
- Modify: `careermind-backend/src/main/java/com/careermind/service/impl/DiscussionEngineImpl.java`

- [ ] **Step 1: 修改 addUserMessage 方法**

将现有 `addUserMessage` 整体替换为：

```java
    @Override
    public DiscussionDto addUserMessage(Long taskId, String content) {
        DiscussionDto dto = transactionTemplate.execute(status -> {
            Discussion discussion = discussionRepository.findByTaskId(taskId)
                    .orElseThrow(() -> new RuntimeException("讨论不存在"));

            Round round = roundRepository.findByDiscussionIdAndRoundNumber(
                    discussion.getId(), discussion.getCurrentRound())
                    .orElseThrow(() -> new RuntimeException("当前轮次不存在"));

            Message userMessage = Message.builder()
                    .round(round)
                    .agent(null)
                    .content("【用户提问】" + content)
                    .messageType(MessageType.USER)
                    .isFinal(false)
                    .build();
            messageRepository.save(userMessage);

            discussion.setHasUserInterjection(true);
            discussion.setInterjectionContent(content);
            discussion.setIsPaused(true);
            discussionRepository.save(discussion);

            webSocketHandler.sendMessageToTask(taskId, MessageDto.builder()
                    .id(userMessage.getId())
                    .agentId(-1L)
                    .agentName("用户")
                    .agentType("USER")
                    .agentAvatar(null)
                    .content("【用户提问】" + content)
                    .messageType(MessageType.USER.name())
                    .isFinal(false)
                    .createdAt(userMessage.getCreatedAt())
                    .build());

            log.info("用户消息已添加到讨论并触发暂停: Task ID={}, Round={}, Content={}",
                    taskId, round.getRoundNumber(), content.substring(0, Math.min(50, content.length())));

            return convertToDto(discussion);
        });

        Discussion discussion = discussionRepository.findById(dto.getId()).orElseThrow();
        handleUserInterjection(discussion, content);

        return dto;
    }
```

确保文件顶部导入 `com.careermind.enums.MessageType`。

- [ ] **Step 2: 新增 buildInterjectionPrompt 方法**

在 `buildPrompt` 方法上方插入：

```java
    private String buildInterjectionPrompt(Agent agent, String userContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("=== 用户插话 ===\n");
        prompt.append("用户在讨论过程中发表了以下观点或提问：\n");
        prompt.append("\"").append(userContent).append("\"\n\n");
        prompt.append("=== 你的任务 ===\n");
        prompt.append("你是").append(agent.getName()).append("。请基于你的角色定位，用 1-2 句话简要回应用户的插话。\n");
        prompt.append("要求：\n");
        prompt.append("1. 直接称呼\"用户\"或\"你\"；\n");
        prompt.append("2. 观点明确，不展开长篇论述；\n");
        prompt.append("3. 如果用户的插话与你之前的观点冲突，简要说明你的立场。\n");
        return prompt.toString();
    }
```

- [ ] **Step 3: 新增 processInterjectionMessageStream 方法**

在 `processAgentMessageStream` 方法下方插入：

```java
    private void processInterjectionMessageStream(Agent agent, Long discussionId, Long taskId, int currentRound, Round round, String userContent) {
        Discussion discussion = discussionRepository.findById(discussionId).orElse(null);
        if (discussion == null || !Boolean.TRUE.equals(discussion.getIsPaused())) {
            return;
        }

        String prompt = buildInterjectionPrompt(agent, userContent);
        StringBuilder contentBuilder = new StringBuilder();

        webSocketHandler.sendInterjectionStreamStart(taskId, agent.getId(), agent.getName(), agent.getType().name(), agent.getAvatarUrl());

        llmGateway.generateAgentResponseStream(agent, prompt, chunk -> {
            contentBuilder.append(chunk);
            webSocketHandler.sendInterjectionStreamChunk(taskId, chunk);
        }, () -> {
            String fullContent = contentBuilder.toString();
            Message message = Message.builder()
                    .round(round)
                    .agent(agent)
                    .content(fullContent)
                    .messageType(MessageType.INTERJECTION)
                    .isFinal(false)
                    .build();
            Message savedMessage = messageRepository.save(message);
            webSocketHandler.sendInterjectionStreamEnd(taskId, savedMessage.getId());
        });
    }
```

- [ ] **Step 4: 新增 handleUserInterjection 方法**

在 `processInterjectionMessageStream` 下方插入：

```java
    private void handleUserInterjection(Discussion discussion, String userContent) {
        Long taskId = discussion.getTask().getId();
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.error("Task不存在，无法处理插话");
            return;
        }

        List<Agent> agents = new ArrayList<>(task.getAgents());
        if (agents.isEmpty()) {
            log.warn("没有Agent参与，跳过插话回应");
            autoResumeDiscussion(taskId, discussion.getId());
            return;
        }

        int currentRound = discussion.getCurrentRound();
        Round round = roundRepository.findByDiscussionIdAndRoundNumber(discussion.getId(), currentRound).orElse(null);
        if (round == null) {
            log.error("当前轮次不存在，无法处理插话");
            autoResumeDiscussion(taskId, discussion.getId());
            return;
        }

        final Round finalRound = round;
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < agents.size(); i++) {
                Agent agent = agents.get(i);
                Discussion currentDiscussion = discussionRepository.findById(discussion.getId()).orElse(null);
                if (currentDiscussion == null || !Boolean.TRUE.equals(currentDiscussion.getIsPaused())) {
                    break;
                }
                processInterjectionMessageStream(agent, discussion.getId(), taskId, currentRound, finalRound, userContent);
                if (i < agents.size() - 1) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            autoResumeDiscussion(taskId, discussion.getId());
        }).exceptionally(e -> {
            log.error("插话回应执行失败", e);
            autoResumeDiscussion(taskId, discussion.getId());
            return null;
        });
    }

    private void autoResumeDiscussion(Long taskId, Long discussionId) {
        transactionTemplate.execute(status -> {
            Discussion d = discussionRepository.findById(discussionId).orElse(null);
            if (d != null) {
                d.setIsPaused(false);
                discussionRepository.save(d);
            }
            return null;
        });
        webSocketHandler.sendDiscussionResumed(taskId);
        log.info("讨论已自动恢复: Task ID={}", taskId);
    }
```

- [ ] **Step 5: 修改 buildPrompt 注入插话内容**

在 `buildPrompt` 方法开头（`StringBuilder prompt = new StringBuilder();` 之后）插入：

```java
        Discussion discussion = discussionRepository.findById(discussionId).orElse(null);
        boolean hasInterjection = discussion != null && Boolean.TRUE.equals(discussion.getHasUserInterjection());
        String interjectionContent = hasInterjection ? discussion.getInterjectionContent() : null;

        boolean agentHasInterjected = false;
        if (hasInterjection && interjectionContent != null) {
            List<Message> interjectionMessages = messageRepository.findByRoundIdAndAgentIdAndMessageType(
                    round.getId(), agent.getId(), MessageType.INTERJECTION);
            agentHasInterjected = !interjectionMessages.isEmpty();
        }
```

然后在 `prompt.append("=== 用户信息 ===\n");` 的代码块之后，紧接插入：

```java
        if (hasInterjection && !agentHasInterjected && interjectionContent != null) {
            prompt.append("=== 用户插话 ===\n");
            prompt.append("用户在讨论中补充了以下内容，请在发言中适当参考：\n");
            prompt.append(interjectionContent).append("\n\n");
        }
```

- [ ] **Step 6: 修改 convertToMessageDto 传递 messageType**

在方法内最后增加一行：

```java
                .messageType(message.getMessageType() != null ? message.getMessageType().name() : MessageType.AGENT.name())
```

- [ ] **Step 7: Commit**

```bash
git add careermind-backend/src/main/java/com/careermind/service/impl/DiscussionEngineImpl.java
git commit -m "feat(backend): implement user interjection with auto-resume"
```

---

### Task 7: 前端 - 扩展 Message 类型定义

**Files:**
- Modify: `careermind-frontend/src/types/index.ts`

- [ ] **Step 1: Message 接口新增字段**

在 `Message` 接口的 `isFinal` 上方插入：

```typescript
  messageType?: 'AGENT' | 'USER' | 'INTERJECTION'
```

- [ ] **Step 2: Commit**

```bash
git add careermind-frontend/src/types/index.ts
git commit -m "feat(frontend): add messageType to Message type"
```

---

### Task 8: 前端 - AgentMessage 支持用户与插话样式

**Files:**
- Modify: `careermind-frontend/src/components/discussion/AgentMessage.vue`

- [ ] **Step 1: 修改 class 绑定**

将第 2 行替换为：

```vue
  <div class="agent-message" :class="{ 'is-final': message.isFinal, 'is-streaming': isStreaming, 'is-user': isUserMessage, 'is-interjection': isInterjection }">
```

- [ ] **Step 2: 修改 header 显示**

将第 16 行替换为：

```vue
        <span class="agent-name">{{ displayName }}</span>
```

- [ ] **Step 3: 修改 script 中 computed 属性**

在 `isUserMessage` computed 下方插入：

```typescript
const isInterjection = computed(() => props.message.messageType === 'INTERJECTION')

const displayName = computed(() => {
  if (isUserMessage.value) return '我'
  if (isInterjection.value) return `${props.message.agentName} · 回应插话`
  return props.message.agentName
})
```

同时修改 `agentColor` computed（让插话回应使用用户绿色以作区分，或保持 agent 原色但降低透明度）：

```typescript
const agentColor = computed(() => {
  if (isInterjection.value) return '#10b981'
  return agentStore.getAgentColor(props.message.agentType)
})
```

- [ ] **Step 4: style 中新增插话样式**

在 `.agent-message.is-user` 样式块下方插入：

```css
.agent-message.is-interjection {
  border-left: 4px solid #f59e0b;
  background: linear-gradient(90deg, #fffbeb 0%, #ffffff 100%);
  padding: 12px 16px;
}

.is-interjection .agent-name {
  color: #b45309;
  font-size: 13px;
}

.is-interjection .message-body {
  font-size: 13px;
  color: #92400e;
}
```

- [ ] **Step 5: Commit**

```bash
git add careermind-frontend/src/components/discussion/AgentMessage.vue
git commit -m "feat(frontend): style user and interjection messages"
```

---

### Task 9: 前端 - 新建 DecisionTree 组件

**Files:**
- Create: `careermind-frontend/src/components/task/DecisionTree.vue`

- [ ] **Step 1: 编写完整组件代码**

```vue
<template>
  <div class="decision-tree-section">
    <h4>决策链路</h4>
    <div v-if="!hasData" class="tree-empty">
      <p>讨论启动后将生成决策树</p>
      <el-button type="primary" text @click="goToDiscussion">查看讨论</el-button>
    </div>
    <div v-else class="tree-wrapper">
      <v-chart class="tree-chart" :option="chartOption" autoresize @click="handleNodeClick" />
      <div class="tree-actions">
        <el-button type="primary" text @click="dialogVisible = true">展开查看完整决策树</el-button>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="决策树" width="90%" top="5vh" destroy-on-close>
      <v-chart class="tree-chart-full" :option="chartOption" autoresize @click="handleNodeClick" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { TreeChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { useAgentStore } from '@/stores/agent'
import type { Task, Discussion, MergeResult } from '@/types'

use([TreeChart, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  task: Task
  discussion: Discussion | null
  mergeResult: MergeResult | null
}>()

const router = useRouter()
const agentStore = useAgentStore()
const dialogVisible = ref(false)

const hasData = computed(() =>
  !!props.discussion && props.discussion.rounds.length > 0
)

const truncate = (text: string, len = 18) =>
  text.length > len ? text.slice(0, len) + '...' : text

const treeData = computed(() => {
  const rootLabel = props.task.goal || props.task.title || '职业困惑'
  const children: any[] = []

  if (props.discussion) {
    for (const round of props.discussion.rounds) {
      const roundNode: any = {
        name: `第${round.roundNumber}轮`,
        value: round.roundType,
        itemStyle: { color: '#3b82f6' },
        children: round.messages.map((m) => ({
          name: truncate(m.content),
          value: m.agentName,
          itemStyle: { color: agentStore.getAgentColor(m.agentType) },
          label: { color: agentStore.getAgentColor(m.agentType) }
        }))
      }
      if (roundNode.children.length) {
        children.push(roundNode)
      }
    }
  }

  if (props.mergeResult?.plans?.length) {
    children.push({
      name: '候选方案',
      itemStyle: { color: '#10b981' },
      children: props.mergeResult.plans.map((p) => ({
        name: truncate(p.title, 16),
        value: `plan:${p.id}`,
        itemStyle: { color: p.isSelected ? '#10b981' : '#6b7280' }
      }))
    })
  }

  return [{
    name: rootLabel,
    itemStyle: { color: '#1f2937' },
    children
  }]
})

const chartOption = computed(() => ({
  tooltip: { trigger: 'item', triggerOn: 'mousemove' },
  series: [{
    type: 'tree',
    data: treeData.value,
    top: '5%',
    left: '5%',
    bottom: '5%',
    right: '15%',
    symbolSize: 10,
    orient: 'RL',
    label: {
      position: 'left',
      verticalAlign: 'middle',
      align: 'right',
      fontSize: 12
    },
    leaves: {
      label: { position: 'right', verticalAlign: 'middle', align: 'left' }
    },
    expandAndCollapse: true,
    animationDuration: 300,
    animationDurationUpdate: 300
  }]
}))

const handleNodeClick = (params: any) => {
  const value = params.data?.value as string
  if (!value) return
  if (value.startsWith('plan:')) {
    router.push(`/results/${props.task.id}`)
  } else {
    router.push(`/discussions/${props.task.id}`)
  }
}

const goToDiscussion = () => {
  router.push(`/discussions/${props.task.id}`)
}
</script>

<style scoped>
.decision-tree-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.decision-tree-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 12px;
}

.tree-empty {
  text-align: center;
  padding: 24px 0;
  color: #9ca3af;
  font-size: 14px;
}

.tree-empty p {
  margin-bottom: 8px;
}

.tree-wrapper {
  text-align: center;
}

.tree-chart {
  width: 100%;
  height: 260px;
}

.tree-actions {
  margin-top: 8px;
}

.tree-chart-full {
  width: 100%;
  height: 70vh;
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add careermind-frontend/src/components/task/DecisionTree.vue
git commit -m "feat(frontend): add DecisionTree component with ECharts"
```

---

### Task 10: 前端 - TaskView 集成 DecisionTree

**Files:**
- Modify: `careermind-frontend/src/views/TaskView.vue`

- [ ] **Step 1: 模板中插入组件**

在 `<div class="detail-section">`（参与专家）闭合 `</div>` 之后、`detail-actions` 之前，插入：

```vue
        <div class="detail-section">
          <DecisionTree
            :task="task"
            :discussion="discussion"
            :merge-result="mergeResult"
          />
        </div>
```

- [ ] **Step 2: script 中引入并加载数据**

在 imports 区域新增：

```typescript
import DecisionTree from '@/components/task/DecisionTree.vue'
import { discussionApi } from '@/api/discussion'
import { mergeApi } from '@/api/merge'
import type { Discussion, MergeResult } from '@/types'
```

在 `const task = ref<Task | null>(null)` 下方新增：

```typescript
const discussion = ref<Discussion | null>(null)
const mergeResult = ref<MergeResult | null>(null)
```

修改 `loadTask` 为：

```typescript
const loadTask = async () => {
  task.value = await taskApi.getTaskById(taskId.value)
  const [disc, merge] = await Promise.allSettled([
    discussionApi.getDiscussion(taskId.value),
    mergeApi.getMergeResult(taskId.value).catch(() => null)
  ])
  discussion.value = disc.status === 'fulfilled' ? (disc.value as Discussion) : null
  mergeResult.value = merge.status === 'fulfilled' ? (merge.value as MergeResult | null) : null
}
```

- [ ] **Step 3: Commit**

```bash
git add careermind-frontend/src/views/TaskView.vue
git commit -m "feat(frontend): integrate DecisionTree into TaskView"
```

---

### Task 11: 前端 - DiscussionPanel 处理插话 WebSocket 与 UI 状态

**Files:**
- Modify: `careermind-frontend/src/components/discussion/DiscussionPanel.vue`

- [ ] **Step 1: 模板中增加插话状态提示条**

在 `<div class="panel-footer">` 上方插入：

```vue
    <!-- 插话状态提示 -->
    <div v-if="interjectionPending" class="interjection-banner">
      <el-icon><Loading /></el-icon>
      <span>插话已发送，专家正在回应...</span>
    </div>
```

- [ ] **Step 2: 导入 Loading 图标**

在现有 imports 中新增 `Loading`：

```typescript
import { Position, DocumentChecked, Loading } from '@element-plus/icons-vue'
```

同时在 `components` 里注册（如果用自动导入则不用，但这里显式 import 了）。若当前文件已显式 import 其他图标，则一并加上 `Loading`。

当前文件 import 的是 `<el-icon><Position /></el-icon>` 和 `<el-icon><DocumentChecked /></el-icon>`。检查脚本区：它并没有显式 import 这些图标组件（因为可能用了自动导入）。实际上脚本里没有 import 图标的代码。所以只需要确保 `<el-icon><Loading /></el-icon>` 能工作即可——Element Plus 自动导入通常会处理。如果项目使用 `unplugin-auto-import` 和 `unplugin-vue-components`，直接写 `<Loading />` 即可。为保险起见，在 script 的 import 区增加：

```typescript
import { Loading } from '@element-plus/icons-vue'
```

- [ ] **Step 3: 增加响应式状态**

在 `const streamingContent = ref('')` 下方新增：

```typescript
const interjectionPending = ref(false)
const interjectionStreamingMessage = ref<Message | null>(null)
const interjectionStreamingContent = ref('')
```

- [ ] **Step 4: 修改 sendMessage 方法**

将现有 `sendMessage` 替换为：

```typescript
const sendMessage = async () => {
  if (!userInput.value.trim()) return

  try {
    await discussionApi.sendMessage(props.taskId, userInput.value)
    userInput.value = ''
    interjectionPending.value = true
    ElMessage.success('消息已发送')
    await loadDiscussion()
  } catch (error) {
    ElMessage.error('发送消息失败')
  }
}
```

- [ ] **Step 5: WebSocket onmessage 增加插话类型处理**

在 `case 'stream_end':` 代码块之后、`case 'message':` 之前插入：

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

- [ ] **Step 6: 模板渲染插话流式消息**

在 `<!-- 流式输出消息 -->` div 的闭合 `</div>` 之后，插入：

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

- [ ] **Step 7: style 中新增插话提示条样式**

在 `<style scoped>` 末尾新增：

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

- [ ] **Step 8: Commit**

```bash
git add careermind-frontend/src/components/discussion/DiscussionPanel.vue
git commit -m "feat(frontend): handle interjection flow and WebSocket events in DiscussionPanel"
```

---

### Task 12: 编译与端到端验证

**Files:**
- 全局编译验证

- [ ] **Step 1: 后端编译**

Run:
```bash
cd careermind-backend && mvn clean compile -DskipTests
```
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端类型检查**

Run:
```bash
cd careermind-frontend && npx vue-tsc --noEmit
```
Expected: 无 TypeScript 错误

- [ ] **Step 3: 启动服务并做功能验证**

启动顺序（按 README）：
1. `cd careermind-rag && docker compose up -d qdrant`
2. `cd careermind-rag && cargo run`（或确认已在运行）
3. `cd careermind-backend && mvn spring-boot:run`
4. `cd careermind-frontend && npm run dev`

验证清单：
- [ ] TaskView 页面显示"决策链路"区块，讨论完成后树图包含 4 轮 + 候选方案
- [ ] 点击树图节点正确跳转到讨论页/结果页
- [ ] 讨论进行中发送用户插话，消息以"我"的样式出现在消息流
- [ ] 插话后看到"专家正在回应"提示条，随后每个 Agent 出现橙色边框的简短回应
- [ ] 全部回应完毕后提示条消失，讨论自动继续

- [ ] **Step 4: Commit 进度更新（如有）**

若验证通过，更新 `PROJECT_STATUS.md` 后提交：

```bash
git add PROJECT_STATUS.md
git commit -m "docs: update PROJECT_STATUS for decision tree and interjection"
```

---

## Self-Review

**1. Spec coverage：**
- 决策树缩略图 + 弹窗全屏 → Task 9 + Task 10
- 树图数据映射（task.goal → rounds → plans）→ Task 9
- 点击跳转 → Task 9
- 用户插话 pause → Task 6
- Agent 简短回应流式推送 → Task 5 + Task 6
- 自动 resume → Task 6
- 未发言 Agent prompt 注入 → Task 6 Step 5
- 前端 UI 区分 USER / INTERJECTION → Task 8 + Task 11

**2. Placeholder scan：** 无 TBD、TODO 或模糊描述，所有步骤均含具体代码与命令。

**3. Type consistency：**
- 后端统一使用 `MessageType` 枚举，DTO 使用 String name
- 前端统一使用字面量联合 `'AGENT' | 'USER' | 'INTERJECTION'`
- WebSocket 类型名前后一致：`interjection_stream_start/chunk/end`、`discussion_resumed`

**4. Gap fix：** 最初未包含 `PROJECT_STATUS.md` 更新，已在 Task 12 补入。

---

## 执行方式选择

**Plan complete and saved to `docs/superpowers/plans/2026-04-14-decision-tree-and-user-interjection.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration.

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints for review.

Which approach do you prefer?
