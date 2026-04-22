<template>
  <div class="skeleton" :class="shape" :style="{ width, height }" />
</template>
<script setup lang="ts">
withDefaults(defineProps<{ shape?: 'line' | 'circle' | 'block'; width?: string; height?: string }>(),
  { shape: 'line', width: '100%', height: '14px' })
</script>
<style scoped>
.skeleton { background: linear-gradient(90deg, var(--bg-elevated) 0%, var(--bg-inset) 50%, var(--bg-elevated) 100%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite; border-radius: var(--radius-sm); }
.circle { border-radius: 9999px; }
.block  { border-radius: var(--radius-md); }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
</style>
