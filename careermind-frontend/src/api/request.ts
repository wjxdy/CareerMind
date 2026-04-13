import axios from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    const userId = localStorage.getItem('userId')

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    if (userId) {
      config.headers['X-User-Id'] = userId
    }

    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResponse<any>

    if (res.code !== undefined && res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res.data !== undefined ? res.data : response.data
  },
  (error) => {
    // 404 错误静默处理（讨论不存在、结果不存在等预期内的情况）
    if (error.response?.status === 404) {
      return Promise.reject(error)
    }

    if (error.response?.status === 403 || error.response?.status === 401) {
      ElMessage.error('登录已过期，请重新登录')
      // 延迟清除和跳转，避免循环
      setTimeout(() => {
        localStorage.removeItem('token')
        localStorage.removeItem('userId')
        localStorage.removeItem('username')
        localStorage.removeItem('email')
        window.location.href = '/login'
      }, 100)
    } else {
      ElMessage.error(error.response?.data?.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
