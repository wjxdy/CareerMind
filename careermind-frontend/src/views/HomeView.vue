<template>
  <div class="home-layout">
    <Sidebar />
    <main class="main-content">
      <div class="welcome-container">
        <div class="welcome-header">
          <h1>CareerMind</h1>
          <p class="subtitle">让不同视角的 AI Agent 为你的职业困惑深度讨论</p>
        </div>

        <!-- 快捷输入区域 -->
        <div class="input-section">
          <div class="quick-options">
            <el-tag
              v-for="option in quickOptions"
              :key="option"
              class="quick-tag"
              effect="plain"
              @click="selectQuickOption(option)"
            >
              {{ option }}
            </el-tag>
          </div>

          <div class="input-box">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="4"
              placeholder="描述你的职业困惑，例如：毕业3年的Java后端，纠结是否转AI方向..."
              resize="none"
              class="question-input"
              @keydown.enter.prevent="handleSubmit"
            />

            <!-- 已选择的专家预览 -->
            <div class="selected-agents-preview" v-if="userStore.isLoggedIn && selectedAgents.length > 0">
              <div class="preview-list">
                <el-tag
                  v-for="agent in selectedAgentList"
                  :key="agent.id"
                  class="agent-tag"
                  :style="{ backgroundColor: agentStore.getAgentColor(agent.type) + '20', borderColor: agentStore.getAgentColor(agent.type), color: agentStore.getAgentColor(agent.type) }"
                >
                  {{ agent.name }}
                </el-tag>
                <el-button class="edit-btn" text size="small" @click="showAgentDialog = true">
                  <el-icon><Edit /></el-icon>
                  修改选择
                </el-button>
              </div>
            </div>

            <!-- 已关联的知识库预览 -->
            <div class="selected-kb-preview" v-if="userStore.isLoggedIn">
              <div class="preview-list">
                <el-tag
                  v-if="selectedKb"
                  class="kb-tag"
                  effect="light"
                  type="success"
                >
                  {{ selectedKb.name }}
                </el-tag>
                <el-button class="edit-btn" text size="small" @click="showKbDialog = true">
                  <el-icon><Edit /></el-icon>
                  {{ selectedKb ? '修改关联' : '关联知识库' }}
                </el-button>
              </div>
            </div>

            <div class="input-actions">
              <template v-if="userStore.isLoggedIn">
                <el-button
                  v-if="selectedAgents.length === 0"
                  type="primary"
                  size="large"
                  @click="showAgentDialog = true"
                >
                  <el-icon><User /></el-icon>
                  选择专家
                </el-button>
                <el-button
                  v-else
                  type="primary"
                  size="large"
                  @click="handleSubmit"
                  :loading="loading"
                >
                  <el-icon><ArrowRight /></el-icon>
                  开始咨询
                </el-button>
              </template>
              <el-button
                v-else
                type="primary"
                size="large"
                @click="handleSubmit"
              >
                <el-icon><ArrowRight /></el-icon>
                开始咨询
              </el-button>
            </div>
          </div>
        </div>

        <!-- 特性介绍 -->
        <div class="features">
          <div class="feature-item">
            <el-icon :size="32" color="#3b82f6"><User /></el-icon>
            <h3>多Agent协作</h3>
            <p>行业分析师、风险警示者、机会挖掘者等多个AI专家共同讨论</p>
          </div>
          <div class="feature-item">
            <el-icon :size="32" color="#10b981"><ChatLineRound /></el-icon>
            <h3>深度对话</h3>
            <p>4轮深度讨论，从独立观点到质疑挑战，再到最终整合</p>
          </div>
          <div class="feature-item">
            <el-icon :size="32" color="#f59e0b"><DocumentChecked /></el-icon>
            <h3>可执行方案</h3>
            <p>输出2-3个候选方案，标注置信度和认知盲区</p>
          </div>
        </div>
      </div>
    </main>

    <!-- Agent 选择弹窗 -->
    <el-dialog
      v-model="showAgentDialog"
      title="选择咨询专家"
      width="600px"
      :close-on-click-modal="false"
    >
      <div class="agent-dialog-content">
        <div class="dialog-header">
          <span>已选择 {{ selectedAgents.length }} 位专家</span>
          <el-button type="primary" text size="small" @click="selectDefaultAgents">
            恢复默认选择
          </el-button>
        </div>

        <!-- 预设 Agent -->
        <div class="agent-section">
          <div class="section-title">预设专家</div>
          <div class="agent-grid">
            <div
              v-for="agent in presetAgents"
              :key="agent.id"
              class="agent-card"
              :class="{ selected: selectedAgents.includes(agent.id) }"
              @click="toggleAgent(agent.id)"
            >
              <div class="agent-header">
                <el-avatar
                  :size="40"
                  :style="{ backgroundColor: agentStore.getAgentColor(agent.type) }"
                >
                  <el-icon :size="20">
                    <component :is="agentStore.getAgentIcon(agent.type)" />
                  </el-icon>
                </el-avatar>
                <div class="agent-check" v-if="selectedAgents.includes(agent.id)">
                  <el-icon><Check /></el-icon>
                </div>
              </div>
              <div class="agent-name">{{ agent.name }}</div>
              <div class="agent-desc">{{ agent.description }}</div>
            </div>
          </div>
        </div>

        <!-- 自定义 Agent -->
        <div class="agent-section" v-if="customAgents.length > 0">
          <div class="section-title">我的自定义专家</div>
          <div class="agent-grid">
            <div
              v-for="agent in customAgents"
              :key="agent.id"
              class="agent-card"
              :class="{ selected: selectedAgents.includes(agent.id), custom: true }"
              @click="toggleAgent(agent.id)"
            >
              <div class="agent-header">
                <el-avatar
                  :size="40"
                  :style="{ backgroundColor: agentStore.getAgentColor('CUSTOM') }"
                >
                  <el-icon :size="20"><User /></el-icon>
                </el-avatar>
                <div class="agent-check" v-if="selectedAgents.includes(agent.id)">
                  <el-icon><Check /></el-icon>
                </div>
              </div>
              <div class="agent-name">{{ agent.name }}</div>
              <div class="agent-desc">{{ agent.description || '自定义专家' }}</div>
            </div>
          </div>
        </div>

        <div class="dialog-tip" v-if="selectedAgents.length === 0">
          <el-icon><InfoFilled /></el-icon>
          请至少选择一位专家
        </div>
      </div>

      <template #footer>
        <el-button @click="showAgentDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAgentSelection" :disabled="selectedAgents.length === 0">
          确认选择 ({{ selectedAgents.length }} 位)
        </el-button>
      </template>
    </el-dialog>

    <!-- 知识库选择弹窗 -->
    <el-dialog
      v-model="showKbDialog"
      title="关联知识库"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="kb-dialog-content">
        <div class="dialog-header">
          <span>选择一个知识库（可选）</span>
          <el-button type="primary" text size="small" @click="selectedKbId = undefined">
            清除选择
          </el-button>
        </div>

        <div class="kb-grid">
          <div
            v-for="kb in kbList"
            :key="kb.id"
            class="kb-card"
            :class="{ selected: selectedKbId === kb.id }"
            @click="selectedKbId = kb.id"
          >
            <div class="kb-header">
              <el-icon :size="24" color="#10b981"><DocumentChecked /></el-icon>
              <div class="kb-check" v-if="selectedKbId === kb.id">
                <el-icon><Check /></el-icon>
              </div>
            </div>
            <div class="kb-name">{{ kb.name }}</div>
            <div class="kb-desc">{{ kb.description || '暂无描述' }}</div>
          </div>
        </div>

        <div class="dialog-tip" v-if="kbList.length === 0">
          <el-icon><InfoFilled /></el-icon>
          暂无知识库，请先前往知识库页面创建
        </div>
      </div>

      <template #footer>
        <el-button @click="showKbDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmKbSelection">
          确认{{ selectedKb ? '（已选择: ' + selectedKb.name + '）' : '（不关联知识库）' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import Sidebar from '@/components/layout/Sidebar.vue'
import { useUserStore } from '@/stores/user'
import { useAgentStore } from '@/stores/agent'
import { taskApi } from '@/api/task'
import { kbApi } from '@/api/kb'
import type { KnowledgeBase } from '@/types/kb'

const router = useRouter()
const userStore = useUserStore()
const agentStore = useAgentStore()
const inputText = ref('')
const loading = ref(false)
const selectedAgents = ref<number[]>([])
const showAgentDialog = ref(false)
const kbList = ref<KnowledgeBase[]>([])
const selectedKbId = ref<number | undefined>(undefined)
const showKbDialog = ref(false)

// 已选择的知识库
const selectedKb = computed(() => {
  return kbList.value.find(kb => kb.id === selectedKbId.value)
})

// 预设 Agent
const presetAgents = computed(() => agentStore.availableAgents.filter(a => a.isPreset))

// 自定义 Agent
const customAgents = computed(() => agentStore.availableAgents.filter(a => !a.isPreset))

// 已选择的 Agent 列表
const selectedAgentList = computed(() => {
  return agentStore.availableAgents.filter(a => selectedAgents.value.includes(a.id))
})

const quickOptions = [
  '转行咨询',
  '跳槽评估',
  '考研vs工作',
  '职业规划',
  '技能提升',
  '创业决策',
]

onMounted(() => {
  // 加载Agent列表
  agentStore.fetchAvailableAgents()
  agentStore.fetchPresetAgents()

  // 默认选中前4个预设Agent
  const checkAgents = setInterval(() => {
    const agents = presetAgents.value
    if (agents.length > 0 && selectedAgents.value.length === 0) {
      selectedAgents.value = agents.slice(0, 4).map(a => a.id)
      clearInterval(checkAgents)
    }
  }, 100)
  setTimeout(() => clearInterval(checkAgents), 3000)

  // 加载知识库列表
  if (userStore.isLoggedIn) {
    kbApi.getKbs({ page: 1, size: 100 }).then(res => {
      kbList.value = res.items
    }).catch(() => {
      // 静默失败
    })
  }
})

const toggleAgent = (agentId: number) => {
  const index = selectedAgents.value.indexOf(agentId)
  if (index > -1) {
    selectedAgents.value.splice(index, 1)
  } else {
    selectedAgents.value.push(agentId)
  }
}

const selectDefaultAgents = () => {
  const agents = presetAgents.value
  if (agents.length > 0) {
    selectedAgents.value = agents.slice(0, 4).map(a => a.id)
  }
}

const confirmAgentSelection = () => {
  if (selectedAgents.value.length === 0) {
    ElMessage.warning('请至少选择一位专家')
    return
  }
  showAgentDialog.value = false
}

const confirmKbSelection = () => {
  showKbDialog.value = false
}

const selectQuickOption = (option: string) => {
  const placeholders: Record<string, string> = {
    '转行咨询': '我想从传统行业转行到互联网行业，目前在做销售，想转产品经理，需要做什么准备？',
    '跳槽评估': '现在工作3年，有一个跳槽机会薪资涨30%但工作强度大，要不要去？',
    '考研vs工作': '本科毕业，纠结是考研还是直接工作，想听听建议',
    '职业规划': '工作5年了，感觉职业发展遇到瓶颈，不知道怎么突破',
    '技能提升': '想学习AI相关技能，不知道从哪里开始，需要多长时间',
    '创业决策': '有一个创业想法，想评估一下风险和可行性',
  }
  inputText.value = placeholders[option] || ''
}

const handleSubmit = async () => {
  if (!inputText.value.trim()) {
    ElMessage.warning('请输入你的职业困惑')
    return
  }

  // 检查是否登录
  if (!userStore.isLoggedIn) {
    const confirmed = await ElMessageBox.confirm(
      '需要登录后才能开始咨询，是否前往登录？',
      '提示',
      {
        confirmButtonText: '去登录',
        cancelButtonText: '取消',
        type: 'info',
      }
    ).catch(() => false)

    if (confirmed) {
      localStorage.setItem('tempQuestion', inputText.value)
      router.push('/login')
    }
    return
  }

  // 检查是否选择了Agent
  if (selectedAgents.value.length === 0) {
    showAgentDialog.value = true
    return
  }

  // 直接创建咨询并跳转到讨论页面
  loading.value = true
  try {
    const userBio = localStorage.getItem('userBio') || ''
    const taskData = {
      title: inputText.value.slice(0, 6),
      background: userBio,
      goal: inputText.value,
      constraints: '',
      agentIds: selectedAgents.value,
      kbId: selectedKbId.value
    }

    const task = await taskApi.createTask(taskData)

    // 刷新任务列表
    window.dispatchEvent(new CustomEvent('task-created', { detail: task }))

    ElMessage.success('咨询创建成功')
    inputText.value = ''

    // 跳转到讨论页面
    router.push(`/discussions/${task.id}`)
  } catch (error: any) {
    ElMessage.error(error.message || '创建咨询失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.home-layout {
  display: flex;
  height: 100vh;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  overflow: hidden;
}

.welcome-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px;
  max-width: 900px;
  margin: 0 auto;
}

.welcome-header {
  text-align: center;
  margin-bottom: 48px;
}

.welcome-header h1 {
  font-size: 48px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 16px;
}

.subtitle {
  font-size: 18px;
  color: #6b7280;
}

.input-section {
  width: 100%;
  max-width: 700px;
}

.quick-options {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin-bottom: 24px;
}

.quick-tag {
  cursor: pointer;
  padding: 8px 16px;
  font-size: 14px;
  border-radius: 20px;
}

.quick-tag:hover {
  background: #e0f2fe;
  border-color: #0ea5e9;
  color: #0ea5e9;
}

.input-box {
  background: #f9fafb;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
}

.question-input :deep(.el-textarea__inner) {
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 16px;
  font-size: 16px;
  resize: none;
}

.question-input :deep(.el-textarea__inner:focus) {
  border-color: #0ea5e9;
}

/* 已选择的专家预览 */
.selected-agents-preview {
  margin-top: 12px;
}

.preview-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.preview-list .edit-btn {
  color: #9ca3af;
  margin-left: 4px;
}

.preview-list .edit-btn:hover {
  color: #6b7280;
}

.agent-tag {
  font-size: 13px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.input-actions :deep(.el-button) {
  border-radius: 8px;
  padding: 12px 32px;
  font-size: 16px;
}

.selected-kb-preview {
  margin-top: 12px;
}

.selected-kb-preview .preview-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.kb-tag {
  font-size: 13px;
}

.features {
  display: flex;
  gap: 48px;
  margin-top: 64px;
}

.feature-item {
  text-align: center;
  max-width: 200px;
}

.feature-item h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 16px 0 8px;
}

.feature-item p {
  font-size: 14px;
  color: #6b7280;
  line-height: 1.5;
}

/* Agent 选择弹窗 */
.agent-dialog-content {
  max-height: 500px;
  overflow-y: auto;
}

.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 14px;
  color: #374151;
}

.agent-section {
  margin-bottom: 24px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 12px;
}

.agent-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.agent-card {
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.agent-card:hover {
  background: #f3f4f6;
}

.agent-card.selected {
  background: #e0f2fe;
  border-color: #0ea5e9;
}

.agent-card.custom {
  background: #f3f4f6;
}

.agent-card.custom.selected {
  background: #e0f2fe;
}

.agent-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.agent-check {
  width: 24px;
  height: 24px;
  background: #0ea5e9;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.agent-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.agent-card.selected .agent-name {
  color: #0ea5e9;
}

.agent-desc {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.dialog-tip {
  margin-top: 16px;
  padding: 12px;
  background: #fffbeb;
  border-radius: 8px;
  font-size: 13px;
  color: #f59e0b;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 知识库选择弹窗 */
.kb-dialog-content {
  max-height: 400px;
  overflow-y: auto;
}

.kb-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.kb-card {
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.kb-card:hover {
  background: #f3f4f6;
}

.kb-card.selected {
  background: #d1fae5;
  border-color: #10b981;
}

.kb-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.kb-check {
  width: 24px;
  height: 24px;
  background: #10b981;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
}

.kb-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.kb-card.selected .kb-name {
  color: #10b981;
}

.kb-desc {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
