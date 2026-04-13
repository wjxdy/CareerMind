<template>
  <div class="page-layout">
    <Sidebar />
    <main class="page-content">
      <div class="task-detail" v-if="task">
        <div class="detail-header">
          <el-button @click="$router.back()" text>
            <el-icon><ArrowLeft /></el-icon>
            返回
          </el-button>
          <el-tag :type="getStatusType(task.status)">
            {{ getStatusLabel(task.status) }}
          </el-tag>
        </div>

        <h2>{{ task.title }}</h2>

        <div class="detail-section" v-if="task.background">
          <h4>背景信息</h4>
          <p>{{ task.background }}</p>
        </div>

        <div class="detail-section" v-if="task.goal">
          <h4>目标/困惑</h4>
          <p>{{ task.goal }}</p>
        </div>

        <div class="detail-section" v-if="task.constraints">
          <h4>约束条件</h4>
          <p>{{ task.constraints }}</p>
        </div>

        <div class="detail-section">
          <h4>参与专家</h4>
          <div class="agent-tags">
            <el-tag
              v-for="agent in task.agents"
              :key="agent.id"
              :style="{ backgroundColor: getAgentColor(agent.type), color: 'white' }"
              class="agent-tag"
            >
              {{ agent.name }}
            </el-tag>
          </div>
        </div>

        <div class="detail-actions">
          <el-button type="primary" @click="goToDiscussion">
            <el-icon><ChatLineRound /></el-icon>
            查看讨论
          </el-button>
          <el-button type="success" @click="goToResult">
            <el-icon><DocumentChecked /></el-icon>
            查看结果
          </el-button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Sidebar from '@/components/layout/Sidebar.vue'
import { taskApi } from '@/api/task'
import { useAgentStore } from '@/stores/agent'
import type { Task, TaskStatus } from '@/types'

const route = useRoute()
const router = useRouter()
const agentStore = useAgentStore()
const taskId = Number(route.params.id)

const task = ref<Task | null>(null)

onMounted(async () => {
  task.value = await taskApi.getTaskById(taskId)
})

const getAgentColor = (type: string) => agentStore.getAgentColor(type)

const getStatusType = (status: TaskStatus) => {
  const types: Record<string, string> = {
    PENDING: 'info',
    DISCUSSING: 'primary',
    MERGING: 'warning',
    COMPLETED: 'success',
    ARCHIVED: 'info'
  }
  return types[status] || 'info'
}

const getStatusLabel = (status: TaskStatus) => {
  const labels: Record<string, string> = {
    PENDING: '待开始',
    DISCUSSING: '讨论中',
    MERGING: '整合中',
    COMPLETED: '已完成',
    ARCHIVED: '已归档'
  }
  return labels[status] || status
}

const goToDiscussion = () => router.push(`/discussions/${taskId}`)
const goToResult = () => router.push(`/results/${taskId}`)
</script>

<style scoped>
.page-layout {
  display: flex;
  height: 100vh;
}

.page-content {
  flex: 1;
  padding: 24px 32px;
  overflow-y: auto;
  background: white;
}

.task-detail {
  max-width: 800px;
  margin: 0 auto;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.task-detail h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 24px;
}

.detail-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.detail-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 8px;
}

.detail-section p {
  font-size: 14px;
  color: #374151;
  line-height: 1.6;
}

.agent-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.agent-tag {
  border: none;
}

.detail-actions {
  display: flex;
  gap: 12px;
  margin-top: 32px;
}
</style>
