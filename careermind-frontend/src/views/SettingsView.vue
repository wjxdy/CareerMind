<template>
  <PageShell>
    <div class="settings-page">
      <h2>个人设置</h2>
      <p class="muted">管理你的偏好与账号信息</p>

      <BaseCard class="settings-card">
        <template #header>外观</template>
        <div class="row">
          <div>
            <p class="row-title">主题</p>
            <p class="row-hint">在浅色与深色之间切换</p>
          </div>
          <ThemeToggle />
        </div>
      </BaseCard>

      <BaseCard class="settings-card">
        <template #header>个人简介</template>
        <BaseInput v-model="bio" textarea :rows="6"
                   placeholder="描述你的教育背景、工作经历、技能特长等，将作为咨询的背景信息" />
        <div class="card-foot">
          <p class="row-hint">新建咨询时自动填入为背景，随时可修改</p>
          <BaseButton variant="primary" size="sm" :loading="saving" @click="saveBio">保存</BaseButton>
        </div>
      </BaseCard>

      <BaseCard class="settings-card" v-if="user">
        <template #header>账号</template>
        <div class="row"><p class="row-title">用户名</p><p class="row-value">{{ user.username }}</p></div>
        <div class="row"><p class="row-title">邮箱</p><p class="row-value">{{ user.email }}</p></div>
      </BaseCard>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageShell from '@/components/ui/PageShell.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import { useUserStore } from '@/stores/user'

const bio = ref('')
const saving = ref(false)
const user = computed(() => useUserStore().user)

onMounted(() => {
  bio.value = localStorage.getItem('userBio') || ''
})

const saveBio = async () => {
  saving.value = true
  try {
    localStorage.setItem('userBio', bio.value)
    ElMessage.success('已保存')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.settings-page { padding: 32px 40px; max-width: 820px; margin: 0 auto; overflow-y: auto; height: 100%; }
.settings-page h2 { margin: 0; font-size: 22px; }
.muted { margin: 4px 0 20px; font-size: 13px; color: var(--text-secondary); }

.settings-card { margin-bottom: 16px; }
.row { display: flex; align-items: center; justify-content: space-between; padding: 10px 0; border-top: 1px solid var(--border-subtle); }
.row:first-child { border-top: none; }
.row-title { margin: 0; font-size: 14px; font-weight: 500; color: var(--text-primary); }
.row-hint  { margin: 2px 0 0; font-size: 12px; color: var(--text-muted); }
.row-value { margin: 0; font-size: 13px; color: var(--text-secondary); font-family: var(--font-mono); }

.card-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
</style>
