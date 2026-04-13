<template>
  <aside class="sidebar">
    <div class="sidebar-header">
      <div class="logo" @click="$router.push('/')">
        <el-icon :size="28" color="#0ea5e9"><ChatDotRound /></el-icon>
        <span class="logo-text">CareerMind</span>
      </div>
    </div>

    <div class="new-chat-btn">
      <el-button
        type="primary"
        size="large"
        class="w-full"
        @click="$router.push('/tasks')"
      >
        <el-icon><Plus /></el-icon>
        新建咨询
      </el-button>
    </div>

    <div class="sidebar-content">
      <div class="section-title">历史对话</div>
      <div class="task-list">
        <div
          v-for="task in taskStore.tasks.slice(0, 10)"
          :key="task.id"
          class="task-item"
          :class="{ active: currentTaskId === task.id }"
          @click="goToTask(task.id)"
        >
          <el-icon><ChatLineRound /></el-icon>
          <span class="task-title">{{ task.title }}</span>
        </div>
      </div>

      <el-empty v-if="taskStore.tasks.length === 0" description="暂无对话" />
    </div>

    <div class="sidebar-footer">
      <!-- 已登录状态 -->
      <div class="user-info" v-if="userStore.user">
        <el-avatar :size="32" :icon="UserFilled" />
        <span class="username">{{ userStore.user.username }}</span>
        <el-dropdown @command="handleCommand">
          <el-icon class="more-icon"><More /></el-icon>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="home">
                <el-icon><Home /></el-icon> 返回首页
              </el-dropdown-item>
              <el-dropdown-item command="kb">
                <el-icon><Collection /></el-icon> 知识库
              </el-dropdown-item>
              <el-dropdown-item command="agents">
                <el-icon><Setting /></el-icon> 管理Agent
              </el-dropdown-item>
              <el-dropdown-item command="settings">
                <el-icon><User /></el-icon> 个人设置
              </el-dropdown-item>
              <el-dropdown-item command="logout" divided>
                <el-icon><SwitchButton /></el-icon> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <!-- 未登录状态 -->
      <div class="login-prompt" v-else>
        <el-button type="primary" class="login-btn" @click="goToLogin">
          <el-icon><User /></el-icon>
          登录 / 注册
        </el-button>
        <p class="login-tip">登录后使用多Agent咨询</p>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTaskStore } from '@/stores/task'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const taskStore = useTaskStore()

// 加载任务列表
const loadTasks = () => {
  if (userStore.user) {
    taskStore.fetchTasks()
  }
}

onMounted(() => {
  loadTasks()
  // 监听任务创建事件
  window.addEventListener('task-created', loadTasks)
})

// 监听路由变化，刷新任务列表
watch(() => route.path, (newPath) => {
  // 当进入任务列表或首页时刷新
  if (newPath === '/tasks' || newPath === '/') {
    loadTasks()
  }
})

// 监听任务列表变化（新建咨询后）
watch(() => taskStore.tasks.length, () => {
  // 自动刷新，无需额外操作
})

const currentTaskId = computed(() => {
  const id = route.params.id || route.params.taskId
  return id ? Number(id) : null
})

const goToTask = (taskId: number) => {
  router.push(`/tasks/${taskId}`)
}

const goToLogin = () => {
  router.push('/login')
}

const handleCommand = (command: string) => {
  switch (command) {
    case 'home':
      router.push('/')
      break
    case 'kb':
      router.push('/kb')
      break
    case 'agents':
      router.push('/agents')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      userStore.logout()
      router.push('/login')
      break
  }
}
</script>

<style scoped>
.sidebar {
  width: 260px;
  height: 100vh;
  background: #f9fafb;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 16px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
}

.logo:hover {
  background: #e5e7eb;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.new-chat-btn {
  padding: 0 16px 16px;
}

.new-chat-btn :deep(.el-button) {
  justify-content: center;
  border-radius: 8px;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px;
}

.section-title {
  font-size: 12px;
  color: #6b7280;
  padding: 8px 12px;
  font-weight: 500;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: #374151;
}

.task-item:hover {
  background: #e5e7eb;
}

.task-item.active {
  background: #dbeafe;
  color: #1d4ed8;
}

.task-title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid #e5e7eb;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.username {
  flex: 1;
  font-size: 14px;
  color: #374151;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more-icon {
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  color: #6b7280;
}

.more-icon:hover {
  background: #e5e7eb;
}

.login-prompt {
  text-align: center;
  padding: 12px;
}

.login-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-radius: 8px;
}

.login-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #9ca3af;
}
</style>
