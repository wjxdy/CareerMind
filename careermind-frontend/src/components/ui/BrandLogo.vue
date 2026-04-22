<template>
  <div class="brand-logo" :class="[variant, size]" @click="$emit('click')">
    <svg :width="iconSize" :height="iconSize" viewBox="0 0 32 32" fill="none">
      <circle cx="16" cy="16" r="2" fill="var(--accent)" />
      <circle cx="16" cy="6"  r="2.5" stroke="var(--accent)" stroke-width="1.6" />
      <circle cx="26" cy="13" r="2.5" stroke="var(--accent)" stroke-width="1.6" />
      <circle cx="22" cy="25" r="2.5" stroke="var(--accent)" stroke-width="1.6" />
      <circle cx="10" cy="25" r="2.5" stroke="var(--accent)" stroke-width="1.6" />
      <circle cx="6"  cy="13" r="2.5" stroke="var(--accent)" stroke-width="1.6" />
      <path d="M16 8 L16 14 M24 14 L18 15 M20 22 L17 17 M12 22 L15 17 M8 14 L14 15"
            stroke="var(--accent)" stroke-width="0.8" opacity="0.5" stroke-linecap="round" />
    </svg>
    <span v-if="variant === 'full'" class="wordmark">CareerMind</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
const props = withDefaults(defineProps<{ variant?: 'full' | 'icon'; size?: 'sm' | 'md' | 'lg' }>(),
  { variant: 'full', size: 'md' })
defineEmits<{ (e: 'click'): void }>()
const iconSize = computed(() => ({ sm: 22, md: 28, lg: 40 }[props.size]))
</script>

<style scoped>
.brand-logo { display: inline-flex; align-items: center; gap: 10px; cursor: pointer; user-select: none; }
.wordmark { font-family: var(--font-sans); font-weight: 700; font-size: 18px; color: var(--text-primary); letter-spacing: -0.01em; }
.brand-logo.full.sm .wordmark { font-size: 15px; }
.brand-logo.full.lg .wordmark { font-size: 24px; }
</style>
