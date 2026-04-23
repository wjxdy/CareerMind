import type { AgentType, AgentCategory } from '@/types'

export interface AgentMeta {
  type: AgentType
  label: string
  short: string
  role: string
  symbol: 'glasses' | 'ruler' | 'shield' | 'arrow' | 'question' | 'merge' | 'user'
          | 'book' | 'scales' | 'handshake' | 'gavel'
  category: AgentCategory
}

export const AGENT_META: Record<AgentType, AgentMeta> = {
  // ====== 职业团 ======
  INDUSTRY_ANALYST:   { type: 'INDUSTRY_ANALYST',   label: '行业分析师', short: '行业', role: '洞察行业趋势与结构',       symbol: 'glasses',   category: 'CAREER' },
  SKILL_ASSESSOR:     { type: 'SKILL_ASSESSOR',     label: '能力评估师', short: '能力', role: '评估当前能力与缺口',       symbol: 'ruler',     category: 'CAREER' },
  RISK_WATCHER:       { type: 'RISK_WATCHER',       label: '风险警示者', short: '风险', role: '警示潜在风险与代价',       symbol: 'shield',    category: 'CAREER' },
  OPPORTUNITY_HUNTER: { type: 'OPPORTUNITY_HUNTER', label: '机会挖掘者', short: '机会', role: '挖掘被忽视的机会',         symbol: 'arrow',     category: 'CAREER' },
  VALUE_EXAMINER:     { type: 'VALUE_EXAMINER',     label: '价值拷问者', short: '价值', role: '叩问价值观与长期意义',     symbol: 'question',  category: 'CAREER' },

  // ====== 法律团 ======
  CONTRACT_REVIEWER:  { type: 'CONTRACT_REVIEWER',  label: '合同审查师', short: '合同', role: '审视条款漏洞与不利表述',   symbol: 'book',      category: 'LEGAL'  },
  LITIGATION_ANALYST: { type: 'LITIGATION_ANALYST', label: '诉讼风险师', short: '诉讼', role: '评估胜诉概率与成本',       symbol: 'scales',    category: 'LEGAL'  },
  RIGHTS_DEFENDER:    { type: 'RIGHTS_DEFENDER',    label: '权益维护者', short: '维权', role: '站在当事人一边主张权利',   symbol: 'shield',    category: 'LEGAL'  },
  PRACTICAL_COUNSEL:  { type: 'PRACTICAL_COUNSEL',  label: '实务执行官', short: '实务', role: '给出可执行的下一步',       symbol: 'arrow',     category: 'LEGAL'  },
  MEDIATION_ADVISOR:  { type: 'MEDIATION_ADVISOR',  label: '调解智者',   short: '调解', role: '从和解角度设计方案',       symbol: 'handshake', category: 'LEGAL'  },

  // ====== 特殊 ======
  MERGE_AGENT:        { type: 'MERGE_AGENT',        label: '整合专家',   short: '整合', role: '汇总观点生成候选方案',     symbol: 'merge',     category: 'SYSTEM' },
  CUSTOM:             { type: 'CUSTOM',             label: '自定义',     short: '自定', role: '用户自定义 Agent',         symbol: 'user',      category: 'CUSTOM' },
}

export const getAgentMeta = (type: AgentType | string | undefined): AgentMeta => {
  if (!type || !(type in AGENT_META)) return AGENT_META.CUSTOM
  return AGENT_META[type as AgentType]
}

export const CATEGORY_LABELS: Record<AgentCategory, string> = {
  CAREER: '职业团',
  LEGAL:  '法律团',
  CUSTOM: '自定义',
  SYSTEM: '系统',
}
