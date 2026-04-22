import { computed, type Ref } from 'vue'
import type { AgentType } from '@/types'

export function useAgentTheme(type: Ref<AgentType | string | undefined>) {
  const attrs = computed(() => ({ 'data-agent-type': type.value || 'CUSTOM' }))
  return { attrs }
}
