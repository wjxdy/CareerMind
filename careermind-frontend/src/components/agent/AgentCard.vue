<template>
  <div class="agent-card" :data-agent-type="meta.type">
    <AgentAvatar :agent-type="type" :size="72" state="idle" class="card-avatar" />
    <h4 class="card-name">{{ name || meta.label }}</h4>
    <p class="card-role">{{ role || meta.role }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AgentAvatar from './AgentAvatar.vue'
import { getAgentMeta } from '@/utils/agent-meta'
import type { AgentType } from '@/types'
const props = defineProps<{ type: AgentType | string; name?: string; role?: string }>()
const meta = computed(() => getAgentMeta(props.type))
</script>

<style scoped>
.agent-card {
  display: flex; flex-direction: column; align-items: center; text-align: center;
  padding: 24px 20px; background: var(--bg-card);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-lg);
  transition: all var(--duration-base) var(--ease-standard);
}
.agent-card:hover {
  border-color: var(--agent); transform: translateY(-4px); box-shadow: var(--shadow-md);
}
.card-avatar { margin-bottom: 16px; }
.card-name  { margin: 0 0 6px; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.card-role  { margin: 0; font-size: 13px; color: var(--text-secondary); line-height: 1.5; }
</style>
