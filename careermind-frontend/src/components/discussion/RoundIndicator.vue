<template>
  <div class="round-indicator">
    <div class="round-steps">
      <div
        v-for="round in 4"
        :key="round"
        class="round-step"
        :class="{ active: round <= currentRound, current: round === currentRound }"
      >
        <div class="step-number">{{ round }}</div>
        <div class="step-label">{{ getRoundLabel(round) }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  currentRound: number
}>()

const getRoundLabel = (round: number) => {
  const labels: Record<number, string> = {
    1: '独立诊断',
    2: '质疑挑战',
    3: '修正完善',
    4: '最终陈述',
  }
  return labels[round] || ''
}
</script>

<style scoped>
.round-indicator {
  padding: 8px 16px;
  background: #f3f4f6;
  border-radius: 8px;
}

.round-steps {
  display: flex;
  gap: 16px;
}

.round-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  opacity: 0.4;
}

.round-step.active {
  opacity: 1;
}

.round-step.current .step-number {
  background: #0ea5e9;
  color: white;
  transform: scale(1.1);
}

.step-number {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #d1d5db;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  color: white;
  transition: all 0.3s;
}

.step-label {
  font-size: 11px;
  color: #6b7280;
  white-space: nowrap;
}

.round-step.active .step-label {
  color: #374151;
  font-weight: 500;
}
</style>
