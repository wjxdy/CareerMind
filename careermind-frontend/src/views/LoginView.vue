<template>
  <div class="login">
    <header class="nav">
      <BrandLogo @click="$router.push('/')" />
      <BaseButton variant="ghost" size="sm" @click="$router.push('/')">返回首页</BaseButton>
    </header>

    <main class="main">
      <div class="card">
        <h1 class="title">欢迎回来</h1>
        <p class="subtitle">继续你的决策讨论</p>

        <form class="form" @submit.prevent="submit">
          <BaseInput v-model="form.email" type="email" label="邮箱" placeholder="email@example.com" />
          <BaseInput v-model="form.password" type="password" label="密码" placeholder="至少 6 位" @keyup="onKey" />

          <BaseButton variant="primary" size="lg" block :loading="userStore.loading" @click="submit">
            登录
          </BaseButton>
        </form>
      </div>

      <p class="tagline">让五位 AI 专家，为你的决定辩一场</p>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message as ElMessage } from '@/utils/naive-discrete'
import { useUserStore } from '@/stores/user'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const form = reactive({ email: '', password: '' })

const redirectPath = computed(() => (route.query.redirect as string) || '/')

const validate = () => {
  if (!form.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) { ElMessage.error('邮箱格式不正确'); return false }
  if (!form.password || form.password.length < 6) { ElMessage.error('密码至少 6 位'); return false }
  return true
}

const submit = async () => {
  if (!validate()) return
  const ok = await userStore.login(form.email, form.password)
  if (ok) { ElMessage.success('登录成功'); setTimeout(() => router.push(redirectPath.value), 100) }
  else ElMessage.error('登录失败，请检查邮箱和密码')
}

const onKey = (ev: KeyboardEvent) => { if (ev.key === 'Enter') submit() }
</script>

<style scoped>
.login { min-height: 100vh; background: var(--bg-page); display: flex; flex-direction: column; }

.nav {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 40px;
}

.main {
  flex: 1;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 40px 24px;
}
.card {
  width: 100%; max-width: 420px;
  padding: 48px 44px;
  background: var(--bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
}
.title { font-size: 36px; margin: 0 0 8px; letter-spacing: -0.025em; }
.subtitle { margin: 0 0 32px; color: var(--text-secondary); font-size: 16px; letter-spacing: -0.005em; }

.form { display: flex; flex-direction: column; gap: 18px; }

.switch { margin: 24px 0 0; text-align: center; font-size: 14px; color: var(--text-secondary); }
.switch a { color: var(--accent); font-weight: 500; }

.tagline { margin-top: 48px; font-size: 13px; color: var(--text-muted); letter-spacing: -0.005em; }

@media (max-width: 600px) {
  .nav { padding: 16px 20px; }
  .card { padding: 36px 28px; }
  .title { font-size: 28px; }
}
</style>
