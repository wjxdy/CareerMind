<template>
  <section class="blinds page-break-avoid">
    <h2>认知盲区</h2>
    <ol>
      <li v-for="(b, i) in items" :key="i">{{ b }}</li>
    </ol>
  </section>
</template>

<script setup lang="ts">
defineProps<{ items: string[] }>()
</script>

<style scoped>
.blinds { padding: 24px 0; break-inside: avoid; }
.blinds h2 { font-size: 18px; margin: 0 0 12px; }
.blinds ol { margin: 0; padding-left: 22px; font-size: 13px; line-height: 1.75; color: var(--text-primary); }
.blinds li { margin-bottom: 6px; }
</style>
