<template>
  <div class="conv-wrap">
    <v-chart class="chart" :option="option" autoresize />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, MarkPointComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import type { GraphResponse } from '@/types/graph'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, MarkPointComponent, CanvasRenderer])

const props = defineProps<{ graph: GraphResponse | null }>()

const option = computed(() => {
  if (!props.graph) return {}
  const rounds = props.graph.rounds
  const consensus = rounds.map(r => Math.round((1 - r.divergence) * 100))
  const final = props.graph.finalConvergence

  return {
    grid: { left: 50, right: 30, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: rounds.map(r => `第 ${r.roundNumber} 轮`),
      axisLine: { lineStyle: { color: '#D4D4D8' } },
      axisLabel: { color: '#52525B' },
    },
    yAxis: {
      type: 'value', min: 0, max: 100,
      axisLabel: { formatter: '{value}%', color: '#52525B' },
      splitLine: { lineStyle: { color: '#E4E4E7' } },
    },
    series: [{
      name: '共识度', type: 'line', smooth: true,
      data: consensus,
      lineStyle: { width: 3, color: '#3B82F6' },
      itemStyle: { color: '#3B82F6' },
      areaStyle: { color: 'rgba(59,130,246,0.08)' },
      markPoint: rounds.length > 0 ? {
        symbolSize: 50,
        data: [{
          name: '最终', coord: [rounds.length - 1, consensus[consensus.length - 1] || 0],
          label: { formatter: `${Math.round(final * 100)}%`, color: '#fff' },
          itemStyle: { color: '#10B981' },
        }],
      } : undefined,
    }],
  }
})
</script>

<style scoped>
.conv-wrap { width: 100%; height: 280px; background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); padding: 16px; }
.chart { width: 100%; height: 100%; }
</style>
