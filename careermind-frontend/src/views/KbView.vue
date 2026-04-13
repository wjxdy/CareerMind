<template>
  <div class="page-layout">
    <Sidebar />
    <main class="page-content">
      <div class="page-header">
        <h2>知识库管理</h2>
        <el-button type="primary" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          新建知识库
        </el-button>
      </div>

      <div class="kb-grid" v-loading="loading">
        <el-card
          v-for="kb in kbList"
          :key="kb.id"
          class="kb-card"
          shadow="hover"
        >
          <div class="card-header">
            <el-tag :type="kb.kb_type === 'PUBLIC' ? 'success' : 'info'" size="small">
              {{ kb.kb_type === 'PUBLIC' ? '公共' : '个人' }}
            </el-tag>
            <el-icon class="delete-icon" @click.stop="handleDeleteKb(kb.id)"><Delete /></el-icon>
          </div>
          <h3 class="kb-name">{{ kb.name }}</h3>
          <p class="kb-desc" v-if="kb.description">{{ kb.description }}</p>
          <div class="kb-stats">
            <span><el-icon><Document /></el-icon> {{ kb.document_count }} 文档</span>
            <span><el-icon><Memo /></el-icon> {{ kb.chunk_count }} 片段</span>
          </div>
          <div class="card-actions">
            <el-button size="small" @click="openUploadDialog(kb.id)">上传文档</el-button>
            <el-button size="small" @click="openDocList(kb.id)">查看文档</el-button>
            <el-button size="small" @click="openTestDialog(kb.id)">测试检索</el-button>
          </div>
        </el-card>
      </div>

      <el-empty v-if="kbList.length === 0 && !loading" description="暂无知识库" />
    </main>

    <!-- 创建知识库对话框 -->
    <el-dialog v-model="showCreateDialog" title="新建知识库" width="500px">
      <el-form :model="createForm" ref="createFormRef" label-position="top">
        <el-form-item label="名称" prop="name" required>
          <el-input v-model="createForm.name" placeholder="知识库名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="createForm.description" type="textarea" :rows="2" placeholder="描述知识库的用途" />
        </el-form-item>
        <el-form-item label="类型" prop="kb_type" required>
          <el-radio-group v-model="createForm.kb_type">
            <el-radio label="PERSONAL">个人</el-radio>
            <el-radio label="PUBLIC">公共</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateKb" :loading="creating">创建</el-button>
      </template>
    </el-dialog>

    <!-- 上传文档对话框 -->
    <el-dialog v-model="showUploadDialog" title="上传文档" width="500px">
      <el-upload
        ref="uploadRef"
        action="#"
        :auto-upload="false"
        :on-change="handleFileChange"
        :limit="1"
      >
        <el-button type="primary">选择文件</el-button>
        <template #tip>
          <div class="el-upload__tip">支持 PDF、Word、Markdown、HTML、TXT</div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpload" :loading="uploading">上传</el-button>
      </template>
    </el-dialog>

    <!-- 文档列表抽屉 -->
    <el-drawer v-model="showDocDrawer" :title="`文档列表`" size="500px">
      <el-table :data="docList" v-loading="docLoading">
        <el-table-column prop="filename" label="文件名" />
        <el-table-column prop="file_type" label="类型" width="80" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'" size="small">
              {{ row.status === 'COMPLETED' ? '完成' : '处理中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button size="small" type="danger" link @click="handleDeleteDoc(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-drawer>

    <!-- 测试检索对话框 -->
    <el-dialog v-model="showTestDialog" title="测试知识库检索" width="600px">
      <el-input
        v-model="testQuery"
        type="textarea"
        :rows="3"
        placeholder="输入查询内容"
      />
      <el-button class="test-btn" type="primary" @click="handleTestQuery" :loading="testing">检索</el-button>
      <div class="test-results" v-if="testResults.length > 0">
        <div v-for="(result, idx) in testResults" :key="idx" class="result-item">
          <div class="result-header">
            <span>来源: {{ result.document.filename }}</span>
            <el-tag size="small">相关度: {{ result.score.toFixed(3) }}</el-tag>
          </div>
          <p class="result-content">{{ result.content }}</p>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import Sidebar from '@/components/layout/Sidebar.vue'
import { kbApi } from '@/api/kb'
import type { KnowledgeBase, QueryResult } from '@/types/kb'
import type { DocumentItem } from '@/types/kb'

const loading = ref(false)
const kbList = ref<KnowledgeBase[]>([])

const showCreateDialog = ref(false)
const creating = ref(false)
const createFormRef = ref()
const createForm = reactive({
  name: '',
  description: '',
  kb_type: 'PERSONAL',
})

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

onMounted(() => {
  fetchKbs()
})

const fetchKbs = async () => {
  loading.value = true
  try {
    const res = await kbApi.getKbs({ page: 1, size: 100 })
    kbList.value = res.items
  } catch (error: any) {
    ElMessage.error(error.message || '获取知识库失败')
  } finally {
    loading.value = false
  }
}

const handleCreateKb = async () => {
  if (!createForm.name) {
    ElMessage.warning('请输入知识库名称')
    return
  }
  creating.value = true
  try {
    await kbApi.createKb({
      name: createForm.name,
      description: createForm.description || undefined,
      kb_type: createForm.kb_type,
    })
    ElMessage.success('创建成功')
    showCreateDialog.value = false
    createForm.name = ''
    createForm.description = ''
    createForm.kb_type = 'PERSONAL'
    fetchKbs()
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败')
  } finally {
    creating.value = false
  }
}

const handleDeleteKb = async (kbId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这个知识库吗？关联的文档和向量数据也会删除。', '提示', {
      type: 'warning',
    })
    await kbApi.deleteKb(kbId)
    ElMessage.success('删除成功')
    fetchKbs()
  } catch {
    // 取消
  }
}

const openUploadDialog = (kbId: number) => {
  currentKbId.value = kbId
  uploadFile.value = null
  showUploadDialog.value = true
}

const handleFileChange = (_file: any, files: any[]) => {
  if (files.length > 0) {
    uploadFile.value = files[0].raw
  }
}

const handleUpload = async () => {
  if (!currentKbId.value || !uploadFile.value) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    await kbApi.uploadDocument(currentKbId.value, uploadFile.value)
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    fetchKbs()
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

const openDocList = async (kbId: number) => {
  currentKbId.value = kbId
  showDocDrawer.value = true
  docLoading.value = true
  try {
    const res = await kbApi.getDocuments(kbId, { page: 1, size: 100 })
    docList.value = res.items
  } catch (error: any) {
    ElMessage.error(error.message || '获取文档列表失败')
  } finally {
    docLoading.value = false
  }
}

const handleDeleteDoc = async (docId: number) => {
  if (!currentKbId.value) return
  try {
    await ElMessageBox.confirm('确定要删除这个文档吗？', '提示', { type: 'warning' })
    await kbApi.deleteDocument(currentKbId.value, docId)
    ElMessage.success('删除成功')
    const res = await kbApi.getDocuments(currentKbId.value, { page: 1, size: 100 })
    docList.value = res.items
    fetchKbs()
  } catch {
    // 取消
  }
}

const openTestDialog = (kbId: number) => {
  currentKbId.value = kbId
  testQuery.value = ''
  testResults.value = []
  showTestDialog.value = true
}

const handleTestQuery = async () => {
  if (!currentKbId.value || !testQuery.value.trim()) {
    ElMessage.warning('请输入查询内容')
    return
  }
  testing.value = true
  try {
    const res = await kbApi.queryKb(currentKbId.value, testQuery.value.trim())
    testResults.value = res.results
  } catch (error: any) {
    ElMessage.error(error.message || '检索失败')
  } finally {
    testing.value = false
  }
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
.kb-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}
.kb-card {
  border-radius: 12px;
  transition: all 0.3s;
}
.kb-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
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
.kb-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 8px;
}
.kb-desc {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 12px;
  min-height: 20px;
}
.kb-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 16px;
}
.kb-stats span {
  display: flex;
  align-items: center;
  gap: 4px;
}
.card-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.test-btn {
  margin-top: 12px;
}
.test-results {
  margin-top: 16px;
  max-height: 300px;
  overflow-y: auto;
}
.result-item {
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 8px;
}
.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 13px;
  color: #374151;
}
.result-content {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;
  white-space: pre-wrap;
}
</style>
