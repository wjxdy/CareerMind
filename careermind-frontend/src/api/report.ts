import request from './request'
import type { ReportResponse } from '@/types/report'

export const reportApi = {
  getReport: (taskId: number, refresh = false): Promise<ReportResponse> =>
    request.get(`/reports/${taskId}`, { params: { refresh } }),
  regenerate: (taskId: number): Promise<ReportResponse> =>
    request.post(`/reports/${taskId}/regenerate-summary`),
}
