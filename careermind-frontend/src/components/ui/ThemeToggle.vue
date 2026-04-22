<template>
  <button class="theme-toggle" :aria-label="label" @click="store.toggle">
    <svg v-if="store.theme === 'light'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
    </svg>
    <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32l1.41 1.41M2 12h2m16 0h2M4.93 19.07l1.41-1.41m11.32-11.32l1.41-1.41"/>
    </svg>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'
const store = useThemeStore()
const label = computed(() => store.theme === 'light' ? '切换到深色' : '切换到浅色')
</script>

<style scoped>
.theme-toggle {
  width: 32px; height: 32px;
  display: inline-flex; align-items: center; justify-content: center;
  background: transparent; border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md); cursor: pointer; color: var(--text-secondary);
  transition: all var(--duration-base) var(--ease-standard);
}
.theme-toggle:hover { background: var(--bg-elevated); color: var(--text-primary); }
</style>
