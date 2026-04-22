<template>
  <div class="stage" ref="stageRef">
    <div class="grid-bg" />

    <div class="topic">
      <div class="topic-cross">✦</div>
      <p class="topic-text" :title="topic">{{ topic }}</p>
    </div>

    <transition name="round-label">
      <div v-if="roundLabel" class="round-label" :key="roundLabel">{{ roundLabel }}</div>
    </transition>

    <div v-for="(slot, i) in slots" :key="slot.agentId" class="seat" :style="seatStyle(i)">
      <AgentAvatar
        :agent-type="slot.type"
        :size="120"
        :state="stateFor(slot.agentId)"
      />
      <span class="seat-name" :data-agent-type="slot.type">{{ slot.name }}</span>

      <div v-if="slot.agentId === currentSpeakerAgentId && streamingContent" class="seat-bubble">
        <SpeechBubble :agent-type="slot.type" :content="streamingContent" is-streaming />
      </div>
    </div>

    <ChallengeFlow v-if="flow" :from="flow.from" :to="flow.to" :w="stageW" :h="stageH" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUpdated, watch } from 'vue'
import AgentAvatar from '@/components/agent/AgentAvatar.vue'
import SpeechBubble from './SpeechBubble.vue'
import ChallengeFlow from './ChallengeFlow.vue'
import type { Agent, AgentType } from '@/types'

interface Slot { agentId: number; type: AgentType; name: string }
interface ChallengePair { fromAgentId: number; toAgentId: number; triggerAt: number }

const props = defineProps<{
  agents: Agent[]
  currentSpeakerAgentId?: number | null
  streamingContent?: string
  topic?: string
  roundLabel?: string
  latestChallenge?: ChallengePair | null
}>()

const stageRef = ref<HTMLElement>()
const stageW = ref(800); const stageH = ref(520)

const slots = computed<Slot[]>(() =>
  props.agents.slice(0, 5).map(a => ({ agentId: a.id, type: a.type as AgentType, name: a.name }))
)

const positions = [
  { x: 50, y: 12 },
  { x: 82, y: 32 },
  { x: 74, y: 74 },
  { x: 26, y: 74 },
  { x: 18, y: 32 },
]
const seatStyle = (i: number) => {
  const p = positions[i] || positions[0]
  return { left: p.x + '%', top: p.y + '%', transform: 'translate(-50%,-50%)' }
}

const stateFor = (agentId: number): 'idle' | 'listening' | 'speaking' | 'challenging' => {
  if (props.currentSpeakerAgentId == null) return 'idle'
  if (agentId === props.currentSpeakerAgentId) return 'speaking'
  if (props.latestChallenge && agentId === props.latestChallenge.toAgentId && Date.now() - props.latestChallenge.triggerAt < 2000) return 'challenging'
  return 'listening'
}

const flow = ref<{ from: { x: number; y: number }; to: { x: number; y: number } } | null>(null)
watch(() => props.latestChallenge?.triggerAt, () => {
  if (!props.latestChallenge) { flow.value = null; return }
  const fromIdx = slots.value.findIndex(s => s.agentId === props.latestChallenge!.fromAgentId)
  const toIdx   = slots.value.findIndex(s => s.agentId === props.latestChallenge!.toAgentId)
  if (fromIdx < 0 || toIdx < 0) return
  const f = positions[fromIdx]; const t = positions[toIdx]
  flow.value = { from: { x: f.x * 10, y: f.y * 6 }, to: { x: t.x * 10, y: t.y * 6 } }
  setTimeout(() => (flow.value = null), 1400)
})

const measure = () => {
  if (!stageRef.value) return
  stageW.value = stageRef.value.clientWidth
  stageH.value = stageRef.value.clientHeight
}
onMounted(() => { measure(); window.addEventListener('resize', measure) })
onUpdated(measure)
</script>

<style scoped>
.stage { position: relative; width: 100%; height: 100%; min-height: 520px; overflow: hidden; background: var(--bg-page); }
.grid-bg {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(var(--border-subtle) 1px, transparent 1px),
    linear-gradient(90deg, var(--border-subtle) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(ellipse at center, black 40%, transparent 80%);
  -webkit-mask-image: radial-gradient(ellipse at center, black 40%, transparent 80%);
  opacity: 0.35;
}
.topic {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); z-index: 1;
  text-align: center; color: var(--text-secondary); max-width: 280px; pointer-events: none;
}
.topic-cross { font-size: 20px; color: var(--accent); margin-bottom: 6px; }
.topic-text  { margin: 0; font-size: 13px; line-height: 1.5; }

.round-label {
  position: absolute; top: 20px; left: 50%; transform: translateX(-50%); z-index: 3;
  padding: 6px 14px; background: var(--bg-card); border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full); font-size: 12px; color: var(--text-secondary); font-weight: 500;
}
.round-label-enter-active, .round-label-leave-active { transition: opacity var(--duration-slow) var(--ease-emphasized), transform var(--duration-slow) var(--ease-emphasized); }
.round-label-enter-from { opacity: 0; transform: translate(-50%, -12px); }
.round-label-leave-to   { opacity: 0; transform: translate(-50%, -4px); }

.seat { position: absolute; z-index: 2; display: flex; flex-direction: column; align-items: center; gap: 6px; }
.seat-name { font-size: 12px; font-weight: 500; color: var(--agent); background: var(--agent-dim); padding: 2px 10px; border-radius: var(--radius-full); white-space: nowrap; }
.seat-bubble { position: absolute; bottom: calc(100% + 12px); left: 50%; transform: translateX(-50%); z-index: 4; }
</style>
