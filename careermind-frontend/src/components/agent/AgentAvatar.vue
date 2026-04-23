<template>
  <div
    class="agent-avatar"
    :class="[state, `size-${size}`]"
    :data-agent-type="meta.type"
    :title="meta.label"
  >
    <div class="avatar-ring" />
    <svg :width="px" :height="px" viewBox="0 0 72 72" class="avatar-svg">
      <circle cx="36" cy="36" r="34" fill="var(--agent-dim)" stroke="var(--agent)" stroke-width="1.5" />
      <circle cx="27" cy="30" r="3" fill="var(--agent)" />
      <circle cx="45" cy="30" r="3" fill="var(--agent)" />
      <path d="M26 46 Q36 54 46 46" stroke="var(--agent)" stroke-width="2.5" stroke-linecap="round" fill="none" />
      <g transform="translate(48 48)">
        <circle r="11" fill="var(--bg-card)" stroke="var(--agent)" stroke-width="1.5" />
        <g stroke="var(--agent)" stroke-width="1.6" fill="none" stroke-linecap="round" stroke-linejoin="round">
          <g v-if="meta.symbol === 'glasses'">
            <circle cx="-3" cy="0" r="3" /><circle cx="3" cy="0" r="3" /><path d="M-6 0 H-8 M6 0 H8" />
          </g>
          <g v-else-if="meta.symbol === 'ruler'">
            <path d="M-5 -5 L5 5" /><path d="M-5 -2 L-3 0 M-2 -5 L0 -3 M1 -2 L3 0 M4 -5 L6 -3" />
          </g>
          <g v-else-if="meta.symbol === 'shield'">
            <path d="M0 -6 L-5 -3 V2 Q-5 5 0 6 Q5 5 5 2 V-3 Z" />
          </g>
          <g v-else-if="meta.symbol === 'arrow'">
            <path d="M-5 3 L5 -3 M1 -3 L5 -3 L5 1" />
          </g>
          <g v-else-if="meta.symbol === 'question'">
            <path d="M-2 -3 Q-2 -6 0 -6 Q3 -6 3 -3 Q3 0 0 1 V3" /><circle cx="0" cy="5" r="0.6" fill="var(--agent)" />
          </g>
          <!-- book (合同/法条) -->
          <g v-else-if="meta.symbol === 'book'">
            <path d="M-5 -5 H5 V5 H-5 Z" /><path d="M0 -5 V5" /><path d="M-3 -2 H-1 M-3 1 H-1 M1 -2 H3 M1 1 H3" />
          </g>
          <!-- scales (天平) -->
          <g v-else-if="meta.symbol === 'scales'">
            <path d="M0 -5 V5 M-4 5 H4" /><circle cx="-4" cy="0" r="2.5" /><circle cx="4" cy="0" r="2.5" />
          </g>
          <!-- handshake (调解) -->
          <g v-else-if="meta.symbol === 'handshake'">
            <path d="M-5 0 L-2 -2 L1 0 L4 -2 L5 0" /><path d="M-5 0 L-5 3 M5 0 L5 3" />
          </g>
          <!-- gavel (法槌) -->
          <g v-else-if="meta.symbol === 'gavel'">
            <path d="M-5 -5 L1 1 M-2 -2 L-4 0 L-2 2 L0 0 Z" /><path d="M1 1 L5 5" />
          </g>
          <g v-else>
            <circle cx="0" cy="-2" r="2" /><path d="M-4 6 Q0 2 4 6" />
          </g>
        </g>
      </g>
    </svg>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AgentType } from '@/types'
import { getAgentMeta } from '@/utils/agent-meta'

const props = withDefaults(defineProps<{
  agentType: AgentType | string
  size?: 30 | 48 | 72 | 120
  state?: 'idle' | 'listening' | 'speaking' | 'challenging'
}>(), { size: 48, state: 'idle' })

const meta = computed(() => getAgentMeta(props.agentType))
const px = computed(() => props.size)
</script>

<style scoped>
.agent-avatar {
  position: relative; display: inline-block; line-height: 0;
  transition: transform var(--duration-base) var(--ease-standard),
              filter var(--duration-base) var(--ease-standard),
              opacity var(--duration-base) var(--ease-standard);
}
.size-30  { width: 30px;  height: 30px; }
.size-48  { width: 48px;  height: 48px; }
.size-72  { width: 72px;  height: 72px; }
.size-120 { width: 120px; height: 120px; }
.avatar-svg { display: block; width: 100%; height: 100%; border-radius: 50%; }

.avatar-ring {
  position: absolute; inset: -4px; border-radius: 50%; pointer-events: none;
  border: 2px solid transparent;
  transition: all var(--duration-base) var(--ease-standard);
}

.listening { filter: grayscale(0.55) opacity(0.75); animation: breathe 3s ease-in-out infinite; }
@keyframes breathe { 0%,100% { transform: scale(1); } 50% { transform: scale(1.015); } }

.speaking { transform: scale(1.05); }
.speaking .avatar-ring {
  border-color: var(--agent);
  box-shadow: 0 0 0 4px var(--agent-dim);
  animation: pulse 1.6s ease-in-out infinite;
}
@keyframes pulse {
  0%   { box-shadow: 0 0 0 0 var(--agent-dim); }
  70%  { box-shadow: 0 0 0 12px rgba(0,0,0,0); }
  100% { box-shadow: 0 0 0 0 rgba(0,0,0,0); }
}

.challenging { transform: rotate(-3deg) scale(1.02); }
.challenging .avatar-ring { border-color: var(--danger); }
</style>
