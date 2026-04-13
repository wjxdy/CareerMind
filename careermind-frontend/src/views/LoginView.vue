<template>
  <div class="login-container">
    <div class="login-box">
      <div class="logo">
        <el-icon :size="48" color="#0ea5e9"><ChatDotRound /></el-icon>
        <h1>CareerMind</h1>
        <p>多Agent职业发展决策系统</p>
      </div>

      <el-tabs v-model="activeTab" class="login-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" :rules="rules" ref="loginFormRef" @submit.prevent="handleLogin">
            <el-form-item prop="email">
              <el-input
                v-model="loginForm.email"
                placeholder="邮箱"
                prefix-icon="Message"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                placeholder="密码"
                prefix-icon="Lock"
                size="large"
                show-password
                @keyup.enter="handleLogin"
              />
            </el-form-item>
            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="userStore.loading"
              @click="handleLogin"
            >
              登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef">
            <el-form-item prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="用户名"
                prefix-icon="User"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="邮箱"
                prefix-icon="Message"
                size="large"
              />
            </el-form-item>
            <el-form-item prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                placeholder="密码"
                prefix-icon="Lock"
                size="large"
                show-password
                @keyup.enter="handleRegister"
              />
            </el-form-item>
            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="userStore.loading"
              @click="handleRegister"
            >
              注册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 获取登录后要跳转的页面
const redirectPath = computed(() => {
  const redirect = route.query.redirect as string
  return redirect || '/'
})

const activeTab = ref('login')
const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

const loginForm = reactive({
  email: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
})

const rules: FormRules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

const registerRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20位', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

const handleLogin = async () => {
  console.log('点击登录按钮')
  if (!loginFormRef.value) {
    console.error('loginFormRef 为空')
    return
  }

  console.log('表单数据:', loginForm)

  try {
    const valid = await loginFormRef.value.validate()
    console.log('表单验证结果:', valid)

    if (valid) {
      console.log('开始登录...')
      const success = await userStore.login(loginForm.email, loginForm.password)
      console.log('登录结果:', success)
      console.log('登录后状态:', {
        isLoggedIn: userStore.isLoggedIn,
        hasToken: !!userStore.token,
        hasUser: !!userStore.user,
        token: userStore.token?.slice(0, 20),
        user: userStore.user
      })
      if (success) {
        ElMessage.success('登录成功')
        // 延迟跳转，确保状态更新
        setTimeout(async () => {
          console.log('准备跳转到:', redirectPath.value)
          await router.push(redirectPath.value)
        }, 100)
      } else {
        ElMessage.error('登录失败，请检查邮箱和密码')
      }
    }
  } catch (error: any) {
    console.error('表单验证失败或登录出错:', error)
    if (error?.message) {
      ElMessage.error(error.message)
    }
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return

  await registerFormRef.value.validate(async (valid) => {
    if (valid) {
      const success = await userStore.register(
        registerForm.username,
        registerForm.email,
        registerForm.password
      )
      if (success) {
        ElMessage.success('注册成功')
        router.push('/')
      }
    }
  })
}

</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 420px;
  padding: 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.logo {
  text-align: center;
  margin-bottom: 30px;
}

.logo h1 {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin-top: 12px;
  margin-bottom: 4px;
}

.logo p {
  font-size: 14px;
  color: #6b7280;
}

.login-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.login-tabs :deep(.el-tabs__nav) {
  width: 100%;
}

.login-tabs :deep(.el-tabs__item) {
  width: 50%;
  text-align: center;
  font-size: 16px;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
}
</style>
