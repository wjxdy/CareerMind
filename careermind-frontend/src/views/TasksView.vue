<template>
  <div class="page-layout">
    <Sidebar />
    <main class="page-content">
      <div class="page-header">
        <h2>我的咨询</h2>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          新建咨询
        </el-button>
      </div>

      <div class="task-grid" v-loading="taskStore.loading">
        <el-card
          v-for="task in taskStore.tasks"
          :key="task.id"
          class="task-card"
          shadow="hover"
          @click="goToTask(task.id)"
        >
          <div class="card-header">
            <el-tag :type="getStatusType(task.status)" size="small">
              {{ getStatusLabel(task.status) }}
            </el-tag>
            <el-icon class="delete-icon" @click.stop="handleDelete(task.id)"><Delete /></el-icon>
          </div>
          <h3 class="task-title">{{ task.title }}</h3>
          <p class="task-goal" v-if="task.goal">{{ task.goal.slice(0, 100) }}...</p>
          <div class="card-footer">
            <span class="task-date">{{ formatDate(task.createdAt) }}</span>
            <div class="agent-avatars" v-if="task.agents?.length">
              <el-avatar
                v-for="agent in task.agents.slice(0, 3)"
                :key="agent.id"
                :size="24"
                :style="{ backgroundColor: agentStore.getAgentColor(agent.type) }"
              >
                {{ agent.name.charAt(0) }}
              </el-avatar>
              <span v-if="task.agents.length > 3" class="more-agents">+{{ task.agents.length - 3 }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <el-empty v-if="taskStore.tasks.length === 0" description="暂无咨询任务" />
    </main>

    <!-- 创建任务对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="新建职业咨询"
      width="600px"
    >
      <el-form :model="createForm" :rules="rules" ref="formRef" label-position="top">
        <el-form-item label="咨询主题" prop="title">
          <el-input v-model="createForm.title" placeholder="给你的咨询起个标题" />
        </el-form-item>

        <el-form-item label="背景信息" prop="background">
          <el-input
            v-model="createForm.background"
            type="textarea"
            :rows="3"
            placeholder="描述你的教育背景、工作经历等（已自动填充个人简介）"
          />
          <div class="form-tip" v-if="createForm.background">
            <el-icon><InfoFilled /></el-icon> 已自动填充个人简介，你可以根据需要修改
          </div>
        </el-form-item>

        <el-form-item label="目标/困惑" prop="goal">
          <el-input
            v-model="createForm.goal"
            type="textarea"
            :rows="4"
            placeholder="详细描述你面临的职业选择或困惑"
          />
        </el-form-item>

        <el-form-item label="约束条件" prop="constraints">
          <el-input
            v-model="createForm.constraints"
            type="textarea"
            :rows="2"
            placeholder="时间、资金、家庭等约束条件（可选）"
          />
        </el-form-item>

        <el-form-item label="选择专家" prop="agentIds">
          <div class="agent-selection">
            <div
              v-for="agent in agentStore.availableAgents"
              :key="agent.id"
              class="agent-option"
              :class="{ selected: createForm.agentIds.includes(agent.id) }"
              @click="toggleAgent(agent.id)"
            >
              <el-avatar
                :size="40"
                :style="{ backgroundColor: agentStore.getAgentColor(agent.type) }"
              >
                <el-icon :size="20">
                  <component :is="agentStore.getAgentIcon(agent.type)" />
                </el-icon>
              </el-avatar>
              <div class="agent-info">
                <span class="agent-name">{{ agent.name }}</span>
                <span class="agent-desc">{{ agent.description }}</span>
              </div>
              <el-icon v-if="createForm.agentIds.includes(agent.id)" class="check-icon"><Check /></el-icon>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">
          创建咨询
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import Sidebar from '@/components/layout/Sidebar.vue'
import { useTaskStore } from '@/stores/task'
import { useAgentStore } from '@/stores/agent'
import type { TaskStatus } from '@/types'
import dayjs from 'dayjs'

const router = useRouter()
const taskStore = useTaskStore()
const agentStore = useAgentStore()

const showCreateDialog = ref(false)
const creating = ref(false)
const formRef = ref<FormInstance>()

const createForm = reactive({
  title: localStorage.getItem('tempQuestion')?.slice(0, 6) || '',
  background: localStorage.getItem('userBio') || '',
  goal: localStorage.getItem('tempQuestion') || '',
  constraints: '',
  agentIds: [] as number[],
})

// 监听目标/困惑变化，自动设置标题为前6个字
watch(() => createForm.goal, (newGoal) => {
  if (newGoal && !createForm.title) {
    createForm.title = newGoal.slice(0, 6)
  }
})

// 监听对话框打开，重新加载个人简介
watch(() => showCreateDialog.value, (isOpen) => {
  if (isOpen) {
    const savedBio = localStorage.getItem('userBio')
    if (savedBio) {
      createForm.background = savedBio
    }
    // 重新预选默认Agent
    if (createForm.agentIds.length === 0 && agentStore.availableAgents.length > 0) {
      const presetIds = agentStore.availableAgents
        .filter(a => a.isPreset)
        .slice(0, 4)
        .map(a => a.id)
      createForm.agentIds = presetIds
    }
  }
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入咨询主题', trigger: 'blur' }],
  goal: [{ required: true, message: '请描述你的目标或困惑', trigger: 'blur' }],
  agentIds: [{ required: true, message: '请至少选择一位专家', trigger: 'change', type: 'array' }],
}

onMounted(() => {
  taskStore.fetchTasks()
  agentStore.fetchAvailableAgents()
  // 预选几个默认Agent
  if (createForm.agentIds.length === 0 && agentStore.availableAgents.length > 0) {
    const presetIds = agentStore.availableAgents
      .filter(a => a.isPreset)
      .slice(0, 4)
      .map(a => a.id)
    createForm.agentIds = presetIds
  }
})

const toggleAgent = (agentId: number) => {
  const index = createForm.agentIds.indexOf(agentId)
  if (index > -1) {
    createForm.agentIds.splice(index, 1)
  } else {
    createForm.agentIds.push(agentId)
  }
}

const handleCreate = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      creating.value = true
      try {
        const task = await taskStore.createTask(createForm)
        ElMessage.success('创建成功')
        showCreateDialog.value = false
        localStorage.removeItem('tempQuestion')
        router.push(`/discussions/${task.id}`)
      } finally {
        creating.value = false
      }
    }
  })
}

const goToTask = (taskId: number) => {
  router.push(`/discussions/${taskId}`)
}

const handleDelete = async (taskId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个咨询任务吗？', '提示', {
      type: 'warning',
    })
    await taskStore.deleteTask(taskId)
    ElMessage.success('删除成功')
  } catch {
    // 取消删除
  }
}

const getStatusType = (status: TaskStatus) => {
  const types: Record<string, string> = {
    PENDING: 'info',
    DISCUSSING: 'primary',
    MERGING: 'warning',
    COMPLETED: 'success',
    ARCHIVED: 'info',
  }
  return types[status] || 'info'
}

const getStatusLabel = (status: TaskStatus) => {
  const labels: Record<string, string> = {
    PENDING: '待开始',
    DISCUSSING: '讨论中',
    MERGING: '整合中',
    COMPLETED: '已完成',
    ARCHIVED: '已归档',
  }
  return labels[status] || status
}

const formatDate = (date: string) => {
  return dayjs(date).format('MM-DD HH:mm')
}
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.task-card {
  cursor: pointer;
  border-radius: 12px;
  transition: all 0.3s;
}

.task-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.delete-icon {
  color: #9ca3af;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
}

.delete-icon:hover {
  color: #ef4444;
  background: #fee2e2;
}

.task-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-goal {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  margin-bottom: 16px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.task-date {
  font-size: 12px;
  color: #9ca3af;
}

.agent-avatars {
  display: flex;
  align-items: center;
  gap: -4px;
}

.agent-avatars .el-avatar {
  margin-left: -8px;
  border: 2px solid white;
}

.more-agents {
  font-size: 12px;
  color: #6b7280;
  margin-left: 4px;
}

.agent-selection {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.agent-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.agent-option:hover {
  background: #f3f4f6;
}

.agent-option.selected {
  background: #e0f2fe;
  border-color: #0ea5e9;
}

.agent-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.agent-name {
  font-weight: 500;
  color: #1f2937;
}

.agent-desc {
  font-size: 12px;
  color: #6b7280;
}

.check-icon {
  color: #0ea5e9;
}

.form-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #059669;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
