<template>
  <PageShell>
    <div class="tasks-page">
      <header class="page-head">
        <div>
          <h2>我的咨询</h2>
          <p class="muted">发起一次新的决策辩论，或继续已有讨论</p>
        </div>
        <BaseButton variant="primary" size="md" @click="openCreate">+ 新建咨询</BaseButton>
      </header>

      <section class="my-tasks">
        <div class="filter-tabs">
          <button v-for="f in filters" :key="f.val" class="tab" :class="{ on: statusFilter === f.val }" @click="statusFilter = f.val">
            {{ f.label }}
          </button>
        </div>

        <n-spin :show="taskStore.loading">
          <div v-if="filtered.length === 0 && !taskStore.loading" class="empty-box">
            <EmptyState title="还没有咨询" description="点击右上角「新建咨询」开始" />
          </div>
          <div v-else class="tasks-grid">
            <BaseCard v-for="t in filtered" :key="t.id" hoverable>
              <div class="card-body" @click="goToTask(t.id)">
                <div class="card-head">
                  <BaseBadge :tone="toneOfStatus(t.status)">{{ labelOfStatus(t.status) }}</BaseBadge>
                  <span class="date">{{ formatDate(t.createdAt) }}</span>
                </div>
                <h4 class="t-title">{{ t.title }}</h4>
                <p class="t-goal">{{ (t.goal || '').slice(0, 100) }}</p>
                <div class="t-meta">
                  <AgentAvatarGroup v-if="t.agents?.length" :agents="t.agents.map(a => ({ id: a.id, type: a.type }))" :size="30" :max="5" />
                  <button class="del-btn" @click.stop="handleDelete(t.id)" title="删除">×</button>
                </div>
              </div>
            </BaseCard>
          </div>
        </n-spin>
      </section>
    </div>

    <n-modal v-model:show="showCreateDialog" preset="card" title="新建职业咨询" :style="{ width: '600px' }" :mask-closable="false">
      <n-form :model="createForm" :rules="rules" ref="formRef" label-placement="top" require-mark-placement="right-hanging">
        <n-form-item label="咨询主题" path="title">
          <n-input v-model:value="createForm.title" placeholder="给你的咨询起个标题" />
        </n-form-item>
        <n-form-item label="背景信息" path="background">
          <n-input v-model:value="createForm.background" type="textarea" :rows="3" placeholder="教育背景、工作经历等" />
        </n-form-item>
        <n-form-item label="目标 / 困惑" path="goal">
          <n-input v-model:value="createForm.goal" type="textarea" :rows="4" placeholder="详细描述你面临的职业选择或困惑" />
        </n-form-item>
        <n-form-item label="约束条件" path="constraints">
          <n-input v-model:value="createForm.constraints" type="textarea" :rows="2" placeholder="时间、资金、家庭等（可选）" />
        </n-form-item>
        <n-form-item label="选择专家" path="agentIds">
          <div class="agent-selection">
            <label v-for="agent in agentStore.availableAgents" :key="agent.id"
                   class="agent-option" :class="{ selected: createForm.agentIds.includes(agent.id) }"
                   :data-agent-type="agent.type" @click="toggleAgent(agent.id)">
              <AgentAvatar :agent-type="agent.type" :size="30" />
              <div class="agent-info">
                <span class="agent-name">{{ agent.name }}</span>
                <span class="agent-desc">{{ agent.description }}</span>
              </div>
              <span v-if="createForm.agentIds.includes(agent.id)" class="check">✓</span>
            </label>
          </div>
        </n-form-item>
        <n-form-item label="关联知识库（可选）" path="kbId">
          <n-select v-model:value="createForm.kbId" :options="kbOptions" placeholder="选择知识库为讨论提供背景资料" clearable />
        </n-form-item>
      </n-form>
      <template #footer>
        <div class="dlg-foot">
          <BaseButton variant="ghost" @click="showCreateDialog = false">取消</BaseButton>
          <BaseButton variant="primary" :loading="creating" @click="handleCreate">创建咨询</BaseButton>
        </div>
      </template>
    </n-modal>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { NModal, NForm, NFormItem, NInput, NSelect, NSpin, type FormInst, type FormRules } from 'naive-ui'
import PageShell from '@/components/ui/PageShell.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import AgentAvatar from '@/components/agent/AgentAvatar.vue'
import AgentAvatarGroup from '@/components/agent/AgentAvatarGroup.vue'
import { useTaskStore } from '@/stores/task'
import { useAgentStore } from '@/stores/agent'
import { kbApi } from '@/api/kb'
import { message, dialog } from '@/utils/naive-discrete'
import type { KnowledgeBase } from '@/types/kb'
import type { TaskStatus } from '@/types'
import dayjs from 'dayjs'

const router = useRouter()
const taskStore = useTaskStore()
const agentStore = useAgentStore()

const statusFilter = ref<'all' | TaskStatus>('all')
const filters = [
  { val: 'all' as const, label: '全部' },
  { val: 'DISCUSSING' as const, label: '讨论中' },
  { val: 'COMPLETED' as const, label: '已完成' },
  { val: 'PENDING' as const, label: '待开始' },
]

const filtered = computed(() =>
  taskStore.tasks.filter(t => statusFilter.value === 'all' || t.status === statusFilter.value)
)

const showCreateDialog = ref(false)
const creating = ref(false)
const formRef = ref<FormInst | null>(null)
const kbList = ref<KnowledgeBase[]>([])
const kbOptions = computed(() => kbList.value.map(k => ({ label: k.name, value: k.id })))

const createForm = reactive({
  title: localStorage.getItem('tempQuestion')?.slice(0, 6) || '',
  background: localStorage.getItem('userBio') || '',
  goal: localStorage.getItem('tempQuestion') || '',
  constraints: '',
  agentIds: [] as number[],
  kbId: undefined as number | undefined,
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入咨询主题', trigger: ['blur', 'input'] }],
  goal: [{ required: true, message: '请描述你的目标或困惑', trigger: ['blur', 'input'] }],
  agentIds: [{
    type: 'array' as const,
    required: true,
    validator: (_rule: any, value: number[]) =>
      Array.isArray(value) && value.length > 0 ? true : new Error('请至少选择一位专家'),
    trigger: ['change'],
  }],
}

watch(() => createForm.goal, (newGoal) => {
  if (newGoal && !createForm.title) createForm.title = newGoal.slice(0, 6)
})

watch(() => showCreateDialog.value, async (isOpen) => {
  if (!isOpen) { createForm.kbId = undefined; return }
  const savedBio = localStorage.getItem('userBio')
  if (savedBio) createForm.background = savedBio
  if (createForm.agentIds.length === 0 && agentStore.availableAgents.length > 0) {
    createForm.agentIds = agentStore.availableAgents.filter(a => a.isPreset).slice(0, 5).map(a => a.id)
  }
  try {
    const res = await kbApi.getKbs({ page: 1, size: 100 })
    kbList.value = res.items
  } catch { /* noop */ }
})

const openCreate = () => { showCreateDialog.value = true }

const toggleAgent = (agentId: number) => {
  const idx = createForm.agentIds.indexOf(agentId)
  if (idx > -1) createForm.agentIds.splice(idx, 1)
  else createForm.agentIds.push(agentId)
}

const handleCreate = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch { return }
  creating.value = true
  try {
    const task = await taskStore.createTask(createForm)
    message.success('创建成功')
    showCreateDialog.value = false
    localStorage.removeItem('tempQuestion')
    router.push(`/discussions/${task.id}`)
  } finally {
    creating.value = false
  }
}

const goToTask = (id: number) => router.push(`/discussions/${id}`)

const handleDelete = async (id: number) => {
  const ok = await dialog.confirm('删除此咨询？', '删除后不可恢复。')
  if (!ok) return
  await taskStore.deleteTask(id)
  message.success('已删除')
}

const toneOfStatus = (s: TaskStatus) => ({
  PENDING: 'neutral', DISCUSSING: 'accent', MERGING: 'warning', COMPLETED: 'success', ARCHIVED: 'neutral',
}[s] as 'neutral'|'accent'|'warning'|'success')

const labelOfStatus = (s: TaskStatus) => ({
  PENDING: '待开始', DISCUSSING: '讨论中', MERGING: '整合中', COMPLETED: '已完成', ARCHIVED: '已归档',
}[s] || s)

const formatDate = (d: string) => dayjs(d).format('MM-DD HH:mm')

onMounted(() => {
  taskStore.fetchTasks()
  agentStore.fetchAvailableAgents()
})
</script>

<style scoped>
.tasks-page { padding: 32px 40px; max-width: 1200px; margin: 0 auto; overflow-y: auto; height: 100%; }
.page-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; gap: 20px; }
.page-head h2 { margin: 0; font-size: 22px; }
.muted { margin: 4px 0 0; font-size: 13px; color: var(--text-secondary); }

.filter-tabs { display: inline-flex; gap: 4px; background: var(--bg-elevated); padding: 3px; border-radius: var(--radius-full); margin-bottom: 20px; }
.tab { padding: 4px 14px; background: transparent; border: none; cursor: pointer; border-radius: var(--radius-full); font-size: 13px; color: var(--text-secondary); }
.tab.on { background: var(--bg-card); color: var(--text-primary); box-shadow: var(--shadow-sm); }

.tasks-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }
.card-body { cursor: pointer; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.date { font-size: 12px; color: var(--text-muted); }
.t-title { margin: 0 0 6px; font-size: 15px; font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.t-goal  { margin: 0 0 16px; font-size: 13px; color: var(--text-secondary); height: 38px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; line-height: 1.4; }
.t-meta  { display: flex; align-items: center; justify-content: space-between; }
.del-btn { background: transparent; border: none; color: var(--text-muted); cursor: pointer; font-size: 18px; line-height: 1; padding: 2px 6px; border-radius: var(--radius-sm); }
.del-btn:hover { background: rgba(239,68,68,0.08); color: var(--danger); }

.empty-box { padding: 48px; background: var(--bg-card); border: 1px dashed var(--border-emphasis); border-radius: var(--radius-lg); }

.agent-selection { display: flex; flex-direction: column; gap: 8px; max-height: 280px; overflow-y: auto; width: 100%; }
.agent-option {
  display: flex; align-items: center; gap: 12px; padding: 10px;
  border-radius: var(--radius-md); cursor: pointer;
  border: 2px solid transparent; transition: all var(--duration-fast) var(--ease-standard);
}
.agent-option:hover { background: var(--bg-elevated); }
.agent-option.selected { background: var(--agent-dim); border-color: var(--agent); }
.agent-info { flex: 1; display: flex; flex-direction: column; }
.agent-name { font-weight: 500; color: var(--text-primary); font-size: 13px; }
.agent-desc { font-size: 12px; color: var(--text-secondary); }
.check { color: var(--agent); font-weight: 700; }

.dlg-foot { display: flex; justify-content: flex-end; gap: 8px; }
</style>
