<template>
  <div class="empty">
    <div v-if="$slots.icon" class="empty-icon"><slot name="icon" /></div>
    <h4 v-if="title" class="empty-title">{{ title }}</h4>
    <p v-if="description" class="empty-desc">{{ description }}</p>
    <div v-if="$slots.action" class="empty-action"><slot name="action" /></div>
  </div>
</template>
<script setup lang="ts">
defineProps<{ title?: string; description?: string }>()
</script>
<style scoped>
.empty { display: flex; flex-direction: column; align-items: center; padding: 48px 24px; text-align: center; color: var(--text-muted); }
.empty-icon { width: 48px; height: 48px; margin-bottom: 12px; color: var(--text-muted); }
.empty-title { margin: 0; font-size: 16px; color: var(--text-primary); }
.empty-desc  { margin: 8px 0 16px; font-size: 13px; }
</style>
