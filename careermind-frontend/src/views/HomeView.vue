<template>
  <!-- 未登录：简洁居中 composer + 右上登录 -->
  <div v-if="!user" class="home home-guest">
    <header class="nav">
      <BrandLogo @click="$router.push('/')" />
      <div class="nav-actions">
        <ThemeToggle />
        <BaseButton variant="ghost" size="sm" @click="$router.push('/login')">登录 / 注册</BaseButton>
      </div>
    </header>
    <main class="main">
      <ComposerBlock
        :question="question"
        :team="team"
        :submitting="submitting"
        :greeting="greeting"
        :subtitle="subtitle"
        :teams="teams"
        :suggestions="currentSuggestions"
        @update:question="question = $event"
        @update:team="team = $event as Team"
        @submit="submit"
        @pick="useSuggestion"
      />
    </main>
  </div>

  <!-- 已登录：PageShell（左 Sidebar 里是历史） + 主区只有 composer -->
  <PageShell v-else>
    <div class="home home-shell">
      <main class="main main-shell">
        <ComposerBlock
          :question="question"
          :team="team"
          :submitting="submitting"
          :greeting="greeting"
          :subtitle="subtitle"
          :teams="teams"
          :suggestions="currentSuggestions"
          @update:question="question = $event"
          @update:team="team = $event as Team"
          @submit="submit"
          @pick="useSuggestion"
        />
      </main>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, defineAsyncComponent } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTaskStore } from '@/stores/task'
import { useAgentStore } from '@/stores/agent'
import { message as ElMessage } from '@/utils/naive-discrete'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import PageShell from '@/components/ui/PageShell.vue'

// 提取成子组件，guest 与 shell 版公用
const ComposerBlock = defineAsyncComponent(() => import('@/components/home/ComposerBlock.vue'))

const router = useRouter()
const userStore = useUserStore()
const taskStore = useTaskStore()
const agentStore = useAgentStore()

const user = computed(() => userStore.user)

const question = ref('')
const submitting = ref(false)

type Team = 'career' | 'legal'
const team = ref<Team>('career')
const teams: { key: Team; label: string }[] = [
  { key: 'career', label: '职业团' },
  { key: 'legal',  label: '法律团' },
]

const suggestionsByTeam: Record<Team, string[]> = {
  career: ['要不要从互联网转行到金融？', '跳槽去大厂还是等晋升？', '毕业3年该不该读研？'],
  legal:  ['这份合同对我不利吗？', '被拖欠工资，值得起诉吗？', '房东要涨租，我能怎么谈？'],
}
const currentSuggestions = computed(() => suggestionsByTeam[team.value])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了，需要一个视角？'
  if (h < 12) return '早上好，今天想讨论什么？'
  if (h < 18) return '今天想讨论点什么？'
  return '晚上好，需要一场辩论吗？'
})
const subtitle = '不管是职业决策、合同纠纷、还是人生选择，让多位 AI 专家辩论后给你答案。'

const useSuggestion = (s: string) => {
  question.value = s
}

const submit = async () => {
  const goal = question.value.trim()
  if (!goal) return
  if (!user.value) {
    localStorage.setItem('tempQuestion', goal)
    router.push('/login')
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    await agentStore.fetchAvailableAgents()
    const teamTypes = team.value === 'legal'
      ? ['CONTRACT_REVIEWER','LITIGATION_ANALYST','RIGHTS_DEFENDER','PRACTICAL_COUNSEL','MEDIATION_ADVISOR']
      : ['INDUSTRY_ANALYST','SKILL_ASSESSOR','RISK_WATCHER','OPPORTUNITY_HUNTER','VALUE_EXAMINER']
    const agentIds = agentStore.availableAgents
      .filter(a => a.isPreset && teamTypes.includes(a.type))
      .map(a => a.id)
    if (agentIds.length === 0) {
      ElMessage.error('Agent 未初始化，请稍后再试')
      return
    }
    const title = goal.slice(0, 12)
    const task = await taskStore.createTask({
      title,
      goal,
      background: localStorage.getItem('userBio') || undefined,
      agentIds,
    })
    router.push(`/discussions/${task.id}`)
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  const q = localStorage.getItem('tempQuestion')
  if (q && user.value) { question.value = q; localStorage.removeItem('tempQuestion') }
  if (user.value) taskStore.fetchTasks()
})
</script>

<style scoped>
.home { min-height: 100vh; background: var(--bg-page); display: flex; flex-direction: column; }

/* Guest (未登录) */
.home-guest .nav {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 32px;
  background: rgba(255,255,255,0.78);
  backdrop-filter: saturate(180%) blur(16px);
  -webkit-backdrop-filter: saturate(180%) blur(16px);
  border-bottom: 1px solid var(--border-subtle);
}
html[data-theme="dark"] .home-guest .nav { background: rgba(0,0,0,0.7); }
.nav-actions { display: flex; align-items: center; gap: 8px; }

.main {
  flex: 1;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 40px 20px 80px;
  max-width: 780px; margin: 0 auto; width: 100%;
}

/* Shell (已登录，主区内容) */
.home-shell { height: 100vh; }
.main-shell {
  height: 100%;
  padding: 0 24px;
  max-width: 820px; margin: 0 auto; width: 100%;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
</style>
