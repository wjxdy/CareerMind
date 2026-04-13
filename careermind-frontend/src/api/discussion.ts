import request from './request'
import type { Discussion } from '@/types'

export const discussionApi = {
  // 获取讨论
  getDiscussion: async (taskId: number): Promise<Discussion | null> => {
    try {
      return await request.get(`/discussions/tasks/${taskId}`)
    } catch (error: any) {
      // 讨论不存在时返回null，不抛出错误
      if (error?.response?.status === 404) {
        return null
      }
      throw error
    }
  },

  // 开始讨论
  startDiscussion: (taskId: number): Promise<Discussion> =>
    request.post(`/discussions/tasks/${taskId}/start`),

  // 暂停讨论
  pauseDiscussion: (taskId: number): Promise<Discussion> =>
    request.post(`/discussions/tasks/${taskId}/pause`),

  // 继续讨论
  resumeDiscussion: (taskId: number): Promise<Discussion> =>
    request.post(`/discussions/tasks/${taskId}/resume`),

  // 停止讨论
  stopDiscussion: (taskId: number): Promise<Discussion> =>
    request.post(`/discussions/tasks/${taskId}/stop`),

  // 进入下一轮
  nextRound: (taskId: number): Promise<Discussion> =>
    request.post(`/discussions/tasks/${taskId}/next-round`),

  // 发送用户消息
  sendMessage: (taskId: number, content: string): Promise<void> =>
    request.post(`/discussions/tasks/${taskId}/messages`, { content }),
}
