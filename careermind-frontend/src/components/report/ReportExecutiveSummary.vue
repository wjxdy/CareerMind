<template>
  <section class="esum page-break-avoid">
    <h2>执行摘要</h2>
    <p>{{ text || '尚未生成摘要。' }}</p>
  </section>
</template>

<script setup lang="ts">
defineProps<{ text: string }>()
</script>

<style scoped>
.esum { padding: 32px 0 24px; }
.esum h2 { font-size: 18px; margin: 0 0 12px; }
.esum p { font-size: 13px; line-height: 1.75; color: var(--text-primary); margin: 0; text-indent: 2em; }
</style>
