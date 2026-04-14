import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Agent } from '@/types'
import type { CreateAgentData } from '@/api/agent'
import { agentApi } from '@/api/agent'

export const useAgentStore = defineStore('agent', () => {
  // State
  const presetAgents = ref<Agent[]>([])
  const availableAgents = ref<Agent[]>([])
  const loading = ref(false)

  // Actions
  const fetchPresetAgents = async () => {
    loading.value = true
    try {
      presetAgents.value = await agentApi.getPresetAgents()
    } finally {
      loading.value = false
    }
  }

  const fetchAvailableAgents = async () => {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      console.log('fetchAvailableAgents: 未登录，跳过加载')
      return
    }
    loading.value = true
    try {
      availableAgents.value = await agentApi.getAvailableAgents()
    } catch (error: any) {
      console.error('fetchAvailableAgents 失败:', error)
    } finally {
      loading.value = false
    }
  }

  const initPresetAgents = async () => {
    await agentApi.initPresetAgents()
    await fetchPresetAgents()
  }

  const createAgent = async (data: CreateAgentData) => {
    const agent = await agentApi.createAgent(data)
    availableAgents.value.push(agent)
    return agent
  }

  const deleteAgent = async (agentId: number) => {
    await agentApi.deleteAgent(agentId)
    availableAgents.value = availableAgents.value.filter(a => a.id !== agentId)
  }

  // Get agent color based on type
  const getAgentColor = (type: string) => {
    const colors: Record<string, string> = {
      INDUSTRY_ANALYST: '#3b82f6',
      SKILL_ASSESSOR: '#10b981',
      RISK_WATCHER: '#ef4444',
      OPPORTUNITY_HUNTER: '#f59e0b',
      VALUE_EXAMINER: '#8b5cf6',
      MERGE_AGENT: '#6b7280',
      CUSTOM: '#64748b',
    }
    return colors[type] || '#64748b'
  }

  // Get agent icon based on type
  const getAgentIcon = (type: string) => {
    const icons: Record<string, string> = {
      INDUSTRY_ANALYST: 'TrendCharts',
      SKILL_ASSESSOR: 'Check',
      RISK_WATCHER: 'Warning',
      OPPORTUNITY_HUNTER: 'Opportunity',
      VALUE_EXAMINER: 'QuestionFilled',
      MERGE_AGENT: 'Connection',
      CUSTOM: 'User',
    }
    return icons[type] || 'User'
  }

  return {
    presetAgents,
    availableAgents,
    loading,
    fetchPresetAgents,
    fetchAvailableAgents,
    initPresetAgents,
    createAgent,
    deleteAgent,
    getAgentColor,
    getAgentIcon,
  }
})
