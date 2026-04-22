<template>
  <span :class="['base-badge', `tone-${tone}`, `size-${size}`]"><slot /></span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  tone?: 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'agent'
  size?: 'sm' | 'md'
}>(), { tone: 'neutral', size: 'sm' })
</script>

<style scoped>
.base-badge {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 8px; font-size: 12px; font-weight: 500; line-height: 1.4;
  border-radius: var(--radius-full);
}
.size-md { padding: 4px 10px; font-size: 13px; }
.tone-neutral { background: var(--bg-elevated); color: var(--text-secondary); }
.tone-accent  { background: var(--accent-dim); color: var(--accent); }
.tone-success { background: rgba(16,185,129,0.1);  color: var(--success); }
.tone-warning { background: rgba(245,158,11,0.1);  color: var(--warning); }
.tone-danger  { background: rgba(239,68,68,0.1);   color: var(--danger); }
.tone-agent   { background: var(--agent-dim);      color: var(--agent); }
</style>
