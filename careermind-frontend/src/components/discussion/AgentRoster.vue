<template>
  <div class="roster">
    <div v-for="a in agents" :key="a.id" class="slot" :class="stateFor(a.id)" :data-agent-type="a.type">
      <AgentAvatar :agent-type="a.type" :size="48" :state="stateFor(a.id)" />
      <div class="slot-meta">
        <span class="slot-name">{{ a.name }}</span>
        <span class="slot-role">{{ getAgentMeta(a.type).role }}</span>
      </div>
      <span v-if="a.id === currentSpeakerAgentId" class="badge-speaking">发言中</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import AgentAvatar from '@/components/agent/AgentAvatar.vue'
import { getAgentMeta } from '@/utils/agent-meta'
import type { Agent } from '@/types'

const props = defineProps<{
  agents: Agent[]
  currentSpeakerAgentId?: number | null
}>()

const stateFor = (agentId: number): 'idle' | 'listening' | 'speaking' => {
  if (props.currentSpeakerAgentId == null) return 'idle'
  return agentId === props.currentSpeakerAgentId ? 'speaking' : 'listening'
}
</script>

<style scoped>
.roster {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 10px;
  padding: 16px 20px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-subtle);
}
.slot {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px;
  background: var(--bg-inset); border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md);
  transition: all var(--duration-base) var(--ease-standard);
  position: relative;
  min-width: 0;
}
.slot.speaking { border-color: var(--agent); background: var(--agent-dim); box-shadow: 0 0 0 2px var(--agent-dim); }
.slot.listening { opacity: 0.6; }

.slot-meta { display: flex; flex-direction: column; gap: 2px; min-width: 0; flex: 1; }
.slot-name { font-size: 13px; font-weight: 600; color: var(--text-primary); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.slot-role { font-size: 11px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.badge-speaking {
  position: absolute; top: -6px; right: 8px;
  background: var(--agent); color: var(--accent-contrast);
  font-size: 10px; padding: 1px 8px; border-radius: var(--radius-full);
  font-weight: 500; letter-spacing: 0.04em;
  animation: pulse-badge 1.6s ease-in-out infinite;
}
@keyframes pulse-badge {
  0%,100% { opacity: 1; }
  50% { opacity: 0.6; }
}
</style>
