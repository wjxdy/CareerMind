<template>
  <div class="home">
    <header class="nav">
      <BrandLogo @click="$router.push('/')" />
      <div class="nav-actions">
        <ThemeToggle />
        <BaseButton v-if="!user" variant="primary" size="sm" @click="$router.push('/login')">登录 / 注册</BaseButton>
        <BaseButton v-else variant="secondary" size="sm" @click="$router.push('/tasks')">进入咨询台</BaseButton>
      </div>
    </header>

    <section class="hero">
      <h1 class="title">让五位 AI 专家<br/>为你的人生辩一场</h1>
      <p class="subtitle">每一次重要决定，都该经过一场严肃的辩论。</p>
      <div class="cta">
        <BaseButton variant="primary" size="lg" @click="start">开始咨询 →</BaseButton>
        <BaseButton variant="ghost"   size="lg" @click="scrollTo('experts')">了解五位专家</BaseButton>
      </div>
    </section>

    <section id="experts" class="experts">
      <h2 class="sec-title">五位 AI 专家，五种视角</h2>
      <div class="experts-grid">
        <AgentCard v-for="t in expertTypes" :key="t" :type="t" />
      </div>
    </section>

    <section class="flow">
      <h2 class="sec-title">4 轮讨论，从分歧到共识</h2>
      <div class="flow-steps">
        <div v-for="(s, i) in flowSteps" :key="i" class="flow-step">
          <div class="step-num">{{ i + 1 }}</div>
          <h4 class="step-name">{{ s.name }}</h4>
          <p class="step-desc">{{ s.desc }}</p>
        </div>
      </div>
    </section>

    <section class="use-cases">
      <h2 class="sec-title">它能帮你做什么</h2>
      <div class="cases-grid">
        <BaseCard v-for="c in useCases" :key="c.title" hoverable>
          <h4 class="case-title">{{ c.title }}</h4>
          <p class="case-desc">{{ c.desc }}</p>
        </BaseCard>
      </div>
    </section>

    <footer class="foot">
      <BrandLogo size="sm" />
      <span class="foot-text">CareerMind · 多 Agent 职业决策系统 · 2026</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import AgentCard from '@/components/agent/AgentCard.vue'
import type { AgentType } from '@/types'

const router = useRouter()
const user = computed(() => useUserStore().user)

const expertTypes: AgentType[] = ['INDUSTRY_ANALYST', 'SKILL_ASSESSOR', 'RISK_WATCHER', 'OPPORTUNITY_HUNTER', 'VALUE_EXAMINER']

const flowSteps = [
  { name: '独立诊断', desc: '5 位专家独立给出各自视角下的判断，不互相干扰。' },
  { name: '质疑挑战', desc: '专家互相质疑观点，暴露盲区与假设。' },
  { name: '修正完善', desc: '每位专家根据质疑修正或坚持自己的立场。' },
  { name: '最终陈述', desc: '汇总为候选方案，并标注共识度与适用条件。' },
]

const useCases = [
  { title: '转行抉择',     desc: '在留守与转行之间权衡能力迁移、机会成本与风险。' },
  { title: '晋升 vs 跳槽', desc: '评估内部晋升路径与外部跳槽窗口的真实价值。' },
  { title: '升学 / 读研',   desc: '考察是否值得为深造让渡 2-3 年时间。' },
]

const start = () => router.push(user.value ? '/tasks' : '/login')
const scrollTo = (id: string) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
</script>

<style scoped>
.home { min-height: 100vh; background: var(--bg-page); color: var(--text-primary); }
.nav {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 40px; background: rgba(250,250,250,0.7); backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border-subtle);
}
html[data-theme="dark"] .nav { background: rgba(9,9,11,0.7); }
.nav-actions { display: flex; align-items: center; gap: 10px; }

.hero { max-width: 820px; margin: 0 auto; padding: 96px 24px 80px; text-align: center; }
.title { font-size: 56px; line-height: 1.15; letter-spacing: -0.02em; font-weight: 700; margin-bottom: 20px; }
.subtitle { font-size: 18px; color: var(--text-secondary); margin: 0 0 32px; }
.cta { display: inline-flex; gap: 12px; }

section { padding: 80px 40px; max-width: 1120px; margin: 0 auto; }
.sec-title { font-size: 28px; font-weight: 600; margin-bottom: 36px; text-align: center; }

.experts-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }
.flow-steps   { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.flow-step    { padding: 20px; border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); background: var(--bg-card); }
.step-num     { width: 28px; height: 28px; border-radius: 50%; background: var(--accent-dim); color: var(--accent); display: inline-flex; align-items: center; justify-content: center; font-weight: 600; margin-bottom: 12px; }
.step-name    { margin: 0 0 6px; font-size: 15px; }
.step-desc    { margin: 0; font-size: 13px; color: var(--text-secondary); line-height: 1.6; }

.cases-grid   { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
.case-title   { margin: 0 0 8px; font-size: 16px; }
.case-desc    { margin: 0; font-size: 13px; color: var(--text-secondary); line-height: 1.6; }

.foot { max-width: 1120px; margin: 0 auto; padding: 40px; display: flex; align-items: center; justify-content: space-between;
  border-top: 1px solid var(--border-subtle); color: var(--text-muted); font-size: 13px; }
@media (max-width: 768px) {
  .title { font-size: 40px; } .flow-steps { grid-template-columns: repeat(2, 1fr); } .nav { padding: 12px 20px; } section { padding: 48px 20px; }
}
</style>
