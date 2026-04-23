<template>
  <div class="login-layout">
    <aside class="left-pane">
      <BrandLogo size="lg" />
      <div class="left-art">
        <div class="orb c-center" data-agent-type="MERGE_AGENT" />
        <div class="orb p-1" data-agent-type="INDUSTRY_ANALYST" />
        <div class="orb p-2" data-agent-type="SKILL_ASSESSOR" />
        <div class="orb p-3" data-agent-type="RISK_WATCHER" />
        <div class="orb p-4" data-agent-type="OPPORTUNITY_HUNTER" />
        <div class="orb p-5" data-agent-type="VALUE_EXAMINER" />
      </div>
      <p class="tagline">让五位 AI 专家为你辩一场</p>
    </aside>

    <section class="right-pane">
      <div class="form-box">
        <h2>{{ isRegister ? '创建账户' : '欢迎回来' }}</h2>
        <p class="muted">{{ isRegister ? '30 秒开启一次 AI 辩论' : '继续你的决策讨论' }}</p>

        <BaseInput v-if="isRegister" v-model="registerForm.username" label="用户名" placeholder="3-20 位" />
        <BaseInput v-model="form.email" type="email" label="邮箱" placeholder="email@example.com" />
        <BaseInput v-model="form.password" type="password" label="密码" placeholder="至少 6 位" @keyup="onKey" />

        <BaseButton variant="primary" size="lg" block :loading="userStore.loading" @click="submit">
          {{ isRegister ? '注册' : '登录' }}
        </BaseButton>

        <p class="switch">
          {{ isRegister ? '已有账户？' : '第一次来？' }}
          <a href="#" @click.prevent="isRegister = !isRegister">{{ isRegister ? '去登录' : '去注册' }}</a>
        </p>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message as ElMessage } from '@/utils/naive-discrete'
import { useUserStore } from '@/stores/user'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isRegister = ref(false)
const form = reactive({ email: '', password: '' })
const registerForm = reactive({ username: '' })

const redirectPath = computed(() => (route.query.redirect as string) || '/')

const validate = () => {
  if (!form.email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) { ElMessage.error('邮箱格式不正确'); return false }
  if (!form.password || form.password.length < 6) { ElMessage.error('密码至少 6 位'); return false }
  if (isRegister.value && (!registerForm.username || registerForm.username.length < 3)) {
    ElMessage.error('用户名至少 3 位'); return false
  }
  return true
}

const submit = async () => {
  if (!validate()) return
  if (isRegister.value) {
    const ok = await userStore.register(registerForm.username, form.email, form.password)
    if (ok) { ElMessage.success('注册成功'); router.push('/') }
  } else {
    const ok = await userStore.login(form.email, form.password)
    if (ok) { ElMessage.success('登录成功'); setTimeout(() => router.push(redirectPath.value), 100) }
    else ElMessage.error('登录失败，请检查邮箱和密码')
  }
}

const onKey = (ev: KeyboardEvent) => { if (ev.key === 'Enter') submit() }
</script>

<style scoped>
.login-layout { display: grid; grid-template-columns: 1fr 1fr; min-height: 100vh; }
.left-pane {
  position: relative; padding: 48px; display: flex; flex-direction: column; justify-content: space-between;
  background: linear-gradient(135deg, var(--bg-elevated) 0%, var(--bg-card) 100%);
  border-right: 1px solid var(--border-subtle); overflow: hidden;
}
.left-art { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; }
.orb { position: absolute; width: 72px; height: 72px; border-radius: 50%; background: var(--agent-dim); border: 2px solid var(--agent); }
.c-center { width: 40px; height: 40px; }
.p-1 { top: 22%; left: 50%; transform: translate(-50%,0); }
.p-2 { top: 38%; right: 18%; }
.p-3 { bottom: 22%; right: 28%; }
.p-4 { bottom: 22%; left: 28%; }
.p-5 { top: 38%; left: 18%; }
.tagline { font-size: 15px; color: var(--text-secondary); margin: 0; z-index: 2; }

.right-pane { display: flex; align-items: center; justify-content: center; padding: 48px; background: var(--bg-page); }
.form-box { width: 360px; display: flex; flex-direction: column; gap: 16px; }
.form-box h2 { font-size: 28px; margin: 0; }
.form-box .muted { margin: 0 0 8px; color: var(--text-secondary); font-size: 14px; }
.switch { margin: 0; text-align: center; font-size: 13px; color: var(--text-muted); }
.switch a { color: var(--accent); font-weight: 500; }
@media (max-width: 768px) { .login-layout { grid-template-columns: 1fr; } .left-pane { display: none; } }
</style>
