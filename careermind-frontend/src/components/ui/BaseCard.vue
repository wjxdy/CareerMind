<template>
  <div :class="['base-card', { hoverable, inset }]" :style="{ padding }">
    <div v-if="$slots.header" class="card-header"><slot name="header" /></div>
    <slot />
    <div v-if="$slots.footer" class="card-footer"><slot name="footer" /></div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ padding?: string; hoverable?: boolean; inset?: boolean }>(),
  { padding: '20px' })
</script>

<style scoped>
.base-card {
  background: var(--bg-card); border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  transition: border-color var(--duration-base) var(--ease-standard),
              box-shadow var(--duration-base) var(--ease-standard),
              transform var(--duration-base) var(--ease-standard);
}
.base-card.inset { background: var(--bg-inset); }
.hoverable { cursor: pointer; }
.hoverable:hover {
  border-color: var(--border-emphasis); box-shadow: var(--shadow-md); transform: translateY(-2px);
}
.card-header { margin-bottom: 12px; font-weight: 600; font-size: 15px; }
.card-footer { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border-subtle); }
</style>
