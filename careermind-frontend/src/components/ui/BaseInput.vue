<template>
  <div class="input-wrap" :class="{ error }">
    <label v-if="label" class="input-label">{{ label }}</label>
    <textarea
      v-if="textarea"
      class="input"
      :rows="rows"
      :value="String(modelValue ?? '')"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
      @keyup="$emit('keyup', $event)"
    />
    <input
      v-else
      class="input"
      :type="type"
      :value="String(modelValue ?? '')"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      @keyup="$emit('keyup', $event)"
    />
    <p v-if="error" class="hint">{{ error }}</p>
    <p v-else-if="hint" class="hint muted">{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue: string | number
  label?: string
  placeholder?: string
  type?: string
  textarea?: boolean
  rows?: number
  hint?: string
  error?: string
  disabled?: boolean
}>(), { type: 'text', rows: 4 })
defineEmits<{
  (e: 'update:modelValue', v: string): void
  (e: 'keyup', ev: KeyboardEvent): void
}>()
</script>

<style scoped>
.input-wrap { display: flex; flex-direction: column; gap: 8px; }
.input-label { font-size: 13px; font-weight: 500; color: var(--text-secondary); letter-spacing: -0.005em; }
.input {
  width: 100%;
  padding: 12px 14px;
  font-size: 15px;
  font-family: inherit;
  background: var(--bg-elevated);
  color: var(--text-primary);
  border: 1px solid transparent;
  border-radius: var(--radius-md);
  transition: background var(--duration-fast) var(--ease-standard),
              border-color var(--duration-fast) var(--ease-standard),
              box-shadow var(--duration-fast) var(--ease-standard);
  outline: none;
}
textarea.input { resize: vertical; min-height: 100px; line-height: 1.6; }
.input::placeholder { color: var(--text-muted); }
.input:hover { background: var(--bg-card); border-color: var(--border-subtle); }
.input:focus { background: var(--bg-card); border-color: var(--accent); box-shadow: 0 0 0 4px var(--accent-dim); }
.input:disabled { opacity: 0.5; cursor: not-allowed; }
.error .input { border-color: var(--danger); box-shadow: 0 0 0 4px rgba(255,69,58,0.10); }
.hint { font-size: 12px; color: var(--danger); margin: 0; }
.hint.muted { color: var(--text-muted); }
</style>
