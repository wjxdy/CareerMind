import request from './request'
import type { User } from '@/types'

export interface LoginData {
  email: string
  password: string
}

export interface RegisterData {
  username: string
  email: string
  password: string
}

export interface AuthResponse {
  token: string
  user: User
}

export const authApi = {
  login: (data: LoginData): Promise<AuthResponse> =>
    request.post('/auth/login', data),

  register: (data: RegisterData): Promise<AuthResponse> =>
    request.post('/auth/register', data),
}
