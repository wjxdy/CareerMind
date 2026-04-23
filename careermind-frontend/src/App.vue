<template>
  <n-config-provider :theme="naiveTheme" :theme-overrides="themeOverrides">
    <n-loading-bar-provider>
      <n-message-provider>
        <n-dialog-provider>
          <n-notification-provider>
            <div id="app">
              <NaiveDiscreteInstaller />
              <router-view v-slot="{ Component }">
                <transition name="page-fade" mode="out-in">
                  <component :is="Component" />
                </transition>
              </router-view>
            </div>
          </n-notification-provider>
        </n-dialog-provider>
      </n-message-provider>
    </n-loading-bar-provider>
  </n-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  NConfigProvider, NMessageProvider, NDialogProvider, NLoadingBarProvider, NNotificationProvider,
  darkTheme,
} from 'naive-ui'
import { useThemeStore } from '@/stores/theme'
import NaiveDiscreteInstaller from '@/components/ui/NaiveDiscreteInstaller.vue'

const themeStore = useThemeStore()
const naiveTheme = computed(() => themeStore.theme === 'dark' ? darkTheme : null)

const themeOverrides = {
  common: {
    primaryColor: '#3B82F6',
    primaryColorHover: '#2563EB',
    primaryColorPressed: '#1D4ED8',
    primaryColorSuppl: '#3B82F6',
    borderRadius: '10px',
    borderRadiusSmall: '6px',
    fontFamily: 'Inter, "Noto Sans SC", -apple-system, BlinkMacSystemFont, sans-serif',
  },
  Button: { borderRadiusMedium: '10px' },
  Card:   { borderRadius: '16px' },
  Dialog: { borderRadius: '16px' },
  Modal:  {},
}
</script>

<style>
.page-fade-enter-active, .page-fade-leave-active {
  transition: opacity var(--duration-base) var(--ease-standard), transform var(--duration-base) var(--ease-standard);
}
.page-fade-enter-from { opacity: 0; transform: translateY(8px); }
.page-fade-leave-to   { opacity: 0; transform: translateY(-4px); }
</style>
