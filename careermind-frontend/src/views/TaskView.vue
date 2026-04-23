<template>
  <PageShell>
    <div class="task-view" v-if="task">
      <header class="tv-head">
        <div>
          <BaseBadge :tone="toneOfStatus(task.status)">{{ labelOfStatus(task.status) }}</BaseBadge>
          <h2>{{ task.title }}</h2>
        </div>
        <div class="tv-actions">
          <BaseButton variant="secondary" @click="$router.push(`/discussions/${task.id}`)">进入讨论</BaseButton>
          <BaseButton variant="primary"   @click="$router.push(`/results/${task.id}`)">查看结果</BaseButton>
        </div>
      </header>

      <BaseCard class="tv-card">
        <template #header>任务信息</template>
        <div class="info-row"><span class="ik">背景</span><p>{{ task.background || '—' }}</p></div>
        <div class="info-row"><span class="ik">目标</span><p>{{ task.goal || '—' }}</p></div>
        <div class="info-row"><span class="ik">约束</span><p>{{ task.constraints || '—' }}</p></div>
        <div class="info-row"><span class="ik">专家</span>
          <AgentAvatarGroup :agents="task.agents.map(a => ({ id: a.id, type: a.type }))" :size="30" :max="5" />
        </div>
      </BaseCard>

      <BaseCard class="tv-card">
        <template #header>
          <div class="tab-head">
            <span>观点演化图</span>
            <VizLegend />
          </div>
        </template>
        <OpinionGraph :graph="graph" />
      </BaseCard>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import PageShell from '@/components/ui/PageShell.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import AgentAvatarGroup from '@/components/agent/AgentAvatarGroup.vue'
import OpinionGraph from '@/components/viz/OpinionGraph.vue'
import VizLegend from '@/components/viz/VizLegend.vue'
import { taskApi } from '@/api/task'
import { graphApi } from '@/api/graph'
import type { Task, TaskStatus } from '@/types'
import type { GraphResponse } from '@/types/graph'

const route = useRoute()
const taskId = computed(() => Number(route.params.id))
const task = ref<Task | null>(null)
const graph = ref<GraphResponse | null>(null)

const load = async () => {
  task.value = await taskApi.getTaskById(taskId.value)
  try { graph.value = await graphApi.getGraph(taskId.value) } catch { graph.value = null }
}
onMounted(load)
watch(taskId, load)

const toneOfStatus = (s: TaskStatus) => ({
  PENDING: 'neutral', DISCUSSING: 'accent', MERGING: 'warning', COMPLETED: 'success', ARCHIVED: 'neutral',
}[s] as 'neutral'|'accent'|'warning'|'success')
const labelOfStatus = (s: TaskStatus) => ({
  PENDING: '待开始', DISCUSSING: '讨论中', MERGING: '整合中', COMPLETED: '已完成', ARCHIVED: '已归档',
}[s] || s)
</script>

<style scoped>
.task-view { padding: 32px 40px; max-width: 1100px; margin: 0 auto; overflow-y: auto; height: 100%; }
.tv-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; gap: 20px; }
.tv-head h2 { margin: 6px 0 0; font-size: 22px; }
.tv-actions { display: flex; gap: 8px; flex-shrink: 0; }
.tv-card { margin-bottom: 16px; }
.info-row { display: grid; grid-template-columns: 60px 1fr; gap: 16px; padding: 10px 0; border-top: 1px solid var(--border-subtle); align-items: center; }
.info-row:first-child { border-top: none; }
.ik { color: var(--text-muted); font-size: 13px; }
.info-row p { margin: 0; font-size: 13px; line-height: 1.6; }
.tab-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
</style>
