import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { message as ElMessage } from '@/utils/naive-discrete'
import type { User } from '@/types'
import { authApi } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string>(localStorage.getItem('token') || '')
  const user = ref<User | null>(null)
  const loading = ref(false)

  // 从 localStorage 恢复用户信息
  const storedUserId = localStorage.getItem('userId')
  const storedUsername = localStorage.getItem('username')
  const storedEmail = localStorage.getItem('email')

  if (storedUserId && storedUsername && storedEmail) {
    user.value = {
      id: parseInt(storedUserId),
      username: storedUsername,
      email: storedEmail
    }
  }

  // Getters
  const isLoggedIn = computed(() => {
    return !!token.value && !!user.value
  })
  const userId = computed(() => user.value?.id)

  // Actions
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setUser = (userData: User) => {
    user.value = userData
    localStorage.setItem('userId', userData.id.toString())
    localStorage.setItem('username', userData.username)
    localStorage.setItem('email', userData.email)
  }

  const login = async (email: string, password: string) => {
    loading.value = true

    try {
      // 登录前清除可能存在的旧 token
      localStorage.removeItem('token')
      localStorage.removeItem('userId')

      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      })

      if (!response.ok) {
        throw new Error(`Request failed with status code ${response.status}`)
      }

      const res = await response.json()

      if (!res || res.code !== 200) {
        return false
      }

      if (res.data?.token) {
        setToken(res.data.token)
      } else {
        return false
      }
      if (res.data?.user) {
        setUser(res.data.user)
      }

      return true
    } catch (error: any) {
      if (error.message?.includes('Network Error')) {
        ElMessage.error('无法连接到服务器，请确保后端已启动')
      } else {
        ElMessage.error(error.response?.data?.message || '登录失败')
      }
      return false
    } finally {
      loading.value = false
    }
  }

  const register = async (username: string, email: string, password: string) => {
    loading.value = true
    try {
      const res = await authApi.register({ username, email, password })
      setToken(res.token)
      setUser(res.user)
      return true
    } catch (error) {
      return false
    } finally {
      loading.value = false
    }
  }

  const logout = () => {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('email')
  }

  return {
    token,
    user,
    loading,
    isLoggedIn,
    userId,
    login,
    register,
    logout,
    setToken,
    setUser,
  }
})
