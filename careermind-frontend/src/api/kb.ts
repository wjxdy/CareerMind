import request from './request'
import type { KnowledgeBase, KbListResponse, CreateKbData, DocListResponse, QueryResult } from '@/types/kb'

export const kbApi = {
  getKbs: (params?: { type?: string; page?: number; size?: number }): Promise<KbListResponse> =>
    request.get('/kb', { params }),

  createKb: (data: CreateKbData): Promise<KnowledgeBase> =>
    request.post('/kb', data),

  deleteKb: (kbId: number): Promise<void> =>
    request.delete(`/kb/${kbId}`),

  getDocuments: (kbId: number, params?: { page?: number; size?: number }): Promise<DocListResponse> =>
    request.get(`/kb/${kbId}/documents`, { params }),

  uploadDocument: (kbId: number, file: File): Promise<any> => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post(`/kb/${kbId}/documents`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },

  deleteDocument: (kbId: number, docId: number): Promise<void> =>
    request.delete(`/kb/${kbId}/documents/${docId}`),

  queryKb: (kbId: number, query: string): Promise<{ query: string; results: QueryResult[] }> =>
    request.post(`/kb/${kbId}/query`, { query, top_k: 5, score_threshold: 0.7 }),
}
