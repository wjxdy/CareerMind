<template>
  <div class="opinion-graph-wrap">
    <div class="toolbar">
      <BaseButton size="sm" variant="ghost" @click="replay">▶ 回放辩论</BaseButton>
      <BaseButton size="sm" variant="ghost" @click="fit">居中</BaseButton>
    </div>
    <div ref="elRef" class="net" />
    <div v-if="!hasData" class="placeholder">
      <p>讨论开始后将生成观点演化图</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, watch, computed } from 'vue'
import { Network, type Options } from 'vis-network/standalone'
import { DataSet } from 'vis-data'
import BaseButton from '@/components/ui/BaseButton.vue'
import type { GraphResponse } from '@/types/graph'
import { getAgentMeta } from '@/utils/agent-meta'

const props = defineProps<{ graph: GraphResponse | null }>()
const elRef = ref<HTMLElement>()
let network: Network | null = null

const hasData = computed(() => !!props.graph && props.graph.nodes.length > 0)

const AGENT_COLOR: Record<string, string> = {
  INDUSTRY_ANALYST: '#1E3A8A', SKILL_ASSESSOR: '#0D9488', RISK_WATCHER: '#B45309',
  OPPORTUNITY_HUNTER: '#CA8A04', VALUE_EXAMINER: '#9333EA', CUSTOM: '#525B6B',
}
const EDGE: Record<string, { color: string; dashes: boolean | number[] }> = {
  CHALLENGE: { color: '#EF4444', dashes: [6, 4] },
  SUPPORT:   { color: '#10B981', dashes: false },
  REVISE:    { color: '#3B82F6', dashes: false },
}

const build = () => {
  if (!props.graph || !elRef.value) return
  if (props.graph.nodes.length === 0) return

  const nodeData = new DataSet(props.graph.nodes.map(n => ({
    id: n.id,
    label: `${getAgentMeta(n.agentType).short}\nR${n.roundNumber}`,
    title: n.snippet,
    color: { background: AGENT_COLOR[n.agentType] || '#525B6B', border: AGENT_COLOR[n.agentType] || '#525B6B' },
    font: { color: '#fff', size: 11, multi: false as const },
    shape: 'dot',
    size: Math.max(14, Math.min(36, 10 + Math.round(n.wordCount / 50))),
  })))
  const edgeData = new DataSet(props.graph.edges.map(e => ({
    id: e.id, from: e.from, to: e.to,
    arrows: 'to',
    color: EDGE[e.type]?.color || '#999',
    dashes: EDGE[e.type]?.dashes || false,
    width: e.type === 'CHALLENGE' ? 1.5 : 1,
  })))

  const options: Options = {
    physics: { solver: 'forceAtlas2Based', stabilization: { iterations: 200, updateInterval: 25 } },
    interaction: { hover: true, tooltipDelay: 100 },
    nodes: { borderWidth: 2 },
  }
  network?.destroy()
  network = new Network(elRef.value, { nodes: nodeData, edges: edgeData }, options)
  network.once('stabilized', () => network?.setOptions({ physics: false }))
}

const replay = () => {
  if (!network || !props.graph) return
  for (let r = 1; r <= 4; r++) {
    setTimeout(() => {
      const ids = props.graph!.nodes.filter(n => n.roundNumber === r).map(n => n.id)
      if (ids.length > 0) network!.selectNodes(ids)
    }, (r - 1) * 1500)
  }
}

const fit = () => network?.fit({ animation: true })

onMounted(build)
onBeforeUnmount(() => network?.destroy())
watch(() => props.graph, build, { deep: true })
</script>

<style scoped>
.opinion-graph-wrap { position: relative; width: 100%; height: 480px; background: var(--bg-inset); border-radius: var(--radius-md); overflow: hidden; }
.net { width: 100%; height: 100%; }
.toolbar { position: absolute; top: 8px; right: 8px; z-index: 2; display: flex; gap: 4px; background: var(--bg-card); padding: 4px; border-radius: var(--radius-md); border: 1px solid var(--border-subtle); }
.placeholder { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; color: var(--text-muted); font-size: 13px; pointer-events: none; }
</style>
