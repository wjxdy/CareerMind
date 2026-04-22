import type { AgentType } from '@/types'

export interface AgentMeta {
  type: AgentType
  label: string
  short: string
  role: string
  symbol: 'glasses' | 'ruler' | 'shield' | 'arrow' | 'question' | 'merge' | 'user'
}

export const AGENT_META: Record<AgentType, AgentMeta> = {
  INDUSTRY_ANALYST:   { type: 'INDUSTRY_ANALYST',   label: '行业分析师', short: '行业', role: '洞察行业趋势与结构',       symbol: 'glasses'  },
  SKILL_ASSESSOR:     { type: 'SKILL_ASSESSOR',     label: '能力评估师', short: '能力', role: '评估当前能力与缺口',       symbol: 'ruler'    },
  RISK_WATCHER:       { type: 'RISK_WATCHER',       label: '风险警示者', short: '风险', role: '警示潜在风险与代价',       symbol: 'shield'   },
  OPPORTUNITY_HUNTER: { type: 'OPPORTUNITY_HUNTER', label: '机会挖掘者', short: '机会', role: '挖掘被忽视的机会',         symbol: 'arrow'    },
  VALUE_EXAMINER:     { type: 'VALUE_EXAMINER',     label: '价值拷问者', short: '价值', role: '叩问价值观与长期意义',     symbol: 'question' },
  MERGE_AGENT:        { type: 'MERGE_AGENT',        label: '整合专家',   short: '整合', role: '汇总观点生成候选方案',     symbol: 'merge'    },
  CUSTOM:             { type: 'CUSTOM',             label: '自定义',     short: '自定', role: '用户自定义 Agent',         symbol: 'user'     },
}

export const getAgentMeta = (type: AgentType | string | undefined): AgentMeta => {
  if (!type || !(type in AGENT_META)) return AGENT_META.CUSTOM
  return AGENT_META[type as AgentType]
}
