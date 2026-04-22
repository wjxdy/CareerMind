<template>
  <PageShell>
    <div class="agents-page">
      <header class="ap-head">
        <div>
          <h2>Agent 管理</h2>
          <p class="muted">5 位预设专家 + 你创建的自定义 Agent</p>
        </div>
        <BaseButton variant="primary" @click="openCreate">+ 新建自定义 Agent</BaseButton>
      </header>

      <section>
        <h3 class="sec-sub">预设专家</h3>
        <div class="grid">
          <AgentCard v-for="a in presetList" :key="a.id" :type="a.type" :name="a.name" :role="a.description || undefined" />
        </div>
      </section>

      <section>
        <h3 class="sec-sub">我的自定义</h3>
        <div v-if="customAgents.length === 0" class="empty-wrap">
          <EmptyState title="还没有自定义 Agent" description="点击右上角新建" />
        </div>
        <div v-else class="grid">
          <AgentCard v-for="a in customAgents" :key="a.id" :type="a.type" :name="a.name" :role="a.description || undefined">
            <div class="card-actions">
              <BaseButton variant="ghost" size="sm" @click="handleEdit(a)">编辑</BaseButton>
              <BaseButton variant="ghost" size="sm" @click="handleDelete(a.id)">删除</BaseButton>
            </div>
          </AgentCard>
        </div>
      </section>

      <el-dialog v-model="showCreateDialog" :title="isEditing ? '编辑 Agent' : '创建 Agent'" width="600px">
        <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
          <el-form-item label="角色名称" prop="name">
            <el-input v-model="form.name" placeholder="如：前大厂面试官、海归学姐" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="form.description" type="textarea" :rows="2" placeholder="简要描述这个角色的特点" />
          </el-form-item>
          <el-form-item label="系统提示词" prop="systemPrompt">
            <el-input v-model="form.systemPrompt" type="textarea" :rows="6" placeholder="定义角色的背景、立场、说话风格…" />
          </el-form-item>
          <el-form-item label="使用模型" prop="modelType">
            <el-select v-model="form.modelType" class="w-full">
              <el-option label="Kimi (Moonshot)" value="KIMI" />
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <BaseButton variant="ghost" @click="showCreateDialog = false">取消</BaseButton>
          <BaseButton variant="primary" :loading="submitting" @click="handleSubmit">
            {{ isEditing ? '保存' : '创建' }}
          </BaseButton>
        </template>
      </el-dialog>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import PageShell from '@/components/ui/PageShell.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import AgentCard from '@/components/agent/AgentCard.vue'
import { useAgentStore } from '@/stores/agent'
import type { Agent } from '@/types'

const agentStore = useAgentStore()

const presetList = computed(() => agentStore.presetAgents)
const customAgents = computed(() => agentStore.availableAgents.filter(a => !a.isPreset))

const showCreateDialog = ref(false)
const isEditing = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const editingId = ref<number | null>(null)

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

const openCreate = () => {
  resetForm()
  showCreateDialog.value = true
}

const handleEdit = (agent: Agent) => {
  isEditing.value = true
  editingId.value = agent.id
  form.name = agent.name
  form.description = agent.description || ''
  form.systemPrompt = agent.systemPrompt || ''
  form.modelType = agent.modelType || 'KIMI'
  showCreateDialog.value = true
}

const handleDelete = async (agentId: number) => {
  try {
    await ElMessageBox.confirm('确定删除此 Agent？', '提示', { type: 'warning' })
    await agentStore.deleteAgent(agentId)
    ElMessage.success('已删除')
  } catch { /* cancelled */ }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEditing.value && editingId.value) {
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
.agents-page { padding: 32px 40px; max-width: 1200px; margin: 0 auto; overflow-y: auto; height: 100%; }
.ap-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; gap: 20px; }
.ap-head h2 { margin: 0; font-size: 22px; }
.muted { margin: 4px 0 0; font-size: 13px; color: var(--text-secondary); }

section { margin-bottom: 32px; }
.sec-sub { font-size: 14px; color: var(--text-secondary); margin: 0 0 12px; font-weight: 500; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px; }

.empty-wrap { padding: 40px; background: var(--bg-card); border: 1px dashed var(--border-emphasis); border-radius: var(--radius-lg); }

.card-actions { display: flex; justify-content: center; gap: 8px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border-subtle); width: 100%; }
.w-full { width: 100%; }
</style>
