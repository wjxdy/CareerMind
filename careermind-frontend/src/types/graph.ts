export interface GraphNode {
  id: string
  agentId: number
  agentType: string
  agentName: string
  roundNumber: number
  messageId: number
  snippet: string
  confidence: number
  wordCount: number
}

export interface GraphEdge {
  id: string
  from: string
  to: string
  type: 'SUPPORT' | 'CHALLENGE' | 'REVISE'
}

export interface GraphRoundStat {
  roundNumber: number
  divergence: number
}

export interface GraphResponse {
  nodes: GraphNode[]
  edges: GraphEdge[]
  rounds: GraphRoundStat[]
  finalConvergence: number
}
