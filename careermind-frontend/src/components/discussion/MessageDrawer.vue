<template>
  <aside class="msg-drawer" :class="{ open }">
    <button class="drawer-toggle" @click="$emit('update:open', !open)" :title="open ? '收起' : '展开消息流'">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path v-if="open" d="M9 18l6-6-6-6"/>
        <path v-else d="M15 18l-6-6 6-6"/>
      </svg>
      <span v-if="!open" class="count">{{ messageCount }}</span>
    </button>

    <div v-if="open" class="drawer-body" ref="bodyRef">
      <div v-for="g in groups" :key="g.roundNumber" class="round-group">
        <div class="round-head">
          <span class="round-pill">第 {{ g.roundNumber }} 轮 · {{ g.label }}</span>
        </div>
        <AgentMessage v-for="m in g.messages" :key="m.id" :message="m" />
      </div>

      <div v-if="streamingMessage" class="round-group">
        <AgentMessage :message="streamingMessage" :is-streaming="true" />
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import AgentMessage from './AgentMessage.vue'
import type { Round, Message, RoundType } from '@/types'

const props = defineProps<{
  open: boolean
  rounds: Round[]
  streamingMessage?: Message | null
}>()
defineEmits<{ (e: 'update:open', v: boolean): void }>()

const bodyRef = ref<HTMLElement>()
const messageCount = computed(() => props.rounds.reduce((s, r) => s + r.messages.length, 0) + (props.streamingMessage ? 1 : 0))
const roundLabels: Record<RoundType, string> = { INDEPENDENT: '独立诊断', CHALLENGE: '质疑挑战', REVISION: '修正完善', FINAL: '最终陈述' }
const groups = computed(() => props.rounds.map(r => ({ roundNumber: r.roundNumber, label: roundLabels[r.roundType] || r.roundType, messages: r.messages })))

watch(messageCount, async () => { await nextTick(); if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight })
</script>

<style scoped>
.msg-drawer { position: relative; width: 48px; background: var(--bg-card); border-left: 1px solid var(--border-subtle); transition: width var(--duration-base) var(--ease-standard); flex-shrink: 0; }
.msg-drawer.open { width: 360px; }

.drawer-toggle {
  position: absolute; top: 16px; left: 8px; z-index: 2;
  width: 32px; height: 32px; border-radius: var(--radius-full);
  background: var(--bg-card); color: var(--text-secondary); border: 1px solid var(--border-emphasis);
  cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.count {
  position: absolute; top: -5px; right: -5px; min-width: 16px; height: 16px;
  background: var(--accent); color: var(--accent-contrast); border-radius: 9999px; font-size: 10px; padding: 0 4px;
  display: inline-flex; align-items: center; justify-content: center;
}
.drawer-body { height: 100%; overflow-y: auto; padding: 56px 14px 80px; }

.round-group { margin-bottom: 20px; }
.round-head { position: sticky; top: -1px; padding: 6px 0; background: var(--bg-card); z-index: 1; }
.round-pill {
  display: inline-block; padding: 3px 10px; border-radius: var(--radius-full);
  background: var(--bg-elevated); color: var(--text-secondary);
  font-size: 11px; font-weight: 500;
}
</style>
