<template>
  <div class="home">
    <header class="nav">
      <BrandLogo @click="$router.push('/')" />
      <div class="nav-actions">
        <ThemeToggle />
        <BaseButton v-if="!user" variant="ghost" size="sm" @click="$router.push('/login')">登录 / 注册</BaseButton>
        <n-dropdown v-else :options="userDropdownOptions" trigger="click" @select="handleCommand">
          <button class="nav-avatar" :title="user.username">{{ user.username.slice(0, 1).toUpperCase() }}</button>
        </n-dropdown>
      </div>
    </header>

    <main class="main">
      <!-- 问候 + 输入框 -->
      <section class="composer-section">
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
      </section>

      <!-- 我的咨询 (仅登录后) -->
      <section v-if="user" class="console">
        <div class="console-head">
          <div>
            <h2 class="console-title">我的咨询</h2>
            <p class="console-sub">{{ taskStore.tasks.length }} 个咨询</p>
          </div>
          <div class="filter-tabs">
            <button v-for="f in filters" :key="f.val" class="tab" :class="{ on: statusFilter === f.val }" @click="statusFilter = f.val">
              {{ f.label }}
            </button>
          </div>
        </div>

        <div v-if="taskStore.loading && taskStore.tasks.length === 0" class="console-empty">
          <div class="loading-dot" />
        </div>
        <div v-else-if="filtered.length === 0" class="console-empty">
          <p>{{ statusFilter === 'all' ? '还没有咨询，开始第一次讨论吧' : '当前筛选下没有咨询' }}</p>
        </div>
        <div v-else class="task-grid">
          <div v-for="t in filtered" :key="t.id" class="task-card" @click="goToTask(t)">
            <div class="task-head">
              <span class="status-dot" :data-status="t.status" />
              <span class="status-text">{{ labelOfStatus(t.status) }}</span>
              <button class="del" @click.stop="handleDelete(t.id)" title="删除">×</button>
            </div>
            <h4 class="task-title">{{ t.title }}</h4>
            <p class="task-goal">{{ (t.goal || '').slice(0, 80) }}</p>
            <div class="task-foot">
              <AgentAvatarGroup v-if="t.agents?.length" :agents="t.agents.map(a => ({ id: a.id, type: a.type }))" :size="30" :max="5" />
              <span class="task-date">{{ formatDate(t.createdAt) }}</span>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { NDropdown } from 'naive-ui'
import { useUserStore } from '@/stores/user'
import { useTaskStore } from '@/stores/task'
import { useAgentStore } from '@/stores/agent'
import { message as ElMessage, dialog } from '@/utils/naive-discrete'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import AgentAvatarGroup from '@/components/agent/AgentAvatarGroup.vue'
import dayjs from 'dayjs'
import type { TaskStatus } from '@/types'

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

// ==== 我的咨询列表 ====
const statusFilter = ref<'all' | TaskStatus>('all')
const filters = [
  { val: 'all' as const,         label: '全部' },
  { val: 'DISCUSSING' as const,  label: '讨论中' },
  { val: 'COMPLETED' as const,   label: '已完成' },
  { val: 'PENDING' as const,     label: '待开始' },
]
const filtered = computed(() =>
  taskStore.tasks.filter(t => statusFilter.value === 'all' || t.status === statusFilter.value)
)
const labelOfStatus = (s: TaskStatus) => ({
  PENDING: '待开始', DISCUSSING: '讨论中', MERGING: '整合中', COMPLETED: '已完成', ARCHIVED: '已归档',
}[s] || s)
const formatDate = (d: string) => dayjs(d).format('MM-DD HH:mm')

const goToTask = (t: { id: number; status: TaskStatus }) => {
  // 已完成 → 结果；其他 → 讨论
  if (t.status === 'COMPLETED') router.push(`/results/${t.id}`)
  else router.push(`/discussions/${t.id}`)
}
const handleDelete = async (id: number) => {
  const ok = await dialog.confirm('删除此咨询？', '删除后不可恢复。')
  if (!ok) return
  await taskStore.deleteTask(id)
  ElMessage.success('已删除')
}

// ==== 用户菜单 ====
const userDropdownOptions = [
  { label: 'Agent 管理', key: 'agents' },
  { label: '知识库',     key: 'kb' },
  { label: '个人设置',   key: 'settings' },
  { type: 'divider' as const, key: 'd1' },
  { label: '退出登录',   key: 'logout' },
]
const handleCommand = (cmd: string) => {
  const map: Record<string, () => void> = {
    agents:   () => router.push('/agents'),
    kb:       () => router.push('/kb'),
    settings: () => router.push('/settings'),
    logout:   () => { userStore.logout(); router.push('/login') },
  }
  map[cmd]?.()
}

onMounted(() => {
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
  background: rgba(255,255,255,0.78);
  backdrop-filter: saturate(180%) blur(16px);
  -webkit-backdrop-filter: saturate(180%) blur(16px);
  border-bottom: 1px solid var(--border-subtle);
}
html[data-theme="dark"] .nav { background: rgba(0,0,0,0.7); }
.nav-actions { display: flex; align-items: center; gap: 8px; }
.nav-avatar {
  width: 34px; height: 34px; border-radius: 50%;
  background: var(--cta-bg); color: var(--cta-text);
  border: none; cursor: pointer;
  font-size: 13px; font-weight: 600;
  transition: transform var(--duration-fast) var(--ease-standard);
}
.nav-avatar:hover { transform: scale(1.05); }

.main {
  flex: 1;
  display: flex; flex-direction: column;
  padding: 40px 24px 80px;
  max-width: 1100px; margin: 0 auto; width: 100%;
}

/* ---- Composer section ---- */
.composer-section {
  display: flex; flex-direction: column; align-items: center;
  padding: 40px 0 48px;
}
.greet { text-align: center; margin-bottom: 32px; }
.eyebrow { font-size: 13px; color: var(--accent); font-weight: 500; margin: 0 0 14px; letter-spacing: 0.02em; }
.hello { font-size: 40px; font-weight: 600; letter-spacing: -0.025em; margin: 0 0 12px; line-height: 1.15; }
.muted { margin: 0; font-size: 15px; color: var(--text-secondary); line-height: 1.5; max-width: 540px; }

.composer {
  width: 100%; max-width: 720px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: 22px;
  padding: 16px 16px 10px;
  box-shadow: var(--shadow-sm);
  transition: all var(--duration-base) var(--ease-standard);
}
.composer.focused { border-color: var(--border-emphasis); box-shadow: var(--shadow-md); }

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

.composer-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-top: 6px; }
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

.suggestions { margin-top: 22px; text-align: center; max-width: 720px; width: 100%; }
.hint { font-size: 12px; color: var(--text-muted); margin: 0 0 10px; }
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

/* ---- Console section ---- */
.console {
  margin-top: 24px;
  padding-top: 36px;
  border-top: 1px solid var(--border-subtle);
}
.console-head {
  display: flex; align-items: flex-end; justify-content: space-between;
  gap: 20px; margin-bottom: 20px;
}
.console-title { font-size: 26px; margin: 0; letter-spacing: -0.02em; font-weight: 600; }
.console-sub { font-size: 13px; color: var(--text-muted); margin: 4px 0 0; }

.filter-tabs { display: inline-flex; gap: 3px; background: var(--bg-elevated); padding: 3px; border-radius: var(--radius-full); flex-shrink: 0; }
.tab {
  padding: 5px 14px; background: transparent; border: none;
  font-size: 12.5px; font-weight: 500; color: var(--text-secondary);
  border-radius: var(--radius-full); cursor: pointer;
  transition: all var(--duration-fast) var(--ease-standard);
}
.tab.on { background: var(--bg-card); color: var(--text-primary); box-shadow: var(--shadow-sm); }

.console-empty {
  padding: 60px 20px; text-align: center; color: var(--text-muted); font-size: 14px;
  background: var(--bg-elevated); border-radius: var(--radius-lg);
}
.loading-dot {
  width: 12px; height: 12px; background: var(--text-muted); border-radius: 50%;
  margin: 0 auto; animation: pulse 1.2s ease-in-out infinite;
}
@keyframes pulse { 0%,100% { opacity: 0.3; } 50% { opacity: 1; } }

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}
.task-card {
  padding: 18px 20px;
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all var(--duration-base) var(--ease-standard);
  position: relative;
}
.task-card:hover {
  border-color: var(--border-emphasis);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}
.task-head {
  display: flex; align-items: center; gap: 8px; margin-bottom: 10px;
  font-size: 12px; color: var(--text-secondary);
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: var(--text-muted); flex-shrink: 0; }
.status-dot[data-status="DISCUSSING"] { background: var(--accent); box-shadow: 0 0 0 3px var(--accent-dim); }
.status-dot[data-status="COMPLETED"]  { background: var(--success); }
.status-dot[data-status="MERGING"]    { background: var(--warning); }
.status-text { flex: 1; }
.del {
  width: 22px; height: 22px; background: transparent; border: none; cursor: pointer;
  color: var(--text-muted); font-size: 16px; line-height: 1; border-radius: var(--radius-sm);
  display: inline-flex; align-items: center; justify-content: center;
  opacity: 0; transition: all var(--duration-fast) var(--ease-standard);
}
.task-card:hover .del { opacity: 1; }
.del:hover { background: rgba(255,69,58,0.08); color: var(--danger); }

.task-title { font-size: 15px; font-weight: 600; margin: 0 0 6px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; letter-spacing: -0.01em; }
.task-goal {
  font-size: 13px; color: var(--text-secondary); line-height: 1.5;
  margin: 0 0 14px;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
  overflow: hidden; height: 38px;
}
.task-foot { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.task-date { font-size: 11px; color: var(--text-muted); font-variant-numeric: tabular-nums; }

@media (max-width: 680px) {
  .nav { padding: 12px 18px; }
  .main { padding: 20px 18px 60px; }
  .composer-section { padding: 20px 0 32px; }
  .hello { font-size: 30px; }
  .muted { font-size: 14px; }
  .console-head { flex-direction: column; align-items: flex-start; }
  .console-title { font-size: 22px; }
}
</style>
