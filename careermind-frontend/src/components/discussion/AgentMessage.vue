<template>
  <div class="agent-message" :class="{ 'is-final': message.isFinal, 'is-streaming': isStreaming, 'is-user': isUserMessage, 'is-interjection': isInterjection }">
    <div class="message-avatar">
      <el-avatar
        :size="40"
        :style="{ backgroundColor: isUserMessage ? '#10b981' : agentColor }"
      >
        <el-icon :size="20" v-if="!isUserMessage">
          <component :is="agentIcon" />
        </el-icon>
        <el-icon :size="20" v-else><User /></el-icon>
      </el-avatar>
    </div>
    <div class="message-content">
      <div class="message-header">
        <span class="agent-name">{{ displayName }}</span>
        <span v-if="isStreaming" class="streaming-indicator">
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
        </span>
        <span v-else class="message-time">{{ formatTime(message.createdAt) }}</span>
      </div>
      <div class="message-body" v-html="formattedContent"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import { useAgentStore } from '@/stores/agent'
import type { Message } from '@/types'
import dayjs from 'dayjs'

const props = defineProps<{
  message: Message
  isLatest?: boolean
  isStreaming?: boolean
}>()

const agentStore = useAgentStore()

const isUserMessage = computed(() => props.message.messageType === 'USER' || props.message.agentId === -1)
const isInterjection = computed(() => props.message.messageType === 'INTERJECTION')

const displayName = computed(() => {
  if (isUserMessage.value) return '我'
  if (isInterjection.value) return `${props.message.agentName} · 回应插话`
  return props.message.agentName
})

const agentColor = computed(() => {
  if (isInterjection.value) return '#10b981'
  return agentStore.getAgentColor(props.message.agentType)
})
const agentIcon = computed(() => agentStore.getAgentIcon(props.message.agentType))

const formattedContent = computed(() => {
  return marked(props.message.content || '')
})

const formatTime = (time: string) => {
  if (!time) return ''
  return dayjs(time).format('HH:mm:ss')
}
</script>

<style scoped>
.agent-message {
  display: flex;
  gap: 12px;
  padding: 16px;
  margin-bottom: 12px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  animation: slideIn 0.3s ease-out;
}

.agent-message.is-final {
  border-left: 4px solid #10b981;
}

.agent-message.is-streaming {
  border-left: 4px solid #3b82f6;
  background: linear-gradient(90deg, #eff6ff 0%, #ffffff 100%);
}

.agent-message.is-user {
  border-left: 4px solid #10b981;
  background: linear-gradient(90deg, #ecfdf5 0%, #ffffff 100%);
}

.agent-message.is-interjection {
  border-left: 4px solid #f59e0b;
  background: linear-gradient(90deg, #fffbeb 0%, #ffffff 100%);
  padding: 12px 16px;
}

.is-interjection .agent-name {
  color: #b45309;
  font-size: 13px;
}

.is-interjection .message-body {
  font-size: 13px;
  color: #92400e;
}

.message-avatar {
  flex-shrink: 0;
}

.message-content {
  flex: 1;
  min-width: 0;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.agent-name {
  font-weight: 600;
  color: #1f2937;
}

.is-user .agent-name {
  color: #059669;
}

.message-time {
  font-size: 12px;
  color: #9ca3af;
}

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 3px;
}

.typing-dot {
  width: 5px;
  height: 5px;
  background: #3b82f6;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.message-body {
  font-size: 14px;
  line-height: 1.8;
  color: #374151;
}

.message-body :deep(p) {
  margin-bottom: 8px;
}

.message-body :deep(ul),
.message-body :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.message-body :deep(li) {
  margin-bottom: 4px;
}

.message-body :deep(strong) {
  color: #1f2937;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
