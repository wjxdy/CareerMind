<template>
  <section class="cover page-break">
    <div class="cover-top"><BrandLogo size="lg" /></div>
    <div class="cover-mid">
      <p class="kicker">职业决策报告</p>
      <h1>{{ title }}</h1>
      <p class="subtitle">由 5 位 AI 专家经 4 轮辩论生成</p>
    </div>
    <div class="cover-meta">
      <div class="meta-line">
        <div class="meta-item"><span class="meta-k">日期</span><span>{{ formatDate(date) }}</span></div>
        <div v-if="username" class="meta-item"><span class="meta-k">用户</span><span>{{ username }}</span></div>
      </div>
      <div class="conv-circle">
        <div class="num">{{ Math.round(convergence * 100) }}%</div>
        <div class="lbl">最终共识度</div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import BrandLogo from '@/components/ui/BrandLogo.vue'
import dayjs from 'dayjs'
defineProps<{ title: string; date?: string; username?: string; convergence: number }>()
const formatDate = (d?: string) => d ? dayjs(d).format('YYYY-MM-DD') : ''
</script>

<style scoped>
.cover {
  display: flex; flex-direction: column;
  min-height: 90vh; padding: 48px;
  background: linear-gradient(135deg, #EFF6FF 0%, #ffffff 60%);
  border-radius: var(--radius-lg);
}
.cover-top { display: flex; }
.cover-mid { flex: 1; display: flex; flex-direction: column; justify-content: center; }
.kicker { font-size: 13px; color: var(--accent); font-weight: 500; letter-spacing: 0.06em; text-transform: uppercase; margin: 0 0 16px; }
.cover-mid h1 { font-size: 48px; line-height: 1.15; margin: 0; letter-spacing: -0.02em; }
.subtitle { font-size: 16px; color: var(--text-secondary); margin: 16px 0 0; }
.cover-meta {
  display: flex; align-items: center; justify-content: space-between;
  padding-top: 24px; border-top: 1px solid var(--border-subtle);
}
.meta-line { display: flex; gap: 32px; }
.meta-item { display: flex; flex-direction: column; gap: 2px; font-size: 13px; color: var(--text-primary); }
.meta-k { font-size: 11px; color: var(--text-muted); }
.conv-circle {
  display: inline-flex; flex-direction: column; align-items: center;
  padding: 14px 20px; border: 2px solid var(--accent); border-radius: 9999px;
}
.num { font-size: 24px; font-weight: 700; color: var(--accent); line-height: 1.1; }
.lbl { font-size: 11px; color: var(--text-muted); }
</style>
