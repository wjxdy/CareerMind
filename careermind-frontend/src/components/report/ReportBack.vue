<template>
  <section class="back page-break">
    <BrandLogo size="md" />
    <p class="msg">本报告由 CareerMind 多 Agent 决策系统生成</p>
    <p class="muted">版权 © 2026 CareerMind · 仅供个人决策参考，最终决定由你自己负责</p>
  </section>
</template>

<script setup lang="ts">
import BrandLogo from '@/components/ui/BrandLogo.vue'
</script>

<style scoped>
.back {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 14px; min-height: 60vh; padding: 60px 40px;
}
.msg { font-size: 14px; color: var(--text-primary); margin: 0; }
.muted { font-size: 12px; color: var(--text-muted); margin: 0; text-align: center; }
</style>
