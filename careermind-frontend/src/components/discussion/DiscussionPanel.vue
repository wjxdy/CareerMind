<template>
  <div class="panel">
    <header class="panel-head" v-if="task">
      <button class="back-btn" @click="$router.push(`/tasks/${task.id}`)">←</button>
      <div class="head-title">
        <h3>{{ task.title }}</h3>
        <p v-if="task.goal" class="head-goal">{{ task.goal.slice(0, 80) }}</p>
      </div>
      <div class="head-right">
        <RoundTimeline :current="discussion?.currentRound || 1" />
        <DiscussionControl
          :is-active="discussion?.isActive || false"
          :is-paused="discussion?.isPaused || false"
          @start="handleStart" @pause="handlePause" @resume="handleResume" @stop="handleStop" @next-round="handleNextRound"
        />
      </div>
    </header>

    <div class="panel-body">
      <div class="stage-wrap">
        <RoundtableStage
          :agents="task?.agents || []"
          :current-speaker-agent-id="streamingMessage?.agentId ?? null"
          :streaming-content="streamingMessage?.content"
          :topic="task?.goal || task?.title"
          :round-label="roundLabelText"
          :latest-challenge="latestChallenge"
        />
        <div v-if="!hasDiscussion" class="stage-overlay">
          <BaseButton variant="primary" size="lg" @click="handleStart">▶ 开始讨论</BaseButton>
          <p class="overlay-hint">5 位 AI 专家将进行 4 轮辩论</p>
        </div>
      </div>
      <MessageDrawer v-model:open="drawerOpen" :rounds="discussion?.rounds || []" :streaming-message="streamingMessage" />
    </div>

    <footer class="panel-foot">
      <div class="foot-input">
        <BaseInput v-model="userInput" placeholder="输入想对专家说的话 (可选)…" @keyup="onInputKeyup" />
      </div>
      <div class="foot-actions">
        <BaseButton variant="secondary" size="md" @click="sendMessage" :disabled="!userInput.trim()">插话</BaseButton>
        <BaseButton variant="secondary" size="md" @click="handleNextRound" :disabled="!discussion?.isActive">下一轮 ▷</BaseButton>
        <BaseButton variant="primary" size="md" @click="goResult">生成报告 📄</BaseButton>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message as ElMessage } from '@/utils/naive-discrete'
import RoundtableStage from './RoundtableStage.vue'
import MessageDrawer from './MessageDrawer.vue'
import DiscussionControl from './DiscussionControl.vue'
import RoundTimeline from './RoundTimeline.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { taskApi } from '@/api/task'
import { discussionApi } from '@/api/discussion'
import type { Task, Discussion, Message } from '@/types'

const props = defineProps<{ taskId: number }>()
const router = useRouter()
const task = ref<Task | null>(null)
const discussion = ref<Discussion | null>(null)
const userInput = ref('')
const drawerOpen = ref(false)
const streamingMessage = ref<Message | null>(null)
const streamingContent = ref('')
const latestChallenge = ref<{ fromAgentId: number; toAgentId: number; triggerAt: number } | null>(null)
let ws: WebSocket | null = null

const hasDiscussion = computed(() => !!discussion.value && (discussion.value.rounds.length > 0 || discussion.value.isActive))
const roundLabelText = computed(() => {
  const r = discussion.value?.currentRound; if (!r) return ''
  const labels: Record<number, string> = { 1: '独立诊断', 2: '质疑挑战', 3: '修正完善', 4: '最终陈述' }
  return `第 ${r} 轮 · ${labels[r] || ''}`
})

onMounted(async () => { await loadTask(); await loadDiscussion(); connect() })
onUnmounted(() => ws?.close())

watch(() => props.taskId, async (n, o) => {
  if (n === o) return
  ws?.close(); streamingMessage.value = null; streamingContent.value = ''
  await loadTask(); await loadDiscussion(); connect()
})

const loadTask = async () => { try { task.value = await taskApi.getTaskById(props.taskId) } catch { task.value = null } }
const loadDiscussion = async () => { try { discussion.value = await discussionApi.getDiscussion(props.taskId) } catch { discussion.value = null } }

const connect = () => {
  const url = `ws://${window.location.host}/ws/discussion?taskId=${props.taskId}`
  ws = new WebSocket(url)
  ws.onmessage = (ev) => {
    const d = JSON.parse(ev.data)
    switch (d.type) {
      case 'stream_start':
        streamingMessage.value = {
          id: Date.now(),
          agentId: d.data.agentId, agentName: d.data.agentName, agentType: d.data.agentType,
          agentAvatar: d.data.agentAvatar, content: '', isFinal: false, createdAt: new Date().toISOString(),
        }
        streamingContent.value = ''
        break
      case 'stream_chunk':
        if (streamingMessage.value) {
          streamingContent.value += d.content
          streamingMessage.value.content = streamingContent.value
        }
        break
      case 'stream_end':
        if (streamingMessage.value && d.data?.replyToAgentId) {
          latestChallenge.value = { fromAgentId: streamingMessage.value.agentId, toAgentId: d.data.replyToAgentId, triggerAt: Date.now() }
        }
        streamingMessage.value = null; streamingContent.value = ''
        loadDiscussion()
        break
      case 'message':
        loadDiscussion()
        break
      case 'result_stream_end':
        ElMessage.success('结果已生成'); break
    }
  }
}

const handleStart    = async () => { try { discussion.value = await discussionApi.startDiscussion(props.taskId) } catch (e:any) { ElMessage.error(e.message || '开始失败') } }
const handlePause    = async () => { try { discussion.value = await discussionApi.pauseDiscussion(props.taskId) } catch { ElMessage.error('暂停失败') } }
const handleResume   = async () => { try { discussion.value = await discussionApi.resumeDiscussion(props.taskId) } catch { ElMessage.error('继续失败') } }
const handleStop     = async () => { try { discussion.value = await discussionApi.stopDiscussion(props.taskId) } catch { ElMessage.error('停止失败') } }
const handleNextRound= async () => { try { discussion.value = await discussionApi.nextRound(props.taskId) } catch (e:any) { ElMessage.error(e.message || '下一轮失败') } }

const sendMessage = async () => {
  const v = userInput.value.trim(); if (!v) return
  try { await discussionApi.sendMessage(props.taskId, v); userInput.value = ''; loadDiscussion() }
  catch { ElMessage.error('发送失败') }
}

const onInputKeyup = (ev: KeyboardEvent) => { if (ev.key === 'Enter') sendMessage() }

const goResult = () => router.push(`/results/${props.taskId}`)
</script>

<style scoped>
.panel { display: flex; flex-direction: column; height: 100%; background: var(--bg-page); }

.panel-head {
  display: flex; align-items: center; gap: 16px;
  padding: 12px 20px; background: var(--bg-card); border-bottom: 1px solid var(--border-subtle); flex-shrink: 0;
}
.back-btn { width: 28px; height: 28px; background: transparent; border: 1px solid var(--border-subtle); border-radius: var(--radius-md); color: var(--text-secondary); cursor: pointer; }
.back-btn:hover { background: var(--bg-elevated); color: var(--text-primary); }
.head-title { flex: 1; min-width: 0; }
.head-title h3 { margin: 0; font-size: 15px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.head-goal { margin: 2px 0 0; font-size: 12px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.head-right { display: flex; align-items: center; gap: 16px; flex-shrink: 0; }

.panel-body { flex: 1; display: flex; min-height: 0; }
.stage-wrap { flex: 1; position: relative; min-width: 0; }
.stage-overlay {
  position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 12px; background: rgba(250,250,250,0.4); backdrop-filter: blur(2px); z-index: 5;
}
html[data-theme="dark"] .stage-overlay { background: rgba(9,9,11,0.4); }
.overlay-hint { font-size: 13px; color: var(--text-muted); margin: 0; }

.panel-foot {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 20px; background: var(--bg-card); border-top: 1px solid var(--border-subtle); flex-shrink: 0;
}
.foot-input { flex: 1; }
.foot-actions { display: flex; gap: 8px; flex-shrink: 0; }
</style>
