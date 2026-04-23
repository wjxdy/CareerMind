<template>
  <section class="round page-break-avoid">
    <div class="r-head">
      <BaseBadge tone="accent">第 {{ round.roundNumber }} 轮</BaseBadge>
      <h3>{{ round.label }}</h3>
      <span class="div">分歧度 {{ Math.round(round.divergence * 100) }}%</span>
    </div>
    <div class="agents">
      <div v-for="(m, i) in round.messages" :key="i" class="agent-row" :data-agent-type="m.agentType">
        <AgentAvatar :agent-type="m.agentType" :size="30" />
        <div class="bubble">
          <div class="meta">
            <span class="name">{{ m.agentName }}</span>
            <span class="conf">置信度 {{ Math.round(m.confidence * 100) }}%</span>
          </div>
          <p class="content">{{ m.content }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import BaseBadge from '@/components/ui/BaseBadge.vue'
import AgentAvatar from '@/components/agent/AgentAvatar.vue'
import type { ReportRound } from '@/types/report'
defineProps<{ round: ReportRound }>()
</script>

<style scoped>
.round { padding: 18px 0; border-top: 1px dashed var(--border-subtle); }
.r-head { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.r-head h3 { font-size: 15px; margin: 0; }
.div { font-size: 12px; color: var(--text-muted); margin-left: auto; }
.agents { display: flex; flex-direction: column; gap: 10px; }
.agent-row { display: flex; gap: 10px; }
.bubble { flex: 1; background: var(--bg-inset); border-left: 3px solid var(--agent); border-radius: var(--radius-md); padding: 10px 12px; min-width: 0; }
.meta { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px; }
.name { color: var(--agent); font-weight: 600; }
.conf { color: var(--text-muted); }
.content { margin: 0; font-size: 12.5px; line-height: 1.65; white-space: pre-wrap; word-break: break-word; }
</style>
