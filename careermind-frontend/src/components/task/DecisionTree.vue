<template>
  <div class="decision-tree-section">
    <h4>决策链路</h4>
    <div v-if="!hasData" class="tree-empty">
      <p>讨论启动后将生成决策树</p>
      <el-button type="primary" text @click="goToDiscussion">查看讨论</el-button>
    </div>
    <div v-else class="tree-wrapper">
      <v-chart class="tree-chart" :option="chartOption" autoresize @click="handleNodeClick" />
      <div class="tree-actions">
        <el-button type="primary" text @click="dialogVisible = true">展开查看完整决策树</el-button>
      </div>
    </div>

    <el-dialog v-model="dialogVisible" title="决策树" width="90%" top="5vh" destroy-on-close>
      <v-chart class="tree-chart-full" :option="chartOption" autoresize @click="handleNodeClick" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { TreeChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { useAgentStore } from '@/stores/agent'
import type { Task, Discussion, MergeResult } from '@/types'

use([TreeChart, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  task: Task
  discussion: Discussion | null
  mergeResult: MergeResult | null
}>()

const router = useRouter()
const agentStore = useAgentStore()
const dialogVisible = ref(false)

const hasData = computed(() =>
  !!props.discussion && props.discussion.rounds.length > 0
)

const truncate = (text: string, len = 10) =>
  text.length > len ? text.slice(0, len) + '...' : text

const treeData = computed(() => {
  const rootLabel = props.task.goal || props.task.title || '职业困惑'
  const children: any[] = []

  if (props.discussion) {
    for (const round of props.discussion.rounds) {
      const roundNode: any = {
        name: `第${round.roundNumber}轮`,
        value: round.roundType,
        itemStyle: { color: '#3b82f6' },
        children: round.messages.map((m) => ({
          name: truncate(m.content),
          value: m.agentName,
          itemStyle: { color: agentStore.getAgentColor(m.agentType) },
          label: { color: agentStore.getAgentColor(m.agentType) }
        }))
      }
      if (roundNode.children.length) {
        children.push(roundNode)
      }
    }
  }

  if (props.mergeResult?.plans?.length) {
    children.push({
      name: '候选方案',
      itemStyle: { color: '#10b981' },
      children: props.mergeResult.plans.map((p) => ({
        name: truncate(p.title),
        value: `plan:${p.id}`,
        itemStyle: { color: p.isSelected ? '#10b981' : '#6b7280' }
      }))
    })
  }

  return [{
    name: rootLabel,
    itemStyle: { color: '#1f2937' },
    children
  }]
})

const chartOption = computed(() => ({
  tooltip: { trigger: 'item', triggerOn: 'mousemove' },
  series: [{
    type: 'tree',
    data: treeData.value,
    top: '5%',
    left: '5%',
    bottom: '5%',
    right: '20%',
    symbolSize: 10,
    orient: 'RL',
    label: {
      position: 'left',
      verticalAlign: 'middle',
      align: 'right',
      fontSize: 12,
      overflow: 'truncate',
      width: 90
    },
    leaves: {
      label: { position: 'right', verticalAlign: 'middle', align: 'left', overflow: 'truncate', width: 90 }
    },
    expandAndCollapse: true,
    animationDuration: 300,
    animationDurationUpdate: 300
  }]
}))

const handleNodeClick = (params: any) => {
  const value = params.data?.value as string
  if (!value) return
  if (value.startsWith('plan:')) {
    router.push(`/results/${props.task.id}`)
  } else {
    router.push(`/discussions/${props.task.id}`)
  }
}

const goToDiscussion = () => {
  router.push(`/discussions/${props.task.id}`)
}
</script>

<style scoped>
.decision-tree-section {
  margin-bottom: 24px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 8px;
}

.decision-tree-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #6b7280;
  margin-bottom: 12px;
}

.tree-empty {
  text-align: center;
  padding: 24px 0;
  color: #9ca3af;
  font-size: 14px;
}

.tree-empty p {
  margin-bottom: 8px;
}

.tree-wrapper {
  text-align: center;
}

.tree-chart {
  width: 100%;
  height: 260px;
}

.tree-actions {
  margin-top: 8px;
}

.tree-chart-full {
  width: 100%;
  height: 70vh;
}
</style>
