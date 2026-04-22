# CareerMind 品牌与 UI/UX 重做 设计文档（P1）

- 创建日期：2026-04-22
- 目标场景：中国大学生计算机设计比赛冲刺（deadline ~2026-05-06）
- 本文档仅描述 P1 阶段（品牌/视觉/核心交互重做）。P2（辩论可视化）、P3（PDF 报告）各有独立 spec。

## 1. 目标

让 CareerMind 在评委 3 分钟 Demo 内产生"这不是普通 LLM 套壳"的第一印象。具体：

- 一套统一的极简设计体系（Notion/Linear 风），覆盖全部 9 个主视图
- 讨论页由"上下消息列表"升级为**圆桌辩论布局**，作为 Demo 核心画面
- 5 位 Agent 视觉人格化（专属色 + SVG 抽象脸 + 状态动效）
- 日/夜模式切换
- 全部基础组件封装，避免后续 P2/P3 再回头改样式

## 2. 非目标

- 不做语音/TTS/情绪 Agent/职业路径模拟器/真实数据接入（留作 PROJECT_STATUS 待办）
- 不做移动端深度适配（仅保证 Demo 电脑 1440x900 与投影 1920x1080 完美）
- 不引入重量级 UI 库（Radix/Headless 等）；Element Plus 保留在 Dialog/Form/Message 等复杂场景

## 3. 设计体系（Design Tokens）

### 3.1 色板（CSS Custom Properties）

定义在 `src/styles/tokens.css`，以 `:root` / `html[data-theme="dark"]` 两套。

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
  --accent-dim:      #1E3A8A33;
}
```

### 3.2 Agent 人格色

一等公民，直接注入 CSS；用 `data-agent-type` 属性选择。

```css
[data-agent-type="INDUSTRY_ANALYST"] { --agent: #1E3A8A; --agent-dim: #1E3A8A1a; }
[data-agent-type="SKILL_ASSESSOR"]   { --agent: #0D9488; --agent-dim: #0D94881a; }
[data-agent-type="RISK_WATCHER"]     { --agent: #B45309; --agent-dim: #B453091a; }
[data-agent-type="OPPORTUNITY_HUNTER"]{ --agent: #CA8A04; --agent-dim: #CA8A041a; }
[data-agent-type="VALUE_EXAMINER"]   { --agent: #9333EA; --agent-dim: #9333EA1a; }
[data-agent-type="CUSTOM"]           { --agent: #525B6B; --agent-dim: #525B6B1a; }
[data-agent-type="MERGE_AGENT"]      { --agent: #111827; --agent-dim: #1118271a; }

html[data-theme="dark"] [data-agent-type="INDUSTRY_ANALYST"] { --agent: #60A5FA; }
/* 其他 agent 深色态同理，见 tokens.css 全量 */
```

### 3.3 字体
- 正文：Inter 400/500 + Noto Sans SC
- 标题加重：Inter 600 + Noto Sans SC 500
- 等宽：JetBrains Mono（代码/数据）
- 报告封面/大标语场景：Noto Serif SC（仅点缀）

通过 Google Fonts CDN 在 `index.html` 引入；字体文件 fallback 到系统栈。

### 3.4 Tailwind 配置
- 覆盖 `tailwind.config.js` 的 `theme.extend`：色值映射到上述 CSS 变量
- 扩展 `boxShadow`、`borderRadius`、`fontFamily`、`transitionTimingFunction`
- 启用 `darkMode: ['class', 'html[data-theme="dark"]']`

## 4. 基础组件（新建）

目录：`src/components/ui/`

| 组件 | 职责 | Props 要点 |
|---|---|---|
| `BaseButton` | 替代/包装按钮 | `variant: primary/secondary/ghost/danger`、`size: sm/md/lg`、`loading` |
| `BaseCard` | 卡片容器 | `padding`、`hoverable` |
| `BaseBadge` | 状态徽章 | `tone: neutral/accent/success/warning/danger` |
| `BaseTag` | 可点击标签 | `removable`、`size` |
| `BaseInput` | 输入框 | 基于 Tailwind；复杂 form 仍用 el-input |
| `BaseSkeleton` | 骨架屏 | `lines`、`shape` |
| `AgentAvatar` | Agent 头像 | `agentType`、`size`、`state: speaking/listening/challenging/idle`、`customAvatarUrl?` |
| `AgentAvatarGroup` | Avatar 堆叠 | `agents`、`max`、`size` |
| `AgentBadge` | 小胶囊 = Avatar + name | `agentType`、`name` |
| `BrandLogo` | 项目 Logo | `variant: full/icon`、`size` |
| `ThemeToggle` | 明暗切换按钮 | 无 |
| `RoundTimeline` | 4 轮进度条 | `current: 1..4`、`completedRounds` |
| `EmptyState` | 空态 | `icon`、`title`、`description`、`action` |
| `PageShell` | 页面外壳（Sidebar + 内容区） | slot |

### 4.1 AgentAvatar 详细设计

- 结构：`<svg>` 30/48/72/120 四档尺寸
- 内部：圆形背景色 = `var(--agent-dim)`，几何脸（两点一弧）描边 = `var(--agent)`，右下角 9×9 小徽章显示 Agent 符号（眼镜/尺规/盾牌/箭头/问号/心形）
- 状态动效：
  - `idle`：无动画
  - `listening`：灰度 60%，0.06 透明度 3s 循环呼吸
  - `speaking`：`ring-2 ring-[var(--agent)]` + `scale-105` + 外圈 2 层扩散光环（`@keyframes ping` 自写版）
  - `challenging`：头像轻微向目标方向倾斜（`rotate: -3deg`），配合父容器画光流线
- 自定义 Agent（用户创建）：用 `CUSTOM` 默认色，支持传 `customAvatarUrl` 覆盖为头像图片

## 5. 页面级重做

### 5.1 HomeView `/`

区块（按从上到下）：
1. **Hero**
   - 左上 `BrandLogo`，右上导航（登录/GitHub）
   - 中央 Slogan：「让五位 AI 专家，为你的人生辩一场」
   - 副文本：「每一次重要决定，都该经过一场严肃的辩论」
   - 主按钮「开始咨询 →」（未登录跳登录，已登录跳 `/tasks` 的快速创建）
   - 背景：淡淡的点阵或射线纹理（SVG pattern），不抢戏
2. **Experts** — 5 张 AgentCard 横排，悬停 Avatar 缩放 + 简介气泡浮现
3. **Flow** — 4 步时间轴说明 4 轮讨论流程
4. **UseCases** — 3 张 BaseCard 说明适用场景（转行 / 晋升 / 升学读研）
5. **Footer** — 版权 + 比赛信息

移除目前 HomeView 里已有的快速输入框与 Agent 快选（这些移到 Tasks 页）。保证 Landing 只服务"说清楚产品是什么"。

### 5.2 Sidebar 重做

- 宽度：展开 260px / 收起 72px（点击 Logo 或快捷键 `[` 切换；本地存储记忆状态）
- 顶部：BrandLogo + 折叠按钮
- 导航（纵向）：首页 / 新咨询 / 知识库 / Agent 管理
- 历史对话（可滚动）：按时间倒序，悬停显示三点菜单（重命名/删除/置顶）
- 底部：用户头像 + 下拉（设置/登出）；明暗切换按钮

收起状态下只显示图标，hover tooltip 展示文字。

### 5.3 DiscussionView `/discussions/:taskId`（核心）

**布局（桌面 1440+）**：
- 顶部栏（64px）：← 返回、咨询标题（可截断）、右侧 RoundTimeline、暂停/继续按钮
- 主内容区域分左右：
  - **左 = 圆桌区**（占 60-70%）
  - **右 = 消息流侧栏**（默认收起到 48px 图标栏；展开后 360px）
- 底部栏（始终固定，96px）：输入框（占满）+ 右侧三个快捷按钮「插话」「下一轮 ▷」「生成报告 📄」

**圆桌区**：
- 背景：淡色网格纸纹理
- 5 个 AgentAvatar 绝对定位，大小 120px：
  - 正上（0%, 50% → translate -50%, 0）
  - 左上 35%, 15%
  - 右上 35%, 85%
  - 左下 80%, 30%
  - 右下 80%, 70%
- 中心：SVG 十字 + 当前讨论主题的简短浮字（任务 goal 前 20 字）
- 发言者 Avatar 状态 = `speaking`；其他 Agent 状态 = `listening`
- 发言者头像**正下方**浮一个"发言气泡"（最新内容流式显示，带打字光标），最多显示 3 行，超出省略
- 质疑动效：当 Agent A 的消息 `replyToMessageId` 指向 Agent B 时，从 A 到 B 画一道 SVG 光流（1.5s），之后转为半透明连线保留 5s 淡出
- 每轮切换时：淡入一个"第 N 轮 · 轮次名"大字（500ms fade + 向上 12px 过渡），1.2s 后消失

**右侧消息流**：
- 收起态：竖排"消息 (12)"图标 + 数字徽章，点击展开
- 展开态：按轮次分组，每条消息顶部是 AgentBadge + 时间；内容渲染 markdown；质疑消息左侧有一条 3px 高亮色条（对应发起者色）
- 用户消息（interjection）：主色高亮底 + "你" 徽章

**空态**（未开始讨论）：
- 圆桌区显示 5 个 Avatar 静态排列 + 中心大按钮「▶ 开始讨论」
- 右侧栏只显示提示文案"开始后在此查看逐条消息"

### 5.4 TasksView `/tasks`

- 顶部：大输入框（快速创建咨询）+ Agent 快选（已有功能的 UI 提升）
- 下方：我的咨询 列表卡片网格（3 列）；每卡显示标题、goal 截断、最后更新、参与 Agent 头像堆叠、状态徽章
- 侧边筛选：状态（全部/进行中/已完成）

### 5.5 TaskView `/tasks/:id`

- 顶：任务摘要卡（标题/背景/目标/参与 Agent）
- 中：决策链路（保留 DecisionTree，但配色换新；P2 里此处加"观点演化图"）
- 下：最新消息片段 + 「查看完整讨论 →」跳转

### 5.6 ResultView `/results/:taskId`

- 顶大横幅：大字"你的职业决策"；副：收敛率、参与轮次、盲区数量
- 候选方案对比视图（3 列或 Tab）：每方案卡片显示标题、描述、置信度（条形进度）、支持者/反对者头像堆叠、里程碑 3 条、风险 3 条
- 盲区清单
- 底部两个大按钮：「导出 PDF 报告 📄」（P3）、「返回讨论 ←」

### 5.7 AgentsView / KbView / SettingsView / LoginView

- 全部使用 BaseCard / BaseButton / BaseInput 重刷
- LoginView：左右分栏，左图右表；左边放 SVG 插画（圆桌 5 Avatar 缩略）
- SettingsView：单列表单，包含主题切换、个人简介、账号安全
- AgentsView：Agent 卡片网格（含 5 预设 + 自定义）；创建/编辑用 el-dialog
- KbView：维持现有 API，仅改样式

## 6. 动效体系

统一原则：
- 所有进入/退出动画 200-240ms，`--ease-standard`
- 强调动画（轮次切换标语）480ms，`--ease-emphasized`
- Avatar 呼吸 3s 循环、闪烁光环 1.6s 循环
- 页面切换：Vue `<Transition name="page-fade">` 简单 fade + 8px translate

动画工具：
- 基础动画：Tailwind + CSS keyframes
- 复杂序列：`@vueuse/motion`（手动触发的入场/滚动触发）
- 光流线：SVG `stroke-dasharray` + `stroke-dashoffset` 动画
- 避免 GSAP（包大）

## 7. 数据 / API 变更

P1 不改后端 API，仅：
- 所有 `Agent` 对象在前端通过 `type` 字段映射到人格色
- 自定义 Agent 的 `avatarUrl` 若为空则渲染 CUSTOM 色的 SVG Avatar
- 无数据库 schema 变更

## 8. 文件变更清单

**新增**：
```
src/styles/tokens.css            设计 token（含日/夜两套 CSS 变量，通过 html[data-theme] 切换）
src/styles/base.css              base reset + typography
src/styles/element-overrides.css Element Plus 深色/浅色 override
src/styles/print.css             （P3 用，占位）
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
src/components/discussion/RoundTimeline.vue       新版；取代 RoundIndicator
src/components/discussion/RoundtableStage.vue     圆桌区
src/components/discussion/MessageDrawer.vue       右侧消息流抽屉
src/components/discussion/SpeechBubble.vue        头像下的发言气泡
src/components/discussion/ChallengeFlow.vue       光流 SVG
src/stores/theme.ts                               主题 store
src/composables/useAgentTheme.ts                  agent 色工具
src/utils/agent-meta.ts                           agent 符号 + label 映射
```

**修改**：
```
src/App.vue                      挂载主题、加载字体
src/main.ts                      引入 tokens.css
src/router/index.ts              若新增 route 需同步
src/views/HomeView.vue           全量重做
src/views/LoginView.vue          样式重做
src/views/TasksView.vue          样式 + 卡片网格重做
src/views/TaskView.vue           样式重做
src/views/DiscussionView.vue     替换内部 DiscussionPanel 为圆桌版
src/views/ResultView.vue         样式重做 + 横幅 + 对比视图
src/views/AgentsView.vue         样式重做
src/views/KbView.vue             样式重做
src/views/SettingsView.vue       增加主题切换 + 样式重做
src/components/layout/Sidebar.vue 重做
src/components/discussion/DiscussionPanel.vue 拆分为 RoundtableStage + MessageDrawer 调度器
src/components/discussion/AgentMessage.vue 样式重做，使用 AgentAvatar
tailwind.config.js               扩展 theme
index.html                       引入字体
```

**删除**：
```
src/components/discussion/RoundIndicator.vue   被 RoundTimeline 取代（或保留并重做，二选一——本 spec 选替换）
```

## 9. 测试

### 9.1 E2E（Playwright）
新增 `e2e-tests/tests/ui-brand.spec.js`：
- 首页渲染 Logo、Slogan、5 张 Expert 卡片
- 主题切换：点击 ThemeToggle 后 `<html data-theme="dark">`
- Discussion 页加载后圆桌渲染出 5 个 AgentAvatar
- Sidebar 折叠状态切换

### 9.2 视觉回归
- Playwright 截图 `ui-brand.spec.js` 关键 3 页面（Home/Discussion/Result）基准图
- 使用 `page.screenshot({ fullPage: true })` 落到 `e2e-tests/__screenshots__/`

### 9.3 组件单测
P1 不引入 Vitest（避免额外配置成本）；组件行为依赖 E2E 覆盖。

## 10. 风险与缓解

| 风险 | 影响 | 缓解 |
|---|---|---|
| Tailwind + Element Plus 样式冲突 | 可能产生类名污染 | 所有 Tailwind 类加前缀 `cm-`？评估后决定——预期不需要，Element 用 BEM 类。若冲突出现，在 `tailwind.config.js` 加 `prefix: 'tw-'` |
| AgentAvatar 在小尺寸下 SVG 细节模糊 | 观感下降 | 提供 30/48/72/120 四档，不做中间值 |
| 暗色模式下 Element Plus 原生控件 | Dialog/Form 色差 | 暗色下用 CSS override `.el-dialog { background: var(--bg-card); }` 等，统一在 `element-overrides.css` |
| 动画性能（多 AgentAvatar 同时呼吸） | 低端机卡顿 | 仅发言者+倾听者有动画；其他 idle；GPU 友好的 transform + opacity |

## 11. 完成定义（Definition of Done）

- [ ] 所有 9 个视图渲染无 console 错误
- [ ] 明/暗两套模式下所有页面 AA 级对比度达标
- [ ] DiscussionView 在 1920×1080 与 1440×900 两种分辨率下圆桌不溢出
- [ ] E2E ui-brand.spec.js 全部通过
- [ ] 手动跑完一次完整用户路径（登录→创建咨询→4 轮讨论→生成结果），UX 连贯无回弹
- [ ] 前端 `npm run build` 成功
- [ ] 截图 3 张关键页面入库作比赛素材
