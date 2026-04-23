<template>
  <section class="prob page-break-avoid">
    <h2>咨询问题与背景</h2>
    <div class="row"><span class="ik">背景</span><p>{{ task.background || '—' }}</p></div>
    <div class="row"><span class="ik">目标</span><p>{{ task.goal || '—' }}</p></div>
    <div class="row"><span class="ik">约束</span><p>{{ task.constraints || '—' }}</p></div>
  </section>
</template>

<script setup lang="ts">
import type { ReportTask } from '@/types/report'
defineProps<{ task: ReportTask }>()
</script>

<style scoped>
.prob { padding: 24px 0; }
.prob h2 { font-size: 18px; margin: 0 0 14px; }
.row { display: grid; grid-template-columns: 60px 1fr; gap: 12px; padding: 10px 0; border-top: 1px solid var(--border-subtle); }
.row:first-of-type { border-top: none; }
.ik { color: var(--text-muted); font-size: 13px; }
.row p { margin: 0; font-size: 13px; line-height: 1.7; color: var(--text-primary); }
</style>
