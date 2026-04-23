<template>
  <div class="msg" :class="{ streaming: isStreaming, user: isUser, interjection: isInterjection }" :data-agent-type="agentType">
    <AgentAvatar v-if="!isUser" :agent-type="agentType" :size="30" :state="isStreaming ? 'speaking' : 'idle'" />
    <div v-else class="user-avatar">你</div>
    <div class="msg-body">
      <div class="msg-head">
        <span class="msg-name">{{ displayName }}</span>
        <span v-if="isStreaming" class="typing">
          <span class="dot" /><span class="dot" /><span class="dot" />
        </span>
        <span v-else class="msg-time">{{ timeText }}</span>
      </div>
      <div class="msg-content" v-html="formatted" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import dayjs from 'dayjs'
import AgentAvatar from '@/components/agent/AgentAvatar.vue'
import type { Message } from '@/types'

const props = defineProps<{ message: Message; isStreaming?: boolean }>()

const isUser = computed(() => props.message.messageType === 'USER' || props.message.agentId === -1)
const isInterjection = computed(() => props.message.messageType === 'INTERJECTION')
const agentType = computed(() => props.message.agentType || 'CUSTOM')
const displayName = computed(() => isUser.value ? '你' : (isInterjection.value ? `${props.message.agentName} · 回应插话` : props.message.agentName))
const timeText = computed(() => props.message.createdAt ? dayjs(props.message.createdAt).format('HH:mm') : '')
const formatted = computed(() => marked(props.message.content || ''))
</script>

<style scoped>
.msg {
  display: flex; gap: 12px; padding: 16px 4px;
  border-bottom: 1px solid var(--border-subtle);
  animation: slideIn var(--duration-base) var(--ease-standard);
}
.msg:last-child { border-bottom: none; }
.msg.user .user-avatar { background: var(--cta-bg); color: var(--cta-text); }
.msg.interjection { background: var(--bg-elevated); border-radius: var(--radius-md); padding: 14px 16px; border-bottom: none; margin-bottom: 2px; }

.user-avatar {
  width: 30px; height: 30px; border-radius: 50%;
  background: var(--cta-bg); color: var(--cta-text);
  display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; flex-shrink: 0;
}

.msg-body { flex: 1; min-width: 0; }
.msg-head { display: flex; align-items: baseline; gap: 10px; margin-bottom: 4px; }
.msg-name { font-size: 14px; font-weight: 600; color: var(--agent); letter-spacing: -0.005em; }
.msg.user .msg-name { color: var(--text-primary); }
.msg-time { font-size: 11px; color: var(--text-muted); }

.typing { display: inline-flex; gap: 3px; align-items: center; }
.dot { width: 4px; height: 4px; background: var(--agent); border-radius: 50%; animation: typing 1.4s infinite ease-in-out both; }
.dot:nth-child(1) { animation-delay: -0.32s; } .dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes typing { 0%,80%,100% { transform: scale(0); opacity: 0.4; } 40% { transform: scale(1); opacity: 1; } }

.msg-content { font-size: 14px; line-height: 1.7; color: var(--text-primary); word-break: break-word; }
.msg-content :deep(p) { margin: 4px 0; }
.msg-content :deep(ul), .msg-content :deep(ol) { margin: 6px 0; padding-left: 20px; }
.msg-content :deep(strong) { color: var(--text-primary); }
.msg-content :deep(a) { color: inherit; text-decoration: none; border-bottom: 1px dotted currentColor; }

@keyframes slideIn { from { opacity: 0; transform: translateY(4px); } to { opacity: 1; transform: translateY(0); } }
</style>
