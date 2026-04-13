<template>
  <div class="discussion-panel">
    <!-- 头部信息 -->
    <div class="panel-header" v-if="task">
      <div class="task-info">
        <h3>{{ task.title }}</h3>
        <p v-if="task.goal" class="goal-preview">{{ task.goal.slice(0, 100) }}...</p>
      </div>
      <div class="header-actions">
        <RoundIndicator :current-round="discussion?.currentRound || 0" />
        <DiscussionControl
          :is-active="discussion?.isActive || false"
          :is-paused="discussion?.isPaused || false"
          @start="handleStart"
          @pause="handlePause"
          @resume="handleResume"
          @stop="handleStop"
          @next-round="handleNextRound"
        />
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="messages-container" ref="messagesContainer">
      <div v-if="allMessages.length === 0 && !streamingMessage" class="empty-state">
        <el-empty description="点击「开始讨论」启动AI专家对话" />
      </div>

      <div v-else class="messages-list">
        <div v-for="(group, index) in messageGroups" :key="index">
          <!-- 轮次分隔线 -->
          <div class="round-divider">
            <el-divider>
              <el-tag size="small" type="info">
                第 {{ group.roundNumber }} 轮 - {{ getRoundLabel(group.roundType) }}
              </el-tag>
            </el-divider>
          </div>

          <!-- 该轮的消息 -->
          <AgentMessage
            v-for="message in group.messages"
            :key="message.id"
            :message="message"
          />
        </div>

        <!-- 流式输出消息 -->
        <div v-if="streamingMessage" class="streaming-message">
          <div class="round-divider">
            <el-divider>
              <el-tag size="small" type="info">
                第 {{ discussion?.currentRound }} 轮 - {{ getRoundLabel(getCurrentRoundType()) }}
              </el-tag>
            </el-divider>
          </div>
          <AgentMessage
            :message="streamingMessage"
            :is-streaming="true"
          />
        </div>
      </div>
    </div>

    <!-- 底部输入区 -->
    <div class="panel-footer">
      <div class="input-area">
        <el-input
          v-model="userInput"
          placeholder="输入你想对专家说的话（可选）..."
          @keyup.enter="sendMessage"
        >
          <template #append>
            <el-button type="primary" @click="sendMessage">
              <el-icon><Position /></el-icon>
            </el-button>
          </template>
        </el-input>
      </div>
      <div class="footer-actions">
        <el-button
          v-if="discussion?.isActive && !discussion?.isPaused"
          type="success"
          @click="generateResult"
        >
          <el-icon><DocumentChecked /></el-icon>
          生成结果
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AgentMessage from './AgentMessage.vue'
import RoundIndicator from './RoundIndicator.vue'
import DiscussionControl from './DiscussionControl.vue'
import { taskApi } from '@/api/task'
import { discussionApi } from '@/api/discussion'
import type { Task, Discussion, Message, RoundType } from '@/types'

const props = defineProps<{
  taskId: number
}>()

const router = useRouter()
const task = ref<Task | null>(null)
const discussion = ref<Discussion | null>(null)
const userInput = ref('')
const messagesContainer = ref<HTMLElement>()
let ws: WebSocket | null = null

// 流式输出相关
const streamingMessage = ref<Message | null>(null)
const streamingContent = ref('')

const allMessages = computed(() => {
  if (!discussion.value) return []
  return discussion.value.rounds.flatMap(r => r.messages)
})

const messageGroups = computed(() => {
  if (!discussion.value) return []
  return discussion.value.rounds.map(round => ({
    roundNumber: round.roundNumber,
    roundType: round.roundType,
    messages: round.messages
  }))
})

onMounted(async () => {
  await loadTask()
  await loadDiscussion()
  connectWebSocket()
})

onUnmounted(() => {
  ws?.close()
})

// 监听 taskId 变化，切换对话时重新加载数据
watch(() => props.taskId, async (newTaskId, oldTaskId) => {
  if (newTaskId !== oldTaskId) {
    // 关闭旧 WebSocket
    ws?.close()
    // 重置状态
    streamingMessage.value = null
    streamingContent.value = ''
    // 重新加载数据
    await loadTask()
    await loadDiscussion()
    connectWebSocket()
  }
})

const loadTask = async () => {
  try {
    task.value = await taskApi.getTaskById(props.taskId)
  } catch {
    // 静默处理，不弹错误提示
    console.error('加载任务失败')
  }
}

const loadDiscussion = async () => {
  try {
    const result = await discussionApi.getDiscussion(props.taskId)
    discussion.value = result
  } catch {
    // 讨论可能还不存在，静默处理
    discussion.value = null
  }
}

const connectWebSocket = () => {
  const wsUrl = `ws://${window.location.host}/ws/discussion?taskId=${props.taskId}`
  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    console.log('WebSocket connected')
  }

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    console.log('Received message:', data)

    switch (data.type) {
      case 'stream_start':
        // 开始流式输出
        streamingMessage.value = {
          id: Date.now(),
          agentId: data.data.agentId,
          agentName: data.data.agentName,
          agentType: data.data.agentType,
          agentAvatar: data.data.agentAvatar,
          content: '',
          isFinal: false,
          createdAt: new Date().toISOString()
        }
        streamingContent.value = ''
        scrollToBottom()
        break

      case 'stream_chunk':
        // 接收流式内容片段
        if (streamingMessage.value) {
          streamingContent.value += data.content
          streamingMessage.value.content = streamingContent.value
          scrollToBottom()
        }
        break

      case 'stream_end':
        // 流式输出结束，刷新完整数据
        streamingMessage.value = null
        streamingContent.value = ''
        loadDiscussion()
        break

      case 'message':
        // 完整消息（兼容旧版本）
        loadDiscussion()
        scrollToBottom()
        break

      case 'result_stream_start':
        // 结果流式开始
        ElMessage.info('正在生成最终结果...')
        break

      case 'result_stream_chunk':
        // 结果流式片段
        break

      case 'result_stream_end':
        // 结果流式结束
        ElMessage.success('结果生成完成')
        break

      default:
        // 默认行为：刷新讨论
        loadDiscussion()
        scrollToBottom()
    }
  }

  ws.onerror = (error) => {
    console.error('WebSocket error:', error)
  }

  ws.onclose = () => {
    console.log('WebSocket closed')
  }
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const handleStart = async () => {
  try {
    discussion.value = await discussionApi.startDiscussion(props.taskId)
    ElMessage.success('讨论已开始')
  } catch (error: any) {
    ElMessage.error(error.message || '开始失败')
  }
}

const handlePause = async () => {
  try {
    discussion.value = await discussionApi.pauseDiscussion(props.taskId)
  } catch {
    ElMessage.error('暂停失败')
  }
}

const handleResume = async () => {
  try {
    discussion.value = await discussionApi.resumeDiscussion(props.taskId)
  } catch {
    ElMessage.error('继续失败')
  }
}

const handleStop = async () => {
  try {
    discussion.value = await discussionApi.stopDiscussion(props.taskId)
  } catch {
    ElMessage.error('停止失败')
  }
}

const handleNextRound = async () => {
  try {
    discussion.value = await discussionApi.nextRound(props.taskId)
    ElMessage.success('进入下一轮')
  } catch (error: any) {
    ElMessage.error(error.message || '进入下一轮失败')
  }
}

const sendMessage = async () => {
  if (!userInput.value.trim()) return

  try {
    await discussionApi.sendMessage(props.taskId, userInput.value)
    userInput.value = ''
    ElMessage.success('消息已发送')
    // 刷新讨论以显示用户消息
    await loadDiscussion()
  } catch (error) {
    ElMessage.error('发送消息失败')
  }
}

const generateResult = () => {
  router.push(`/results/${props.taskId}`)
}

const getRoundLabel = (type: RoundType) => {
  const labels: Record<string, string> = {
    INDEPENDENT: '独立诊断',
    CHALLENGE: '质疑挑战',
    REVISION: '修正完善',
    FINAL: '最终陈述'
  }
  return labels[type] || type
}

const getCurrentRoundType = (): RoundType => {
  const roundNum = discussion.value?.currentRound || 1
  switch (roundNum) {
    case 1: return 'INDEPENDENT'
    case 2: return 'CHALLENGE'
    case 3: return 'REVISION'
    case 4: return 'FINAL'
    default: return 'INDEPENDENT'
  }
}
</script>

<style scoped>
.discussion-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f9fafb;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: white;
  border-bottom: 1px solid #e5e7eb;
}

.task-info h3 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.goal-preview {
  font-size: 13px;
  color: #6b7280;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.empty-state {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.messages-list {
  max-width: 900px;
  margin: 0 auto;
}

.round-divider {
  margin: 24px 0 16px;
}

.round-divider :deep(.el-divider__text) {
  background: #f9fafb;
}

.streaming-message {
  max-width: 900px;
  margin: 0 auto;
}

.panel-footer {
  padding: 16px 24px;
  background: white;
  border-top: 1px solid #e5e7eb;
}

.input-area {
  max-width: 900px;
  margin: 0 auto;
}

.input-area :deep(.el-input__wrapper) {
  border-radius: 24px;
}

.footer-actions {
  display: flex;
  justify-content: center;
  margin-top: 12px;
  gap: 12px;
}
</style>
