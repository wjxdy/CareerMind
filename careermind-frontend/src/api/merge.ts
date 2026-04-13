import request from './request'
import type { MergeResult } from '@/types'

export const mergeApi = {
  generateMergeResult: (taskId: number): Promise<MergeResult> =>
    request.post(`/merge/tasks/${taskId}/generate`),

  getMergeResult: (taskId: number): Promise<MergeResult> =>
    request.get(`/merge/tasks/${taskId}`),

  selectPlan: (mergeResultId: number, planId: number): Promise<MergeResult> =>
    request.post(`/merge/${mergeResultId}/select-plan`, { planId }),
}
