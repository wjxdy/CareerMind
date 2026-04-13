export interface KnowledgeBase {
  id: number
  name: string
  description?: string
  kb_type: string
  document_count: number
  chunk_count: number
  created_at: string
}

export interface KbListResponse {
  items: KnowledgeBase[]
  total: number
  page: number
  size: number
}

export interface CreateKbData {
  name: string
  description?: string
  kb_type: string
  chunk_size?: number
  chunk_overlap?: number
}

export interface DocumentItem {
  id: number
  filename: string
  file_type: string
  file_size: number
  status: string
  chunk_count: number
  created_at: string
}

export interface DocListResponse {
  items: DocumentItem[]
  total: number
  page: number
  size: number
}

export interface QueryResult {
  content: string
  score: number
  document: {
    id: number
    filename: string
    file_type: string
  }
}
