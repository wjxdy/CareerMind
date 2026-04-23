<template>
  <div class="thermo">
    <div class="track">
      <div class="indicator" :style="{ left: pct + '%' }" />
    </div>
    <div class="labels">
      <span>分歧</span>
      <span class="val">{{ Math.round(consensusPct) }}% 共识</span>
      <span>共识</span>
    </div>
    <transition name="bump">
      <div v-if="bump" class="bump-text">{{ bump }}</div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = defineProps<{ divergence: number; deltaText?: string | null }>()

const consensusPct = computed(() => 100 * (1 - Math.max(0, Math.min(1, props.divergence))))
const pct = consensusPct
const bump = ref<string | null>(null)

watch(() => props.deltaText, (v) => {
  if (v) {
    bump.value = v
    setTimeout(() => (bump.value = null), 1400)
  }
})
</script>

<style scoped>
.thermo { position: relative; width: 240px; }
.track {
  position: relative; height: 6px; border-radius: 9999px;
  background: linear-gradient(90deg, var(--danger) 0%, var(--warning) 50%, var(--success) 100%);
}
.indicator {
  position: absolute; top: -3px;
  width: 12px; height: 12px; border-radius: 50%;
  background: var(--bg-card); border: 2px solid var(--text-primary);
  transform: translateX(-50%);
  transition: left var(--duration-slow) var(--ease-emphasized);
  box-shadow: var(--shadow-sm);
}
.labels {
  display: flex; justify-content: space-between;
  margin-top: 6px; font-size: 11px; color: var(--text-muted);
}
.val { color: var(--text-primary); font-weight: 500; }

.bump-text {
  position: absolute; left: 50%; bottom: 100%; transform: translateX(-50%);
  background: var(--accent); color: var(--accent-contrast);
  font-size: 11px; padding: 2px 8px; border-radius: var(--radius-full);
  white-space: nowrap; margin-bottom: 6px;
}
.bump-enter-active, .bump-leave-active { transition: all var(--duration-base) var(--ease-emphasized); }
.bump-enter-from, .bump-leave-to { opacity: 0; transform: translate(-50%, 4px); }
</style>
