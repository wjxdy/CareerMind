<template>
  <div class="composer-block">
    <div class="greet">
      <p class="eyebrow">多位 AI 专家 · 协同咨询</p>
      <h1 class="hello">{{ greeting }}</h1>
      <p class="muted">{{ subtitle }}</p>
    </div>

    <div class="composer" :class="{ focused: isFocused }">
      <textarea
        ref="textareaRef"
        :value="question"
        class="input"
        :placeholder="placeholder"
        rows="1"
        @input="onInput"
        @focus="isFocused = true"
        @blur="isFocused = false"
        @keydown.enter.exact.prevent="$emit('submit')"
      />
      <div class="composer-bar">
        <div class="team-pills">
          <button v-for="t in teams" :key="t.key" class="pill" :class="{ on: team === t.key }" @click="$emit('update:team', t.key)">
            {{ t.label }}
          </button>
        </div>
        <button class="submit" :class="{ active: canSubmit }" :disabled="!canSubmit" @click="$emit('submit')" title="开始讨论">
          <svg v-if="!submitting" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 19V5M5 12l7-7 7 7"/>
          </svg>
          <span v-else class="spinner" />
        </button>
      </div>
    </div>

    <div class="suggestions">
      <p class="hint">试试这些：</p>
      <div class="chips">
        <button v-for="s in suggestions" :key="s" class="chip" @click="$emit('pick', s)">{{ s }}</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'

const props = defineProps<{
  question: string
  team: string
  submitting?: boolean
  greeting: string
  subtitle: string
  teams: { key: string; label: string }[]
  suggestions: string[]
}>()
const emit = defineEmits<{
  (e: 'update:question', v: string): void
  (e: 'update:team', v: string): void
  (e: 'submit'): void
  (e: 'pick', v: string): void
}>()

const textareaRef = ref<HTMLTextAreaElement>()
const isFocused = ref(false)

const placeholders = [
  '描述你的问题，让多位专家为你辩论…',
  '输入你遇到的困惑，比如"要不要裸辞"…',
  '把决策难题说给专家团，让他们先辩一辩…',
]
const placeholder = placeholders[Math.floor(Math.random() * placeholders.length)]

const canSubmit = computed(() => !props.submitting && props.question.trim().length >= 3)

const onInput = (e: Event) => {
  const t = e.target as HTMLTextAreaElement
  emit('update:question', t.value)
  autoGrow()
}
const autoGrow = () => {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 240) + 'px'
}
watch(() => props.question, () => nextTick(autoGrow))
onMounted(() => nextTick(autoGrow))
</script>

<style scoped>
.composer-block { width: 100%; max-width: 720px; display: flex; flex-direction: column; align-items: center; }

.greet { text-align: center; margin-bottom: 32px; }
.eyebrow { font-size: 13px; color: var(--accent); font-weight: 500; margin: 0 0 14px; letter-spacing: 0.02em; }
.hello { font-size: 40px; font-weight: 600; letter-spacing: -0.025em; margin: 0 0 12px; line-height: 1.15; }
.muted { margin: 0; font-size: 15px; color: var(--text-secondary); line-height: 1.5; max-width: 540px; }

.composer {
  width: 100%; max-width: 720px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 22px;
  padding: 16px 16px 10px;
  box-shadow: var(--shadow-sm);
  transition: all var(--duration-base) var(--ease-standard);
}
.composer.focused { border-color: var(--border-emphasis); box-shadow: var(--shadow-md); }

.input {
  width: 100%;
  font-family: var(--font-sans);
  font-size: 16px; line-height: 1.55;
  color: var(--text-primary);
  background: transparent;
  border: none; outline: none; resize: none;
  padding: 4px 4px 8px;
  max-height: 240px; min-height: 28px;
  letter-spacing: -0.005em;
}
.input::placeholder { color: var(--text-muted); }

.composer-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-top: 6px; }
.team-pills { display: inline-flex; gap: 4px; background: var(--bg-elevated); padding: 3px; border-radius: var(--radius-full); }
.pill {
  padding: 5px 14px; background: transparent; border: none;
  font-size: 12.5px; font-weight: 500; color: var(--text-secondary);
  border-radius: var(--radius-full); cursor: pointer;
  transition: all var(--duration-fast) var(--ease-standard);
}
.pill.on { background: var(--bg-card); color: var(--text-primary); box-shadow: var(--shadow-sm); }

.submit {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--bg-elevated); color: var(--text-muted);
  border: none; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
  transition: all var(--duration-fast) var(--ease-standard);
}
.submit:disabled { cursor: not-allowed; }
.submit.active { background: var(--cta-bg); color: var(--cta-text); }
.submit.active:hover { transform: scale(1.06); }
.spinner {
  width: 14px; height: 14px; border: 2px solid currentColor; border-top-color: transparent;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.suggestions { margin-top: 22px; text-align: center; max-width: 720px; width: 100%; }
.hint { font-size: 12px; color: var(--text-muted); margin: 0 0 10px; }
.chips { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; }
.chip {
  padding: 8px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  font-size: 13px; color: var(--text-primary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-standard);
  letter-spacing: -0.005em;
}
.chip:hover { background: var(--bg-elevated); border-color: var(--border-emphasis); transform: translateY(-1px); }

@media (max-width: 680px) {
  .hello { font-size: 30px; }
  .muted { font-size: 14px; }
  .team-pills .pill { padding: 4px 10px; font-size: 12px; }
}
</style>
