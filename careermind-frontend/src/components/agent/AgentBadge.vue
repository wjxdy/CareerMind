<template>
  <span class="agent-badge" :data-agent-type="meta.type">
    <AgentAvatar :agent-type="type" :size="30" state="idle" />
    <span class="agent-name">{{ displayName || meta.label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AgentAvatar from './AgentAvatar.vue'
import { getAgentMeta } from '@/utils/agent-meta'
import type { AgentType } from '@/types'

const props = defineProps<{ type: AgentType | string; displayName?: string }>()
const meta = computed(() => getAgentMeta(props.type))
</script>

<style scoped>
.agent-badge { display: inline-flex; align-items: center; gap: 8px;
  padding: 4px 10px 4px 4px; background: var(--agent-dim); color: var(--agent);
  border-radius: var(--radius-full); font-size: 13px; font-weight: 500; }
.agent-name { line-height: 1; }
</style>
