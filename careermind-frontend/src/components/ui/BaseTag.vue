<template>
  <span class="base-tag" @click="$emit('click')">
    <slot />
    <button v-if="removable" class="remove" @click.stop="$emit('remove')">×</button>
  </span>
</template>

<script setup lang="ts">
defineProps<{ removable?: boolean }>()
defineEmits<{ (e: 'click'): void; (e: 'remove'): void }>()
</script>

<style scoped>
.base-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px 10px; font-size: 13px;
  background: var(--bg-elevated); color: var(--text-secondary);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-sm);
  cursor: pointer; transition: all var(--duration-fast) var(--ease-standard);
}
.base-tag:hover { border-color: var(--border-emphasis); color: var(--text-primary); }
.remove { background: none; border: none; color: inherit; cursor: pointer; font-size: 16px; line-height: 1; padding: 0 2px; }
</style>
