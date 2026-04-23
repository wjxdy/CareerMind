<template>
  <PageShell>
    <div class="kb-page">
      <header class="kb-head">
        <div>
          <h2>知识库</h2>
          <p class="muted">上传背景资料，Agent 讨论时将结合知识库检索</p>
        </div>
        <BaseButton variant="primary" @click="showCreateDialog = true">+ 新建知识库</BaseButton>
      </header>

      <n-spin :show="loading">
        <div v-if="kbList.length === 0 && !loading" class="empty-wrap">
          <EmptyState title="暂无知识库" description="点击右上角新建" />
        </div>
        <div v-else class="kb-grid">
          <BaseCard v-for="kb in kbList" :key="kb.id" hoverable>
            <div class="card-head">
              <BaseBadge :tone="kb.kb_type === 'PUBLIC' ? 'success' : 'neutral'">{{ kb.kb_type === 'PUBLIC' ? '公共' : '个人' }}</BaseBadge>
              <button class="del-btn" @click.stop="handleDeleteKb(kb.id)" title="删除">×</button>
            </div>
            <h4 class="kb-name">{{ kb.name }}</h4>
            <p v-if="kb.description" class="kb-desc">{{ kb.description }}</p>
            <div class="kb-stats">
              <span>📄 {{ kb.document_count }} 文档</span>
              <span>🧩 {{ kb.chunk_count }} 片段</span>
            </div>
            <div class="card-actions">
              <BaseButton size="sm" variant="secondary" @click="openUploadDialog(kb.id)">上传</BaseButton>
              <BaseButton size="sm" variant="ghost" @click="openDocList(kb.id)">文档</BaseButton>
              <BaseButton size="sm" variant="ghost" @click="openTestDialog(kb.id)">测试</BaseButton>
            </div>
          </BaseCard>
        </div>
      </n-spin>

      <n-modal v-model:show="showCreateDialog" preset="card" title="新建知识库" :style="{ width: '500px' }">
        <n-form :model="createForm" ref="createFormRef" label-placement="top">
          <n-form-item label="名称" path="name" :rule="{ required: true, message: '请输入名称' }">
            <n-input v-model:value="createForm.name" placeholder="知识库名称" />
          </n-form-item>
          <n-form-item label="描述" path="description">
            <n-input v-model:value="createForm.description" type="textarea" :rows="2" />
          </n-form-item>
          <n-form-item label="类型" path="kb_type">
            <n-radio-group v-model:value="createForm.kb_type">
              <n-radio value="PERSONAL">个人</n-radio>
              <n-radio value="PUBLIC">公共</n-radio>
            </n-radio-group>
          </n-form-item>
        </n-form>
        <template #footer>
          <div class="dlg-foot">
            <BaseButton variant="ghost" @click="showCreateDialog = false">取消</BaseButton>
            <BaseButton variant="primary" :loading="creating" @click="handleCreateKb">创建</BaseButton>
          </div>
        </template>
      </n-modal>

      <n-modal v-model:show="showUploadDialog" preset="card" title="上传文档" :style="{ width: '500px' }">
        <n-upload ref="uploadRef" :max="1" :default-upload="false" :on-change="handleFileChange">
          <BaseButton variant="primary">选择文件</BaseButton>
        </n-upload>
        <p class="muted" style="margin-top:8px;font-size:12px;">支持 PDF、Word、Markdown、HTML、TXT</p>
        <template #footer>
          <div class="dlg-foot">
            <BaseButton variant="ghost" @click="showUploadDialog = false">取消</BaseButton>
            <BaseButton variant="primary" :loading="uploading" @click="handleUpload">上传</BaseButton>
          </div>
        </template>
      </n-modal>

      <n-drawer v-model:show="showDocDrawer" :width="500">
        <n-drawer-content title="文档列表">
          <n-data-table :columns="docColumns" :data="docList" :loading="docLoading" :bordered="false" />
        </n-drawer-content>
      </n-drawer>

      <n-modal v-model:show="showTestDialog" preset="card" title="测试知识库检索" :style="{ width: '600px' }">
        <n-input v-model:value="testQuery" type="textarea" :rows="3" placeholder="输入查询内容" />
        <div style="margin-top:12px">
          <BaseButton variant="primary" :loading="testing" @click="handleTestQuery">检索</BaseButton>
        </div>
        <div class="test-results" v-if="testResults.length > 0">
          <div v-for="(r, idx) in testResults" :key="idx" class="result-item">
            <div class="result-header">
              <span>来源: {{ r.document.filename }}</span>
              <BaseBadge tone="accent">相关度 {{ r.score.toFixed(3) }}</BaseBadge>
            </div>
            <p class="result-content">{{ r.content }}</p>
          </div>
        </div>
      </n-modal>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { h, ref, reactive, onMounted } from 'vue'
import {
  NModal, NForm, NFormItem, NInput, NRadioGroup, NRadio, NUpload, NDrawer, NDrawerContent,
  NDataTable, NSpin, type UploadFileInfo, type DataTableColumns,
} from 'naive-ui'
import PageShell from '@/components/ui/PageShell.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { kbApi } from '@/api/kb'
import { message, dialog } from '@/utils/naive-discrete'
import type { KnowledgeBase, QueryResult, DocumentItem } from '@/types/kb'

const loading = ref(false)
const kbList = ref<KnowledgeBase[]>([])

const showCreateDialog = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({ name: '', description: '', kb_type: 'PERSONAL' })

const showUploadDialog = ref(false)
const uploading = ref(false)
const currentKbId = ref<number | null>(null)
const uploadFile = ref<File | null>(null)

const showDocDrawer = ref(false)
const docLoading = ref(false)
const docList = ref<DocumentItem[]>([])

const showTestDialog = ref(false)
const testing = ref(false)
const testQuery = ref('')
const testResults = ref<QueryResult[]>([])

const docColumns: DataTableColumns<DocumentItem> = [
  { title: '文件名', key: 'filename' },
  { title: '类型', key: 'file_type', width: 80 },
  {
    title: '状态', key: 'status', width: 90,
    render: (row) => h(BaseBadge, { tone: row.status === 'COMPLETED' ? 'success' : 'warning' }, {
      default: () => row.status === 'COMPLETED' ? '完成' : '处理中',
    }),
  },
  {
    title: '操作', key: 'actions', width: 90,
    render: (row) => h(BaseButton, { size: 'sm', variant: 'ghost', onClick: () => handleDeleteDoc(row.id) }, { default: () => '删除' }),
  },
]

onMounted(() => { fetchKbs() })

const fetchKbs = async () => {
  loading.value = true
  try {
    const res = await kbApi.getKbs({ page: 1, size: 100 })
    kbList.value = res.items
  } catch (e: any) { message.error(e.message || '获取知识库失败') }
  finally { loading.value = false }
}

const handleCreateKb = async () => {
  if (!createForm.name) { message.warning('请输入名称'); return }
  creating.value = true
  try {
    await kbApi.createKb({
      name: createForm.name,
      description: createForm.description || undefined,
      kb_type: createForm.kb_type,
    })
    message.success('创建成功')
    showCreateDialog.value = false
    createForm.name = ''; createForm.description = ''; createForm.kb_type = 'PERSONAL'
    fetchKbs()
  } catch (e: any) { message.error(e.message || '创建失败') }
  finally { creating.value = false }
}

const handleDeleteKb = async (kbId: number) => {
  const ok = await dialog.confirm('删除此知识库？', '关联文档和向量也会一并删除。')
  if (!ok) return
  await kbApi.deleteKb(kbId)
  message.success('已删除')
  fetchKbs()
}

const openUploadDialog = (kbId: number) => {
  currentKbId.value = kbId
  uploadFile.value = null
  showUploadDialog.value = true
}

const handleFileChange = ({ fileList }: { fileList: UploadFileInfo[] }) => {
  if (fileList.length > 0 && fileList[0].file) uploadFile.value = fileList[0].file
}

const handleUpload = async () => {
  if (!currentKbId.value || !uploadFile.value) { message.warning('请选择文件'); return }
  uploading.value = true
  try {
    await kbApi.uploadDocument(currentKbId.value, uploadFile.value)
    message.success('上传成功')
    showUploadDialog.value = false
    fetchKbs()
  } catch (e: any) { message.error(e.message || '上传失败') }
  finally { uploading.value = false }
}

const openDocList = async (kbId: number) => {
  currentKbId.value = kbId
  showDocDrawer.value = true
  docLoading.value = true
  try {
    const res = await kbApi.getDocuments(kbId, { page: 1, size: 100 })
    docList.value = res.items
  } catch (e: any) { message.error(e.message || '获取文档列表失败') }
  finally { docLoading.value = false }
}

const handleDeleteDoc = async (docId: number) => {
  if (!currentKbId.value) return
  const ok = await dialog.confirm('删除此文档？')
  if (!ok) return
  await kbApi.deleteDocument(currentKbId.value, docId)
  message.success('已删除')
  const res = await kbApi.getDocuments(currentKbId.value, { page: 1, size: 100 })
  docList.value = res.items
  fetchKbs()
}

const openTestDialog = (kbId: number) => {
  currentKbId.value = kbId
  testQuery.value = ''
  testResults.value = []
  showTestDialog.value = true
}

const handleTestQuery = async () => {
  if (!currentKbId.value || !testQuery.value.trim()) { message.warning('请输入查询内容'); return }
  testing.value = true
  try {
    const res = await kbApi.queryKb(currentKbId.value, testQuery.value.trim())
    testResults.value = res.results
  } catch (e: any) { message.error(e.message || '检索失败') }
  finally { testing.value = false }
}
</script>

<style scoped>
.kb-page { padding: 32px 40px; max-width: 1200px; margin: 0 auto; overflow-y: auto; height: 100%; }
.kb-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 24px; gap: 20px; }
.kb-head h2 { margin: 0; font-size: 22px; }
.muted { margin: 4px 0 0; font-size: 13px; color: var(--text-secondary); }

.empty-wrap { padding: 48px; background: var(--bg-card); border: 1px dashed var(--border-emphasis); border-radius: var(--radius-lg); }
.kb-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; }

.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.del-btn { background: transparent; border: none; color: var(--text-muted); cursor: pointer; font-size: 18px; line-height: 1; padding: 2px 6px; border-radius: var(--radius-sm); }
.del-btn:hover { background: rgba(239,68,68,0.08); color: var(--danger); }
.kb-name { font-size: 15px; font-weight: 600; margin: 0 0 6px; color: var(--text-primary); }
.kb-desc { font-size: 13px; color: var(--text-secondary); margin: 0 0 12px; min-height: 18px; }
.kb-stats { display: flex; gap: 14px; font-size: 12px; color: var(--text-muted); margin-bottom: 12px; }
.card-actions { display: flex; gap: 6px; flex-wrap: wrap; padding-top: 10px; border-top: 1px solid var(--border-subtle); }

.test-results { margin-top: 16px; max-height: 300px; overflow-y: auto; }
.result-item { padding: 12px; background: var(--bg-inset); border-radius: var(--radius-md); margin-bottom: 8px; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 13px; color: var(--text-secondary); }
.result-content { font-size: 13px; color: var(--text-primary); line-height: 1.5; white-space: pre-wrap; margin: 0; }

.dlg-foot { display: flex; justify-content: flex-end; gap: 8px; }
</style>
