<template>
  <div class="home">
    <header class="nav">
      <BrandLogo @click="$router.push('/')" />
      <div class="nav-actions">
        <ThemeToggle />
        <BaseButton v-if="!user" variant="ghost" size="sm" @click="$router.push('/login')">登录 / 注册</BaseButton>
        <button v-else class="nav-avatar" @click="$router.push('/tasks')" :title="user.username">
          {{ user.username.slice(0, 1).toUpperCase() }}
        </button>
      </div>
    </header>

    <main class="main">
      <div class="greet">
        <p class="eyebrow">多位 AI 专家 · 协同咨询</p>
        <h1 class="hello">{{ greeting }}</h1>
        <p class="muted">{{ subtitle }}</p>
      </div>

      <div class="composer" :class="{ focused: isFocused }">
        <textarea
          ref="textareaRef"
          v-model="question"
          class="input"
          :placeholder="activePlaceholder"
          rows="1"
          @input="autoGrow"
          @focus="isFocused = true"
          @blur="isFocused = false"
          @keydown.enter.exact.prevent="submit"
        />
        <div class="composer-bar">
          <div class="team-pills">
            <button v-for="t in teams" :key="t.key" class="pill" :class="{ on: team === t.key }" @click="team = t.key">
              {{ t.label }}
            </button>
          </div>
          <button class="submit" :class="{ active: canSubmit }" :disabled="!canSubmit" @click="submit" title="开始讨论">
            <svg v-if="!submitting" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 19V5M5 12l7-7 7 7"/>
            </svg>
            <span v-else class="spinner" />
          </button>
        </div>
      </div>

      <div class="suggestions">
        <p class="hint">试试这些：</p>
        <div class="chips">
          <button v-for="s in currentSuggestions" :key="s" class="chip" @click="useSuggestion(s)">{{ s }}</button>
        </div>
      </div>

      <div v-if="user && recentTasks.length > 0" class="recent">
        <p class="hint">继续之前的讨论</p>
        <div class="recent-list">
          <button v-for="t in recentTasks" :key="t.id" class="recent-item" @click="$router.push(`/discussions/${t.id}`)">
            <span class="dot" :data-status="t.status" />
            <span class="title">{{ t.title }}</span>
            <span class="ago">{{ ago(t.createdAt) }}</span>
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTaskStore } from '@/stores/task'
import { useAgentStore } from '@/stores/agent'
import { message as ElMessage } from '@/utils/naive-discrete'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const taskStore = useTaskStore()
const agentStore = useAgentStore()

const user = computed(() => userStore.user)

const question = ref('')
const textareaRef = ref<HTMLTextAreaElement>()
const isFocused = ref(false)
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

const placeholders = [
  '描述你的问题，让多位专家为你辩论…',
  '输入你遇到的困惑，比如"要不要裸辞"…',
  '把决策难题说给专家团，让他们先辩一辩…',
]
const activePlaceholder = ref(placeholders[Math.floor(Math.random() * placeholders.length)])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了，需要一个视角？'
  if (h < 12) return '早上好，今天想讨论什么？'
  if (h < 18) return '今天想讨论点什么？'
  return '晚上好，需要一场辩论吗？'
})
const subtitle = '不管是职业决策、合同纠纷、还是人生选择，让多位 AI 专家辩论后给你答案。'

const autoGrow = () => {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 240) + 'px'
}

const canSubmit = computed(() => !submitting.value && question.value.trim().length >= 3)

const useSuggestion = (s: string) => {
  question.value = s
  nextTick(() => { autoGrow(); textareaRef.value?.focus() })
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
    // 选定团对应的预设 Agent ids
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

// 最近 5 条讨论
const recentTasks = computed(() => taskStore.tasks.slice(0, 5))
const ago = (d?: string) => d ? dayjs(d).format('MM-DD HH:mm') : ''

onMounted(() => {
  // 未登录返回后带着之前的 tempQuestion
  const q = localStorage.getItem('tempQuestion')
  if (q && user.value) { question.value = q; localStorage.removeItem('tempQuestion') }
  if (user.value) taskStore.fetchTasks()
  nextTick(autoGrow)
})
</script>

<style scoped>
.home { min-height: 100vh; background: var(--bg-page); display: flex; flex-direction: column; }

.nav {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 32px;
  background: var(--bg-page);
}
.nav-actions { display: flex; align-items: center; gap: 8px; }
.nav-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--cta-bg); color: var(--cta-text);
  border: none; cursor: pointer;
  font-size: 13px; font-weight: 600;
  transition: transform var(--duration-fast) var(--ease-standard);
}
.nav-avatar:hover { transform: scale(1.05); }

/* ---- Main centered ---- */
.main {
  flex: 1;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 40px 20px 80px;
  max-width: 780px; margin: 0 auto; width: 100%;
}

.greet { text-align: center; margin-bottom: 40px; }
.eyebrow { font-size: 13px; color: var(--accent); font-weight: 500; margin: 0 0 16px; letter-spacing: 0.02em; }
.hello {
  font-size: 44px; font-weight: 600; letter-spacing: -0.025em;
  margin: 0 0 14px; line-height: 1.15;
}
.muted { margin: 0; font-size: 16px; color: var(--text-secondary); line-height: 1.5; max-width: 540px; }

/* ---- Composer ---- */
.composer {
  width: 100%; max-width: 720px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 22px;
  padding: 16px 16px 10px;
  box-shadow: var(--shadow-sm);
  transition: all var(--duration-base) var(--ease-standard);
}
.composer.focused {
  border-color: var(--border-emphasis);
  box-shadow: var(--shadow-md);
}

.input {
  width: 100%;
  font-family: var(--font-sans);
  font-size: 16px; line-height: 1.55;
  color: var(--text-primary);
  background: transparent;
  border: none; outline: none; resize: none;
  padding: 4px 4px 8px;
  max-height: 240px; min-height: 28px;
  letter-spacing: -0.005em;
}
.input::placeholder { color: var(--text-muted); }

.composer-bar {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding-top: 6px;
}

.team-pills { display: inline-flex; gap: 4px; background: var(--bg-elevated); padding: 3px; border-radius: var(--radius-full); }
.pill {
  padding: 5px 14px; background: transparent; border: none;
  font-size: 12.5px; font-weight: 500; color: var(--text-secondary);
  border-radius: var(--radius-full); cursor: pointer;
  transition: all var(--duration-fast) var(--ease-standard);
}
.pill.on { background: var(--bg-card); color: var(--text-primary); box-shadow: var(--shadow-sm); }

.submit {
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--bg-elevated); color: var(--text-muted);
  border: none; cursor: pointer;
  display: inline-flex; align-items: center; justify-content: center;
  transition: all var(--duration-fast) var(--ease-standard);
}
.submit:disabled { cursor: not-allowed; }
.submit.active { background: var(--cta-bg); color: var(--cta-text); }
.submit.active:hover { transform: scale(1.06); }
.spinner {
  width: 14px; height: 14px; border: 2px solid currentColor; border-top-color: transparent;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ---- Suggestions ---- */
.suggestions {
  margin-top: 28px;
  text-align: center;
  max-width: 720px; width: 100%;
}
.hint { font-size: 12px; color: var(--text-muted); margin: 0 0 12px; }
.chips { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; }
.chip {
  padding: 8px 16px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  font-size: 13px; color: var(--text-primary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-standard);
  letter-spacing: -0.005em;
}
.chip:hover { background: var(--bg-elevated); border-color: var(--border-emphasis); transform: translateY(-1px); }

/* ---- Recent ---- */
.recent { margin-top: 64px; width: 100%; max-width: 720px; }
.recent .hint { text-align: left; margin-bottom: 10px; }
.recent-list { display: flex; flex-direction: column; gap: 2px; }
.recent-item {
  display: flex; align-items: center; gap: 12px;
  padding: 11px 14px; background: transparent;
  border: none; border-radius: var(--radius-md);
  cursor: pointer; text-align: left;
  transition: background var(--duration-fast) var(--ease-standard);
  font-family: inherit;
}
.recent-item:hover { background: var(--bg-elevated); }
.dot { width: 6px; height: 6px; border-radius: 50%; background: var(--text-muted); flex-shrink: 0; }
.dot[data-status="DISCUSSING"] { background: var(--accent); }
.dot[data-status="COMPLETED"]  { background: var(--success); }
.dot[data-status="PENDING"]    { background: var(--text-muted); }
.title { flex: 1; font-size: 14px; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ago { font-size: 12px; color: var(--text-muted); font-variant-numeric: tabular-nums; }

@media (max-width: 680px) {
  .nav { padding: 12px 18px; }
  .hello { font-size: 32px; }
  .muted { font-size: 14px; }
  .composer { padding: 14px 14px 8px; }
  .input { font-size: 15px; }
  .team-pills .pill { padding: 4px 10px; font-size: 12px; }
}
</style>
