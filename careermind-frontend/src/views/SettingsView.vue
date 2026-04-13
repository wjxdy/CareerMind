<template>
  <div class="page-layout">
    <Sidebar />
    <main class="page-content">
      <div class="settings-container">
        <div class="page-header">
          <h2>个人设置</h2>
        </div>

        <el-card class="settings-card">
          <template #header>
            <div class="card-header">
              <span>个人简介</span>
              <el-tag type="info">新建咨询时将自动使用</el-tag>
            </div>
          </template>

          <el-form :model="form" label-position="top">
            <el-form-item label="个人简介">
              <el-input
                v-model="form.bio"
                type="textarea"
                :rows="6"
                placeholder="请描述你的教育背景、工作经历、技能特长等，这将作为咨询的背景信息..."
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="saveBio" :loading="saving">
                保存设置
              </el-button>
            </el-form-item>
          </el-form>

          <div class="tips">
            <h4>💡 提示</h4>
            <ul>
              <li>填写完整的个人简介后，新建咨询时会自动将其作为背景信息</li>
              <li>咨询主题会自动取自你输入的目标/困惑的前6个字</li>
              <li>你可以随时修改个人简介，不会影响已创建的咨询</li>
            </ul>
          </div>
        </el-card>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import Sidebar from '@/components/layout/Sidebar.vue'

const form = ref({
  bio: ''
})
const saving = ref(false)

onMounted(() => {
  // 从 localStorage 加载个人简介
  const savedBio = localStorage.getItem('userBio')
  if (savedBio) {
    form.value.bio = savedBio
  }
})

const saveBio = async () => {
  saving.value = true
  try {
    // 保存到 localStorage
    localStorage.setItem('userBio', form.value.bio)
    ElMessage.success('个人简介已保存')
  } catch (error) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
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
  background: #f9fafb;
}

.settings-container {
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.settings-card {
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tips {
  margin-top: 24px;
  padding: 16px;
  background: #eff6ff;
  border-radius: 8px;
  border-left: 4px solid #3b82f6;
}

.tips h4 {
  margin: 0 0 8px 0;
  color: #1e40af;
}

.tips ul {
  margin: 0;
  padding-left: 20px;
  color: #1e40af;
}

.tips li {
  margin-bottom: 4px;
}
</style>
