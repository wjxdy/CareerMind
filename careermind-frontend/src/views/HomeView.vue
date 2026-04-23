<template>
  <div class="home">
    <header class="nav">
      <BrandLogo @click="$router.push('/')" />
      <div class="nav-actions">
        <ThemeToggle />
        <BaseButton v-if="!user" variant="primary" size="sm" @click="$router.push('/login')">登录</BaseButton>
        <BaseButton v-else variant="primary" size="sm" @click="$router.push('/tasks')">进入咨询台</BaseButton>
      </div>
    </header>

    <section class="hero">
      <p class="eyebrow">多 Agent 职业决策系统</p>
      <h1 class="title">五位 AI 专家<br/>为你的决定辩一场</h1>
      <p class="subtitle">
        每一次人生转折都该经过一场严肃的辩论，<br class="break-lg"/>
        而不是一次 ChatGPT。
      </p>
      <div class="cta">
        <BaseButton variant="primary" size="lg" @click="start">开始咨询</BaseButton>
        <a class="learn-link" href="#experts" @click.prevent="scrollTo('experts')">
          了解五位专家 <span class="arrow">→</span>
        </a>
      </div>
    </section>

    <section id="experts" class="experts">
      <p class="eyebrow">专家团</p>
      <h2 class="sec-title">五种视角，一次讨论</h2>
      <div class="experts-grid">
        <AgentCard v-for="t in expertTypes" :key="t" :type="t" />
      </div>
    </section>

    <section class="flow">
      <p class="eyebrow">工作流程</p>
      <h2 class="sec-title">从分歧到共识，四轮递进</h2>
      <div class="flow-steps">
        <div v-for="(s, i) in flowSteps" :key="i" class="flow-step">
          <div class="step-num">0{{ i + 1 }}</div>
          <h4 class="step-name">{{ s.name }}</h4>
          <p class="step-desc">{{ s.desc }}</p>
        </div>
      </div>
    </section>

    <section class="use-cases">
      <p class="eyebrow">适用场景</p>
      <h2 class="sec-title">它能帮你做什么</h2>
      <div class="cases-grid">
        <div v-for="c in useCases" :key="c.title" class="case">
          <h4 class="case-title">{{ c.title }}</h4>
          <p class="case-desc">{{ c.desc }}</p>
        </div>
      </div>
    </section>

    <section class="final-cta">
      <h2>准备好，开一场辩论？</h2>
      <BaseButton variant="primary" size="lg" @click="start">免费开始</BaseButton>
    </section>

    <footer class="foot">
      <BrandLogo size="sm" />
      <span class="foot-text">CareerMind · 多 Agent 职业决策系统</span>
      <span class="foot-right">© 2026</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import AgentCard from '@/components/agent/AgentCard.vue'
import type { AgentType } from '@/types'

const router = useRouter()
const user = computed(() => useUserStore().user)

const expertTypes: AgentType[] = ['INDUSTRY_ANALYST', 'SKILL_ASSESSOR', 'RISK_WATCHER', 'OPPORTUNITY_HUNTER', 'VALUE_EXAMINER']

const flowSteps = [
  { name: '独立诊断', desc: '五位专家独立给出各自视角的判断，不互相干扰。' },
  { name: '质疑挑战', desc: '专家相互质疑对方的观点，暴露盲区与假设。' },
  { name: '修正完善', desc: '每位专家基于反馈修正或坚持自己的立场。' },
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
.home {
  min-height: 100vh;
  background: var(--bg-page);
  color: var(--text-primary);
}
.nav {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 40px;
  background: rgba(255,255,255,0.72);
  backdrop-filter: saturate(180%) blur(20px);
  -webkit-backdrop-filter: saturate(180%) blur(20px);
  border-bottom: 1px solid var(--border-subtle);
}
html[data-theme="dark"] .nav { background: rgba(0,0,0,0.72); }
.nav-actions { display: flex; align-items: center; gap: 10px; }

/* ---- Hero ---- */
.hero {
  max-width: 980px; margin: 0 auto;
  padding: 140px 24px 120px;
  text-align: center;
}
.eyebrow {
  font-size: 13px; font-weight: 500;
  color: var(--accent);
  letter-spacing: 0.02em;
  margin: 0 0 20px;
  text-transform: none;
}
.title {
  font-size: 88px;
  font-weight: 700;
  letter-spacing: -0.035em;
  line-height: 1.05;
  margin: 0 0 24px;
}
.subtitle {
  font-size: 22px;
  font-weight: 400;
  color: var(--text-secondary);
  letter-spacing: -0.01em;
  line-height: 1.4;
  margin: 0 auto 44px;
  max-width: 680px;
}
.cta {
  display: inline-flex; align-items: center; gap: 24px;
}
.learn-link {
  color: var(--accent);
  font-size: 16px; font-weight: 500;
  display: inline-flex; align-items: center; gap: 4px;
}
.learn-link .arrow { transition: transform var(--duration-fast) var(--ease-standard); }
.learn-link:hover .arrow { transform: translateX(4px); }

/* ---- Sections ---- */
section {
  padding: 110px 40px;
  max-width: 1200px;
  margin: 0 auto;
}
.sec-title {
  font-size: 52px; font-weight: 600;
  letter-spacing: -0.025em;
  margin: 0 0 56px;
}

/* Experts grid — bigger, more spacing */
.experts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(210px, 1fr));
  gap: 14px;
}

/* Flow steps */
.flow-steps {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.flow-step {
  padding: 32px 24px;
  background: var(--bg-elevated);
  border-radius: var(--radius-lg);
  transition: transform var(--duration-base) var(--ease-standard);
}
.flow-step:hover { transform: translateY(-4px); }
.step-num {
  font-family: var(--font-mono);
  font-size: 13px; color: var(--accent); font-weight: 500;
  margin-bottom: 18px; letter-spacing: 0;
}
.step-name { font-size: 22px; margin: 0 0 10px; letter-spacing: -0.015em; }
.step-desc { margin: 0; font-size: 14px; color: var(--text-secondary); line-height: 1.6; }

/* Use cases — cards without borders */
.cases-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 14px;
}
.case {
  padding: 36px 28px;
  background: var(--bg-elevated);
  border-radius: var(--radius-lg);
}
.case-title { font-size: 24px; margin: 0 0 12px; letter-spacing: -0.015em; }
.case-desc  { margin: 0; font-size: 15px; color: var(--text-secondary); line-height: 1.6; }

/* Final CTA */
.final-cta {
  text-align: center;
  padding: 140px 40px;
}
.final-cta h2 {
  font-size: 64px;
  margin: 0 0 32px;
  letter-spacing: -0.03em;
}

/* Footer */
.foot {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px;
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  border-top: 1px solid var(--border-subtle);
  color: var(--text-muted); font-size: 12px;
}
.foot-text { flex: 1; color: var(--text-muted); }

.break-lg { display: inline; }

@media (max-width: 768px) {
  .nav { padding: 12px 20px; }
  .hero { padding: 80px 20px 60px; }
  .title { font-size: 48px; }
  .subtitle { font-size: 17px; }
  .sec-title { font-size: 34px; margin-bottom: 40px; }
  section { padding: 70px 20px; }
  .flow-steps { grid-template-columns: repeat(2, 1fr); }
  .final-cta h2 { font-size: 36px; }
  .break-lg { display: none; }
  .cta { flex-direction: column; gap: 14px; }
}
</style>
