// 用户类型
export interface User {
  id: number
  username: string
  email: string
  avatarUrl?: string
}

// Agent类型
export type AgentType =
  | 'INDUSTRY_ANALYST'
  | 'SKILL_ASSESSOR'
  | 'RISK_WATCHER'
  | 'OPPORTUNITY_HUNTER'
  | 'VALUE_EXAMINER'
  | 'MERGE_AGENT'
  | 'CUSTOM'

export interface Agent {
  id: number
  name: string
  type: AgentType
  systemPrompt?: string
  modelType?: string
  avatarUrl?: string
  description?: string
  isPreset: boolean
}

// Task状态
export type TaskStatus = 'PENDING' | 'DISCUSSING' | 'MERGING' | 'COMPLETED' | 'ARCHIVED'

export interface Task {
  id: number
  title: string
  background?: string
  goal?: string
  constraints?: string
  status: TaskStatus
  agents: Agent[]
  kbId?: number
  createdAt: string
  updatedAt: string
}

// 讨论相关
export type RoundType = 'INDEPENDENT' | 'CHALLENGE' | 'REVISION' | 'FINAL'

export interface Message {
  id: number
  agentId: number
  agentName: string
  agentAvatar?: string
  agentType: AgentType
  content: string
  replyToMessageId?: number
  messageType?: 'AGENT' | 'USER' | 'INTERJECTION'
  isFinal: boolean
  createdAt: string
}

export interface Round {
  id: number
  roundNumber: number
  roundType: RoundType
  isCompleted: boolean
  messages: Message[]
  createdAt: string
}

export interface Discussion {
  id: number
  taskId: number
  currentRound: number
  isActive: boolean
  isPaused: boolean
  rounds: Round[]
}

// Merge结果
export interface Plan {
  id: number
  title: string
  description: string
  confidence: number
  supporters: string[]
  opponents: string[]
  milestones: string[]
  risks: string[]
  applicableConditions: string
  isSelected: boolean
}

export interface MergeResult {
  id: number
  summary: string
  plans: Plan[]
  blindSpots: string[]
  convergenceRate: number
}

// 创建任务数据
export interface CreateTaskData {
  title: string
  background?: string
  goal?: string
  constraints?: string
  agentIds: number[]
  kbId?: number
}

// API响应
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}
