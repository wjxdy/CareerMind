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
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'accent'
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
  letter-spacing: -0.01em;
  border: 1px solid transparent;
  border-radius: var(--radius-full);
  cursor: pointer; user-select: none;
  transition: background var(--duration-fast) var(--ease-standard),
              border-color var(--duration-fast) var(--ease-standard),
              color var(--duration-fast) var(--ease-standard),
              opacity var(--duration-fast) var(--ease-standard),
              transform 80ms ease-out;
}
.base-btn:disabled { opacity: 0.4; cursor: not-allowed; }
.base-btn:not(:disabled):active { transform: scale(0.97); }
.block { width: 100%; }

.size-sm { padding: 7px 16px;  font-size: 13px; min-height: 32px; }
.size-md { padding: 10px 22px; font-size: 14px; min-height: 40px; }
.size-lg { padding: 14px 32px; font-size: 16px; min-height: 52px; }

/* Primary: Apple's black pill */
.variant-primary {
  background: var(--cta-bg); color: var(--cta-text); border-color: transparent;
}
.variant-primary:hover:not(:disabled) { background: var(--cta-bg-hover); }

/* Accent: Apple blue pill (for CTAs that need to pop) */
.variant-accent {
  background: var(--accent); color: var(--accent-contrast);
}
.variant-accent:hover:not(:disabled) { background: var(--accent-hover); }

/* Secondary: neutral outline */
.variant-secondary {
  background: transparent; color: var(--text-primary); border-color: var(--border-emphasis);
}
.variant-secondary:hover:not(:disabled) { background: var(--bg-elevated); border-color: var(--border-strong); }

/* Ghost: text-only link */
.variant-ghost { background: transparent; color: var(--text-primary); }
.variant-ghost:hover:not(:disabled) { background: var(--bg-elevated); }

/* Danger */
.variant-danger { background: var(--danger); color: white; }
.variant-danger:hover:not(:disabled) { filter: brightness(1.05); }

.spinner {
  width: 14px; height: 14px; border: 2px solid currentColor; border-top-color: transparent;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
