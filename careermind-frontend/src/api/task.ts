import request from './request'
import type { Task } from '@/types'

export interface CreateTaskData {
  title: string
  background?: string
  goal?: string
  constraints?: string
  agentIds: number[]
  kbId?: number
}

export const taskApi = {
  getUserTasks: (): Promise<Task[]> =>
    request.get('/tasks'),

  getTaskById: (taskId: number): Promise<Task> =>
    request.get(`/tasks/${taskId}`),

  createTask: (data: CreateTaskData): Promise<Task> =>
    request.post('/tasks', data),

  updateTaskStatus: (taskId: number, status: string): Promise<Task> =>
    request.put(`/tasks/${taskId}/status`, null, { params: { status } }),

  deleteTask: (taskId: number): Promise<void> =>
    request.delete(`/tasks/${taskId}`),
}
