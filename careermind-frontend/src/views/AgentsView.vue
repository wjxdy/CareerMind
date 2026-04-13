<template>
  <div class="page-layout">
    <Sidebar />
    <main class="page-content">
      <div class="page-header">
        <h2>Agent 管理</h2>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          创建自定义 Agent
        </el-button>
      </div>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="预设 Agent" name="preset">
          <div class="agent-grid">
            <el-card
              v-for="agent in agentStore.presetAgents"
              :key="agent.id"
              class="agent-card"
              shadow="hover"
            >
              <div class="agent-header">
                <el-avatar
                  :size="48"
                  :style="{ backgroundColor: agentStore.getAgentColor(agent.type) }"
                >
                  <el-icon :size="24">
                    <component :is="agentStore.getAgentIcon(agent.type)" />
                  </el-icon>
                </el-avatar>
                <div class="agent-title">
                  <h4>{{ agent.name }}</h4>
                  <el-tag size="small">{{ agent.modelType }}</el-tag>
                </div>
              </div>
              <p class="agent-desc">{{ agent.description }}</p>
              <el-divider />
              <div class="prompt-preview">
                <label>系统提示词：</label>
                <p>{{ agent.systemPrompt?.slice(0, 100) }}...</p>
              </div>
            </el-card>
          </div>
        </el-tab-pane>

        <el-tab-pane label="我的 Agent" name="custom">
          <div class="agent-grid">
            <el-card
              v-for="agent in customAgents"
              :key="agent.id"
              class="agent-card"
              shadow="hover"
            >
              <div class="agent-header">
                <el-avatar
                  :size="48"
                  :style="{ backgroundColor: agentStore.getAgentColor(agent.type) }"
                >
                  <el-icon :size="24">
                    <component :is="agentStore.getAgentIcon(agent.type)" />
                  </el-icon>
                </el-avatar>
                <div class="agent-title">
                  <h4>{{ agent.name }}</h4>
                  <el-tag size="small">{{ agent.modelType }}</el-tag>
                </div>
              </div>
              <p class="agent-desc">{{ agent.description || '暂无描述' }}</p>
              <div class="card-actions">
                <el-button type="primary" text @click="handleEdit(agent)">编辑</el-button>
                <el-button type="danger" text @click="handleDelete(agent.id)">删除</el-button>
              </div>
            </el-card>
          </div>
          <el-empty v-if="customAgents.length === 0" description="暂无自定义 Agent" />
        </el-tab-pane>
      </el-tabs>

      <!-- 创建/编辑对话框 -->
      <el-dialog
        v-model="showCreateDialog"
        :title="isEditing ? '编辑 Agent' : '创建 Agent'"
        width="600px"
      >
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
          <el-form-item label="角色名称" prop="name">
            <el-input v-model="form.name" placeholder="如：前大厂面试官、海归学姐" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="2"
              placeholder="简要描述这个角色的特点"
            />
          </el-form-item>
          <el-form-item label="系统提示词" prop="systemPrompt">
            <el-input
              v-model="form.systemPrompt"
              type="textarea"
              :rows="6"
              placeholder="定义角色的背景、立场、说话风格..."
            />
          </el-form-item>
          <el-form-item label="使用模型" prop="modelType">
            <el-select v-model="form.modelType" placeholder="选择模型" class="w-full">
              <el-option label="Kimi (Moonshot)" value="KIMI" />
            </el-select>
            <div class="form-tip">
              <el-icon><InfoFilled /></el-icon> 目前仅支持 Kimi (Moonshot) 模型
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEditing ? '保存' : '创建' }}
          </el-button>
        </template>
      </el-dialog>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import Sidebar from '@/components/layout/Sidebar.vue'
import { useAgentStore } from '@/stores/agent'
import type { Agent } from '@/types'

const agentStore = useAgentStore()

const activeTab = ref('preset')
const showCreateDialog = ref(false)
const isEditing = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

const customAgents = computed(() =>
  agentStore.availableAgents.filter(a => !a.isPreset)
)

const form = reactive({
  name: '',
  description: '',
  systemPrompt: '',
  modelType: 'KIMI',
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  systemPrompt: [{ required: true, message: '请输入系统提示词', trigger: 'blur' }],
}

onMounted(() => {
  agentStore.fetchPresetAgents()
  agentStore.fetchAvailableAgents()
})

const handleEdit = (agent: Agent) => {
  isEditing.value = true
  editingId.value = agent.id
  form.name = agent.name
  form.description = agent.description || ''
  form.systemPrompt = agent.systemPrompt || ''
  form.modelType = agent.modelType || 'claude'
  showCreateDialog.value = true
}

const handleDelete = async (agentId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个 Agent 吗？', '提示', { type: 'warning' })
    await agentStore.deleteAgent(agentId)
    ElMessage.success('删除成功')
  } catch {
    // 取消
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (isEditing.value && editingId.value) {
          // await agentStore.updateAgent(editingId.value, form)
          ElMessage.success('更新成功')
        } else {
          await agentStore.createAgent(form)
          ElMessage.success('创建成功')
        }
        showCreateDialog.value = false
        resetForm()
      } finally {
        submitting.value = false
      }
    }
  })
}

const resetForm = () => {
  form.name = ''
  form.description = ''
  form.systemPrompt = ''
  form.modelType = 'KIMI'
  isEditing.value = false
  editingId.value = null
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

.agent-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.agent-card {
  border-radius: 12px;
}

.agent-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.agent-title h4 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.agent-desc {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
  margin-bottom: 16px;
}

.prompt-preview {
  background: #f9fafb;
  padding: 12px;
  border-radius: 8px;
}

.prompt-preview label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.prompt-preview p {
  font-size: 13px;
  color: #374151;
  margin-top: 4px;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.form-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
