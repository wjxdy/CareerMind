<template>
  <svg class="challenge-flow" :width="w" :height="h" viewBox="0 0 1000 600" preserveAspectRatio="none">
    <defs>
      <linearGradient :id="gradId" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%"  stop-color="var(--danger)" stop-opacity="0" />
        <stop offset="50%" stop-color="var(--danger)" stop-opacity="1" />
        <stop offset="100%" stop-color="var(--danger)" stop-opacity="0" />
      </linearGradient>
    </defs>
    <path :d="path" fill="none" :stroke="`url(#${gradId})`" stroke-width="2" stroke-linecap="round"
          stroke-dasharray="8 4" class="flow-stroke" />
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ from: { x: number; y: number }; to: { x: number; y: number }; w?: number; h?: number }>()
const w = computed(() => props.w ?? 1000)
const h = computed(() => props.h ?? 600)
const path = computed(() => {
  const { from, to } = props
  const mx = (from.x + to.x) / 2; const my = (from.y + to.y) / 2 - 40
  return `M ${from.x} ${from.y} Q ${mx} ${my} ${to.x} ${to.y}`
})
const gradId = `flow-${Math.random().toString(36).slice(2, 8)}`
</script>

<style scoped>
.challenge-flow { position: absolute; inset: 0; pointer-events: none; overflow: visible; }
.flow-stroke { animation: flow 1.2s linear; }
@keyframes flow { from { stroke-dashoffset: 80; } to { stroke-dashoffset: 0; } }
</style>
