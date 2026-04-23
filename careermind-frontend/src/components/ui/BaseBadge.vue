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
  padding: 3px 10px; font-size: 12px; font-weight: 500; line-height: 1.4;
  border-radius: var(--radius-full); letter-spacing: -0.005em;
}
.size-md { padding: 5px 12px; font-size: 13px; }
.tone-neutral { background: var(--bg-elevated); color: var(--text-secondary); }
.tone-accent  { background: var(--accent-dim); color: var(--accent); }
.tone-success { background: rgba(48,209,88,0.12); color: #248A3D; }
.tone-warning { background: rgba(255,159,10,0.12); color: #B25000; }
.tone-danger  { background: rgba(255,69,58,0.12); color: #B22219; }
.tone-agent   { background: var(--agent-dim); color: var(--agent); }

html[data-theme="dark"] .tone-success { color: #30D158; }
html[data-theme="dark"] .tone-warning { color: #FF9F0A; }
html[data-theme="dark"] .tone-danger  { color: #FF453A; }
</style>
