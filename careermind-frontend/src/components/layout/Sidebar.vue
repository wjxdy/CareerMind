<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sb-head">
      <BrandLogo :variant="collapsed ? 'icon' : 'full'" @click="$router.push('/')" />
      <button class="collapse-btn" @click="$emit('toggle')" :title="collapsed ? '展开' : '收起'">
        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
          <path v-if="!collapsed" d="M15 18l-6-6 6-6" />
          <path v-else d="M9 18l6-6-6-6" />
        </svg>
      </button>
    </div>

    <div class="sb-cta">
      <BaseButton variant="primary" size="md" block @click="$router.push('/')">
        <span v-if="!collapsed">＋ 新建咨询</span>
        <span v-else>＋</span>
      </BaseButton>
    </div>

    <nav class="sb-nav">
      <RouterLink v-for="n in navItems" :key="n.path" :to="n.path" class="sb-nav-item" :title="collapsed ? n.label : ''">
        <span class="ic" v-html="n.icon" />
        <span v-if="!collapsed" class="lbl">{{ n.label }}</span>
      </RouterLink>
    </nav>

    <div class="sb-tasks">
      <template v-if="collapsed">
        <div v-for="t in taskStore.tasks.slice(0, 20)" :key="t.id"
             class="sb-task compact" :class="{ active: currentTaskId === t.id }" :title="t.title"
             @click="goToTask(t.id)">
          <span class="t-dot" :data-status="t.status" />
        </div>
      </template>
      <template v-else>
        <div v-for="g in groupedTasks" :key="g.key" class="sb-group">
          <div class="sb-group-title">{{ g.label }}</div>
          <div v-for="t in g.tasks" :key="t.id"
               class="sb-task" :class="{ active: currentTaskId === t.id }" :title="t.title"
               @click="goToTask(t.id)">
            <span class="t-title">{{ t.title }}</span>
            <button class="t-del" @click.stop="handleDelete(t)" title="删除">×</button>
          </div>
        </div>
        <EmptyState v-if="taskStore.tasks.length === 0" description="暂无咨询" />
      </template>
    </div>

    <div class="sb-foot">
      <div class="foot-row">
        <ThemeToggle />
        <div v-if="userStore.user" class="sb-user" :title="userStore.user.username">
          <div class="u-avatar">{{ userStore.user.username.slice(0,1).toUpperCase() }}</div>
          <span v-if="!collapsed" class="u-name">{{ userStore.user.username }}</span>
          <n-dropdown v-if="!collapsed" :options="dropdownOptions" trigger="click" @select="handleCommand">
            <button class="u-more">⋯</button>
          </n-dropdown>
        </div>
      </div>
      <BaseButton v-if="!userStore.user" variant="primary" size="sm" block @click="$router.push('/login')">
        {{ collapsed ? '→' : '登录 / 注册' }}
      </BaseButton>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { NDropdown } from 'naive-ui'
import { useUserStore } from '@/stores/user'
import { useTaskStore } from '@/stores/task'
import { message as ElMessage, dialog } from '@/utils/naive-discrete'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import dayjs from 'dayjs'
import type { Task } from '@/types'

const dropdownOptions = [
  { label: '知识库', key: 'kb' },
  { label: 'Agent 管理', key: 'agents' },
  { label: '个人设置', key: 'settings' },
  { type: 'divider' as const, key: 'd1' },
  { label: '退出登录', key: 'logout' },
]

defineProps<{ collapsed?: boolean }>()
defineEmits<{ (e: 'toggle'): void }>()

const router = useRouter()
const route  = useRoute()
const userStore = useUserStore()
const taskStore = useTaskStore()

const navItems = [
  { path: '/',       label: '首页',     icon: '<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12l9-9 9 9M5 10v11h14V10"/></svg>' },
  { path: '/kb',     label: '知识库',   icon: '<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5v14z"/></svg>' },
  { path: '/agents', label: 'Agent',    icon: '<svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="9" cy="7" r="4"/><path d="M3 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2M16 11a4 4 0 0 0 0-8"/></svg>' },
]

const loadTasks = () => { if (userStore.user) taskStore.fetchTasks() }
onMounted(() => { loadTasks(); window.addEventListener('task-created', loadTasks) })
watch(() => route.path, p => { if (p === '/tasks' || p === '/') loadTasks() })
const currentTaskId = computed(() => Number(route.params.id || route.params.taskId) || null)

const goToTask = (id: number) => {
  const t = taskStore.tasks.find(x => x.id === id)
  if (t?.status === 'COMPLETED') router.push(`/results/${id}`)
  else router.push(`/discussions/${id}`)
}

const handleDelete = async (t: Task) => {
  const ok = await dialog.confirm('删除此咨询？', t.title)
  if (!ok) return
  await taskStore.deleteTask(t.id)
  ElMessage.success('已删除')
  if (currentTaskId.value === t.id) router.push('/')
}

// 按时间分组
const groupedTasks = computed(() => {
  const groups: { key: string; label: string; tasks: Task[] }[] = [
    { key: 'today',    label: '今天',   tasks: [] },
    { key: 'yesterday',label: '昨天',   tasks: [] },
    { key: 'week',     label: '7 天内', tasks: [] },
    { key: 'earlier',  label: '更早',   tasks: [] },
  ]
  const today = dayjs().startOf('day')
  for (const t of taskStore.tasks) {
    const d = dayjs(t.createdAt)
    const diffDays = today.diff(d.startOf('day'), 'day')
    if (diffDays <= 0) groups[0].tasks.push(t)
    else if (diffDays === 1) groups[1].tasks.push(t)
    else if (diffDays <= 7) groups[2].tasks.push(t)
    else groups[3].tasks.push(t)
  }
  return groups.filter(g => g.tasks.length > 0)
})

const handleCommand = (cmd: string) => {
  const map: Record<string, () => void> = {
    kb:       () => router.push('/kb'),
    agents:   () => router.push('/agents'),
    settings: () => router.push('/settings'),
    logout:   () => { userStore.logout(); router.push('/login') },
  }
  map[cmd]?.()
}
</script>

<style scoped>
.sidebar {
  width: 260px; height: 100vh; flex-shrink: 0;
  background: var(--bg-elevated);
  display: flex; flex-direction: column;
  transition: width var(--duration-base) var(--ease-standard);
  border-right: 1px solid var(--border-subtle);
}
.sidebar.collapsed { width: 72px; }

.sb-head { display: flex; align-items: center; justify-content: space-between; padding: 20px 18px 12px; gap: 10px; }
.collapse-btn {
  width: 26px; height: 26px; display: inline-flex; align-items: center; justify-content: center;
  background: transparent; border: none; border-radius: var(--radius-sm);
  color: var(--text-muted); cursor: pointer;
  flex-shrink: 0;
}
.collapse-btn:hover { background: var(--bg-card); color: var(--text-primary); }
.sidebar.collapsed .sb-head { flex-direction: column; gap: 10px; padding: 18px 10px 10px; }

.sb-cta { padding: 0 14px 14px; }
.sidebar.collapsed .sb-cta { padding: 0 10px 12px; }

.sb-nav { display: flex; flex-direction: column; padding: 4px 12px; gap: 2px; }
.sb-nav-item {
  display: flex; align-items: center; gap: 12px; padding: 9px 12px;
  border-radius: var(--radius-md); color: var(--text-secondary);
  text-decoration: none; font-size: 14px; font-weight: 500;
  transition: all var(--duration-fast) var(--ease-standard);
}
.sb-nav-item:hover { background: var(--bg-card); color: var(--text-primary); }
.sb-nav-item.router-link-active { background: var(--bg-card); color: var(--text-primary); }
.sb-nav-item .ic { display: inline-flex; width: 17px; flex-shrink: 0; }

.sb-tasks { flex: 1; overflow-y: auto; padding: 12px 12px; }
.sb-group { margin-bottom: 16px; }
.sb-group-title {
  padding: 8px 12px 4px;
  font-size: 11px; color: var(--text-muted); font-weight: 500;
  letter-spacing: 0;
}
.sb-task {
  position: relative;
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-md); cursor: pointer; color: var(--text-secondary);
  font-size: 13px;
  transition: all var(--duration-fast) var(--ease-standard);
}
.sb-task:hover { background: var(--bg-card); color: var(--text-primary); }
.sb-task.active { background: var(--bg-card); color: var(--text-primary); font-weight: 500; }
.sb-task.compact { justify-content: center; padding: 8px 0; }
.t-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--text-muted); flex-shrink: 0;
}
.t-dot[data-status="DISCUSSING"] { background: var(--accent); }
.t-dot[data-status="COMPLETED"]  { background: var(--success); }
.t-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.t-del {
  width: 20px; height: 20px;
  display: inline-flex; align-items: center; justify-content: center;
  background: transparent; border: none; cursor: pointer;
  font-size: 15px; line-height: 1; color: var(--text-muted);
  border-radius: var(--radius-sm);
  opacity: 0;
  transition: all var(--duration-fast) var(--ease-standard);
}
.sb-task:hover .t-del { opacity: 1; }
.t-del:hover { background: rgba(255,69,58,0.12); color: var(--danger); }

.sb-foot { padding: 14px 18px 20px; display: flex; flex-direction: column; gap: 10px; border-top: 1px solid var(--border-subtle); }
.sidebar.collapsed .sb-foot { padding: 14px 10px 18px; }
.foot-row { display: flex; align-items: center; gap: 10px; }
.sb-user { flex: 1; display: flex; align-items: center; gap: 10px; min-width: 0; }
.u-avatar {
  width: 30px; height: 30px; border-radius: 50%;
  background: var(--cta-bg); color: var(--cta-text);
  display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600;
  flex-shrink: 0;
}
.u-name { flex: 1; font-size: 13px; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.u-more {
  background: transparent; border: none; color: var(--text-muted); cursor: pointer;
  font-size: 16px; line-height: 1; padding: 4px 8px; border-radius: var(--radius-sm);
}
.u-more:hover { background: var(--bg-card); color: var(--text-primary); }
</style>
