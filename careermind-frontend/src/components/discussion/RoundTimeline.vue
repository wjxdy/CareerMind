<template>
  <div class="round-timeline">
    <div v-for="r in rounds" :key="r.num" class="round-step" :class="statusOf(r.num)">
      <div class="dot"><span v-if="statusOf(r.num) === 'done'">✓</span><span v-else>{{ r.num }}</span></div>
      <span class="label">{{ r.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{ current: number; completed?: number[] }>(),
  { completed: () => [] as number[] })

const rounds = [
  { num: 1, label: '独立诊断' },
  { num: 2, label: '质疑挑战' },
  { num: 3, label: '修正完善' },
  { num: 4, label: '最终陈述' },
]

const statusOf = (n: number) => {
  if (props.completed.includes(n) || n < props.current) return 'done'
  if (n === props.current) return 'active'
  return 'idle'
}
</script>

<style scoped>
.round-timeline { display: inline-flex; align-items: center; gap: 6px; }
.round-step { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px 4px 4px; border-radius: var(--radius-full);
  font-size: 12px; color: var(--text-muted); transition: all var(--duration-base) var(--ease-standard); }
.round-step + .round-step::before {
  content: ''; display: block; width: 12px; height: 1px; background: var(--border-emphasis); margin: 0 2px;
}
.dot { width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center;
  background: var(--bg-elevated); color: var(--text-muted); border: 1px solid var(--border-emphasis);
  border-radius: 50%; font-size: 11px; font-weight: 600; }
.label { white-space: nowrap; }

.active .dot  { background: var(--accent); color: var(--accent-contrast); border-color: var(--accent); }
.active       { color: var(--text-primary); font-weight: 500; background: var(--accent-dim); }
.done .dot    { background: var(--success); color: white; border-color: var(--success); }
.done         { color: var(--text-secondary); }
</style>
