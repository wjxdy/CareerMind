<template>
  <div class="bubble" :data-agent-type="agentType">
    <div class="bubble-content">
      <p class="bubble-text">{{ truncated }}<span v-if="isStreaming" class="caret">▋</span></p>
    </div>
    <div class="bubble-tail" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AgentType } from '@/types'
const props = withDefaults(defineProps<{
  agentType: AgentType | string
  content: string
  isStreaming?: boolean
  maxChars?: number
}>(), { maxChars: 140 })
const truncated = computed(() => {
  const t = (props.content || '').trim()
  return t.length > props.maxChars ? t.slice(0, props.maxChars) + '…' : t
})
</script>

<style scoped>
.bubble { position: relative; max-width: 320px; animation: pop var(--duration-base) var(--ease-emphasized); }
@keyframes pop { from { opacity: 0; transform: translateY(-4px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
.bubble-content {
  background: var(--bg-card); border: 1px solid var(--agent);
  color: var(--text-primary); padding: 10px 14px;
  border-radius: var(--radius-lg); box-shadow: var(--shadow-md);
}
.bubble-text { margin: 0; font-size: 13px; line-height: 1.5; }
.bubble-tail {
  position: absolute; top: -6px; left: 50%; transform: translateX(-50%) rotate(45deg);
  width: 10px; height: 10px; background: var(--bg-card);
  border-left: 1px solid var(--agent); border-top: 1px solid var(--agent);
}
.caret { display: inline-block; animation: blink 1s steps(2) infinite; color: var(--agent); }
@keyframes blink { 50% { opacity: 0; } }
</style>
