<template>
  <div class="feed" ref="feedRef">
    <template v-if="totalCount === 0 && !streamingMessage">
      <div class="empty">
        <p class="empty-title">等待讨论开始</p>
        <p class="empty-sub">5 位 AI 专家将进行 4 轮辩论，所有消息将按时间顺序显示在这里</p>
      </div>
    </template>

    <template v-else>
      <div v-for="g in groups" :key="g.roundNumber" class="round-block">
        <div class="round-divider">
          <span class="round-line" />
          <span class="round-pill">
            第 {{ g.roundNumber }} 轮 · {{ g.label }}
            <span v-if="g.divergence != null" class="divergence">
              共识 {{ Math.round((1 - g.divergence) * 100) }}%
            </span>
          </span>
          <span class="round-line" />
        </div>
        <AgentMessage v-for="m in g.messages" :key="m.id" :message="m" />
      </div>

      <div v-if="streamingMessage" class="streaming-placeholder">
        <AgentMessage :message="streamingMessage" :is-streaming="true" />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import AgentMessage from './AgentMessage.vue'
import type { Round, Message, RoundType } from '@/types'

const props = defineProps<{
  rounds: Round[]
  streamingMessage?: Message | null
  roundDivergences?: Record<number, number>
}>()

const feedRef = ref<HTMLElement>()

const roundLabels: Record<RoundType, string> = {
  INDEPENDENT: '独立诊断',
  CHALLENGE: '质疑挑战',
  REVISION: '修正完善',
  FINAL: '最终陈述',
}

const groups = computed(() =>
  props.rounds.map(r => ({
    roundNumber: r.roundNumber,
    label: roundLabels[r.roundType] || r.roundType,
    messages: r.messages,
    divergence: props.roundDivergences?.[r.roundNumber],
  })),
)

const totalCount = computed(() => props.rounds.reduce((s, r) => s + r.messages.length, 0))

const streamingHash = computed(() => {
  const m = props.streamingMessage
  return m ? `${m.id}-${(m.content || '').length}` : ''
})

const scrollToBottom = async () => {
  await nextTick()
  if (feedRef.value) feedRef.value.scrollTop = feedRef.value.scrollHeight
}
watch([totalCount, streamingHash], scrollToBottom, { flush: 'post' })
</script>

<style scoped>
.feed {
  flex: 1; overflow-y: auto;
  padding: 24px 32px 32px;
  background: var(--bg-page);
}
.empty {
  margin: 80px auto; max-width: 420px; text-align: center; color: var(--text-muted);
}
.empty-title { margin: 0 0 8px; font-size: 16px; color: var(--text-secondary); font-weight: 500; }
.empty-sub { margin: 0; font-size: 13px; line-height: 1.7; }

.round-block { max-width: 860px; margin: 0 auto; }

.round-divider {
  display: flex; align-items: center; gap: 12px;
  margin: 24px 0 14px;
}
.round-line { flex: 1; height: 1px; background: var(--border-subtle); }
.round-pill {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 4px 14px; background: var(--bg-card); border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  font-size: 12px; color: var(--text-secondary); font-weight: 500;
  white-space: nowrap;
}
.divergence {
  padding: 1px 6px; background: var(--accent-dim); color: var(--accent);
  border-radius: var(--radius-full); font-size: 11px;
}

.streaming-placeholder { max-width: 860px; margin: 0 auto; }
</style>
