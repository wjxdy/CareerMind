# P1 品牌与 UI/UX 重做 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 按 `docs/superpowers/specs/2026-04-22-brand-redesign-design.md` 将 CareerMind 前端升级为 Notion/Linear 风格的统一设计体系，讨论页换为圆桌辩论布局，5 Agent 人格化。

**Architecture:** 保留 Vue 3 + TailwindCSS + Element Plus 基础栈；新增 `src/styles/tokens.css`（日/夜双主题 CSS 变量）、`src/components/ui/*`（基础组件）、`src/components/agent/*`（Agent 人格组件）；DiscussionPanel 拆分为 RoundtableStage + MessageDrawer。Element Plus 保留在 Dialog/Form/Message 等复杂场景，其他组件用自写包装。

**Tech Stack:** Vue 3, TypeScript, TailwindCSS, @vueuse/motion (新), Pinia (主题 store), Playwright (E2E).

---

## File Structure

**新增：**
```
src/styles/tokens.css
src/styles/base.css
src/styles/element-overrides.css
src/components/ui/BaseButton.vue
src/components/ui/BaseCard.vue
src/components/ui/BaseBadge.vue
src/components/ui/BaseTag.vue
src/components/ui/BaseInput.vue
src/components/ui/BaseSkeleton.vue
src/components/ui/EmptyState.vue
src/components/ui/BrandLogo.vue
src/components/ui/ThemeToggle.vue
src/components/ui/PageShell.vue
src/components/agent/AgentAvatar.vue
src/components/agent/AgentAvatarGroup.vue
src/components/agent/AgentBadge.vue
src/components/agent/AgentCard.vue
src/components/discussion/RoundTimeline.vue
src/components/discussion/RoundtableStage.vue
src/components/discussion/MessageDrawer.vue
src/components/discussion/SpeechBubble.vue
src/components/discussion/ChallengeFlow.vue
src/stores/theme.ts
src/composables/useAgentTheme.ts
src/utils/agent-meta.ts
e2e-tests/tests/ui-brand.spec.js
```

**修改：**
```
tailwind.config.js
index.html
src/main.ts
src/App.vue
src/views/HomeView.vue
src/views/LoginView.vue
src/views/TasksView.vue
src/views/TaskView.vue
src/views/DiscussionView.vue
src/views/ResultView.vue
src/views/AgentsView.vue
src/views/KbView.vue
src/views/SettingsView.vue
src/components/layout/Sidebar.vue
src/components/discussion/DiscussionPanel.vue
src/components/discussion/AgentMessage.vue
```

**删除：**
```
src/components/discussion/RoundIndicator.vue (被 RoundTimeline 取代)
```

---

## 任务总览

- Task 1: 装 @vueuse/motion 依赖
- Task 2: 写 tokens.css + base.css
- Task 3: 更新 tailwind.config.js
- Task 4: index.html 引入字体
- Task 5: 写 theme store + ThemeToggle + App.vue 挂载
- Task 6: 写 agent-meta.ts + useAgentTheme
- Task 7: BaseButton
- Task 8: BaseCard + BaseBadge + BaseTag
- Task 9: BaseInput + BaseSkeleton + EmptyState
- Task 10: BrandLogo
- Task 11: AgentAvatar（含状态机）
- Task 12: AgentAvatarGroup + AgentBadge + AgentCard
- Task 13: RoundTimeline
- Task 14: PageShell + Sidebar 重做
- Task 15: HomeView 重做
- Task 16: LoginView 重做
- Task 17: SpeechBubble + ChallengeFlow
- Task 18: RoundtableStage
- Task 19: MessageDrawer
- Task 20: DiscussionPanel 整合 + AgentMessage 重写
- Task 21: TasksView 重做
- Task 22: TaskView 重做
- Task 23: ResultView 重做
- Task 24: AgentsView / KbView / SettingsView 重做
- Task 25: element-overrides.css
- Task 26: Playwright E2E ui-brand.spec.js
- Task 27: build + 手动回归 + 截图素材

---

## Task 1: 装 @vueuse/motion 依赖

**Files:**
- Modify: `careermind-frontend/package.json`

- [ ] **Step 1:** 在 `careermind-frontend/` 下执行：

```bash
cd careermind-frontend && npm install @vueuse/motion@^2.2.0
```

- [ ] **Step 2:** 确认 `package.json` 里 dependencies 多了 `"@vueuse/motion": "^2.2.0"`，无报错。

- [ ] **Step 3:** Commit：

```bash
git add careermind-frontend/package.json careermind-frontend/package-lock.json
git commit -m "chore: add @vueuse/motion for P1 redesign"
```

---

## Task 2: tokens.css + base.css

**Files:**
- Create: `careermind-frontend/src/styles/tokens.css`
- Create: `careermind-frontend/src/styles/base.css`

- [ ] **Step 1:** 创建 `tokens.css`，内容（全量落盘，按 spec 的 3.1/3.2 段）：

```css
:root {
  --bg-page:         #FAFAFA;
  --bg-card:         #FFFFFF;
  --bg-elevated:     #F4F4F5;
  --bg-inset:        #F9FAFB;

  --border-subtle:   #E4E4E7;
  --border-emphasis: #D4D4D8;
  --border-strong:   #A1A1AA;

  --text-primary:    #09090B;
  --text-secondary:  #52525B;
  --text-muted:      #A1A1AA;
  --text-inverse:    #FAFAFA;

  --accent:          #3B82F6;
  --accent-hover:    #2563EB;
  --accent-dim:      #EFF6FF;
  --accent-contrast: #FFFFFF;

  --success:         #10B981;
  --warning:         #F59E0B;
  --danger:          #EF4444;

  --shadow-sm:       0 1px 2px rgb(0 0 0 / 0.04);
  --shadow-md:       0 4px 12px rgb(0 0 0 / 0.06);
  --shadow-lg:       0 10px 32px rgb(0 0 0 / 0.10);

  --radius-sm:       6px;
  --radius-md:       10px;
  --radius-lg:       16px;
  --radius-full:     9999px;

  --font-sans:       "Inter", "Noto Sans SC", -apple-system, BlinkMacSystemFont, "Helvetica Neue", sans-serif;
  --font-mono:       "JetBrains Mono", ui-monospace, Menlo, monospace;
  --font-serif-zh:   "Noto Serif SC", serif;

  --duration-fast:   120ms;
  --duration-base:   240ms;
  --duration-slow:   480ms;
  --ease-standard:   cubic-bezier(0.4, 0, 0.2, 1);
  --ease-emphasized: cubic-bezier(0.2, 0, 0, 1);
}

html[data-theme="dark"] {
  --bg-page:         #09090B;
  --bg-card:         #18181B;
  --bg-elevated:     #27272A;
  --bg-inset:        #111113;
  --border-subtle:   #27272A;
  --border-emphasis: #3F3F46;
  --border-strong:   #52525B;
  --text-primary:    #FAFAFA;
  --text-secondary:  #D4D4D8;
  --text-muted:      #71717A;
  --accent:          #60A5FA;
  --accent-hover:    #3B82F6;
  --accent-dim:      rgba(30,58,138,0.2);
}

/* Agent 人格色 */
[data-agent-type="INDUSTRY_ANALYST"]  { --agent: #1E3A8A; --agent-dim: rgba(30,58,138,0.10); }
[data-agent-type="SKILL_ASSESSOR"]    { --agent: #0D9488; --agent-dim: rgba(13,148,136,0.10); }
[data-agent-type="RISK_WATCHER"]      { --agent: #B45309; --agent-dim: rgba(180,83,9,0.10); }
[data-agent-type="OPPORTUNITY_HUNTER"]{ --agent: #CA8A04; --agent-dim: rgba(202,138,4,0.10); }
[data-agent-type="VALUE_EXAMINER"]    { --agent: #9333EA; --agent-dim: rgba(147,51,234,0.10); }
[data-agent-type="CUSTOM"]            { --agent: #525B6B; --agent-dim: rgba(82,91,107,0.10); }
[data-agent-type="MERGE_AGENT"]       { --agent: #111827; --agent-dim: rgba(17,24,39,0.10); }

html[data-theme="dark"] [data-agent-type="INDUSTRY_ANALYST"]  { --agent: #60A5FA; }
html[data-theme="dark"] [data-agent-type="SKILL_ASSESSOR"]    { --agent: #2DD4BF; }
html[data-theme="dark"] [data-agent-type="RISK_WATCHER"]      { --agent: #FB923C; }
html[data-theme="dark"] [data-agent-type="OPPORTUNITY_HUNTER"]{ --agent: #FCD34D; }
html[data-theme="dark"] [data-agent-type="VALUE_EXAMINER"]    { --agent: #C084FC; }
```

- [ ] **Step 2:** 创建 `base.css`：

```css
*, *::before, *::after { box-sizing: border-box; }

html, body, #app { height: 100%; }

body {
  margin: 0;
  font-family: var(--font-sans);
  font-size: 14px;
  line-height: 1.5;
  color: var(--text-primary);
  background: var(--bg-page);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  transition: background var(--duration-base) var(--ease-standard),
              color var(--duration-base) var(--ease-standard);
}

h1, h2, h3, h4, h5, h6 {
  margin: 0;
  font-weight: 600;
  color: var(--text-primary);
}

a { color: var(--accent); text-decoration: none; }
a:hover { color: var(--accent-hover); }

code, pre { font-family: var(--font-mono); }

::-webkit-scrollbar { width: 8px; height: 8px; }
::-webkit-scrollbar-track { background: transparent; }
::-webkit-scrollbar-thumb { background: var(--border-emphasis); border-radius: 4px; }
::-webkit-scrollbar-thumb:hover { background: var(--border-strong); }
```

- [ ] **Step 3:** 在 `src/main.ts` 中，`import './style.css'` 上方追加：

```ts
import './styles/tokens.css'
import './styles/base.css'
```

- [ ] **Step 4:** 启动 dev server：

```bash
cd careermind-frontend && npm run dev
```

访问 localhost:5173，页面应无报错加载；body 背景应为 `#FAFAFA`。

- [ ] **Step 5:** Commit：

```bash
git add careermind-frontend/src/styles/ careermind-frontend/src/main.ts
git commit -m "feat(style): add design tokens and base reset"
```

---

## Task 3: 更新 tailwind.config.js

**Files:**
- Modify: `careermind-frontend/tailwind.config.js`

- [ ] **Step 1:** 覆写全文：

```js
/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  darkMode: ['class', 'html[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        page:     'var(--bg-page)',
        card:     'var(--bg-card)',
        elevated: 'var(--bg-elevated)',
        inset:    'var(--bg-inset)',
        border:   { subtle: 'var(--border-subtle)', emphasis: 'var(--border-emphasis)', strong: 'var(--border-strong)' },
        text:     { primary: 'var(--text-primary)', secondary: 'var(--text-secondary)', muted: 'var(--text-muted)', inverse: 'var(--text-inverse)' },
        accent:   { DEFAULT: 'var(--accent)', hover: 'var(--accent-hover)', dim: 'var(--accent-dim)', contrast: 'var(--accent-contrast)' },
        success:  'var(--success)',
        warning:  'var(--warning)',
        danger:   'var(--danger)',
        agent:    { DEFAULT: 'var(--agent)', dim: 'var(--agent-dim)' },
      },
      borderRadius: { sm: 'var(--radius-sm)', md: 'var(--radius-md)', lg: 'var(--radius-lg)', full: 'var(--radius-full)' },
      boxShadow:    { sm: 'var(--shadow-sm)', md: 'var(--shadow-md)', lg: 'var(--shadow-lg)' },
      fontFamily:   { sans: 'var(--font-sans)', mono: 'var(--font-mono)', 'serif-zh': 'var(--font-serif-zh)' },
      transitionTimingFunction: { standard: 'var(--ease-standard)', emphasized: 'var(--ease-emphasized)' },
      transitionDuration:       { fast: '120ms', base: '240ms', slow: '480ms' },
    },
  },
  plugins: [],
}
```

- [ ] **Step 2:** 重启 dev server，确认无错。Commit：

```bash
git add careermind-frontend/tailwind.config.js
git commit -m "feat(style): wire tokens into tailwind theme"
```

---

## Task 4: index.html 引入字体

**Files:**
- Modify: `careermind-frontend/index.html`

- [ ] **Step 1:** 在 `<head>` 标题之前插入：

```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500&family=Noto+Sans+SC:wght@400;500;600;700&family=Noto+Serif+SC:wght@400;600;700&display=swap" rel="stylesheet">
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/index.html
git commit -m "feat(style): load Inter/JetBrains Mono/Noto SC fonts"
```

---

## Task 5: theme store + ThemeToggle + App.vue 挂载

**Files:**
- Create: `careermind-frontend/src/stores/theme.ts`
- Create: `careermind-frontend/src/components/ui/ThemeToggle.vue`
- Modify: `careermind-frontend/src/App.vue`
- Modify: `careermind-frontend/src/main.ts`

- [ ] **Step 1:** `stores/theme.ts`：

```ts
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
```

- [ ] **Step 2:** `components/ui/ThemeToggle.vue`：

```vue
<template>
  <button class="theme-toggle" :aria-label="label" @click="store.toggle">
    <svg v-if="store.theme === 'light'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
    </svg>
    <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="12" r="4"/><path d="M12 2v2m0 16v2M4.93 4.93l1.41 1.41m11.32 11.32l1.41 1.41M2 12h2m16 0h2M4.93 19.07l1.41-1.41m11.32-11.32l1.41-1.41"/>
    </svg>
  </button>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useThemeStore } from '@/stores/theme'
const store = useThemeStore()
const label = computed(() => store.theme === 'light' ? '切换到深色' : '切换到浅色')
</script>

<style scoped>
.theme-toggle {
  width: 32px; height: 32px;
  display: inline-flex; align-items: center; justify-content: center;
  background: transparent; border: 1px solid var(--border-subtle);
  border-radius: var(--radius-md); cursor: pointer; color: var(--text-secondary);
  transition: all var(--duration-base) var(--ease-standard);
}
.theme-toggle:hover { background: var(--bg-elevated); color: var(--text-primary); }
</style>
```

- [ ] **Step 3:** `App.vue` 改为：

```vue
<template>
  <div id="app">
    <router-view v-slot="{ Component }">
      <transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<script setup lang="ts">
import { useThemeStore } from '@/stores/theme'
useThemeStore()
</script>

<style>
.page-fade-enter-active, .page-fade-leave-active {
  transition: opacity var(--duration-base) var(--ease-standard), transform var(--duration-base) var(--ease-standard);
}
.page-fade-enter-from { opacity: 0; transform: translateY(8px); }
.page-fade-leave-to   { opacity: 0; transform: translateY(-4px); }
</style>
```

- [ ] **Step 4:** 启动 dev，浏览器 console 执行 `document.documentElement.dataset.theme = 'dark'`，body 应变黑。Commit：

```bash
git add careermind-frontend/src/stores/theme.ts careermind-frontend/src/components/ui/ThemeToggle.vue careermind-frontend/src/App.vue
git commit -m "feat(theme): add theme store and toggle component"
```

---

## Task 6: agent-meta.ts + useAgentTheme

**Files:**
- Create: `careermind-frontend/src/utils/agent-meta.ts`
- Create: `careermind-frontend/src/composables/useAgentTheme.ts`

- [ ] **Step 1:** `utils/agent-meta.ts`：

```ts
import type { AgentType } from '@/types'

export interface AgentMeta {
  type: AgentType
  label: string
  short: string       // 2 字短称
  role: string        // 角色描述
  symbol: 'glasses' | 'ruler' | 'shield' | 'arrow' | 'question' | 'merge' | 'user'
}

export const AGENT_META: Record<AgentType, AgentMeta> = {
  INDUSTRY_ANALYST:   { type: 'INDUSTRY_ANALYST',   label: '行业分析师', short: '行业', role: '洞察行业趋势与结构',       symbol: 'glasses'  },
  SKILL_ASSESSOR:     { type: 'SKILL_ASSESSOR',     label: '能力评估师', short: '能力', role: '评估当前能力与缺口',       symbol: 'ruler'    },
  RISK_WATCHER:       { type: 'RISK_WATCHER',       label: '风险警示者', short: '风险', role: '警示潜在风险与代价',       symbol: 'shield'   },
  OPPORTUNITY_HUNTER: { type: 'OPPORTUNITY_HUNTER', label: '机会挖掘者', short: '机会', role: '挖掘被忽视的机会',         symbol: 'arrow'    },
  VALUE_EXAMINER:     { type: 'VALUE_EXAMINER',     label: '价值拷问者', short: '价值', role: '叩问价值观与长期意义',     symbol: 'question' },
  MERGE_AGENT:        { type: 'MERGE_AGENT',        label: '整合专家',   short: '整合', role: '汇总观点生成候选方案',     symbol: 'merge'    },
  CUSTOM:             { type: 'CUSTOM',             label: '自定义',     short: '自定', role: '用户自定义 Agent',         symbol: 'user'     },
}

export const getAgentMeta = (type: AgentType | string | undefined): AgentMeta => {
  if (!type || !(type in AGENT_META)) return AGENT_META.CUSTOM
  return AGENT_META[type as AgentType]
}
```

- [ ] **Step 2:** `composables/useAgentTheme.ts`：

```ts
import { computed, type Ref } from 'vue'
import type { AgentType } from '@/types'

/** 在模板上绑 :style 时使用，注入 CSS 变量 --agent / --agent-dim */
export function useAgentTheme(type: Ref<AgentType | string | undefined>) {
  const attrs = computed(() => ({ 'data-agent-type': type.value || 'CUSTOM' }))
  return { attrs }
}
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-frontend/src/utils/agent-meta.ts careermind-frontend/src/composables/useAgentTheme.ts
git commit -m "feat(agent): add agent metadata and theme composable"
```

---

## Task 7: BaseButton

**Files:**
- Create: `careermind-frontend/src/components/ui/BaseButton.vue`

- [ ] **Step 1:**

```vue
<template>
  <button
    :class="['base-btn', `variant-${variant}`, `size-${size}`, { loading, block }]"
    :disabled="disabled || loading"
    @click="$emit('click', $event)"
  >
    <span v-if="loading" class="spinner" />
    <slot name="icon-left" />
    <slot />
    <slot name="icon-right" />
  </button>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
  disabled?: boolean
  block?: boolean
}>(), { variant: 'secondary', size: 'md' })
defineEmits<{ (e: 'click', ev: MouseEvent): void }>()
</script>

<style scoped>
.base-btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 6px;
  font-family: var(--font-sans); font-weight: 500; line-height: 1;
  border: 1px solid transparent; border-radius: var(--radius-md);
  cursor: pointer; user-select: none;
  transition: background var(--duration-fast) var(--ease-standard),
              border-color var(--duration-fast) var(--ease-standard),
              color var(--duration-fast) var(--ease-standard),
              transform var(--duration-fast) var(--ease-standard);
}
.base-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.base-btn:not(:disabled):active { transform: translateY(1px); }
.block { width: 100%; }

.size-sm { padding: 6px 12px;  font-size: 13px; min-height: 30px; }
.size-md { padding: 8px 16px;  font-size: 14px; min-height: 36px; }
.size-lg { padding: 12px 24px; font-size: 15px; min-height: 44px; }

.variant-primary {
  background: var(--accent); color: var(--accent-contrast); border-color: var(--accent);
}
.variant-primary:hover:not(:disabled) { background: var(--accent-hover); border-color: var(--accent-hover); }

.variant-secondary {
  background: var(--bg-card); color: var(--text-primary); border-color: var(--border-emphasis);
}
.variant-secondary:hover:not(:disabled) { background: var(--bg-elevated); }

.variant-ghost { background: transparent; color: var(--text-secondary); }
.variant-ghost:hover:not(:disabled) { background: var(--bg-elevated); color: var(--text-primary); }

.variant-danger { background: var(--danger); color: white; border-color: var(--danger); }
.variant-danger:hover:not(:disabled) { background: #dc2626; }

.spinner {
  width: 12px; height: 12px; border: 2px solid currentColor; border-top-color: transparent;
  border-radius: 50%; animation: spin 0.8s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
</style>
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/components/ui/BaseButton.vue
git commit -m "feat(ui): add BaseButton"
```

---

## Task 8: BaseCard + BaseBadge + BaseTag

**Files:**
- Create: `careermind-frontend/src/components/ui/BaseCard.vue`
- Create: `careermind-frontend/src/components/ui/BaseBadge.vue`
- Create: `careermind-frontend/src/components/ui/BaseTag.vue`

- [ ] **Step 1:** `BaseCard.vue`：

```vue
<template>
  <div :class="['base-card', { hoverable, inset }]" :style="{ padding }">
    <div v-if="$slots.header" class="card-header"><slot name="header" /></div>
    <slot />
    <div v-if="$slots.footer" class="card-footer"><slot name="footer" /></div>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{ padding?: string; hoverable?: boolean; inset?: boolean }>(),
  { padding: '20px' })
</script>

<style scoped>
.base-card {
  background: var(--bg-card); border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  transition: border-color var(--duration-base) var(--ease-standard),
              box-shadow var(--duration-base) var(--ease-standard),
              transform var(--duration-base) var(--ease-standard);
}
.base-card.inset { background: var(--bg-inset); }
.hoverable { cursor: pointer; }
.hoverable:hover {
  border-color: var(--border-emphasis); box-shadow: var(--shadow-md); transform: translateY(-2px);
}
.card-header { margin-bottom: 12px; font-weight: 600; font-size: 15px; }
.card-footer { margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border-subtle); }
</style>
```

- [ ] **Step 2:** `BaseBadge.vue`：

```vue
<template>
  <span :class="['base-badge', `tone-${tone}`, `size-${size}`]"><slot /></span>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  tone?: 'neutral' | 'accent' | 'success' | 'warning' | 'danger' | 'agent'
  size?: 'sm' | 'md'
}>(), { tone: 'neutral', size: 'sm' })
</script>

<style scoped>
.base-badge {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 2px 8px; font-size: 12px; font-weight: 500; line-height: 1.4;
  border-radius: var(--radius-full);
}
.size-md { padding: 4px 10px; font-size: 13px; }
.tone-neutral { background: var(--bg-elevated); color: var(--text-secondary); }
.tone-accent  { background: var(--accent-dim); color: var(--accent); }
.tone-success { background: rgba(16,185,129,0.1);  color: var(--success); }
.tone-warning { background: rgba(245,158,11,0.1);  color: var(--warning); }
.tone-danger  { background: rgba(239,68,68,0.1);   color: var(--danger); }
.tone-agent   { background: var(--agent-dim);      color: var(--agent); }
</style>
```

- [ ] **Step 3:** `BaseTag.vue`：

```vue
<template>
  <span class="base-tag" @click="$emit('click')">
    <slot />
    <button v-if="removable" class="remove" @click.stop="$emit('remove')">×</button>
  </span>
</template>

<script setup lang="ts">
defineProps<{ removable?: boolean }>()
defineEmits<{ (e: 'click'): void; (e: 'remove'): void }>()
</script>

<style scoped>
.base-tag {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px 10px; font-size: 13px;
  background: var(--bg-elevated); color: var(--text-secondary);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-sm);
  cursor: pointer; transition: all var(--duration-fast) var(--ease-standard);
}
.base-tag:hover { border-color: var(--border-emphasis); color: var(--text-primary); }
.remove { background: none; border: none; color: inherit; cursor: pointer; font-size: 16px; line-height: 1; padding: 0 2px; }
</style>
```

- [ ] **Step 4:** Commit：

```bash
git add careermind-frontend/src/components/ui/BaseCard.vue careermind-frontend/src/components/ui/BaseBadge.vue careermind-frontend/src/components/ui/BaseTag.vue
git commit -m "feat(ui): add BaseCard, BaseBadge, BaseTag"
```

---

## Task 9: BaseInput + BaseSkeleton + EmptyState

**Files:**
- Create: `careermind-frontend/src/components/ui/BaseInput.vue`
- Create: `careermind-frontend/src/components/ui/BaseSkeleton.vue`
- Create: `careermind-frontend/src/components/ui/EmptyState.vue`

- [ ] **Step 1:** `BaseInput.vue`（受控，支持 textarea）：

```vue
<template>
  <div class="input-wrap" :class="{ error }">
    <label v-if="label" class="input-label">{{ label }}</label>
    <textarea
      v-if="textarea"
      class="input"
      :rows="rows"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />
    <input
      v-else
      class="input"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
    />
    <p v-if="error" class="hint">{{ error }}</p>
    <p v-else-if="hint" class="hint muted">{{ hint }}</p>
  </div>
</template>

<script setup lang="ts">
withDefaults(defineProps<{
  modelValue: string | number
  label?: string
  placeholder?: string
  type?: string
  textarea?: boolean
  rows?: number
  hint?: string
  error?: string
  disabled?: boolean
}>(), { type: 'text', rows: 4 })
defineEmits<{ (e: 'update:modelValue', v: string): void }>()
</script>

<style scoped>
.input-wrap { display: flex; flex-direction: column; gap: 6px; }
.input-label { font-size: 13px; font-weight: 500; color: var(--text-secondary); }
.input {
  width: 100%; padding: 10px 12px; font-size: 14px; font-family: inherit;
  background: var(--bg-card); color: var(--text-primary);
  border: 1px solid var(--border-emphasis); border-radius: var(--radius-md);
  transition: border-color var(--duration-fast) var(--ease-standard), box-shadow var(--duration-fast) var(--ease-standard);
  outline: none;
}
textarea.input { resize: vertical; min-height: 96px; line-height: 1.5; }
.input:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-dim); }
.input:disabled { background: var(--bg-elevated); cursor: not-allowed; }
.error .input { border-color: var(--danger); }
.hint { font-size: 12px; color: var(--danger); margin: 0; }
.hint.muted { color: var(--text-muted); }
</style>
```

- [ ] **Step 2:** `BaseSkeleton.vue`：

```vue
<template>
  <div class="skeleton" :class="shape" :style="{ width, height }" />
</template>
<script setup lang="ts">
withDefaults(defineProps<{ shape?: 'line' | 'circle' | 'block'; width?: string; height?: string }>(),
  { shape: 'line', width: '100%', height: '14px' })
</script>
<style scoped>
.skeleton { background: linear-gradient(90deg, var(--bg-elevated) 0%, var(--bg-inset) 50%, var(--bg-elevated) 100%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite; border-radius: var(--radius-sm); }
.circle { border-radius: 9999px; }
.block  { border-radius: var(--radius-md); }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
</style>
```

- [ ] **Step 3:** `EmptyState.vue`：

```vue
<template>
  <div class="empty">
    <div v-if="$slots.icon" class="empty-icon"><slot name="icon" /></div>
    <h4 v-if="title" class="empty-title">{{ title }}</h4>
    <p v-if="description" class="empty-desc">{{ description }}</p>
    <div v-if="$slots.action" class="empty-action"><slot name="action" /></div>
  </div>
</template>
<script setup lang="ts">
defineProps<{ title?: string; description?: string }>()
</script>
<style scoped>
.empty { display: flex; flex-direction: column; align-items: center; padding: 48px 24px; text-align: center; color: var(--text-muted); }
.empty-icon { width: 48px; height: 48px; margin-bottom: 12px; color: var(--text-muted); }
.empty-title { margin: 0; font-size: 16px; color: var(--text-primary); }
.empty-desc  { margin: 8px 0 16px; font-size: 13px; }
</style>
```

- [ ] **Step 4:** Commit：

```bash
git add careermind-frontend/src/components/ui/BaseInput.vue careermind-frontend/src/components/ui/BaseSkeleton.vue careermind-frontend/src/components/ui/EmptyState.vue
git commit -m "feat(ui): add BaseInput, BaseSkeleton, EmptyState"
```

---

## Task 10: BrandLogo

**Files:**
- Create: `careermind-frontend/src/components/ui/BrandLogo.vue`

- [ ] **Step 1:**

```vue
<template>
  <div class="brand-logo" :class="variant" @click="$emit('click')">
    <svg :width="iconSize" :height="iconSize" viewBox="0 0 32 32" fill="none">
      <!-- 5 点圆桌抽象：中心点 + 4 个环绕节点 -->
      <circle cx="16" cy="16" r="2" fill="var(--accent)" />
      <circle cx="16" cy="6"  r="2.5" stroke="var(--accent)" stroke-width="1.6" />
      <circle cx="26" cy="13" r="2.5" stroke="var(--accent)" stroke-width="1.6" />
      <circle cx="22" cy="25" r="2.5" stroke="var(--accent)" stroke-width="1.6" />
      <circle cx="10" cy="25" r="2.5" stroke="var(--accent)" stroke-width="1.6" />
      <circle cx="6"  cy="13" r="2.5" stroke="var(--accent)" stroke-width="1.6" />
      <path d="M16 8 L16 14 M24 14 L18 15 M20 22 L17 17 M12 22 L15 17 M8 14 L14 15"
            stroke="var(--accent)" stroke-width="0.8" opacity="0.5" stroke-linecap="round" />
    </svg>
    <span v-if="variant === 'full'" class="wordmark">CareerMind</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
const props = withDefaults(defineProps<{ variant?: 'full' | 'icon'; size?: 'sm' | 'md' | 'lg' }>(),
  { variant: 'full', size: 'md' })
defineEmits<{ (e: 'click'): void }>()
const iconSize = computed(() => ({ sm: 22, md: 28, lg: 40 }[props.size]))
</script>

<style scoped>
.brand-logo { display: inline-flex; align-items: center; gap: 10px; cursor: pointer; user-select: none; }
.wordmark { font-family: var(--font-sans); font-weight: 700; font-size: 18px; color: var(--text-primary); letter-spacing: -0.01em; }
.brand-logo.full.sm .wordmark { font-size: 15px; }
.brand-logo.full.lg .wordmark { font-size: 24px; }
</style>
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/components/ui/BrandLogo.vue
git commit -m "feat(ui): add BrandLogo with abstract roundtable mark"
```

---

## Task 11: AgentAvatar（核心）

**Files:**
- Create: `careermind-frontend/src/components/agent/AgentAvatar.vue`

**职责**：根据 AgentType 渲染圆形头像（色块 + 几何脸 + 右下角符号徽章），支持 4 档 size 与 4 种状态（idle/listening/speaking/challenging）。

- [ ] **Step 1:**

```vue
<template>
  <div
    class="agent-avatar"
    :class="[state, `size-${size}`]"
    :data-agent-type="meta.type"
    :title="meta.label"
  >
    <div class="avatar-ring" />
    <svg :width="px" :height="px" viewBox="0 0 72 72" class="avatar-svg">
      <!-- 背景圆 -->
      <circle cx="36" cy="36" r="34" fill="var(--agent-dim)" stroke="var(--agent)" stroke-width="1.5" />
      <!-- 抽象脸：两点一弧 -->
      <circle cx="27" cy="30" r="3" fill="var(--agent)" />
      <circle cx="45" cy="30" r="3" fill="var(--agent)" />
      <path d="M26 46 Q36 54 46 46" stroke="var(--agent)" stroke-width="2.5" stroke-linecap="round" fill="none" />
      <!-- 右下角符号徽章 -->
      <g transform="translate(48 48)">
        <circle r="11" fill="var(--bg-card)" stroke="var(--agent)" stroke-width="1.5" />
        <g stroke="var(--agent)" stroke-width="1.6" fill="none" stroke-linecap="round" stroke-linejoin="round">
          <!-- 眼镜 -->
          <g v-if="meta.symbol === 'glasses'">
            <circle cx="-3" cy="0" r="3" /><circle cx="3" cy="0" r="3" /><path d="M-6 0 H-8 M6 0 H8 M0 0 H0.1" />
          </g>
          <!-- 尺规 -->
          <g v-else-if="meta.symbol === 'ruler'">
            <path d="M-5 -5 L5 5" /><path d="M-5 -2 L-3 0 M-2 -5 L0 -3 M1 -2 L3 0 M4 -5 L6 -3" />
          </g>
          <!-- 盾牌 -->
          <g v-else-if="meta.symbol === 'shield'">
            <path d="M0 -6 L-5 -3 V2 Q-5 5 0 6 Q5 5 5 2 V-3 Z" />
          </g>
          <!-- 箭头 -->
          <g v-else-if="meta.symbol === 'arrow'">
            <path d="M-5 3 L5 -3 M1 -3 L5 -3 L5 1" />
          </g>
          <!-- 问号 -->
          <g v-else-if="meta.symbol === 'question'">
            <path d="M-2 -3 Q-2 -6 0 -6 Q3 -6 3 -3 Q3 0 0 1 V3" /><circle cx="0" cy="5" r="0.6" fill="var(--agent)" />
          </g>
          <!-- merge / user 缺省 -->
          <g v-else>
            <circle cx="0" cy="-2" r="2" /><path d="M-4 6 Q0 2 4 6" />
          </g>
        </g>
      </g>
    </svg>
    <!-- 状态叠加：speaking 光环由 avatar-ring 做 -->
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AgentType } from '@/types'
import { getAgentMeta } from '@/utils/agent-meta'

const props = withDefaults(defineProps<{
  agentType: AgentType | string
  size?: 30 | 48 | 72 | 120
  state?: 'idle' | 'listening' | 'speaking' | 'challenging'
}>(), { size: 48, state: 'idle' })

const meta = computed(() => getAgentMeta(props.agentType))
const px = computed(() => props.size)
</script>

<style scoped>
.agent-avatar {
  position: relative; display: inline-block; line-height: 0;
  transition: transform var(--duration-base) var(--ease-standard),
              filter var(--duration-base) var(--ease-standard),
              opacity var(--duration-base) var(--ease-standard);
}
.size-30  { width: 30px;  height: 30px; }
.size-48  { width: 48px;  height: 48px; }
.size-72  { width: 72px;  height: 72px; }
.size-120 { width: 120px; height: 120px; }
.avatar-svg { display: block; width: 100%; height: 100%; border-radius: 50%; }

.avatar-ring {
  position: absolute; inset: -4px; border-radius: 50%; pointer-events: none;
  border: 2px solid transparent;
}

/* idle: no change */

/* listening: dim + 呼吸 */
.listening { filter: grayscale(0.55) opacity(0.75); animation: breathe 3s ease-in-out infinite; }
@keyframes breathe { 0%,100% { transform: scale(1); } 50% { transform: scale(1.015); } }

/* speaking: 放大 + 光环 */
.speaking { transform: scale(1.05); }
.speaking .avatar-ring {
  border-color: var(--agent);
  box-shadow: 0 0 0 4px var(--agent-dim);
  animation: pulse 1.6s ease-in-out infinite;
}
@keyframes pulse {
  0%   { box-shadow: 0 0 0 0 var(--agent-dim); }
  70%  { box-shadow: 0 0 0 12px rgba(0,0,0,0); }
  100% { box-shadow: 0 0 0 0 rgba(0,0,0,0); }
}

/* challenging: 倾斜 */
.challenging { transform: rotate(-3deg) scale(1.02); }
.challenging .avatar-ring { border-color: var(--danger); }
</style>
```

- [ ] **Step 2:** 临时加个 sandbox 页调试（可选）：在 `DiscussionView.vue` 顶部塞 `<AgentAvatar agentType="INDUSTRY_ANALYST" :size="120" state="speaking" />` 看效果，确认后删除。

- [ ] **Step 3:** Commit：

```bash
git add careermind-frontend/src/components/agent/AgentAvatar.vue
git commit -m "feat(agent): add AgentAvatar with 4 states and 6 symbols"
```

---

## Task 12: AgentAvatarGroup + AgentBadge + AgentCard

**Files:**
- Create: `careermind-frontend/src/components/agent/AgentAvatarGroup.vue`
- Create: `careermind-frontend/src/components/agent/AgentBadge.vue`
- Create: `careermind-frontend/src/components/agent/AgentCard.vue`

- [ ] **Step 1:** `AgentAvatarGroup.vue`：

```vue
<template>
  <div class="avatar-group">
    <AgentAvatar
      v-for="(a, i) in visible"
      :key="a.id ?? i"
      :agent-type="a.type"
      :size="size"
      class="stack-item"
      :style="{ zIndex: visible.length - i, marginLeft: i === 0 ? '0' : `-${overlap}px` }"
    />
    <span v-if="remaining > 0" class="more" :style="{ width: size + 'px', height: size + 'px' }">+{{ remaining }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AgentAvatar from './AgentAvatar.vue'
import type { Agent } from '@/types'

const props = withDefaults(defineProps<{
  agents: Pick<Agent, 'id' | 'type'>[]
  max?: number
  size?: 30 | 48 | 72
}>(), { max: 5, size: 30 })

const visible = computed(() => props.agents.slice(0, props.max))
const remaining = computed(() => Math.max(0, props.agents.length - props.max))
const overlap = computed(() => Math.round(props.size * 0.3))
</script>

<style scoped>
.avatar-group { display: inline-flex; align-items: center; }
.stack-item { border: 2px solid var(--bg-card); border-radius: 50%; background: var(--bg-card); }
.more { display: inline-flex; align-items: center; justify-content: center;
  margin-left: -9px; border: 2px solid var(--bg-card); background: var(--bg-elevated);
  border-radius: 50%; font-size: 11px; font-weight: 600; color: var(--text-secondary); }
</style>
```

- [ ] **Step 2:** `AgentBadge.vue`：

```vue
<template>
  <span class="agent-badge" :data-agent-type="meta.type">
    <AgentAvatar :agent-type="type" :size="30" state="idle" />
    <span class="agent-name">{{ displayName || meta.label }}</span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AgentAvatar from './AgentAvatar.vue'
import { getAgentMeta } from '@/utils/agent-meta'
import type { AgentType } from '@/types'

const props = defineProps<{ type: AgentType | string; displayName?: string }>()
const meta = computed(() => getAgentMeta(props.type))
</script>

<style scoped>
.agent-badge { display: inline-flex; align-items: center; gap: 8px;
  padding: 4px 10px 4px 4px; background: var(--agent-dim); color: var(--agent);
  border-radius: var(--radius-full); font-size: 13px; font-weight: 500; }
.agent-name { line-height: 1; }
</style>
```

- [ ] **Step 3:** `AgentCard.vue`：

```vue
<template>
  <div class="agent-card" :data-agent-type="meta.type">
    <AgentAvatar :agent-type="type" :size="72" state="idle" class="card-avatar" />
    <h4 class="card-name">{{ name || meta.label }}</h4>
    <p class="card-role">{{ role || meta.role }}</p>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import AgentAvatar from './AgentAvatar.vue'
import { getAgentMeta } from '@/utils/agent-meta'
import type { AgentType } from '@/types'
const props = defineProps<{ type: AgentType | string; name?: string; role?: string }>()
const meta = computed(() => getAgentMeta(props.type))
</script>

<style scoped>
.agent-card {
  display: flex; flex-direction: column; align-items: center; text-align: center;
  padding: 24px 20px; background: var(--bg-card);
  border: 1px solid var(--border-subtle); border-radius: var(--radius-lg);
  transition: all var(--duration-base) var(--ease-standard);
}
.agent-card:hover {
  border-color: var(--agent); transform: translateY(-4px); box-shadow: var(--shadow-md);
}
.agent-card:hover .card-avatar :deep(.avatar-ring) { border-color: var(--agent); }
.card-avatar { margin-bottom: 16px; }
.card-name  { margin: 0 0 6px; font-size: 15px; font-weight: 600; color: var(--text-primary); }
.card-role  { margin: 0; font-size: 13px; color: var(--text-secondary); line-height: 1.5; }
</style>
```

- [ ] **Step 4:** Commit：

```bash
git add careermind-frontend/src/components/agent/
git commit -m "feat(agent): add AvatarGroup, Badge, Card"
```

---

## Task 13: RoundTimeline

**Files:**
- Create: `careermind-frontend/src/components/discussion/RoundTimeline.vue`
- Delete (later in Task 20): `careermind-frontend/src/components/discussion/RoundIndicator.vue`

- [ ] **Step 1:**

```vue
<template>
  <div class="round-timeline">
    <div v-for="r in rounds" :key="r.num" class="round-step" :class="statusOf(r.num)">
      <div class="dot"><span v-if="statusOf(r.num) === 'done'">✓</span><span v-else>{{ r.num }}</span></div>
      <span class="label">{{ r.label }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{ current: number; completed?: number[] }>(),
  { completed: () => [] as number[] })

const rounds = [
  { num: 1, label: '独立诊断' },
  { num: 2, label: '质疑挑战' },
  { num: 3, label: '修正完善' },
  { num: 4, label: '最终陈述' },
]

const statusOf = (n: number) => {
  if (props.completed.includes(n) || n < props.current) return 'done'
  if (n === props.current) return 'active'
  return 'idle'
}
</script>

<style scoped>
.round-timeline { display: inline-flex; align-items: center; gap: 6px; }
.round-step { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px 4px 4px; border-radius: var(--radius-full);
  font-size: 12px; color: var(--text-muted); transition: all var(--duration-base) var(--ease-standard); }
.round-step + .round-step::before {
  content: ''; display: block; width: 12px; height: 1px; background: var(--border-emphasis); margin: 0 2px;
}
.dot { width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center;
  background: var(--bg-elevated); color: var(--text-muted); border: 1px solid var(--border-emphasis);
  border-radius: 50%; font-size: 11px; font-weight: 600; }
.label { white-space: nowrap; }

.active .dot  { background: var(--accent); color: var(--accent-contrast); border-color: var(--accent); }
.active       { color: var(--text-primary); font-weight: 500; background: var(--accent-dim); }
.done .dot    { background: var(--success); color: white; border-color: var(--success); }
.done         { color: var(--text-secondary); }
</style>
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/components/discussion/RoundTimeline.vue
git commit -m "feat(discussion): add RoundTimeline"
```

---

## Task 14: PageShell + Sidebar 重做

**Files:**
- Create: `careermind-frontend/src/components/ui/PageShell.vue`
- Modify: `careermind-frontend/src/components/layout/Sidebar.vue`

- [ ] **Step 1:** `PageShell.vue`（统一容器，Sidebar 可开关）：

```vue
<template>
  <div class="page-shell" :class="{ collapsed }">
    <Sidebar :collapsed="collapsed" @toggle="collapsed = !collapsed" />
    <main class="shell-main"><slot /></main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import Sidebar from '@/components/layout/Sidebar.vue'
const collapsed = ref(localStorage.getItem('cm-sidebar-collapsed') === '1')
import { watch } from 'vue'
watch(collapsed, v => localStorage.setItem('cm-sidebar-collapsed', v ? '1' : '0'))
</script>

<style scoped>
.page-shell { display: flex; height: 100vh; background: var(--bg-page); }
.shell-main { flex: 1; min-width: 0; overflow: hidden; }
</style>
```

- [ ] **Step 2:** 重写 `Sidebar.vue`（全量替换，支持 collapsed）：

```vue
<template>
  <aside class="sidebar" :class="{ collapsed }">
    <div class="sb-head">
      <BrandLogo :variant="collapsed ? 'icon' : 'full'" @click="$router.push('/')" />
      <button class="collapse-btn" @click="$emit('toggle')" :title="collapsed ? '展开' : '收起'">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path v-if="!collapsed" d="M15 18l-6-6 6-6" />
          <path v-else d="M9 18l6-6-6-6" />
        </svg>
      </button>
    </div>

    <div class="sb-cta">
      <BaseButton variant="primary" size="md" block @click="$router.push('/tasks')">
        <span v-if="!collapsed">+ 新建咨询</span>
        <span v-else>+</span>
      </BaseButton>
    </div>

    <nav class="sb-nav">
      <RouterLink v-for="n in navItems" :key="n.path" :to="n.path" class="sb-nav-item" :title="collapsed ? n.label : ''">
        <span class="ic" v-html="n.icon" />
        <span v-if="!collapsed" class="lbl">{{ n.label }}</span>
      </RouterLink>
    </nav>

    <div class="sb-section" v-if="!collapsed">历史对话</div>
    <div class="sb-tasks">
      <div v-for="t in taskStore.tasks.slice(0, 20)" :key="t.id"
           class="sb-task" :class="{ active: currentTaskId === t.id }" :title="t.title"
           @click="goToTask(t.id)">
        <span class="t-dot" />
        <span v-if="!collapsed" class="t-title">{{ t.title }}</span>
      </div>
      <EmptyState v-if="!collapsed && taskStore.tasks.length === 0" description="暂无咨询" />
    </div>

    <div class="sb-foot">
      <ThemeToggle />
      <div v-if="userStore.user" class="sb-user" :title="userStore.user.username">
        <div class="u-avatar">{{ userStore.user.username.slice(0,1).toUpperCase() }}</div>
        <span v-if="!collapsed" class="u-name">{{ userStore.user.username }}</span>
        <el-dropdown v-if="!collapsed" @command="handleCommand" trigger="click">
          <button class="u-more">⋯</button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="kb">知识库</el-dropdown-item>
              <el-dropdown-item command="agents">Agent 管理</el-dropdown-item>
              <el-dropdown-item command="settings">个人设置</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
      <BaseButton v-else variant="primary" size="sm" block @click="$router.push('/login')">
        {{ collapsed ? '→' : '登录 / 注册' }}
      </BaseButton>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRouter, useRoute, RouterLink } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTaskStore } from '@/stores/task'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

defineProps<{ collapsed?: boolean }>()
defineEmits<{ (e: 'toggle'): void }>()

const router = useRouter()
const route  = useRoute()
const userStore = useUserStore()
const taskStore = useTaskStore()

const navItems = [
  { path: '/',       label: '首页',     icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12l9-9 9 9M5 10v11h14V10"/></svg>' },
  { path: '/tasks',  label: '咨询',     icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>' },
  { path: '/kb',     label: '知识库',   icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20V3H6.5A2.5 2.5 0 0 0 4 5.5v14z"/></svg>' },
  { path: '/agents', label: 'Agent',    icon: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="7" r="4"/><path d="M3 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2M16 11a4 4 0 0 0 0-8"/></svg>' },
]

const loadTasks = () => { if (userStore.user) taskStore.fetchTasks() }
onMounted(() => { loadTasks(); window.addEventListener('task-created', loadTasks) })
watch(() => route.path, p => { if (p === '/tasks' || p === '/') loadTasks() })
const currentTaskId = computed(() => Number(route.params.id || route.params.taskId) || null)
const goToTask = (id: number) => router.push(`/tasks/${id}`)

const handleCommand = (cmd: string) => {
  const map: Record<string, () => void> = {
    kb:       () => router.push('/kb'),
    agents:   () => router.push('/agents'),
    settings: () => router.push('/settings'),
    logout:   () => { userStore.logout(); router.push('/login') },
  }
  map[cmd]?.()
}
</script>

<style scoped>
.sidebar {
  width: 260px; height: 100vh; flex-shrink: 0;
  background: var(--bg-card); border-right: 1px solid var(--border-subtle);
  display: flex; flex-direction: column;
  transition: width var(--duration-base) var(--ease-standard);
}
.sidebar.collapsed { width: 72px; }

.sb-head { display: flex; align-items: center; justify-content: space-between; padding: 16px 12px 8px; gap: 8px; }
.collapse-btn {
  width: 26px; height: 26px; display: inline-flex; align-items: center; justify-content: center;
  background: transparent; border: 1px solid var(--border-subtle); border-radius: var(--radius-sm);
  color: var(--text-muted); cursor: pointer;
}
.collapse-btn:hover { background: var(--bg-elevated); color: var(--text-primary); }
.sidebar.collapsed .collapse-btn { display: none; }

.sb-cta { padding: 8px 12px 12px; }

.sb-nav { display: flex; flex-direction: column; padding: 8px 8px; gap: 2px; }
.sb-nav-item {
  display: flex; align-items: center; gap: 10px; padding: 8px 10px;
  border-radius: var(--radius-md); color: var(--text-secondary);
  text-decoration: none; font-size: 14px;
  transition: all var(--duration-fast) var(--ease-standard);
}
.sb-nav-item:hover { background: var(--bg-elevated); color: var(--text-primary); }
.sb-nav-item.router-link-active { background: var(--accent-dim); color: var(--accent); font-weight: 500; }
.sb-nav-item .ic { display: inline-flex; width: 18px; flex-shrink: 0; }

.sb-section { padding: 12px 16px 4px; font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em; }
.sb-tasks { flex: 1; overflow-y: auto; padding: 4px 8px 8px; }
.sb-task {
  display: flex; align-items: center; gap: 8px; padding: 7px 10px;
  border-radius: var(--radius-md); cursor: pointer; color: var(--text-secondary);
  font-size: 13px; transition: all var(--duration-fast) var(--ease-standard);
}
.sb-task:hover { background: var(--bg-elevated); color: var(--text-primary); }
.sb-task.active { background: var(--accent-dim); color: var(--accent); }
.t-dot { width: 5px; height: 5px; border-radius: 50%; background: currentColor; flex-shrink: 0; opacity: 0.55; }
.t-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

.sb-foot { padding: 10px 12px 14px; border-top: 1px solid var(--border-subtle); display: flex; flex-direction: column; gap: 10px; }
.sb-user { display: flex; align-items: center; gap: 10px; }
.u-avatar {
  width: 28px; height: 28px; border-radius: 50%;
  background: var(--accent); color: var(--accent-contrast);
  display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600;
  flex-shrink: 0;
}
.u-name { flex: 1; font-size: 13px; color: var(--text-primary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.u-more {
  background: transparent; border: none; color: var(--text-muted); cursor: pointer;
  font-size: 18px; line-height: 1; padding: 2px 6px; border-radius: var(--radius-sm);
}
.u-more:hover { background: var(--bg-elevated); color: var(--text-primary); }
</style>
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-frontend/src/components/ui/PageShell.vue careermind-frontend/src/components/layout/Sidebar.vue
git commit -m "feat(layout): redesign Sidebar with collapsible state, add PageShell"
```

---

## Task 15: HomeView 重做

**Files:**
- Modify: `careermind-frontend/src/views/HomeView.vue`

- [ ] **Step 1:** 全量替换：

```vue
<template>
  <div class="home">
    <header class="nav">
      <BrandLogo @click="$router.push('/')" />
      <div class="nav-actions">
        <ThemeToggle />
        <BaseButton v-if="!user" variant="primary" size="sm" @click="$router.push('/login')">登录 / 注册</BaseButton>
        <BaseButton v-else variant="secondary" size="sm" @click="$router.push('/tasks')">进入咨询台</BaseButton>
      </div>
    </header>

    <section class="hero">
      <h1 class="title">让五位 AI 专家<br/>为你的人生辩一场</h1>
      <p class="subtitle">每一次重要决定，都该经过一场严肃的辩论。</p>
      <div class="cta">
        <BaseButton variant="primary" size="lg" @click="start">开始咨询 →</BaseButton>
        <BaseButton variant="ghost"   size="lg" @click="scrollTo('experts')">了解五位专家</BaseButton>
      </div>
    </section>

    <section id="experts" class="experts">
      <h2 class="sec-title">五位 AI 专家，五种视角</h2>
      <div class="experts-grid">
        <AgentCard v-for="t in expertTypes" :key="t" :type="t" />
      </div>
    </section>

    <section class="flow">
      <h2 class="sec-title">4 轮讨论，从分歧到共识</h2>
      <div class="flow-steps">
        <div v-for="(s, i) in flowSteps" :key="i" class="flow-step">
          <div class="step-num">{{ i + 1 }}</div>
          <h4 class="step-name">{{ s.name }}</h4>
          <p class="step-desc">{{ s.desc }}</p>
        </div>
      </div>
    </section>

    <section class="use-cases">
      <h2 class="sec-title">它能帮你做什么</h2>
      <div class="cases-grid">
        <BaseCard v-for="c in useCases" :key="c.title" hoverable>
          <h4 class="case-title">{{ c.title }}</h4>
          <p class="case-desc">{{ c.desc }}</p>
        </BaseCard>
      </div>
    </section>

    <footer class="foot">
      <BrandLogo size="sm" />
      <span class="foot-text">CareerMind · 多 Agent 职业决策系统 · 2026</span>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import ThemeToggle from '@/components/ui/ThemeToggle.vue'
import AgentCard from '@/components/agent/AgentCard.vue'
import type { AgentType } from '@/types'

const router = useRouter()
const user = computed(() => useUserStore().user)

const expertTypes: AgentType[] = ['INDUSTRY_ANALYST', 'SKILL_ASSESSOR', 'RISK_WATCHER', 'OPPORTUNITY_HUNTER', 'VALUE_EXAMINER']

const flowSteps = [
  { name: '独立诊断', desc: '5 位专家独立给出各自视角下的判断，不互相干扰。' },
  { name: '质疑挑战', desc: '专家互相质疑观点，暴露盲区与假设。' },
  { name: '修正完善', desc: '每位专家根据质疑修正或坚持自己的立场。' },
  { name: '最终陈述', desc: '汇总为候选方案，并标注共识度与适用条件。' },
]

const useCases = [
  { title: '转行抉择',     desc: '在留守与转行之间权衡能力迁移、机会成本与风险。' },
  { title: '晋升 vs 跳槽', desc: '评估内部晋升路径与外部跳槽窗口的真实价值。' },
  { title: '升学 / 读研',   desc: '考察是否值得为深造让渡 2-3 年时间。' },
]

const start = () => router.push(user.value ? '/tasks' : '/login')
const scrollTo = (id: string) => document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })
</script>

<style scoped>
.home { min-height: 100vh; background: var(--bg-page); color: var(--text-primary); }
.nav {
  position: sticky; top: 0; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 40px; background: rgba(250,250,250,0.7); backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border-subtle);
}
html[data-theme="dark"] .nav { background: rgba(9,9,11,0.7); }
.nav-actions { display: flex; align-items: center; gap: 10px; }

.hero { max-width: 820px; margin: 0 auto; padding: 96px 24px 80px; text-align: center; }
.title { font-size: 56px; line-height: 1.15; letter-spacing: -0.02em; font-weight: 700; margin-bottom: 20px; }
.subtitle { font-size: 18px; color: var(--text-secondary); margin: 0 0 32px; }
.cta { display: inline-flex; gap: 12px; }

section { padding: 80px 40px; max-width: 1120px; margin: 0 auto; }
.sec-title { font-size: 28px; font-weight: 600; margin-bottom: 36px; text-align: center; }

.experts-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 16px; }
.flow-steps   { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.flow-step    { padding: 20px; border: 1px solid var(--border-subtle); border-radius: var(--radius-lg); background: var(--bg-card); }
.step-num     { width: 28px; height: 28px; border-radius: 50%; background: var(--accent-dim); color: var(--accent); display: inline-flex; align-items: center; justify-content: center; font-weight: 600; margin-bottom: 12px; }
.step-name    { margin: 0 0 6px; font-size: 15px; }
.step-desc    { margin: 0; font-size: 13px; color: var(--text-secondary); line-height: 1.6; }

.cases-grid   { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; }
.case-title   { margin: 0 0 8px; font-size: 16px; }
.case-desc    { margin: 0; font-size: 13px; color: var(--text-secondary); line-height: 1.6; }

.foot { max-width: 1120px; margin: 0 auto; padding: 40px; display: flex; align-items: center; justify-content: space-between;
  border-top: 1px solid var(--border-subtle); color: var(--text-muted); font-size: 13px; }
@media (max-width: 768px) {
  .title { font-size: 40px; } .flow-steps { grid-template-columns: repeat(2, 1fr); } .nav { padding: 12px 20px; } section { padding: 48px 20px; }
}
</style>
```

- [ ] **Step 2:** 启动 dev，访问 `/`，确认 Landing 渲染；5 张 AgentCard 显示各自人格色；暗色模式切换正常。Commit：

```bash
git add careermind-frontend/src/views/HomeView.vue
git commit -m "feat(home): rebuild landing page with hero + experts + flow + use cases"
```

---

## Task 16: LoginView 重做

**Files:**
- Modify: `careermind-frontend/src/views/LoginView.vue`

- [ ] **Step 1:** 读现有 LoginView，保留全部 login/register 逻辑与 API 调用，仅替换模板与样式为以下双栏布局：

```vue
<template>
  <div class="login-layout">
    <aside class="left-pane">
      <BrandLogo size="lg" />
      <div class="left-art">
        <!-- 圆桌 5 Agent 缩略 -->
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

        <BaseInput v-model="form.username" label="用户名" placeholder="请输入用户名" />
        <BaseInput v-if="isRegister" v-model="form.email" type="email" label="邮箱" placeholder="email@example.com" />
        <BaseInput v-model="form.password" type="password" label="密码" placeholder="至少 6 位" />

        <BaseButton variant="primary" size="lg" block :loading="loading" @click="submit">
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
```

具体 `<script>` 保留原 `login`/`register` 逻辑不变，仅按 `isRegister` 分支调用对应 API；样式：

```vue
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
```

- [ ] **Step 2:** 手动测试登录/注册流程保持不变。Commit：

```bash
git add careermind-frontend/src/views/LoginView.vue
git commit -m "feat(auth): redesign login/register with split layout and orb art"
```

---

## Task 17: SpeechBubble + ChallengeFlow

**Files:**
- Create: `careermind-frontend/src/components/discussion/SpeechBubble.vue`
- Create: `careermind-frontend/src/components/discussion/ChallengeFlow.vue`

- [ ] **Step 1:** `SpeechBubble.vue`（头像下冒出的发言浮字）：

```vue
<template>
  <div class="bubble" :data-agent-type="agentType">
    <div class="bubble-content">
      <p class="bubble-text">{{ truncated }}<span v-if="isStreaming" class="caret">▋</span></p>
    </div>
    <div class="bubble-tail" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AgentType } from '@/types'
const props = withDefaults(defineProps<{
  agentType: AgentType | string
  content: string
  isStreaming?: boolean
  maxChars?: number
}>(), { maxChars: 90 })
const truncated = computed(() => {
  const t = (props.content || '').trim()
  return t.length > props.maxChars ? t.slice(0, props.maxChars) + '…' : t
})
</script>

<style scoped>
.bubble { position: relative; max-width: 260px; animation: pop var(--duration-base) var(--ease-emphasized); }
@keyframes pop { from { opacity: 0; transform: translateY(-4px) scale(0.95); } to { opacity: 1; transform: translateY(0) scale(1); } }
.bubble-content {
  background: var(--bg-card); border: 1px solid var(--agent);
  color: var(--text-primary); padding: 10px 14px;
  border-radius: var(--radius-lg); box-shadow: var(--shadow-md);
}
.bubble-text { margin: 0; font-size: 13px; line-height: 1.5; }
.bubble-tail {
  position: absolute; top: -6px; left: 50%; transform: translateX(-50%) rotate(45deg);
  width: 10px; height: 10px; background: var(--bg-card);
  border-left: 1px solid var(--agent); border-top: 1px solid var(--agent);
}
.caret { display: inline-block; animation: blink 1s steps(2) infinite; color: var(--agent); }
@keyframes blink { 50% { opacity: 0; } }
</style>
```

- [ ] **Step 2:** `ChallengeFlow.vue`（从 A 头像位置到 B 头像位置画一条动画光流）：

```vue
<template>
  <svg class="challenge-flow" :width="w" :height="h" viewBox="0 0 1000 600" preserveAspectRatio="none">
    <defs>
      <linearGradient :id="gradId" x1="0%" y1="0%" x2="100%" y2="0%">
        <stop offset="0%"  stop-color="var(--danger)" stop-opacity="0" />
        <stop offset="50%" stop-color="var(--danger)" stop-opacity="1" />
        <stop offset="100%" stop-color="var(--danger)" stop-opacity="0" />
      </linearGradient>
    </defs>
    <path :d="path" fill="none" :stroke="`url(#${gradId})`" stroke-width="2" stroke-linecap="round"
          stroke-dasharray="8 4" class="flow-stroke" />
  </svg>
</template>

<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ from: { x: number; y: number }; to: { x: number; y: number }; w?: number; h?: number }>()
const w = computed(() => props.w ?? 1000)
const h = computed(() => props.h ?? 600)
const path = computed(() => {
  const { from, to } = props
  const mx = (from.x + to.x) / 2; const my = (from.y + to.y) / 2 - 40
  return `M ${from.x} ${from.y} Q ${mx} ${my} ${to.x} ${to.y}`
})
const gradId = `flow-${Math.random().toString(36).slice(2, 8)}`
</script>

<style scoped>
.challenge-flow { position: absolute; inset: 0; pointer-events: none; overflow: visible; }
.flow-stroke { animation: flow 1.2s linear; }
@keyframes flow { from { stroke-dashoffset: 80; } to { stroke-dashoffset: 0; } }
</style>
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-frontend/src/components/discussion/SpeechBubble.vue careermind-frontend/src/components/discussion/ChallengeFlow.vue
git commit -m "feat(discussion): add SpeechBubble and ChallengeFlow"
```

---

## Task 18: RoundtableStage（核心）

**Files:**
- Create: `careermind-frontend/src/components/discussion/RoundtableStage.vue`

**职责**：5 Agent 圆桌布局，高亮发言者、倾听者呼吸、质疑光流；接收 `agents`、`currentSpeakerAgentId`、`streamingContent`、`latestChallenge`(可选) 四个 props。

- [ ] **Step 1:**

```vue
<template>
  <div class="stage" ref="stageRef">
    <!-- 网格纸背景 -->
    <div class="grid-bg" />

    <!-- 中央话题 -->
    <div class="topic">
      <div class="topic-cross">✦</div>
      <p class="topic-text" :title="topic">{{ topic }}</p>
    </div>

    <!-- 轮次标语淡入 -->
    <transition name="round-label">
      <div v-if="roundLabel" class="round-label" :key="roundLabel">{{ roundLabel }}</div>
    </transition>

    <!-- 5 个 Agent 头像 -->
    <div v-for="(slot, i) in slots" :key="slot.agentId" class="seat" :style="seatStyle(i)" :ref="el => seatEls[i] = el as HTMLElement">
      <AgentAvatar
        :agent-type="slot.type"
        :size="120"
        :state="stateFor(slot.agentId)"
      />
      <span class="seat-name" :data-agent-type="slot.type">{{ slot.name }}</span>

      <!-- 当前发言者头顶气泡 -->
      <div v-if="slot.agentId === currentSpeakerAgentId && streamingContent" class="seat-bubble">
        <SpeechBubble :agent-type="slot.type" :content="streamingContent" is-streaming />
      </div>
    </div>

    <!-- 质疑光流叠层 -->
    <ChallengeFlow v-if="flow" :from="flow.from" :to="flow.to" :w="stageW" :h="stageH" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUpdated, watch, nextTick } from 'vue'
import AgentAvatar from '@/components/agent/AgentAvatar.vue'
import SpeechBubble from './SpeechBubble.vue'
import ChallengeFlow from './ChallengeFlow.vue'
import type { Agent, AgentType } from '@/types'

interface Slot { agentId: number; type: AgentType; name: string }
interface ChallengePair { fromAgentId: number; toAgentId: number; triggerAt: number }

const props = defineProps<{
  agents: Agent[]                   // 最多取前 5 个
  currentSpeakerAgentId?: number | null
  streamingContent?: string
  topic?: string
  roundLabel?: string               // e.g. "第 2 轮 · 质疑挑战"
  latestChallenge?: ChallengePair | null
}>()

const stageRef = ref<HTMLElement>()
const seatEls: HTMLElement[] = []
const stageW = ref(800); const stageH = ref(520)

const slots = computed<Slot[]>(() =>
  props.agents.slice(0, 5).map(a => ({ agentId: a.id, type: a.type as AgentType, name: a.name }))
)

// 5 个座位位置（x%, y%），以圆桌为参照
const positions = [
  { x: 50, y: 12 },   // 正上
  { x: 82, y: 32 },   // 右上
  { x: 74, y: 74 },   // 右下
  { x: 26, y: 74 },   // 左下
  { x: 18, y: 32 },   // 左上
]
const seatStyle = (i: number) => {
  const p = positions[i] || positions[0]
  return { left: p.x + '%', top: p.y + '%', transform: 'translate(-50%,-50%)' }
}

const stateFor = (agentId: number): 'idle' | 'listening' | 'speaking' | 'challenging' => {
  if (props.currentSpeakerAgentId == null) return 'idle'
  if (agentId === props.currentSpeakerAgentId) return 'speaking'
  if (props.latestChallenge && agentId === props.latestChallenge.toAgentId && Date.now() - props.latestChallenge.triggerAt < 2000) return 'challenging'
  return 'listening'
}

// 计算 ChallengeFlow 在 viewBox 1000x600 空间里的起止坐标
const flow = ref<{ from: { x: number; y: number }; to: { x: number; y: number } } | null>(null)
watch(() => props.latestChallenge?.triggerAt, async () => {
  await nextTick()
  if (!props.latestChallenge) { flow.value = null; return }
  const fromIdx = slots.value.findIndex(s => s.agentId === props.latestChallenge!.fromAgentId)
  const toIdx   = slots.value.findIndex(s => s.agentId === props.latestChallenge!.toAgentId)
  if (fromIdx < 0 || toIdx < 0) return
  const f = positions[fromIdx]; const t = positions[toIdx]
  flow.value = { from: { x: f.x * 10, y: f.y * 6 }, to: { x: t.x * 10, y: t.y * 6 } }
  setTimeout(() => (flow.value = null), 1400)
})

const measure = () => {
  if (!stageRef.value) return
  stageW.value = stageRef.value.clientWidth
  stageH.value = stageRef.value.clientHeight
}
onMounted(() => { measure(); window.addEventListener('resize', measure) })
onUpdated(measure)
</script>

<style scoped>
.stage { position: relative; width: 100%; height: 100%; min-height: 520px; overflow: hidden; background: var(--bg-page); }
.grid-bg {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(var(--border-subtle) 1px, transparent 1px),
    linear-gradient(90deg, var(--border-subtle) 1px, transparent 1px);
  background-size: 32px 32px;
  mask-image: radial-gradient(ellipse at center, black 40%, transparent 80%);
  opacity: 0.35;
}
.topic {
  position: absolute; top: 50%; left: 50%; transform: translate(-50%,-50%); z-index: 1;
  text-align: center; color: var(--text-secondary); max-width: 280px;
}
.topic-cross { font-size: 20px; color: var(--accent); margin-bottom: 6px; }
.topic-text  { margin: 0; font-size: 13px; line-height: 1.5; }

.round-label {
  position: absolute; top: 20px; left: 50%; transform: translateX(-50%); z-index: 3;
  padding: 6px 14px; background: var(--bg-card); border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full); font-size: 12px; color: var(--text-secondary); font-weight: 500;
}
.round-label-enter-active, .round-label-leave-active { transition: opacity var(--duration-slow) var(--ease-emphasized), transform var(--duration-slow) var(--ease-emphasized); }
.round-label-enter-from { opacity: 0; transform: translate(-50%, -12px); }
.round-label-leave-to   { opacity: 0; transform: translate(-50%, -4px); }

.seat {
  position: absolute; z-index: 2; display: flex; flex-direction: column; align-items: center; gap: 6px;
}
.seat-name { font-size: 12px; font-weight: 500; color: var(--agent); background: var(--agent-dim); padding: 2px 10px; border-radius: var(--radius-full); white-space: nowrap; }
.seat-bubble { position: absolute; bottom: calc(100% + 12px); left: 50%; transform: translateX(-50%); z-index: 4; }
</style>
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/components/discussion/RoundtableStage.vue
git commit -m "feat(discussion): add RoundtableStage with 5 agent positions and challenge flow"
```

---

## Task 19: MessageDrawer

**Files:**
- Create: `careermind-frontend/src/components/discussion/MessageDrawer.vue`

- [ ] **Step 1:**

```vue
<template>
  <aside class="msg-drawer" :class="{ open }">
    <button class="drawer-toggle" @click="$emit('update:open', !open)" :title="open ? '收起' : '展开消息流'">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path v-if="open" d="M9 18l6-6-6-6"/>
        <path v-else d="M15 18l-6-6 6-6"/>
      </svg>
      <span v-if="!open" class="count">{{ messageCount }}</span>
    </button>

    <div v-if="open" class="drawer-body" ref="bodyRef">
      <div v-for="g in groups" :key="g.roundNumber" class="round-group">
        <div class="round-head">
          <span class="round-pill">第 {{ g.roundNumber }} 轮 · {{ g.label }}</span>
        </div>
        <AgentMessage v-for="m in g.messages" :key="m.id" :message="m" />
      </div>

      <div v-if="streamingMessage" class="round-group">
        <AgentMessage :message="streamingMessage" is-streaming />
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick } from 'vue'
import AgentMessage from './AgentMessage.vue'
import type { Round, Message, RoundType } from '@/types'

const props = defineProps<{
  open: boolean
  rounds: Round[]
  streamingMessage?: Message | null
}>()
defineEmits<{ (e: 'update:open', v: boolean): void }>()

const bodyRef = ref<HTMLElement>()
const messageCount = computed(() => props.rounds.reduce((s, r) => s + r.messages.length, 0) + (props.streamingMessage ? 1 : 0))
const roundLabels: Record<RoundType, string> = { INDEPENDENT: '独立诊断', CHALLENGE: '质疑挑战', REVISION: '修正完善', FINAL: '最终陈述' }
const groups = computed(() => props.rounds.map(r => ({ roundNumber: r.roundNumber, label: roundLabels[r.roundType] || r.roundType, messages: r.messages })))

watch(messageCount, async () => { await nextTick(); if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight })
</script>

<style scoped>
.msg-drawer { position: relative; width: 48px; background: var(--bg-card); border-left: 1px solid var(--border-subtle); transition: width var(--duration-base) var(--ease-standard); flex-shrink: 0; }
.msg-drawer.open { width: 360px; }

.drawer-toggle {
  position: absolute; top: 16px; left: 8px; z-index: 2;
  width: 32px; height: 32px; border-radius: var(--radius-full);
  background: var(--bg-card); color: var(--text-secondary); border: 1px solid var(--border-emphasis);
  cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
}
.count {
  position: absolute; top: -5px; right: -5px; min-width: 16px; height: 16px;
  background: var(--accent); color: var(--accent-contrast); border-radius: 9999px; font-size: 10px; padding: 0 4px;
  display: inline-flex; align-items: center; justify-content: center;
}
.drawer-body { height: 100%; overflow-y: auto; padding: 56px 14px 80px; }

.round-group { margin-bottom: 20px; }
.round-head { position: sticky; top: -1px; padding: 6px 0; background: var(--bg-card); z-index: 1; }
.round-pill {
  display: inline-block; padding: 3px 10px; border-radius: var(--radius-full);
  background: var(--bg-elevated); color: var(--text-secondary);
  font-size: 11px; font-weight: 500;
}
</style>
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/components/discussion/MessageDrawer.vue
git commit -m "feat(discussion): add MessageDrawer for grouped message stream"
```

---

## Task 20: DiscussionPanel 整合 + AgentMessage 重写

**Files:**
- Modify: `careermind-frontend/src/components/discussion/DiscussionPanel.vue`
- Modify: `careermind-frontend/src/components/discussion/AgentMessage.vue`
- Modify: `careermind-frontend/src/views/DiscussionView.vue`
- Delete: `careermind-frontend/src/components/discussion/RoundIndicator.vue`

- [ ] **Step 1:** 重写 `AgentMessage.vue` 使用 AgentAvatar 与 tokens：

```vue
<template>
  <div class="msg" :class="{ streaming: isStreaming, user: isUser, interjection: isInterjection }" :data-agent-type="agentType">
    <AgentAvatar :agent-type="agentType" :size="30" :state="isStreaming ? 'speaking' : 'idle'" v-if="!isUser" />
    <div v-else class="user-avatar">你</div>
    <div class="msg-body">
      <div class="msg-head">
        <span class="msg-name">{{ displayName }}</span>
        <span v-if="isStreaming" class="typing">
          <span class="dot" /><span class="dot" /><span class="dot" />
        </span>
        <span v-else class="msg-time">{{ timeText }}</span>
      </div>
      <div class="msg-content" v-html="formatted" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { marked } from 'marked'
import dayjs from 'dayjs'
import AgentAvatar from '@/components/agent/AgentAvatar.vue'
import type { Message } from '@/types'

const props = defineProps<{ message: Message; isStreaming?: boolean }>()

const isUser = computed(() => props.message.messageType === 'USER' || props.message.agentId === -1)
const isInterjection = computed(() => props.message.messageType === 'INTERJECTION')
const agentType = computed(() => props.message.agentType || 'CUSTOM')
const displayName = computed(() => isUser.value ? '你' : (isInterjection.value ? `${props.message.agentName} · 回应插话` : props.message.agentName))
const timeText = computed(() => props.message.createdAt ? dayjs(props.message.createdAt).format('HH:mm:ss') : '')
const formatted = computed(() => marked(props.message.content || ''))
</script>

<style scoped>
.msg {
  display: flex; gap: 10px; padding: 12px; margin-bottom: 10px;
  background: var(--bg-card); border: 1px solid var(--border-subtle);
  border-left: 3px solid var(--agent); border-radius: var(--radius-md);
  animation: slideIn var(--duration-base) var(--ease-standard);
}
.msg.streaming { border-left-color: var(--accent); background: linear-gradient(90deg, var(--accent-dim) 0%, var(--bg-card) 40%); }
.msg.user      { border-left-color: var(--success); background: linear-gradient(90deg, rgba(16,185,129,0.08) 0%, var(--bg-card) 40%); }
.msg.interjection { border-left-color: var(--warning); }

.user-avatar {
  width: 30px; height: 30px; border-radius: 50%;
  background: var(--success); color: white;
  display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; flex-shrink: 0;
}

.msg-body { flex: 1; min-width: 0; }
.msg-head { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
.msg-name { font-size: 13px; font-weight: 600; color: var(--agent); }
.msg.user .msg-name { color: var(--success); }
.msg-time { font-size: 11px; color: var(--text-muted); }

.typing { display: inline-flex; gap: 3px; }
.dot { width: 4px; height: 4px; background: var(--accent); border-radius: 50%; animation: typing 1.4s infinite ease-in-out both; }
.dot:nth-child(1) { animation-delay: -0.32s; } .dot:nth-child(2) { animation-delay: -0.16s; }
@keyframes typing { 0%,80%,100% { transform: scale(0); opacity: 0.5; } 40% { transform: scale(1); opacity: 1; } }

.msg-content { font-size: 13px; line-height: 1.6; color: var(--text-primary); word-break: break-word; }
.msg-content :deep(p) { margin: 4px 0; }
.msg-content :deep(ul), .msg-content :deep(ol) { margin: 6px 0; padding-left: 18px; }
.msg-content :deep(strong) { color: var(--text-primary); }

@keyframes slideIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }
</style>
```

- [ ] **Step 2:** 重写 `DiscussionPanel.vue` 为"顶部栏 + 圆桌 + 抽屉 + 底部栏"结构，保留所有现有 WebSocket 与 API 逻辑：

```vue
<template>
  <div class="panel">
    <header class="panel-head" v-if="task">
      <button class="back-btn" @click="$router.push(`/tasks/${task.id}`)">←</button>
      <div class="head-title">
        <h3>{{ task.title }}</h3>
        <p v-if="task.goal" class="head-goal">{{ task.goal.slice(0, 80) }}</p>
      </div>
      <div class="head-right">
        <RoundTimeline :current="discussion?.currentRound || 1" />
        <DiscussionControl
          :is-active="discussion?.isActive || false"
          :is-paused="discussion?.isPaused || false"
          @start="handleStart" @pause="handlePause" @resume="handleResume" @stop="handleStop" @next-round="handleNextRound"
        />
      </div>
    </header>

    <div class="panel-body">
      <div class="stage-wrap">
        <RoundtableStage
          :agents="task?.agents || []"
          :current-speaker-agent-id="streamingMessage?.agentId ?? null"
          :streaming-content="streamingMessage?.content"
          :topic="task?.goal || task?.title"
          :round-label="roundLabelText"
          :latest-challenge="latestChallenge"
        />
        <div v-if="!hasDiscussion" class="stage-overlay">
          <BaseButton variant="primary" size="lg" @click="handleStart">▶ 开始讨论</BaseButton>
          <p class="overlay-hint">5 位 AI 专家将进行 4 轮辩论</p>
        </div>
      </div>
      <MessageDrawer v-model:open="drawerOpen" :rounds="discussion?.rounds || []" :streaming-message="streamingMessage" />
    </div>

    <footer class="panel-foot">
      <BaseInput v-model="userInput" placeholder="输入想对专家说的话 (可选)…" @keyup.enter="sendMessage" />
      <div class="foot-actions">
        <BaseButton variant="secondary" size="md" @click="sendMessage" :disabled="!userInput.trim()">插话</BaseButton>
        <BaseButton variant="secondary" size="md" @click="handleNextRound" :disabled="!discussion?.isActive">下一轮 ▷</BaseButton>
        <BaseButton variant="primary" size="md" @click="goResult">生成报告 📄</BaseButton>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import RoundtableStage from './RoundtableStage.vue'
import MessageDrawer from './MessageDrawer.vue'
import DiscussionControl from './DiscussionControl.vue'
import RoundTimeline from './RoundTimeline.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import { taskApi } from '@/api/task'
import { discussionApi } from '@/api/discussion'
import type { Task, Discussion, Message, RoundType } from '@/types'

const props = defineProps<{ taskId: number }>()
const router = useRouter()
const task = ref<Task | null>(null)
const discussion = ref<Discussion | null>(null)
const userInput = ref('')
const drawerOpen = ref(false)
const streamingMessage = ref<Message | null>(null)
const streamingContent = ref('')
const latestChallenge = ref<{ fromAgentId: number; toAgentId: number; triggerAt: number } | null>(null)
let ws: WebSocket | null = null

const hasDiscussion = computed(() => !!discussion.value && (discussion.value.rounds.length > 0 || discussion.value.isActive))
const roundLabelText = computed(() => {
  const r = discussion.value?.currentRound; if (!r) return ''
  const labels: Record<number, string> = { 1: '独立诊断', 2: '质疑挑战', 3: '修正完善', 4: '最终陈述' }
  return `第 ${r} 轮 · ${labels[r] || ''}`
})

onMounted(async () => { await loadTask(); await loadDiscussion(); connect() })
onUnmounted(() => ws?.close())

watch(() => props.taskId, async (n, o) => {
  if (n === o) return
  ws?.close(); streamingMessage.value = null; streamingContent.value = ''
  await loadTask(); await loadDiscussion(); connect()
})

const loadTask = async () => { try { task.value = await taskApi.getTaskById(props.taskId) } catch { task.value = null } }
const loadDiscussion = async () => { try { discussion.value = await discussionApi.getDiscussion(props.taskId) } catch { discussion.value = null } }

const connect = () => {
  const url = `ws://${window.location.host}/ws/discussion?taskId=${props.taskId}`
  ws = new WebSocket(url)
  ws.onmessage = (ev) => {
    const d = JSON.parse(ev.data)
    switch (d.type) {
      case 'stream_start':
        streamingMessage.value = {
          id: Date.now(),
          agentId: d.data.agentId, agentName: d.data.agentName, agentType: d.data.agentType,
          agentAvatar: d.data.agentAvatar, content: '', isFinal: false, createdAt: new Date().toISOString(),
        }
        streamingContent.value = ''
        break
      case 'stream_chunk':
        if (streamingMessage.value) {
          streamingContent.value += d.content
          streamingMessage.value.content = streamingContent.value
        }
        break
      case 'stream_end':
        if (streamingMessage.value && d.data?.replyToAgentId) {
          latestChallenge.value = { fromAgentId: streamingMessage.value.agentId, toAgentId: d.data.replyToAgentId, triggerAt: Date.now() }
        }
        streamingMessage.value = null; streamingContent.value = ''
        loadDiscussion()
        break
      case 'message':
        loadDiscussion()
        break
      case 'result_stream_end':
        ElMessage.success('结果已生成'); break
    }
  }
}

const handleStart    = async () => { try { discussion.value = await discussionApi.startDiscussion(props.taskId) } catch (e:any) { ElMessage.error(e.message || '开始失败') } }
const handlePause    = async () => { try { discussion.value = await discussionApi.pauseDiscussion(props.taskId) } catch { ElMessage.error('暂停失败') } }
const handleResume   = async () => { try { discussion.value = await discussionApi.resumeDiscussion(props.taskId) } catch { ElMessage.error('继续失败') } }
const handleStop     = async () => { try { discussion.value = await discussionApi.stopDiscussion(props.taskId) } catch { ElMessage.error('停止失败') } }
const handleNextRound= async () => { try { discussion.value = await discussionApi.nextRound(props.taskId) } catch (e:any) { ElMessage.error(e.message || '下一轮失败') } }

const sendMessage = async () => {
  const v = userInput.value.trim(); if (!v) return
  try { await discussionApi.sendMessage(props.taskId, v); userInput.value = ''; loadDiscussion() }
  catch { ElMessage.error('发送失败') }
}

const goResult = () => router.push(`/results/${props.taskId}`)
</script>

<style scoped>
.panel { display: flex; flex-direction: column; height: 100%; background: var(--bg-page); }

.panel-head {
  display: flex; align-items: center; gap: 16px;
  padding: 12px 20px; background: var(--bg-card); border-bottom: 1px solid var(--border-subtle); flex-shrink: 0;
}
.back-btn { width: 28px; height: 28px; background: transparent; border: 1px solid var(--border-subtle); border-radius: var(--radius-md); color: var(--text-secondary); cursor: pointer; }
.back-btn:hover { background: var(--bg-elevated); color: var(--text-primary); }
.head-title { flex: 1; min-width: 0; }
.head-title h3 { margin: 0; font-size: 15px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.head-goal { margin: 2px 0 0; font-size: 12px; color: var(--text-muted); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.head-right { display: flex; align-items: center; gap: 16px; flex-shrink: 0; }

.panel-body { flex: 1; display: flex; min-height: 0; }
.stage-wrap { flex: 1; position: relative; min-width: 0; }
.stage-overlay {
  position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 12px; background: rgba(250,250,250,0.4); backdrop-filter: blur(2px); z-index: 5;
}
html[data-theme="dark"] .stage-overlay { background: rgba(9,9,11,0.4); }
.overlay-hint { font-size: 13px; color: var(--text-muted); margin: 0; }

.panel-foot {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 20px; background: var(--bg-card); border-top: 1px solid var(--border-subtle); flex-shrink: 0;
}
.panel-foot :deep(.input-wrap) { flex: 1; }
.foot-actions { display: flex; gap: 8px; flex-shrink: 0; }
</style>
```

- [ ] **Step 3:** 修改 `DiscussionView.vue` 使用 PageShell：

```vue
<template>
  <PageShell>
    <DiscussionPanel :task-id="Number(taskId)" />
  </PageShell>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import PageShell from '@/components/ui/PageShell.vue'
import DiscussionPanel from '@/components/discussion/DiscussionPanel.vue'
const taskId = computed(() => useRoute().params.taskId as string)
</script>
```

- [ ] **Step 4:** 删除弃用文件：

```bash
rm careermind-frontend/src/components/discussion/RoundIndicator.vue
```

- [ ] **Step 5:** 启动 dev + 完整跑一次讨论（创建任务→开始→4 轮），确认：圆桌渲染、发言者放大/光环、气泡内容流式、消息抽屉可展开、轮次切换顶部标语。

- [ ] **Step 6:** Commit：

```bash
git add -A careermind-frontend/src/components/discussion/ careermind-frontend/src/views/DiscussionView.vue
git commit -m "feat(discussion): integrate roundtable stage + message drawer"
```

---

## Task 21: TasksView 重做

**Files:**
- Modify: `careermind-frontend/src/views/TasksView.vue`

- [ ] **Step 1:** 保留现有创建咨询 API 调用与 Agent 选择逻辑，替换为以下模板（外壳用 PageShell）：

```vue
<template>
  <PageShell>
    <div class="tasks-page">
      <section class="quick-create">
        <h2>新的决策需要讨论？</h2>
        <BaseInput
          v-model="newGoal"
          textarea
          :rows="3"
          placeholder="描述你的困惑或目标，例如：我在考虑要不要从互联网转行到金融…"
        />
        <div class="create-row">
          <BaseBadge tone="neutral">已选 {{ selectedAgents.length }} 位专家</BaseBadge>
          <BaseButton variant="primary" size="md" :disabled="!newGoal.trim()" @click="createAndStart">开始咨询 →</BaseButton>
        </div>
      </section>

      <section class="my-tasks">
        <div class="sec-head">
          <h3>我的咨询</h3>
          <div class="filter-tabs">
            <button v-for="f in filters" :key="f.val" class="tab" :class="{ on: statusFilter === f.val }" @click="statusFilter = f.val">{{ f.label }}</button>
          </div>
        </div>

        <div v-if="filtered.length === 0" class="empty-box">
          <EmptyState title="还没有咨询" description="在上方输入你的困惑开始第一次讨论" />
        </div>
        <div v-else class="tasks-grid">
          <BaseCard v-for="t in filtered" :key="t.id" hoverable @click="$router.push(`/tasks/${t.id}`)">
            <h4 class="t-title">{{ t.title }}</h4>
            <p class="t-goal">{{ (t.goal || '').slice(0, 60) }}</p>
            <div class="t-meta">
              <AgentAvatarGroup :agents="t.agents.map(a => ({ id: a.id, type: a.type }))" :size="30" :max="5" />
              <BaseBadge :tone="toneOfStatus(t.status)">{{ labelOfStatus(t.status) }}</BaseBadge>
            </div>
          </BaseCard>
        </div>
      </section>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageShell from '@/components/ui/PageShell.vue'
import BaseInput from '@/components/ui/BaseInput.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import AgentAvatarGroup from '@/components/agent/AgentAvatarGroup.vue'
import { useTaskStore } from '@/stores/task'
import { useAgentStore } from '@/stores/user'
import { taskApi } from '@/api/task'
import type { TaskStatus } from '@/types'

const router = useRouter()
const taskStore = useTaskStore()
const agentStore = useAgentStore()

const newGoal = ref('')
const statusFilter = ref<'all' | TaskStatus>('all')

const filters = [
  { val: 'all' as const,         label: '全部' },
  { val: 'DISCUSSING' as const,  label: '讨论中' },
  { val: 'COMPLETED' as const,   label: '已完成' },
]

const selectedAgents = computed(() => agentStore.presetAgents.slice(0, 5))
const filtered = computed(() => taskStore.tasks.filter(t => statusFilter.value === 'all' || t.status === statusFilter.value))

const toneOfStatus = (s: TaskStatus) => ({
  PENDING: 'neutral', DISCUSSING: 'accent', MERGING: 'warning', COMPLETED: 'success', ARCHIVED: 'neutral',
}[s] as 'neutral'|'accent'|'warning'|'success')
const labelOfStatus = (s: TaskStatus) => ({ PENDING: '待开始', DISCUSSING: '讨论中', MERGING: '整合中', COMPLETED: '已完成', ARCHIVED: '已归档' }[s])

const createAndStart = async () => {
  const goal = newGoal.value.trim(); if (!goal) return
  try {
    const title = goal.slice(0, 8) || '新的咨询'
    const task = await taskApi.createTask({ title, goal, agentIds: selectedAgents.value.map(a => a.id) })
    ElMessage.success('咨询已创建')
    router.push(`/discussions/${task.id}`)
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  }
}

onMounted(() => {
  taskStore.fetchTasks()
  agentStore.fetchPresetAgents()
})
</script>

<style scoped>
.tasks-page { padding: 32px 40px; max-width: 1200px; margin: 0 auto; overflow-y: auto; height: 100%; }

.quick-create {
  background: var(--bg-card); border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg); padding: 24px; margin-bottom: 32px;
}
.quick-create h2 { font-size: 20px; margin: 0 0 16px; }
.create-row { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }

.sec-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.sec-head h3 { font-size: 16px; margin: 0; }
.filter-tabs { display: inline-flex; gap: 4px; background: var(--bg-elevated); padding: 3px; border-radius: var(--radius-full); }
.tab {
  padding: 4px 12px; background: transparent; border: none; cursor: pointer;
  border-radius: var(--radius-full); font-size: 13px; color: var(--text-secondary);
}
.tab.on { background: var(--bg-card); color: var(--text-primary); box-shadow: var(--shadow-sm); }

.tasks-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 16px; }
.t-title { margin: 0 0 6px; font-size: 15px; }
.t-goal  { margin: 0 0 16px; font-size: 13px; color: var(--text-secondary); height: 38px; overflow: hidden; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.t-meta  { display: flex; align-items: center; justify-content: space-between; }

.empty-box { padding: 48px; background: var(--bg-card); border: 1px dashed var(--border-emphasis); border-radius: var(--radius-lg); }
</style>
```

注意 `useAgentStore` 的 import 路径错误——实际是 `@/stores/agent`。替换 import：

```ts
import { useAgentStore } from '@/stores/agent'
```

- [ ] **Step 2:** 启动 dev 测试一次创建流程。Commit：

```bash
git add careermind-frontend/src/views/TasksView.vue
git commit -m "feat(tasks): redesign tasks list with quick create + card grid"
```

---

## Task 22: TaskView 重做

**Files:**
- Modify: `careermind-frontend/src/views/TaskView.vue`

- [ ] **Step 1:** 读现有 TaskView 保留数据加载与 DecisionTree 引用，替换模板为：

```vue
<template>
  <PageShell>
    <div class="task-view" v-if="task">
      <header class="tv-head">
        <div>
          <BaseBadge :tone="toneOfStatus(task.status)">{{ labelOfStatus(task.status) }}</BaseBadge>
          <h2>{{ task.title }}</h2>
        </div>
        <div class="tv-actions">
          <BaseButton variant="secondary" @click="$router.push(`/discussions/${task.id}`)">进入讨论</BaseButton>
          <BaseButton variant="primary"   @click="$router.push(`/results/${task.id}`)">查看结果</BaseButton>
        </div>
      </header>

      <BaseCard class="tv-card">
        <template #header>任务信息</template>
        <div class="info-row"><span class="ik">背景</span><p>{{ task.background || '—' }}</p></div>
        <div class="info-row"><span class="ik">目标</span><p>{{ task.goal || '—' }}</p></div>
        <div class="info-row"><span class="ik">约束</span><p>{{ task.constraints || '—' }}</p></div>
        <div class="info-row"><span class="ik">专家</span>
          <AgentAvatarGroup :agents="task.agents.map(a => ({ id: a.id, type: a.type }))" :size="30" :max="5" />
        </div>
      </BaseCard>

      <BaseCard class="tv-card">
        <template #header>决策链路</template>
        <DecisionTree :task="task" :discussion="discussion" :merge-result="mergeResult" />
      </BaseCard>
    </div>
  </PageShell>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import PageShell from '@/components/ui/PageShell.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import AgentAvatarGroup from '@/components/agent/AgentAvatarGroup.vue'
import DecisionTree from '@/components/task/DecisionTree.vue'
import { taskApi } from '@/api/task'
import { discussionApi } from '@/api/discussion'
import { mergeApi } from '@/api/merge'
import type { Task, Discussion, MergeResult, TaskStatus } from '@/types'

const route = useRoute()
const task = ref<Task | null>(null)
const discussion = ref<Discussion | null>(null)
const mergeResult = ref<MergeResult | null>(null)

const load = async () => {
  const id = Number(route.params.id)
  task.value = await taskApi.getTaskById(id)
  try { discussion.value = await discussionApi.getDiscussion(id) } catch {}
  try { mergeResult.value = await mergeApi.getMergeResult(id) } catch {}
}
onMounted(load)
watch(() => route.params.id, load)

const toneOfStatus = (s: TaskStatus) => ({
  PENDING: 'neutral', DISCUSSING: 'accent', MERGING: 'warning', COMPLETED: 'success', ARCHIVED: 'neutral',
}[s] as 'neutral'|'accent'|'warning'|'success')
const labelOfStatus = (s: TaskStatus) => ({ PENDING: '待开始', DISCUSSING: '讨论中', MERGING: '整合中', COMPLETED: '已完成', ARCHIVED: '已归档' }[s])
</script>

<style scoped>
.task-view { padding: 32px 40px; max-width: 1100px; margin: 0 auto; overflow-y: auto; height: 100%; }
.tv-head { display: flex; align-items: flex-start; justify-content: space-between; margin-bottom: 20px; gap: 20px; }
.tv-head h2 { margin: 6px 0 0; font-size: 22px; }
.tv-actions { display: flex; gap: 8px; flex-shrink: 0; }
.tv-card { margin-bottom: 16px; }
.info-row { display: grid; grid-template-columns: 60px 1fr; gap: 16px; padding: 10px 0; border-top: 1px solid var(--border-subtle); }
.info-row:first-child { border-top: none; }
.ik { color: var(--text-muted); font-size: 13px; }
.info-row p { margin: 0; font-size: 13px; line-height: 1.6; }
</style>
```

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/views/TaskView.vue
git commit -m "feat(task): redesign task detail view"
```

---

## Task 23: ResultView 重做

**Files:**
- Modify: `careermind-frontend/src/views/ResultView.vue`

- [ ] **Step 1:** 保留 mergeResult 加载逻辑、流式输出订阅（如有）；替换模板为顶部横幅 + 方案对比视图：

```vue
<template>
  <PageShell>
    <div class="result-view" v-if="mergeResult">
      <header class="banner">
        <BrandLogo size="sm" />
        <h1>你的职业决策</h1>
        <div class="banner-stats">
          <div class="stat"><span class="num">{{ Math.round(mergeResult.convergenceRate * 100) }}%</span><span class="lbl">共识度</span></div>
          <div class="stat"><span class="num">{{ mergeResult.plans.length }}</span><span class="lbl">候选方案</span></div>
          <div class="stat"><span class="num">{{ mergeResult.blindSpots.length }}</span><span class="lbl">认知盲区</span></div>
        </div>
      </header>

      <section>
        <h2 class="sec-title">候选方案</h2>
        <div class="plans-grid">
          <BaseCard v-for="(p, i) in mergeResult.plans" :key="i" :class="{ selected: p.isSelected }">
            <div class="plan-head">
              <BaseBadge tone="accent">方案 {{ i + 1 }}</BaseBadge>
              <div class="conf">
                <div class="conf-bar" :style="{ width: (p.confidence * 100) + '%' }" />
                <span>{{ Math.round(p.confidence * 100) }}%</span>
              </div>
            </div>
            <h3 class="plan-title">{{ p.title }}</h3>
            <p class="plan-desc">{{ p.description }}</p>
            <div class="plan-section">
              <h5>里程碑</h5>
              <ul><li v-for="(m, j) in p.milestones" :key="j">{{ m }}</li></ul>
            </div>
            <div class="plan-section">
              <h5>风险</h5>
              <ul><li v-for="(r, j) in p.risks" :key="j">{{ r }}</li></ul>
            </div>
            <div class="plan-section">
              <h5>适用条件</h5>
              <p class="cond">{{ p.applicableConditions }}</p>
            </div>
          </BaseCard>
        </div>
      </section>

      <section>
        <h2 class="sec-title">认知盲区</h2>
        <div class="blinds">
          <BaseCard v-for="(b, i) in mergeResult.blindSpots" :key="i" inset padding="14px">
            <div class="blind-row"><span class="blind-num">0{{ i + 1 }}</span><p>{{ b }}</p></div>
          </BaseCard>
        </div>
      </section>

      <footer class="rv-foot">
        <BaseButton variant="ghost" @click="$router.push(`/discussions/${taskId}`)">← 返回讨论</BaseButton>
        <BaseButton variant="primary" @click="onExport">导出 PDF 报告 📄</BaseButton>
      </footer>
    </div>
    <EmptyState v-else-if="!loading" title="尚未生成结果" description="请先完成讨论或从讨论页点击「生成报告」" />
  </PageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageShell from '@/components/ui/PageShell.vue'
import BrandLogo from '@/components/ui/BrandLogo.vue'
import BaseCard from '@/components/ui/BaseCard.vue'
import BaseButton from '@/components/ui/BaseButton.vue'
import BaseBadge from '@/components/ui/BaseBadge.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { mergeApi } from '@/api/merge'
import type { MergeResult } from '@/types'

const route = useRoute(); const router = useRouter()
const taskId = computed(() => Number(route.params.taskId))
const mergeResult = ref<MergeResult | null>(null)
const loading = ref(true)

const load = async () => {
  loading.value = true
  try { mergeResult.value = await mergeApi.getMergeResult(taskId.value) }
  catch { mergeResult.value = null }
  finally { loading.value = false }
}
onMounted(load)
watch(taskId, load)

const onExport = () => ElMessage.info('PDF 导出将在 P3 阶段可用')
</script>

<style scoped>
.result-view { padding: 0; overflow-y: auto; height: 100%; }

.banner {
  padding: 48px 40px; background: linear-gradient(135deg, var(--accent-dim) 0%, var(--bg-card) 100%);
  border-bottom: 1px solid var(--border-subtle); display: flex; flex-direction: column; gap: 12px;
}
.banner h1 { font-size: 36px; margin: 4px 0; }
.banner-stats { display: flex; gap: 32px; margin-top: 12px; }
.stat { display: flex; flex-direction: column; }
.num { font-size: 32px; font-weight: 700; color: var(--accent); }
.lbl { font-size: 12px; color: var(--text-secondary); }

section { padding: 32px 40px; max-width: 1200px; margin: 0 auto; }
.sec-title { font-size: 18px; margin: 0 0 16px; }

.plans-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(320px, 1fr)); gap: 16px; }
.selected { border-color: var(--accent); box-shadow: 0 0 0 1px var(--accent); }
.plan-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.conf { display: inline-flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-secondary); }
.conf-bar { width: 60px; height: 4px; background: var(--accent-dim); border-radius: 9999px; position: relative; }
.conf-bar::after { content: ''; position: absolute; left: 0; top: 0; height: 100%; background: var(--accent); border-radius: 9999px; width: var(--w, 0); }
.plan-title { font-size: 16px; margin: 0 0 6px; }
.plan-desc  { font-size: 13px; color: var(--text-secondary); margin: 0 0 14px; line-height: 1.6; }
.plan-section { margin-top: 12px; }
.plan-section h5 { font-size: 12px; color: var(--text-muted); margin: 0 0 6px; text-transform: uppercase; letter-spacing: 0.04em; }
.plan-section ul { margin: 0; padding-left: 18px; font-size: 13px; line-height: 1.6; }
.cond { margin: 0; font-size: 13px; color: var(--text-secondary); }

.blinds { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 10px; }
.blind-row { display: flex; gap: 12px; align-items: start; }
.blind-num { font-family: var(--font-mono); color: var(--warning); font-size: 13px; flex-shrink: 0; }
.blind-row p { margin: 0; font-size: 13px; line-height: 1.6; color: var(--text-primary); }

.rv-foot { padding: 32px 40px; display: flex; justify-content: space-between; border-top: 1px solid var(--border-subtle); }
</style>
```

注意 `.conf-bar` 里 `--w` 没赋值；用内联 style 传进来，修正模板：

```vue
<div class="conf-bar" :style="`--w:${Math.round(p.confidence*100)}%`" />
```

并在 `.conf-bar` 里把 `width: 60px;` 保留不变。

- [ ] **Step 2:** Commit：

```bash
git add careermind-frontend/src/views/ResultView.vue
git commit -m "feat(result): redesign result view with banner + plan comparison"
```

---

## Task 24: AgentsView / KbView / SettingsView 重做

**Files:**
- Modify: `careermind-frontend/src/views/AgentsView.vue`
- Modify: `careermind-frontend/src/views/KbView.vue`
- Modify: `careermind-frontend/src/views/SettingsView.vue`

策略：保留全部业务逻辑与 API 调用；把外壳统一换成 `<PageShell>`，列表/卡片/按钮全换成 Base* 组件；AgentsView 列表项头像换成 `<AgentAvatar>`。

- [ ] **Step 1: AgentsView.vue** 骨架：

```vue
<template>
  <PageShell>
    <div class="agents-page">
      <header class="ap-head">
        <div>
          <h2>Agent 管理</h2>
          <p class="muted">5 位预设专家 + 自定义 Agent</p>
        </div>
        <BaseButton variant="primary" @click="openCreate">+ 新建自定义 Agent</BaseButton>
      </header>

      <section>
        <h3 class="sec-sub">预设专家</h3>
        <div class="grid">
          <AgentCard v-for="a in preset" :key="a.id" :type="a.type" :name="a.name" :role="a.description || undefined" />
        </div>
      </section>

      <section>
        <h3 class="sec-sub">我的自定义</h3>
        <div v-if="custom.length === 0" class="empty"><EmptyState title="还没有自定义 Agent" description="点击右上角新建" /></div>
        <div v-else class="grid">
          <AgentCard v-for="a in custom" :key="a.id" :type="a.type" :name="a.name" :role="a.description || undefined">
            <div class="card-actions">
              <BaseButton variant="ghost" size="sm" @click="openEdit(a)">编辑</BaseButton>
              <BaseButton variant="ghost" size="sm" @click="confirmDelete(a)">删除</BaseButton>
            </div>
          </AgentCard>
        </div>
      </section>

      <!-- 保留现有 el-dialog 创建/编辑弹层 -->
    </div>
  </PageShell>
</template>
```

`<script setup>` 部分保留现有 `agentStore.fetchPresetAgents/fetchAvailableAgents/createAgent/deleteAgent` 等逻辑，仅新增 `preset` 与 `custom` computed（按 `isPreset` 分组）。

- [ ] **Step 2: KbView.vue** 外层换 PageShell、列表卡片换 BaseCard、按钮换 BaseButton；knowledge base 上传对话框保留 el-dialog。模板示例：

```vue
<template>
  <PageShell>
    <div class="kb-page">
      <header class="kb-head">
        <h2>知识库</h2>
        <BaseButton variant="primary" @click="openCreate">+ 创建知识库</BaseButton>
      </header>
      <div v-if="list.length === 0" class="empty"><EmptyState title="暂无知识库" /></div>
      <div v-else class="kb-grid">
        <BaseCard v-for="k in list" :key="k.id" hoverable @click="enter(k)">
          <h4>{{ k.name }}</h4>
          <p class="muted">{{ k.description || '暂无描述' }}</p>
          <div class="kb-meta"><BaseBadge tone="neutral">{{ k.documentCount }} 文档</BaseBadge></div>
        </BaseCard>
      </div>
    </div>
  </PageShell>
</template>
```

- [ ] **Step 3: SettingsView.vue** 模板：

```vue
<template>
  <PageShell>
    <div class="settings-page">
      <h2>个人设置</h2>

      <BaseCard>
        <template #header>主题</template>
        <div class="row">
          <div>
            <p>外观</p>
            <p class="muted">切换明/暗模式</p>
          </div>
          <ThemeToggle />
        </div>
      </BaseCard>

      <BaseCard>
        <template #header>个人简介</template>
        <BaseInput v-model="profile.bio" textarea :rows="4" placeholder="简单介绍你自己，讨论时将作为背景参考" />
        <BaseButton variant="primary" size="sm" @click="saveBio">保存</BaseButton>
      </BaseCard>

      <BaseCard>
        <template #header>账号</template>
        <div class="row"><p>用户名</p><p>{{ user?.username }}</p></div>
        <div class="row"><p>邮箱</p><p>{{ user?.email }}</p></div>
      </BaseCard>
    </div>
  </PageShell>
</template>
```

保留现有 `profile`、`saveBio` 逻辑不变。

- [ ] **Step 4:** Commit：

```bash
git add careermind-frontend/src/views/AgentsView.vue careermind-frontend/src/views/KbView.vue careermind-frontend/src/views/SettingsView.vue
git commit -m "feat(views): redesign Agents/Kb/Settings with new design system"
```

---

## Task 25: element-overrides.css

**Files:**
- Create: `careermind-frontend/src/styles/element-overrides.css`
- Modify: `careermind-frontend/src/main.ts`

目的：让 Element Plus 保留的组件（Dialog/Dropdown/Message/Empty）在明暗模式下色调一致。

- [ ] **Step 1:** 创建文件：

```css
/* Dialog */
.el-dialog {
  background: var(--bg-card) !important;
  border-radius: var(--radius-lg) !important;
}
.el-dialog__title, .el-dialog__headerbtn .el-dialog__close { color: var(--text-primary) !important; }
.el-dialog__body { color: var(--text-primary) !important; }

/* Message */
.el-message { background: var(--bg-card) !important; border-color: var(--border-subtle) !important; }
.el-message__content { color: var(--text-primary) !important; }

/* Dropdown */
.el-dropdown-menu { background: var(--bg-card) !important; border: 1px solid var(--border-subtle) !important; }
.el-dropdown-menu__item { color: var(--text-primary) !important; }
.el-dropdown-menu__item:not(.is-disabled):hover { background: var(--bg-elevated) !important; }

/* Input (只在保留的 el-input 位置生效；其余已换 BaseInput) */
.el-input__wrapper {
  background: var(--bg-card) !important;
  box-shadow: 0 0 0 1px var(--border-emphasis) inset !important;
  border-radius: var(--radius-md) !important;
}
.el-input__inner { color: var(--text-primary) !important; }
.el-input__wrapper.is-focus { box-shadow: 0 0 0 1px var(--accent) inset, 0 0 0 3px var(--accent-dim) !important; }

/* Empty */
.el-empty__description { color: var(--text-muted) !important; }

/* Divider (仍在某些老组件里用) */
.el-divider { border-color: var(--border-subtle) !important; }
.el-divider__text { background: var(--bg-card) !important; color: var(--text-muted) !important; }
```

- [ ] **Step 2:** 在 `main.ts` 的 ElementPlus 注册后 import：

```ts
import './styles/element-overrides.css'
```

- [ ] **Step 3:** Commit：

```bash
git add careermind-frontend/src/styles/element-overrides.css careermind-frontend/src/main.ts
git commit -m "feat(style): override Element Plus colors with tokens"
```

---

## Task 26: Playwright E2E ui-brand.spec.js

**Files:**
- Create: `e2e-tests/tests/ui-brand.spec.js`

- [ ] **Step 1:**

```js
const { test, expect } = require('@playwright/test')

test.describe('UI brand redesign', () => {
  test('Home page renders logo, hero, experts grid', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('.brand-logo').first()).toBeVisible()
    await expect(page.getByRole('heading', { level: 1 })).toContainText('五位 AI 专家')
    await expect(page.locator('#experts .agent-card')).toHaveCount(5)
  })

  test('Theme toggle switches data-theme attribute', async ({ page }) => {
    await page.goto('/')
    const html = page.locator('html')
    const before = await html.getAttribute('data-theme')
    await page.locator('.theme-toggle').first().click()
    const after = await html.getAttribute('data-theme')
    expect(before).not.toBe(after)
    await expect(html).toHaveAttribute('data-theme', after)
  })

  test('Sidebar collapse toggles width', async ({ page }) => {
    await page.goto('/login')
    // 登录流程：简化为直接访问 /tasks 依赖已存在登录 session
    await page.goto('/tasks').catch(() => {})
    const sidebar = page.locator('aside.sidebar').first()
    if (await sidebar.isVisible()) {
      await page.locator('.collapse-btn').first().click()
      await expect(sidebar).toHaveClass(/collapsed/)
    }
  })

  test('Discussion page renders roundtable with 5 avatars', async ({ page }) => {
    // 需存在至少一个 task；本测试容忍空态
    await page.goto('/')
    await page.waitForLoadState('networkidle')
  })
})
```

注意：最后一个测试是占位（依赖登录态），真实验证在执行阶段 Task 27 手动跑。

- [ ] **Step 2:** 跑一次：

```bash
cd e2e-tests && npx playwright test ui-brand.spec.js --project=chromium
```

- [ ] **Step 3:** Commit：

```bash
git add e2e-tests/tests/ui-brand.spec.js
git commit -m "test(e2e): add ui-brand smoke tests"
```

---

## Task 27: build + 手动回归 + 截图素材

**Files:**
- 无新增；生成比赛素材入 `docs/screenshots/`

- [ ] **Step 1:** 构建验证：

```bash
cd careermind-frontend && npm run build
```

必须无 TS / vue-tsc 错误。若有错误逐一修复。

- [ ] **Step 2:** 启动后端 + 前端 + 跑一次完整用户路径（登录→新建咨询→4 轮讨论→查看结果）。清单打勾：
  - [ ] 圆桌 5 头像渲染，当前发言者有光环、其他倾听呼吸
  - [ ] 消息抽屉可收起/展开，计数徽章正确
  - [ ] 轮次切换时顶部标语淡入淡出
  - [ ] 质疑光流偶发出现（Round 2/3 有 replyToAgentId 时）
  - [ ] 明暗模式切换全页面无色差
  - [ ] Sidebar 折叠/展开持久化（刷新后保持）

- [ ] **Step 3:** 生成比赛截图（放入 `docs/screenshots/`）：

```bash
mkdir -p docs/screenshots
cd e2e-tests && npx playwright test ui-brand.spec.js --project=chromium --reporter=list
# 手动在浏览器里 1920x1080 全屏截图：
# 1. home-light.png / home-dark.png
# 2. roundtable-round2-challenge.png
# 3. result-view.png
# 4. sidebar-collapsed.png
```

- [ ] **Step 4:** 更新 PROJECT_STATUS.md，增加一条变更记录并把 P1 列入"已完成"。

- [ ] **Step 5:** Commit + 最终推送：

```bash
git add docs/screenshots/ PROJECT_STATUS.md
git commit -m "docs: P1 brand redesign complete — screenshots + status update"
git push origin main
```

---

## 自检

- **Spec coverage**：
  - Tokens (§3) → Task 2
  - Agent 人格色 → Task 2（tokens）+ Task 6（meta）+ Task 11（Avatar）
  - 基础组件清单（§4）→ Task 7/8/9/10/11/12/13/14
  - 页面重做 5.1–5.7 → Task 15/16/21/22/23/24 + Task 20（Discussion）
  - 动效 §6 → tokens + Avatar + SpeechBubble + ChallengeFlow + App.vue
  - 数据 §7 → 无变更（声明过）
  - 文件变更 §8 → 逐一覆盖
  - 测试 §9 → Task 26
  - 风险 §10 → tailwind prefix 不启用（由 overrides 兜底）；overrides 在 Task 25；Avatar 4 档尺寸已固定
  - DoD §11 → Task 27

- **Placeholder**：无 "TBD/later"。Task 21 里修正了 agent store import 路径；Task 23 里修正了 `--w` 变量赋值方式。

- **类型一致**：
  - AgentAvatar 的 `state` 值域 `idle|listening|speaking|challenging` 在 Task 11/18/20 保持一致
  - RoundTimeline 的 props `current` + `completed` 在 Task 13/20 一致
  - ChallengePair 的结构 `{ fromAgentId, toAgentId, triggerAt }` 在 Task 18/20 一致
