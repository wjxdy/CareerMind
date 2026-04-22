<template>
  <PageShell>
    <DiscussionPanel :task-id="Number(taskId)" />
  </PageShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PageShell from '@/components/ui/PageShell.vue'
import DiscussionPanel from '@/components/discussion/DiscussionPanel.vue'
const taskId = computed(() => useRoute().params.taskId as string)
</script>
