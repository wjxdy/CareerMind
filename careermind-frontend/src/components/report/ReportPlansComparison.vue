<template>
  <section class="plans page-break">
    <h2>候选方案对比</h2>
    <div class="grid">
      <div v-for="(p, i) in plans" :key="i" class="plan" :class="{ selected: p.isSelected }">
        <div class="ph">
          <span class="num">方案 {{ i + 1 }}<span v-if="p.isSelected" class="badge">已选</span></span>
          <span class="conf">{{ Math.round(p.confidence * 100) }}%</span>
        </div>
        <h4>{{ p.title }}</h4>
        <p class="desc">{{ p.description }}</p>
        <div v-if="p.milestones?.length" class="sec">
          <h5>里程碑</h5>
          <ul><li v-for="(m, j) in p.milestones" :key="j">{{ m }}</li></ul>
        </div>
        <div v-if="p.risks?.length" class="sec">
          <h5>风险</h5>
          <ul><li v-for="(r, j) in p.risks" :key="j">{{ r }}</li></ul>
        </div>
        <div v-if="p.applicableConditions" class="sec">
          <h5>适用条件</h5>
          <p class="cond">{{ p.applicableConditions }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { Plan } from '@/types'
defineProps<{ plans: Plan[] }>()
</script>

<style scoped>
.plans { padding: 28px 0 24px; }
.plans h2 { font-size: 18px; margin: 0 0 14px; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.plan { padding: 14px; border: 1px solid var(--border-subtle); border-radius: var(--radius-md); break-inside: avoid; background: var(--bg-card); }
.plan.selected { border-color: var(--accent); box-shadow: 0 0 0 1px var(--accent); }
.ph { display: flex; justify-content: space-between; font-size: 12px; color: var(--text-muted); }
.num { display: inline-flex; align-items: center; gap: 6px; }
.badge { background: var(--accent); color: var(--accent-contrast); padding: 1px 6px; border-radius: var(--radius-full); font-size: 10px; }
.plan h4 { font-size: 14px; margin: 6px 0; }
.desc { margin: 0 0 10px; font-size: 13px; color: var(--text-secondary); line-height: 1.5; }
.sec { margin-top: 8px; }
.sec h5 { font-size: 11px; color: var(--text-muted); margin: 0 0 4px; text-transform: uppercase; letter-spacing: 0.04em; }
.sec ul { margin: 0; padding-left: 16px; font-size: 12px; line-height: 1.55; }
.cond { margin: 0; font-size: 12px; color: var(--text-secondary); }
</style>
