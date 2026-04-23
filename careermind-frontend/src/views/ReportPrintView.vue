<template>
  <div class="report-shell">
    <div class="toolbar no-print">
      <BaseButton variant="ghost" @click="back">← 返回</BaseButton>
      <div class="toolbar-right">
        <BaseButton variant="secondary" @click="onPrint">打印</BaseButton>
        <BaseButton variant="primary" :loading="downloading" @click="onDownload">下载 PDF</BaseButton>
      </div>
    </div>

    <div v-if="loading" class="state">正在加载报告…</div>
    <div v-else-if="!report" class="state">报告不可用</div>

    <div v-else id="report-root" class="report-doc">
      <ReportCover
        :title="report.task.title"
        :date="report.task.createdAt"
        :username="report.task.username"
        :convergence="report.graph?.finalConvergence ?? 0.5"
      />
      <ReportExecutiveSummary :text="report.extras?.executiveSummary || ''" />
      <ReportProblem :task="report.task" />

      <h2 class="sec-title">四轮讨论</h2>
      <ReportRoundSummary v-for="r in report.rounds" :key="r.roundNumber" :round="r" />

      <ReportPlansComparison v-if="report.mergeResult?.plans?.length" :plans="report.mergeResult.plans" />
      <ReportBlindSpots v-if="report.mergeResult?.blindSpots?.length" :items="report.mergeResult.blindSpots" />
      <ReportActionPlan v-if="report.extras?.actionPlan" :plan="report.extras.actionPlan" />
      <ReportBack />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BaseButton from '@/components/ui/BaseButton.vue'
import ReportCover from '@/components/report/ReportCover.vue'
import ReportExecutiveSummary from '@/components/report/ReportExecutiveSummary.vue'
import ReportProblem from '@/components/report/ReportProblem.vue'
import ReportRoundSummary from '@/components/report/ReportRoundSummary.vue'
import ReportPlansComparison from '@/components/report/ReportPlansComparison.vue'
import ReportBlindSpots from '@/components/report/ReportBlindSpots.vue'
import ReportActionPlan from '@/components/report/ReportActionPlan.vue'
import ReportBack from '@/components/report/ReportBack.vue'
import { reportApi } from '@/api/report'
import { exportElementToPdf } from '@/utils/pdf-export'
import { message } from '@/utils/naive-discrete'
import type { ReportResponse } from '@/types/report'
import '@/styles/print.css'

const route = useRoute()
const router = useRouter()
const taskId = computed(() => Number(route.params.taskId))

const report = ref<ReportResponse | null>(null)
const loading = ref(true)
const downloading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    report.value = await reportApi.getReport(taskId.value)
  } catch (e: any) {
    message.error(e.message || '加载报告失败')
  } finally {
    loading.value = false
  }
})

const onDownload = async () => {
  const el = document.getElementById('report-root')
  if (!el) return
  downloading.value = true
  try {
    const safeTitle = (report.value?.task.title || 'report').replace(/[\\/:*?"<>|]/g, '_')
    await exportElementToPdf(el, `CareerMind-${safeTitle}.pdf`)
    message.success('PDF 已下载')
  } catch (e: any) {
    message.error('PDF 导出失败：' + (e?.message || ''))
  } finally {
    downloading.value = false
  }
}

const onPrint = () => window.print()

const back = () => {
  if (window.history.length > 1) router.back()
  else router.push(`/results/${taskId.value}`)
}
</script>

<style scoped>
.report-shell { min-height: 100vh; background: var(--bg-page); padding: 24px; }
.toolbar {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between; gap: 8px;
  padding: 12px 16px; background: var(--bg-card); border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md); margin-bottom: 16px;
}
.toolbar-right { display: flex; gap: 8px; }
.state { padding: 80px 0; text-align: center; color: var(--text-muted); }
.report-doc {
  max-width: 800px; margin: 0 auto; padding: 32px 40px;
  background: var(--bg-card); border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
}
.sec-title { font-size: 18px; margin: 28px 0 8px; }
</style>
