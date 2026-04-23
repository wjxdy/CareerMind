import request from './request'
import type { GraphResponse } from '@/types/graph'

export const graphApi = {
  getGraph: (taskId: number): Promise<GraphResponse> =>
    request.get(`/discussions/tasks/${taskId}/graph`),
}
