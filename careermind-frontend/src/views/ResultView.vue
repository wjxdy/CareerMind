<template>
  <PageShell>
    <div class="result-view" v-if="mergeResult">
      <header class="banner">
        <BrandLogo size="sm" />
        <h1>你的职业决策</h1>
        <div class="banner-stats">
          <div class="stat"><span class="num">{{ Math.round(mergeResult.convergenceRate * 100) }}%</span><span class="lbl">共识度</span></div>
          <div class="stat"><span class="num">{{ mergeResult.plans.length }}</span><span class="lbl">候选方案</span></div>
          <div class="stat"><span class="num">{{ mergeResult.blindSpots.length }}</span><span class="lbl">认知盲区</span></div>
        </div>
      </header>

      <section v-if="mergeResult.summary">
        <h2 class="sec-title">整合总结</h2>
        <BaseCard>
          <p class="summary-text">{{ mergeResult.summary }}</p>
        </BaseCard>
      </section>

      <section>
        <h2 class="sec-title">候选方案</h2>
        <div class="plans-grid">
          <BaseCard v-for="(p, i) in mergeResult.plans" :key="i" :class="{ selected: p.isSelected }">
            <div class="plan-head">
              <BaseBadge tone="accent">方案 {{ i + 1 }}</BaseBadge>
              <div class="conf">
                <div class="conf-bar" :style="{ '--w': Math.round(p.confidence*100) + '%' } as any" />
                <span>{{ Math.round(p.confidence * 100) }}%</span>
              </div>
            </div>
            <h3 class="plan-title">{{ p.title }}</h3>
            <p class="plan-desc">{{ p.description }}</p>
            <div class="plan-section" v-if="p.milestones?.length">
              <h5>里程碑</h5>
              <ul><li v-for="(m, j) in p.milestones" :key="j">{{ m }}</li></ul>
            </div>
            <div class="plan-section" v-if="p.risks?.length">
              <h5>风险</h5>
              <ul><li v-for="(r, j) in p.risks" :key="j">{{ r }}</li></ul>
            </div>
            <div class="plan-section" v-if="p.applicableConditions">
              <h5>适用条件</h5>
              <p class="cond">{{ p.applicableConditions }}</p>
            </div>
          </BaseCard>
        </div>
      </section>

      <section v-if="mergeResult.blindSpots?.length">
        <h2 class="sec-title">认知盲区</h2>
        <div class="blinds">
          <BaseCard v-for="(b, i) in mergeResult.blindSpots" :key="i" inset padding="14px">
            <div class="blind-row"><span class="blind-num">0{{ i + 1 }}</span><p>{{ b }}</p></div>
          </BaseCard>
        </div>
      </section>

      <footer class="rv-foot">
        <BaseButton variant="ghost" @click="$router.push(`/discussions/${taskId}`)">← 返回讨论</BaseButton>
        <BaseButton variant="primary" @click="onExport">导出 PDF 报告 📄</BaseButton>
      </footer>
    </div>

    <div v-else-if="!loading" class="empty-wrap">
      <EmptyState title="尚未生成结果" description="请先完成讨论或从讨论页点击「生成报告」">
        <template #action>
          <BaseButton variant="primary" :loading="generating" @click="handleGenerate">生成结果</BaseButton>
        </template>
      </EmptyState>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { message as ElMessage } from '@/utils/naive-discrete'
import PageShell from '@/components/ui/PageShell.vue'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { mergeApi } from '@/api/merge'
import type { MergeResult } from '@/types'

const route = useRoute()
const taskId = computed(() => Number(route.params.taskId))
const mergeResult = ref<MergeResult | null>(null)
const loading = ref(true)
const generating = ref(false)

const load = async () => {
  loading.value = true
  try { mergeResult.value = await mergeApi.getMergeResult(taskId.value) }
  catch { mergeResult.value = null }
  finally { loading.value = false }
}
onMounted(load)
watch(taskId, load)

const handleGenerate = async () => {
  generating.value = true
  try {
    mergeResult.value = await mergeApi.generateMergeResult(taskId.value)
    ElMessage.success('结果已生成')
  } catch (e: any) {
    ElMessage.error(e.message || '生成失败')
  } finally {
    generating.value = false
  }
}

const onExport = () => ElMessage.info('PDF 导出将在 P3 阶段可用')
</script>

<style scoped>
.result-view { padding: 0; overflow-y: auto; height: 100%; }

.banner {
  padding: 48px 40px;
  background: linear-gradient(135deg, var(--accent-dim) 0%, var(--bg-card) 100%);
  border-bottom: 1px solid var(--border-subtle);
  display: flex; flex-direction: column; gap: 12px;
}
.banner h1 { font-size: 36px; margin: 4px 0; }
.banner-stats { display: flex; gap: 32px; margin-top: 12px; }
.stat { display: flex; flex-direction: column; }
.num { font-size: 32px; font-weight: 700; color: var(--accent); line-height: 1.2; }
.lbl { font-size: 12px; color: var(--text-secondary); }

section { padding: 32px 40px; max-width: 1200px; margin: 0 auto; }
.sec-title { font-size: 18px; margin: 0 0 16px; }
.summary-text { margin: 0; font-size: 14px; line-height: 1.7; color: var(--text-primary); }

.plans-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px; }
.selected { border-color: var(--accent) !important; box-shadow: 0 0 0 1px var(--accent); }
.plan-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.conf { display: inline-flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-secondary); }
.conf-bar { width: 60px; height: 4px; background: var(--accent-dim); border-radius: 9999px; position: relative; }
.conf-bar::after { content: ''; position: absolute; left: 0; top: 0; height: 100%; background: var(--accent); border-radius: 9999px; width: var(--w, 0); }
.plan-title { font-size: 16px; margin: 0 0 6px; }
.plan-desc  { font-size: 13px; color: var(--text-secondary); margin: 0 0 14px; line-height: 1.6; }
.plan-section { margin-top: 12px; }
.plan-section h5 { font-size: 12px; color: var(--text-muted); margin: 0 0 6px; text-transform: uppercase; letter-spacing: 0.04em; }
.plan-section ul { margin: 0; padding-left: 18px; font-size: 13px; line-height: 1.6; }
.cond { margin: 0; font-size: 13px; color: var(--text-secondary); }

.blinds { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 10px; }
.blind-row { display: flex; gap: 12px; align-items: start; }
.blind-num { font-family: var(--font-mono); color: var(--warning); font-size: 13px; flex-shrink: 0; }
.blind-row p { margin: 0; font-size: 13px; line-height: 1.6; color: var(--text-primary); }

.rv-foot { padding: 32px 40px; display: flex; justify-content: space-between; border-top: 1px solid var(--border-subtle); max-width: 1200px; margin: 0 auto; }
.empty-wrap { padding: 120px 40px; display: flex; justify-content: center; }
</style>
