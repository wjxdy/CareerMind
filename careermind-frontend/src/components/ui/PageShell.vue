<template>
  <div class="page-shell" :class="{ collapsed }">
    <Sidebar :collapsed="collapsed" @toggle="collapsed = !collapsed" />
    <main class="shell-main"><slot /></main>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import Sidebar from '@/components/layout/Sidebar.vue'
const collapsed = ref(localStorage.getItem('cm-sidebar-collapsed') === '1')
watch(collapsed, v => localStorage.setItem('cm-sidebar-collapsed', v ? '1' : '0'))
</script>

<style scoped>
.page-shell { display: flex; height: 100vh; background: var(--bg-page); }
.shell-main { flex: 1; min-width: 0; overflow: hidden; }
</style>
