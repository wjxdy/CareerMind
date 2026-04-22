<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sb-head">
      <BrandLogo :variant="collapsed ? 'icon' : 'full'" @click="$router.push('/')" />
      <button class="collapse-btn" @click="$emit('toggle')" :title="collapsed ? '展开' : '收起'">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path v-if="!collapsed" d="M15 18l-6-6 6-6" />
          <path v-else d="M9 18l6-6-6-6" />
        </svg>
      </button>
    </div>

    <div class="sb-cta">
      <BaseButton variant="primary" size="md" block @click="$router.push('/tasks')">
        <span v-if="!collapsed">+ 新建咨询</span>
        <span v-else>+</span>
      </BaseButton>
    </div>

    <nav class="sb-nav">
      <RouterLink v-for="n in navItems" :key="n.path" :to="n.path" class="sb-nav-item" :title="collapsed ? n.label : ''">
        <span class="ic" v-html="n.icon" />
        <span v-if="!collapsed" class="lbl">{{ n.label }}</span>
      </RouterLink>
    </nav>

    <div class="sb-section" v-if="!collapsed">历史对话</div>
    <div class="sb-tasks">
      <div v-for="t in taskStore.tasks.slice(0, 20)" :key="t.id"
           class="sb-task" :class="{ active: currentTaskId === t.id }" :title="t.title"
           @click="goToTask(t.id)">
        <span class="t-dot" />
        <span v-if="!collapsed" class="t-title">{{ t.title }}</span>
      </div>
      <EmptyState v-if="!collapsed && taskStore.tasks.length === 0" description="暂无咨询" />
    </div>

    <div class="sb-foot">
      <ThemeToggle />
      <div v-if="userStore.user" class="sb-user" :title="userStore.user.username">
        <div class="u-avatar">{{ userStore.user.username.slice(0,1).toUpperCase() }}</div>
        <span v-if="!collapsed" class="u-name">{{ userStore.user.username }}</span>
        <el-dropdown v-if="!collapsed" @command="handleCommand" trigger="click">
          <button class="u-more">⋯</button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="kb">知识库</el-dropdown-item>
              <el-dropdown-item command="agents">Agent 管理</el-dropdown-item>
              <el-dropdown-item command="settings">个人设置</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <BaseButton v-else variant="primary" size="sm" block @click="$router.push('/login')">
        {{ collapsed ? '→' : '登录 / 注册' }}
      </BaseButton>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTaskStore } from '@/stores/task'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

defineProps<{ collapsed?: boolean }>()
defineEmits<{ (e: 'toggle'): void }>()

const router = useRouter()
const route  = useRoute()
const userStore = useUserStore()
const taskStore = useTaskStore()

const navItems = [
  { path: '/',       label: '首页',     icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12l9-9 9 9M5 10v11h14V10"/></svg>' },
  { path: '/tasks',  label: '咨询',     icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>' },
  { path: '/kb',     label: '知识库',   icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5v14z"/></svg>' },
  { path: '/agents', label: 'Agent',    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="7" r="4"/><path d="M3 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2M16 11a4 4 0 0 0 0-8"/></svg>' },
]

const loadTasks = () => { if (userStore.user) taskStore.fetchTasks() }
onMounted(() => { loadTasks(); window.addEventListener('task-created', loadTasks) })
watch(() => route.path, p => { if (p === '/tasks' || p === '/') loadTasks() })
const currentTaskId = computed(() => Number(route.params.id || route.params.taskId) || null)
const goToTask = (id: number) => router.push(`/tasks/${id}`)

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
  background: var(--bg-card); border-right: 1px solid var(--border-subtle);
  display: flex; flex-direction: column;
  transition: width var(--duration-base) var(--ease-standard);
}
.sidebar.collapsed { width: 72px; }

.sb-head { display: flex; align-items: center; justify-content: space-between; padding: 16px 12px 8px; gap: 8px; }
.collapse-btn {
  width: 26px; height: 26px; display: inline-flex; align-items: center; justify-content: center;
  background: transparent; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm);
  color: var(--text-muted); cursor: pointer;
}
.collapse-btn:hover { background: var(--bg-elevated); color: var(--text-primary); }
.sidebar.collapsed .collapse-btn { display: none; }

.sb-cta { padding: 8px 12px 12px; }

.sb-nav { display: flex; flex-direction: column; padding: 8px 8px; gap: 2px; }
.sb-nav-item {
  display: flex; align-items: center; gap: 10px; padding: 8px 10px;
  border-radius: var(--radius-md); color: var(--text-secondary);
  text-decoration: none; font-size: 14px;
  transition: all var(--duration-fast) var(--ease-standard);
}
.sb-nav-item:hover { background: var(--bg-elevated); color: var(--text-primary); }
.sb-nav-item.router-link-active { background: var(--accent-dim); color: var(--accent); font-weight: 500; }
.sb-nav-item .ic { display: inline-flex; width: 18px; flex-shrink: 0; }

.sb-section { padding: 12px 16px 4px; font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em; }
.sb-tasks { flex: 1; overflow-y: auto; padding: 4px 8px 8px; }
.sb-task {
  display: flex; align-items: center; gap: 8px; padding: 7px 10px;
  border-radius: var(--radius-md); cursor: pointer; color: var(--text-secondary);
  font-size: 13px; transition: all var(--duration-fast) var(--ease-standard);
}
.sb-task:hover { background: var(--bg-elevated); color: var(--text-primary); }
.sb-task.active { background: var(--accent-dim); color: var(--accent); }
.t-dot { width: 5px; height: 5px; border-radius: 50%; background: currentColor; flex-shrink: 0; opacity: 0.55; }
.t-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.sb-foot { padding: 10px 12px 14px; border-top: 1px solid var(--border-subtle); display: flex; flex-direction: column; gap: 10px; }
.sb-user { display: flex; align-items: center; gap: 10px; }
.u-avatar {
  width: 28px; height: 28px; border-radius: 50%;
  background: var(--accent); color: var(--accent-contrast);
  display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600;
  flex-shrink: 0;
}
.u-name { flex: 1; font-size: 13px; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.u-more {
  background: transparent; border: none; color: var(--text-muted); cursor: pointer;
  font-size: 18px; line-height: 1; padding: 2px 6px; border-radius: var(--radius-sm);
}
.u-more:hover { background: var(--bg-elevated); color: var(--text-primary); }
</style>
