<template>
  <section class="ap page-break">
    <h2>行动清单</h2>
    <div class="cols">
      <div class="col">
        <h3>未来 7 天</h3>
        <ul v-if="plan.day7?.length"><li v-for="(s, i) in plan.day7" :key="i">{{ s }}</li></ul>
        <p v-else class="empty">尚无</p>
      </div>
      <div class="col">
        <h3>未来 30 天</h3>
        <ul v-if="plan.day30?.length"><li v-for="(s, i) in plan.day30" :key="i">{{ s }}</li></ul>
        <p v-else class="empty">尚无</p>
      </div>
      <div class="col">
        <h3>未来 90 天</h3>
        <ul v-if="plan.day90?.length"><li v-for="(s, i) in plan.day90" :key="i">{{ s }}</li></ul>
        <p v-else class="empty">尚无</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ActionPlan } from '@/types/report'
defineProps<{ plan: ActionPlan }>()
</script>

<style scoped>
.ap { padding: 24px 0; }
.ap h2 { font-size: 18px; margin: 0 0 14px; }
.cols { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 12px; }
.col { padding: 14px; background: var(--bg-inset); border-radius: var(--radius-md); break-inside: avoid; }
.col h3 { font-size: 13px; color: var(--accent); margin: 0 0 8px; }
.col ul { margin: 0; padding-left: 18px; font-size: 12px; line-height: 1.65; }
.col li { margin-bottom: 4px; }
.empty { font-size: 12px; color: var(--text-muted); margin: 0; }
</style>
