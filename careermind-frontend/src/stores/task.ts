import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Task, CreateTaskData } from '@/types'
import { taskApi } from '@/api/task'

export const useTaskStore = defineStore('task', () => {
  // State
  const tasks = ref<Task[]>([])
  const currentTask = ref<Task | null>(null)
  const loading = ref(false)

  // Actions
  const fetchTasks = async () => {
    const userId = localStorage.getItem('userId')
    if (!userId) {
      return
    }
    loading.value = true
    try {
      tasks.value = await taskApi.getUserTasks()
    } catch (error: any) {
      console.error('fetchTasks 失败:', error.message)
    } finally {
      loading.value = false
    }
  }

  const fetchTaskById = async (taskId: number) => {
    loading.value = true
    try {
      currentTask.value = await taskApi.getTaskById(taskId)
    } finally {
      loading.value = false
    }
  }

  const createTask = async (data: CreateTaskData) => {
    const task = await taskApi.createTask(data)
    tasks.value.unshift(task)
    // 触发事件通知其他组件任务列表已更新
    window.dispatchEvent(new CustomEvent('task-created', { detail: task }))
    return task
  }

  const deleteTask = async (taskId: number) => {
    await taskApi.deleteTask(taskId)
    tasks.value = tasks.value.filter(t => t.id !== taskId)
  }

  return {
    tasks,
    currentTask,
    loading,
    fetchTasks,
    fetchTaskById,
    createTask,
    deleteTask,
  }
})
