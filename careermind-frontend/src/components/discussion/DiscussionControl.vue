<template>
  <div class="discussion-control">
    <template v-if="!isActive">
      <el-button type="primary" @click="$emit('start')">
        <el-icon><VideoPlay /></el-icon>
        开始讨论
      </el-button>
    </template>
    <template v-else>
      <el-button v-if="isPaused" type="primary" @click="$emit('resume')">
        <el-icon><VideoPlay /></el-icon>
        继续
      </el-button>
      <el-button v-else type="warning" @click="$emit('pause')">
        <el-icon><VideoPause /></el-icon>
        暂停
      </el-button>
      <el-button type="info" @click="$emit('stop')">
        <el-icon><CircleClose /></el-icon>
        停止
      </el-button>
      <el-button type="success" @click="$emit('next-round')">
        <el-icon><ArrowRight /></el-icon>
        下一轮
      </el-button>
    </template>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  isActive: boolean
  isPaused: boolean
}>()

defineEmits<{
  start: []
  pause: []
  resume: []
  stop: []
  'next-round': []
}>()
</script>

<style scoped>
.discussion-control {
  display: flex;
  gap: 8px;
}

.discussion-control :deep(.el-button) {
  display: flex;
  align-items: center;
  gap: 4px;
}
</style>
