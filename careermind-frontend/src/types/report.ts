import type { GraphResponse } from './graph'
import type { MergeResult } from './index'

export interface ActionPlan {
  day7: string[]
  day30: string[]
  day90: string[]
}

export interface ReportExtras {
  executiveSummary: string
  actionPlan: ActionPlan
}

export interface ReportRoundItem {
  agentId: number
  agentName: string
  agentType: string
  content: string
  confidence: number
}

export interface ReportRound {
  roundNumber: number
  label: string
  divergence: number
  messages: ReportRoundItem[]
}

export interface ReportTask {
  id: number
  title: string
  background?: string
  goal?: string
  constraints?: string
  createdAt?: string
  username?: string
}

export interface ReportDiscussionMeta {
  currentRound?: number
  startedAt?: string
  endedAt?: string
  totalMessages?: number
}

export interface ReportResponse {
  task: ReportTask
  discussion: ReportDiscussionMeta
  rounds: ReportRound[]
  graph: GraphResponse
  mergeResult: MergeResult | null
  extras: ReportExtras
}
