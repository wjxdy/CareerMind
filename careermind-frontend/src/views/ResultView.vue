<template>
  <div class="page-layout">
    <Sidebar />
    <main class="page-content">
      <div class="result-container">
        <div class="result-header">
          <h2>讨论结果</h2>
          <div class="header-actions">
            <el-tag v-if="mergeResult" type="success">整合完成</el-tag>
            <el-tag v-else-if="isStreaming" type="warning">生成中...</el-tag>
            <el-button @click="$router.back()">返回</el-button>
          </div>
        </div>

        <div v-if="loading" class="loading-state">
          <el-skeleton :rows="10" animated />
        </div>

        <div v-else-if="isStreaming" class="streaming-state">
          <el-card class="summary-card">
            <template #header>
              <div class="card-header">
                <span>正在生成综合总结</span>
                <span class="streaming-indicator">
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                  <span class="typing-dot"></span>
                </span>
              </div>
            </template>
            <div class="summary-content markdown-body" v-html="formatContent(streamingContent)"></div>
          </el-card>
        </div>

        <template v-else-if="mergeResult">
          <!-- 总结 -->
          <el-card class="summary-card">
            <template #header>
              <div class="card-header">
                <span>综合总结</span>
                <el-tag>共识度 {{ (mergeResult.convergenceRate * 100).toFixed(0) }}%</el-tag>
              </div>
            </template>
            <div class="summary-content markdown-body" v-html="formatContent(mergeResult.summary)"></div>
          </el-card>

          <!-- 候选方案 -->
          <h3 class="section-title">候选方案</h3>
          <div class="plans-grid">
            <el-card
              v-for="plan in mergeResult.plans"
              :key="plan.id"
              class="plan-card"
              :class="{ selected: plan.isSelected }"
            >
              <div class="plan-header">
                <h4>{{ plan.title }}</h4>
                <el-tag :type="getConfidenceType(plan.confidence)">
                  置信度 {{ plan.confidence }}%
                </el-tag>
              </div>

              <p class="plan-description">{{ plan.description }}</p>

              <div class="plan-section">
                <span class="label">支持者：</span>
                <el-tag
                  v-for="s in plan.supporters"
                  :key="s"
                  size="small"
                  type="success"
                  effect="plain"
                  class="mr-2"
                >
                  {{ s }}
                </el-tag>
              </div>

              <div class="plan-section">
                <span class="label">反对者：</span>
                <el-tag
                  v-for="o in plan.opponents"
                  :key="o"
                  size="small"
                  type="danger"
                  effect="plain"
                  class="mr-2"
                >
                  {{ o }}
                </el-tag>
              </div>

              <div class="plan-section">
                <span class="label">里程碑：</span>
                <ul>
                  <li v-for="m in plan.milestones" :key="m">{{ m }}</li>
                </ul>
              </div>

              <div class="plan-section">
                <span class="label">风险提示：</span>
                <ul class="risk-list">
                  <li v-for="r in plan.risks" :key="r">{{ r }}</li>
                </ul>
              </div>

              <div class="plan-section">
                <span class="label">适用条件：</span>
                <span class="conditions">{{ plan.applicableConditions }}</span>
              </div>

              <el-button
                v-if="!plan.isSelected"
                type="primary"
                class="select-btn"
                @click="handleSelectPlan(plan.id)"
              >
                选择此方案
              </el-button>
              <el-tag v-else type="success" effect="dark" class="selected-tag">已选择</el-tag>
            </el-card>
          </div>

          <!-- 认知盲区 -->
          <h3 class="section-title">认知盲区</h3>
          <el-card class="blindspot-card">
            <ul class="blindspot-list">
              <li v-for="(spot, index) in mergeResult.blindSpots" :key="index">
                <el-icon color="#f59e0b"><Warning /></el-icon>
                <span>{{ spot }}</span>
              </li>
            </ul>
          </el-card>
        </template>

        <div v-else class="empty-state">
          <el-empty description="正在准备生成结果...">
            <el-button type="primary" @click="generateResult">立即生成</el-button>
          </el-empty>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import Sidebar from '@/components/layout/Sidebar.vue'
import { mergeApi } from '@/api/merge'
import type { MergeResult } from '@/types'

const route = useRoute()
const taskId = computed(() => Number(route.params.taskId))

const loading = ref(false)
const mergeResult = ref<MergeResult | null>(null)
const isStreaming = ref(false)
const streamingContent = ref('')
let ws: WebSocket | null = null

onMounted(async () => {
  connectWebSocket()
  await fetchMergeResult()
})

onUnmounted(() => {
  ws?.close()
})

// 监听 taskId 变化，切换结果页时重新加载
watch(taskId, async (newTaskId, oldTaskId) => {
  if (newTaskId !== oldTaskId) {
    ws?.close()
    isStreaming.value = false
    streamingContent.value = ''
    connectWebSocket()
    await fetchMergeResult()
  }
})

const connectWebSocket = () => {
  const wsUrl = `ws://${window.location.host}/ws/discussion?taskId=${taskId.value}`
  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    console.log('Result WebSocket connected')
  }

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    console.log('Result WebSocket received:', data)

    switch (data.type) {
      case 'result_stream_start':
        isStreaming.value = true
        streamingContent.value = ''
        break

      case 'result_stream_chunk':
        if (isStreaming.value) {
          streamingContent.value += data.content
        }
        break

      case 'result_stream_end':
        isStreaming.value = false
        streamingContent.value = ''
        fetchMergeResult()
        break
    }
  }

  ws.onerror = (error) => {
    console.error('Result WebSocket error:', error)
  }

  ws.onclose = () => {
    console.log('Result WebSocket closed')
  }
}

const fetchMergeResult = async () => {
  loading.value = true
  try {
    mergeResult.value = await mergeApi.getMergeResult(taskId.value)
  } catch (error: any) {
    // 结果不存在，静默处理，显示生成按钮
    if (error?.response?.status === 404) {
      mergeResult.value = null
      // 自动触发结果生成
      generateResult()
    } else {
      ElMessage.error('获取结果失败')
    }
  } finally {
    loading.value = false
  }
}

const generateResult = async () => {
  try {
    await mergeApi.generateMergeResult(taskId.value)
    ElMessage.info('正在生成结果，请稍候...')
  } catch (error) {
    ElMessage.error('生成结果失败')
  }
}

const handleSelectPlan = async (planId: number) => {
  if (!mergeResult.value) return
  try {
    await mergeApi.selectPlan(mergeResult.value.id, planId)
    ElMessage.success('方案已选择')
    await fetchMergeResult()
  } catch {
    ElMessage.error('选择失败')
  }
}

const formatContent = (content: string) => {
  return marked(content)
}

const getConfidenceType = (confidence: number) => {
  if (confidence >= 80) return 'success'
  if (confidence >= 60) return 'warning'
  return 'info'
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

.result-container {
  max-width: 1200px;
  margin: 0 auto;
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.result-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.loading-state {
  padding: 40px;
}

.streaming-state {
  animation: fadeIn 0.3s ease-out;
}

.empty-state {
  height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-card {
  margin-bottom: 24px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 3px;
}

.typing-dot {
  width: 5px;
  height: 5px;
  background: #3b82f6;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.summary-content {
  line-height: 1.8;
  color: #374151;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
  margin: 32px 0 16px;
}

.plans-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 20px;
}

.plan-card {
  border-radius: 12px;
  transition: all 0.3s;
}

.plan-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.plan-card.selected {
  border: 2px solid #10b981;
}

.plan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.plan-header h4 {
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.plan-description {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.6;
  margin-bottom: 16px;
}

.plan-section {
  margin-bottom: 12px;
  font-size: 14px;
}

.plan-section .label {
  color: #6b7280;
  font-weight: 500;
}

.plan-section ul {
  margin: 8px 0;
  padding-left: 20px;
}

.plan-section li {
  color: #4b5563;
  margin-bottom: 4px;
}

.risk-list li {
  color: #dc2626;
}

.conditions {
  color: #4b5563;
}

.select-btn {
  width: 100%;
  margin-top: 16px;
}

.selected-tag {
  display: block;
  width: 100%;
  text-align: center;
  margin-top: 16px;
}

.blindspot-card {
  background: #fffbeb;
  border-color: #f59e0b;
}

.blindspot-list {
  list-style: none;
  padding: 0;
}

.blindspot-list li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #fcd34d;
  color: #92400e;
}

.blindspot-list li:last-child {
  border-bottom: none;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 16px 0 12px;
  color: #1f2937;
}

.markdown-body :deep(p) {
  margin-bottom: 12px;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.markdown-body :deep(li) {
  margin-bottom: 4px;
}

.markdown-body :deep(strong) {
  color: #1f2937;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #e5e7eb;
  padding-left: 16px;
  margin: 16px 0;
  color: #6b7280;
}
</style>
