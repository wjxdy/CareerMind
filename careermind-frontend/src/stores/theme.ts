import { defineStore } from 'pinia'
import { ref, watch } from 'vue'

export type Theme = 'light' | 'dark'

export const useThemeStore = defineStore('theme', () => {
  const saved = (localStorage.getItem('cm-theme') as Theme | null) ?? 'light'
  const theme = ref<Theme>(saved)

  const apply = (t: Theme) => {
    document.documentElement.setAttribute('data-theme', t)
    localStorage.setItem('cm-theme', t)
  }
  apply(theme.value)

  watch(theme, apply)

  const toggle = () => { theme.value = theme.value === 'light' ? 'dark' : 'light' }
  return { theme, toggle }
})
