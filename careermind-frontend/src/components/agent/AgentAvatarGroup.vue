<template>
  <div class="avatar-group">
    <AgentAvatar
      v-for="(a, i) in visible"
      :key="(a.id ?? i) + '-' + i"
      :agent-type="a.type"
      :size="size"
      class="stack-item"
      :style="{ zIndex: visible.length - i, marginLeft: i === 0 ? '0' : `-${overlap}px` }"
    />
    <span v-if="remaining > 0" class="more" :style="{ width: size + 'px', height: size + 'px' }">+{{ remaining }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AgentAvatar from './AgentAvatar.vue'
import type { Agent } from '@/types'

const props = withDefaults(defineProps<{
  agents: Pick<Agent, 'id' | 'type'>[]
  max?: number
  size?: 30 | 48 | 72
}>(), { max: 5, size: 30 })

const visible = computed(() => props.agents.slice(0, props.max))
const remaining = computed(() => Math.max(0, props.agents.length - props.max))
const overlap = computed(() => Math.round(props.size * 0.3))
</script>

<style scoped>
.avatar-group { display: inline-flex; align-items: center; }
.stack-item { border: 2px solid var(--bg-card); border-radius: 50%; background: var(--bg-card); }
.more { display: inline-flex; align-items: center; justify-content: center;
  margin-left: -9px; border: 2px solid var(--bg-card); background: var(--bg-elevated);
  border-radius: 50%; font-size: 11px; font-weight: 600; color: var(--text-secondary); }
</style>
