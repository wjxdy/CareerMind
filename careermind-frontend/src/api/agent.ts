import request from './request'
import type { Agent } from '@/types'

export interface CreateAgentData {
  name: string
  type?: string
  systemPrompt?: string
  modelType?: string
  description?: string
}

export const agentApi = {
  getPresetAgents: (): Promise<Agent[]> =>
    request.get('/agents/preset'),

  getAvailableAgents: (): Promise<Agent[]> =>
    request.get('/agents'),

  createAgent: (data: CreateAgentData): Promise<Agent> =>
    request.post('/agents', data),

  updateAgent: (agentId: number, data: CreateAgentData): Promise<Agent> =>
    request.put(`/agents/${agentId}`, data),

  deleteAgent: (agentId: number): Promise<void> =>
    request.delete(`/agents/${agentId}`),

  initPresetAgents: (): Promise<void> =>
    request.post('/agents/init'),
}
