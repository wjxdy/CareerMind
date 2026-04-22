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

      <div v-if="kbList.length === 0 && !loading" class="empty-wrap">
        <EmptyState title="暂无知识库" description="点击右上角新建" />
      </div>
      <div v-else v-loading="loading" class="kb-grid">
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

      <el-dialog v-model="showCreateDialog" title="新建知识库" width="500px">
        <el-form :model="createForm" ref="createFormRef" label-position="top">
          <el-form-item label="名称" prop="name" required>
            <el-input v-model="createForm.name" placeholder="知识库名称" />
          </el-form-item>
          <el-form-item label="描述" prop="description">
            <el-input v-model="createForm.description" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="类型" prop="kb_type" required>
            <el-radio-group v-model="createForm.kb_type">
              <el-radio label="PERSONAL">个人</el-radio>
              <el-radio label="PUBLIC">公共</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
        <template #footer>
          <BaseButton variant="ghost" @click="showCreateDialog = false">取消</BaseButton>
          <BaseButton variant="primary" :loading="creating" @click="handleCreateKb">创建</BaseButton>
        </template>
      </el-dialog>

      <el-dialog v-model="showUploadDialog" title="上传文档" width="500px">
        <el-upload ref="uploadRef" action="#" :auto-upload="false" :on-change="handleFileChange" :limit="1">
          <BaseButton variant="primary">选择文件</BaseButton>
          <template #tip>
            <div class="muted" style="margin-top:6px;font-size:12px;">支持 PDF、Word、Markdown、HTML、TXT</div>
          </template>
        </el-upload>
        <template #footer>
          <BaseButton variant="ghost" @click="showUploadDialog = false">取消</BaseButton>
          <BaseButton variant="primary" :loading="uploading" @click="handleUpload">上传</BaseButton>
        </template>
      </el-dialog>

      <el-drawer v-model="showDocDrawer" title="文档列表" size="500px">
        <el-table :data="docList" v-loading="docLoading">
          <el-table-column prop="filename" label="文件名" />
          <el-table-column prop="file_type" label="类型" width="80" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <BaseBadge :tone="row.status === 'COMPLETED' ? 'success' : 'warning'">
                {{ row.status === 'COMPLETED' ? '完成' : '处理中' }}
              </BaseBadge>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <BaseButton size="sm" variant="ghost" @click="handleDeleteDoc(row.id)">删除</BaseButton>
            </template>
          </el-table-column>
        </el-table>
      </el-drawer>

      <el-dialog v-model="showTestDialog" title="测试知识库检索" width="600px">
        <el-input v-model="testQuery" type="textarea" :rows="3" placeholder="输入查询内容" />
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
      </el-dialog>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageShell from '@/components/ui/PageShell.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { kbApi } from '@/api/kb'
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

onMounted(() => { fetchKbs() })

const fetchKbs = async () => {
  loading.value = true
  try {
    const res = await kbApi.getKbs({ page: 1, size: 100 })
    kbList.value = res.items
  } catch (e: any) { ElMessage.error(e.message || '获取知识库失败') }
  finally { loading.value = false }
}

const handleCreateKb = async () => {
  if (!createForm.name) { ElMessage.warning('请输入名称'); return }
  creating.value = true
  try {
    await kbApi.createKb({
      name: createForm.name,
      description: createForm.description || undefined,
      kb_type: createForm.kb_type,
    })
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.name = ''; createForm.description = ''; createForm.kb_type = 'PERSONAL'
    fetchKbs()
  } catch (e: any) { ElMessage.error(e.message || '创建失败') }
  finally { creating.value = false }
}

const handleDeleteKb = async (kbId: number) => {
  try {
    await ElMessageBox.confirm('确定删除此知识库？关联文档和向量也会删除。', '提示', { type: 'warning' })
    await kbApi.deleteKb(kbId)
    ElMessage.success('已删除')
    fetchKbs()
  } catch { /* cancelled */ }
}

const openUploadDialog = (kbId: number) => {
  currentKbId.value = kbId
  uploadFile.value = null
  showUploadDialog.value = true
}

const handleFileChange = (_file: any, files: any[]) => {
  if (files.length > 0) uploadFile.value = files[0].raw
}

const handleUpload = async () => {
  if (!currentKbId.value || !uploadFile.value) { ElMessage.warning('请选择文件'); return }
  uploading.value = true
  try {
    await kbApi.uploadDocument(currentKbId.value, uploadFile.value)
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    fetchKbs()
  } catch (e: any) { ElMessage.error(e.message || '上传失败') }
  finally { uploading.value = false }
}

const openDocList = async (kbId: number) => {
  currentKbId.value = kbId
  showDocDrawer.value = true
  docLoading.value = true
  try {
    const res = await kbApi.getDocuments(kbId, { page: 1, size: 100 })
    docList.value = res.items
  } catch (e: any) { ElMessage.error(e.message || '获取文档列表失败') }
  finally { docLoading.value = false }
}

const handleDeleteDoc = async (docId: number) => {
  if (!currentKbId.value) return
  try {
    await ElMessageBox.confirm('确定删除此文档？', '提示', { type: 'warning' })
    await kbApi.deleteDocument(currentKbId.value, docId)
    ElMessage.success('已删除')
    const res = await kbApi.getDocuments(currentKbId.value, { page: 1, size: 100 })
    docList.value = res.items
    fetchKbs()
  } catch { /* cancelled */ }
}

const openTestDialog = (kbId: number) => {
  currentKbId.value = kbId
  testQuery.value = ''
  testResults.value = []
  showTestDialog.value = true
}

const handleTestQuery = async () => {
  if (!currentKbId.value || !testQuery.value.trim()) { ElMessage.warning('请输入查询内容'); return }
  testing.value = true
  try {
    const res = await kbApi.queryKb(currentKbId.value, testQuery.value.trim())
    testResults.value = res.results
  } catch (e: any) { ElMessage.error(e.message || '检索失败') }
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
</style>
