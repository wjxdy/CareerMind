<template>
  <div class="panel">
    <header class="panel-head" v-if="task">
      <button class="back-btn" @click="$router.push(`/tasks/${task.id}`)" title="返回">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"><path d="M15 18l-6-6 6-6"/></svg>
      </button>
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

    <AgentRoster
      :agents="task?.agents || []"
      :current-speaker-agent-id="streamingMessage?.agentId ?? null"
    />

    <SpeakerSpotlight
      :show="!!streamingMessage"
      :agent-type="streamingMessage?.agentType"
      :agent-name="streamingMessage?.agentName"
      :content="streamingMessage?.content"
    />

    <ConversationFeed
      :rounds="discussion?.rounds || []"
      :streaming-message="streamingMessage"
      :round-divergences="roundDivergences"
    />

    <div v-if="!hasDiscussion" class="start-overlay">
      <div class="overlay-inner">
        <h2>{{ task?.title || '准备开始讨论' }}</h2>
        <p class="overlay-hint">5 位 AI 专家将进行 4 轮辩论</p>
        <BaseButton variant="primary" size="lg" @click="handleStart">开始讨论</BaseButton>
      </div>
    </div>

    <footer class="panel-foot">
      <div class="foot-left">
        <ThermoBar :divergence="currentDivergence" :delta-text="deltaText" />
      </div>
      <div class="foot-input">
        <BaseInput v-model="userInput" placeholder="向专家插话（可选）" @keyup="onInputKeyup" />
      </div>
      <div class="foot-actions">
        <BaseButton variant="secondary" size="md" @click="sendMessage" :disabled="!userInput.trim()">插话</BaseButton>
        <BaseButton variant="secondary" size="md" @click="handleNextRound" :disabled="!discussion?.isActive">下一轮</BaseButton>
        <BaseButton variant="primary" size="md" @click="goResult">查看结果</BaseButton>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message as ElMessage } from '@/utils/naive-discrete'
import AgentRoster from './AgentRoster.vue'
import SpeakerSpotlight from './SpeakerSpotlight.vue'
import ConversationFeed from './ConversationFeed.vue'
import DiscussionControl from './DiscussionControl.vue'
import RoundTimeline from './RoundTimeline.vue'
import ThermoBar from './ThermoBar.vue'
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
const streamingMessage = ref<Message | null>(null)
const streamingContent = ref('')
const currentDivergence = ref(0.5)
const deltaText = ref<string | null>(null)
const roundDivergences = ref<Record<number, number>>({})
let ws: WebSocket | null = null

const hasDiscussion = computed(() => !!discussion.value && (discussion.value.rounds.length > 0 || discussion.value.isActive))

onMounted(async () => { await loadTask(); await loadDiscussion(); connect() })
onUnmounted(() => ws?.close())

watch(() => props.taskId, async (n, o) => {
  if (n === o) return
  ws?.close(); streamingMessage.value = null; streamingContent.value = ''
  roundDivergences.value = {}
  await loadTask(); await loadDiscussion(); connect()
})

const loadTask = async () => { try { task.value = await taskApi.getTaskById(props.taskId) } catch { task.value = null } }
const loadDiscussion = async () => {
  try {
    discussion.value = await discussionApi.getDiscussion(props.taskId)
    // hydrate roundDivergences from backend if present on rounds (as side effect of Discussion API)
    // 若后端在 RoundDto 带了 divergence 字段则读取；否则走 WS graph_delta 累积
    const rounds = discussion.value?.rounds as Array<{ roundNumber: number; divergence?: number }> | undefined
    if (rounds) {
      for (const r of rounds) {
        if (typeof r.divergence === 'number') roundDivergences.value[r.roundNumber] = r.divergence
      }
    }
  } catch { discussion.value = null }
}

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
        streamingMessage.value = null; streamingContent.value = ''
        loadDiscussion()
        break
      case 'message':
        loadDiscussion()
        break
      case 'graph_delta':
        if (typeof d.divergence === 'number') {
          currentDivergence.value = d.divergence
          roundDivergences.value[d.roundNumber] = d.divergence
          deltaText.value = `第 ${d.roundNumber} 轮 共识度 ${Math.round((1 - d.divergence) * 100)}%`
          setTimeout(() => (deltaText.value = null), 1500)
        }
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
.panel { display: flex; flex-direction: column; height: 100%; background: var(--bg-page); position: relative; }

.panel-head {
  display: flex; align-items: center; gap: 16px;
  padding: 14px 24px;
  background: rgba(255,255,255,0.85);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid var(--border-subtle);
  flex-shrink: 0;
  position: relative; z-index: 5;
}
html[data-theme="dark"] .panel-head { background: rgba(0,0,0,0.7); }
.back-btn {
  width: 32px; height: 32px;
  display: inline-flex; align-items: center; justify-content: center;
  background: transparent; border: none; border-radius: var(--radius-full);
  color: var(--text-secondary); cursor: pointer;
  transition: all var(--duration-fast) var(--ease-standard);
}
.back-btn:hover { background: var(--bg-elevated); color: var(--text-primary); }
.head-title { flex: 1; min-width: 0; }
.head-title h3 { margin: 0; font-size: 15px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; letter-spacing: -0.01em; }
.head-goal { margin: 2px 0 0; font-size: 12px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.head-right { display: flex; align-items: center; gap: 18px; flex-shrink: 0; }

.start-overlay {
  position: absolute; inset: 0; z-index: 20;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}
html[data-theme="dark"] .start-overlay { background: rgba(0,0,0,0.85); }
.overlay-inner {
  text-align: center; padding: 48px;
  display: flex; flex-direction: column; align-items: center; gap: 14px;
}
.overlay-inner h2 { font-size: 36px; margin: 0 0 4px; letter-spacing: -0.025em; }
.overlay-hint { font-size: 16px; color: var(--text-secondary); margin: 0 0 16px; }

.panel-foot {
  display: flex; align-items: center; gap: 16px;
  padding: 14px 24px;
  background: var(--bg-card);
  border-top: 1px solid var(--border-subtle);
  flex-shrink: 0;
}
.foot-left { flex-shrink: 0; }
.foot-input { flex: 1; }
.foot-actions { display: flex; gap: 8px; flex-shrink: 0; }
</style>
