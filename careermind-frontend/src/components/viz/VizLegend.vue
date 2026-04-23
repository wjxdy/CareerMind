<template>
  <div class="legend">
    <div class="grp">
      <span class="grp-title">Agent</span>
      <span v-for="a in agents" :key="a.type" class="item">
        <span class="dot" :style="{ background: a.color }" />{{ a.label }}
      </span>
    </div>
    <div class="grp">
      <span class="grp-title">关系</span>
      <span class="item"><span class="line dashed" />质疑</span>
      <span class="item"><span class="line solid" />支持</span>
      <span class="item"><span class="line blue" />修正</span>
    </div>
  </div>
</template>

<script setup lang="ts">
const agents = [
  { type: 'INDUSTRY_ANALYST',   label: '行业', color: '#1E3A8A' },
  { type: 'SKILL_ASSESSOR',     label: '能力', color: '#0D9488' },
  { type: 'RISK_WATCHER',       label: '风险', color: '#B45309' },
  { type: 'OPPORTUNITY_HUNTER', label: '机会', color: '#CA8A04' },
  { type: 'VALUE_EXAMINER',     label: '价值', color: '#9333EA' },
]
</script>

<style scoped>
.legend { display: flex; gap: 24px; padding: 8px 12px; background: var(--bg-card); border: 1px solid var(--border-subtle); border-radius: var(--radius-md); font-size: 12px; flex-wrap: wrap; }
.grp { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.grp-title { color: var(--text-muted); font-weight: 500; }
.item { display: inline-flex; align-items: center; gap: 4px; color: var(--text-secondary); }
.dot { width: 10px; height: 10px; border-radius: 50%; }
.line { width: 16px; height: 2px; }
.line.dashed { background: repeating-linear-gradient(90deg, var(--danger) 0 4px, transparent 4px 8px); }
.line.solid  { background: var(--success); }
.line.blue   { background: var(--accent); }
</style>
