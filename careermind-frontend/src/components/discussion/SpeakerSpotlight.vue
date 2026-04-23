<template>
  <transition name="spotlight">
    <div v-if="show" class="spotlight" :data-agent-type="agentType">
      <AgentAvatar :agent-type="agentType || 'CUSTOM'" :size="72" state="speaking" />
      <div class="sl-body">
        <div class="sl-head">
          <span class="sl-name">{{ agentName }}</span>
          <span class="sl-state">
            <span class="dot" /><span class="dot" /><span class="dot" />
            正在发言
          </span>
        </div>
        <p class="sl-content">{{ displayContent }}<span class="caret">▋</span></p>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AgentAvatar from '@/components/agent/AgentAvatar.vue'
import { getAgentMeta } from '@/utils/agent-meta'
import type { AgentType } from '@/types'

const props = defineProps<{
  show: boolean
  agentType?: AgentType | string
  agentName?: string
  content?: string
}>()

// 截取最后 ~240 字，够凸显"当前说到哪"又不至于占满屏
const displayContent = computed(() => {
  const t = (props.content || '').trim()
  if (t.length <= 240) return t
  return '…' + t.slice(-240)
})

// ensure getAgentMeta isn't tree-shaken if we only need visual theming
void getAgentMeta
</script>

<style scoped>
.spotlight {
  display: flex; gap: 16px; align-items: flex-start;
  padding: 20px 24px;
  background: linear-gradient(180deg, var(--agent-dim) 0%, var(--bg-card) 100%);
  border-bottom: 1px solid var(--border-subtle);
  border-left: 4px solid var(--agent);
}
.sl-body { flex: 1; min-width: 0; }
.sl-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.sl-name { font-size: 15px; font-weight: 600; color: var(--agent); }
.sl-state {
  display: inline-flex; align-items: center; gap: 6px;
  font-size: 12px; color: var(--text-muted);
}
.dot {
  width: 4px; height: 4px; background: var(--agent); border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}
.dot:nth-child(1) { animation-delay: -0.32s; }
.dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes typing { 0%,80%,100% { transform: scale(0); opacity: 0.4; } 40% { transform: scale(1); opacity: 1; } }

.sl-content {
  margin: 0; font-size: 14px; line-height: 1.7; color: var(--text-primary);
  white-space: pre-wrap; word-break: break-word;
  max-height: 9em; overflow: hidden;
}
.caret { display: inline-block; animation: blink 1s steps(2) infinite; color: var(--agent); }
@keyframes blink { 50% { opacity: 0; } }

.spotlight-enter-active, .spotlight-leave-active {
  transition: opacity var(--duration-base) var(--ease-standard),
              transform var(--duration-base) var(--ease-standard),
              max-height var(--duration-base) var(--ease-standard);
  overflow: hidden;
}
.spotlight-enter-from, .spotlight-leave-to { opacity: 0; transform: translateY(-6px); max-height: 0; }
</style>
