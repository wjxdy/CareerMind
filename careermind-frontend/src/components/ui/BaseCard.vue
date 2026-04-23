<template>
  <div :class="['base-card', { hoverable, inset, elevated, flat }]" :style="{ padding }">
    <div v-if="$slots.header" class="card-header"><slot name="header" /></div>
    <slot />
    <div v-if="$slots.footer" class="card-footer"><slot name="footer" /></div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  padding?: string
  hoverable?: boolean
  inset?: boolean
  elevated?: boolean
  flat?: boolean
}>(), { padding: '24px' })
</script>

<style scoped>
.base-card {
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  transition: transform var(--duration-base) var(--ease-standard),
              box-shadow var(--duration-base) var(--ease-standard);
}
/* Default card: very subtle shadow, no border */
.base-card:not(.flat):not(.inset) { box-shadow: var(--shadow-sm); }

.base-card.elevated { box-shadow: var(--shadow-md); }
.base-card.inset { background: var(--bg-elevated); box-shadow: none; }
.base-card.flat { box-shadow: none; border: 1px solid var(--border-subtle); }

.hoverable { cursor: pointer; }
.hoverable:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }

.card-header { margin-bottom: 16px; font-weight: 600; font-size: 17px; letter-spacing: -0.015em; }
.card-footer { margin-top: 16px; padding-top: 16px; border-top: 1px solid var(--border-subtle); }
</style>
