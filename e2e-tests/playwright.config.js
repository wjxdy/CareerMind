// @ts-check
const { defineConfig, devices } = require('@playwright/test');

/**
 * CareerMind E2E 测试配置
 *
 * 功能:
 * 1. 跨浏览器测试 (Chromium, Firefox, WebKit)
 * 2. 移动端视图测试
 * 3. 截图对比
 * 4. 测试报告生成
 */
module.exports = defineConfig({
  testDir: './tests',

  /* 每个测试的超时时间 */
  timeout: 30 * 1000,

  /* 全局设置 */
  expect: {
    /* 断言超时 */
    timeout: 5000,
    /* 截图对比阈值 */
    toHaveScreenshot: {
      maxDiffPixels: 100,
    },
  },

  /* 完全并行运行测试 */
  fullyParallel: true,

  /* 失败时不停止 */
  forbidOnly: !!process.env.CI,

  /* 重试次数 */
  retries: process.env.CI ? 2 : 0,

  /* 并行工作数 */
  workers: process.env.CI ? 1 : undefined,

  /* 报告器配置 */
  reporter: [
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
    ['json', { outputFile: 'playwright-report/test-results.json' }],
    ['list'],
  ],

  /* 共享配置 */
  use: {
    /* 基础 URL */
    baseURL: process.env.BASE_URL || 'http://localhost:5173',

    /* 截图设置 */
    screenshot: 'only-on-failure',

    /* 视频录制 */
    video: 'retain-on-failure',

    /* 追踪 */
    trace: 'on-first-retry',

    /* 视口 */
    viewport: { width: 1280, height: 720 },

    /* 动作超时 */
    actionTimeout: 10000,
  },

  /* 项目配置 */
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    /* 测试移动端视图 */
    {
      name: 'Mobile Chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'Mobile Safari',
      use: { ...devices['iPhone 12'] },
    },
  ],

  /* 本地开发服务器配置 */
  webServer: process.env.SKIP_WEBSERVER ? undefined : {
    command: 'cd /Users/xulei/Documents/CareerMind/careermind-frontend && npm run dev',
    url: 'http://localhost:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
});
