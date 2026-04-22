<template>
  <button
    :class="['base-btn', `variant-${variant}`, `size-${size}`, { loading, block }]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="spinner" />
    <slot name="icon-left" />
    <slot />
    <slot name="icon-right" />
  </button>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
  block?: boolean
}>(), { variant: 'secondary', size: 'md' })
defineEmits<{ (e: 'click', ev: MouseEvent): void }>()
</script>

<style scoped>
.base-btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  font-family: var(--font-sans); font-weight: 500; line-height: 1;
  border: 1px solid transparent; border-radius: var(--radius-md);
  cursor: pointer; user-select: none;
  transition: background var(--duration-fast) var(--ease-standard),
              border-color var(--duration-fast) var(--ease-standard),
              color var(--duration-fast) var(--ease-standard),
              transform var(--duration-fast) var(--ease-standard);
}
.base-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.base-btn:not(:disabled):active { transform: translateY(1px); }
.block { width: 100%; }

.size-sm { padding: 6px 12px;  font-size: 13px; min-height: 30px; }
.size-md { padding: 8px 16px;  font-size: 14px; min-height: 36px; }
.size-lg { padding: 12px 24px; font-size: 15px; min-height: 44px; }

.variant-primary {
  background: var(--accent); color: var(--accent-contrast); border-color: var(--accent);
}
.variant-primary:hover:not(:disabled) { background: var(--accent-hover); border-color: var(--accent-hover); }

.variant-secondary {
  background: var(--bg-card); color: var(--text-primary); border-color: var(--border-emphasis);
}
.variant-secondary:hover:not(:disabled) { background: var(--bg-elevated); }

.variant-ghost { background: transparent; color: var(--text-secondary); }
.variant-ghost:hover:not(:disabled) { background: var(--bg-elevated); color: var(--text-primary); }

.variant-danger { background: var(--danger); color: white; border-color: var(--danger); }
.variant-danger:hover:not(:disabled) { background: #dc2626; }

.spinner {
  width: 12px; height: 12px; border: 2px solid currentColor; border-top-color: transparent;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
