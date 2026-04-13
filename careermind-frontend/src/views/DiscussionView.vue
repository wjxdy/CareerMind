<template>
  <div class="page-layout">
    <Sidebar />
    <main class="page-content">
      <DiscussionPanel :task-id="Number(taskId)" />
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import Sidebar from '@/components/layout/Sidebar.vue'
import DiscussionPanel from '@/components/discussion/DiscussionPanel.vue'

const route = useRoute()
const taskId = route.params.taskId as string
</script>

<style scoped>
.page-layout {
  display: flex;
  height: 100vh;
}

.page-content {
  flex: 1;
  overflow: hidden;
  background: white;
}
</style>
