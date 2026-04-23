<template>
  <div class="discussion-control">
    <template v-if="!isActive">
      <BaseButton variant="primary" size="sm" @click="$emit('start')">▶ 开始讨论</BaseButton>
    </template>
    <template v-else>
      <BaseButton v-if="isPaused" variant="primary" size="sm" @click="$emit('resume')">▶ 继续</BaseButton>
      <BaseButton v-else variant="secondary" size="sm" @click="$emit('pause')">⏸ 暂停</BaseButton>
      <BaseButton variant="ghost" size="sm" @click="$emit('stop')">停止</BaseButton>
    </template>
  </div>
</template>

<script setup lang="ts">
import BaseButton from '@/components/ui/BaseButton.vue'
defineProps<{ isActive: boolean; isPaused: boolean }>()
defineEmits<{ start: []; pause: []; resume: []; stop: []; 'next-round': [] }>()
</script>

<style scoped>
.discussion-control { display: flex; gap: 6px; }
</style>
