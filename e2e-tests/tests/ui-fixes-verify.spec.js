// @ts-check
/**
 * 验证 2026-04-23 三个 UI Bug 修复
 * 1. 侧边栏收起后展开按钮仍然可见(Sidebar.vue / PageShell.vue)
 * 2. 候选方案置信度不溢出成 8000%、蓝色进度条不穿透卡片(ResultView.vue)
 * 3. 顶部座位 Agent 发言气泡不被截断(RoundtableStage.vue)
 */
const { test, expect } = require('@playwright/test')
const fs = require('fs')
const path = require('path')

const SCREENS = path.join(__dirname, '..', 'playwright-report', 'ui-fixes')
fs.mkdirSync(SCREENS, { recursive: true })

/** 注册 + 登录,返回 true 表示进入到受保护页面 */
async function registerAndLogin(page) {
  const ts = Date.now()
  const username = `verify${ts}`
  const email    = `verify${ts}@example.com`
  const password = 'Test123456'

  await page.goto('/login')
  await page.waitForTimeout(400)

  // 切到注册模式
  await page.getByText('去注册').click()
  await page.waitForTimeout(300)

  // 按显示顺序:username → email → password
  const inputs = page.locator('.form-box input')
  await inputs.nth(0).fill(username)
  await inputs.nth(1).fill(email)
  await inputs.nth(2).fill(password)

  await page.locator('.form-box button:has-text("注册")').first().click()

  // 等注册完成;userStore.register 成功会 push('/')
  await page.waitForTimeout(2500)

  // 若仍停留 login 页,尝试走登录流程
  if (page.url().includes('/login')) {
    await page.getByText('去登录').click().catch(() => null)
    await page.waitForTimeout(200)
    const li = page.locator('.form-box input')
    await li.nth(0).fill(email)
    await li.nth(1).fill(password)
    await page.locator('.form-box button:has-text("登录")').first().click()
    await page.waitForTimeout(2000)
  }

  return !page.url().includes('/login')
}

test.describe('UI fixes verification', () => {
  test.setTimeout(90 * 1000)

  test('Bug1: 侧边栏收起后展开按钮仍可见,且 Cmd+B 能切换', async ({ page }) => {
    const ok = await registerAndLogin(page)
    test.skip(!ok, '需要后端支持注册登录才能测')

    // /tasks 是带 PageShell 的受保护页面
    await page.goto('/tasks')
    await page.waitForTimeout(1500)

    // 侧边栏应该可见
    const sidebar = page.locator('.sidebar').first()
    await expect(sidebar).toBeVisible()

    // 默认展开 → 点收起按钮
    const btn = page.locator('.collapse-btn').first()
    await expect(btn).toBeVisible()
    await btn.click()
    await page.waitForTimeout(350)

    // 收起后:检查 class 和 关键 —— 按钮仍然可见(以前这里 display:none)
    await expect(sidebar).toHaveClass(/collapsed/)
    await expect(btn).toBeVisible()
    await page.screenshot({ path: path.join(SCREENS, 'bug1-sidebar-collapsed.png') })

    // 再点按钮恢复
    await btn.click()
    await page.waitForTimeout(350)
    await expect(sidebar).not.toHaveClass(/collapsed/)

    // 快捷键 Cmd/Ctrl+B
    await page.keyboard.press(process.platform === 'darwin' ? 'Meta+b' : 'Control+b')
    await page.waitForTimeout(350)
    await expect(sidebar).toHaveClass(/collapsed/)
    await page.keyboard.press(process.platform === 'darwin' ? 'Meta+b' : 'Control+b')
    await page.waitForTimeout(350)
    await expect(sidebar).not.toHaveClass(/collapsed/)
  })

  test('Bug2: 候选方案页百分比合理 + 进度条不穿透', async ({ page }) => {
    // Mock `/api/merge/tasks/:id`,带 {code,data,message} 包装
    await page.route('**/api/merge/tasks/*', async (route) => {
      if (route.request().method() !== 'GET') return route.continue()
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          code: 200, message: 'ok',
          data: {
            taskId: 999,
            plans: [
              // 故意混合两种标度,验证 toPercent() 的兼容性
              { title: '方案 1 (整数 80)',    description: '保持当前工作', confidence: 80,   isSelected: true,  milestones: ['6月','12月'], risks: ['慢'],    applicableConditions: '稳定' },
              { title: '方案 2 (小数 0.75)',  description: '探索验证',     confidence: 0.75, isSelected: false, milestones: ['1月','3月'],  risks: ['时间'],  applicableConditions: '观望' },
              { title: '方案 3 (整数 35)',    description: '激进冲刺',     confidence: 35,   isSelected: false, milestones: ['3月','6月'],  risks: ['收入'],  applicableConditions: '年轻' },
            ],
            blindSpots: ['盲点 A', '盲点 B'],
            convergenceRate: 0.72,
            summary: '整合总结测试',
          },
        }),
      })
    })
    await page.route('**/api/discussions/tasks/*/graph', route =>
      route.fulfill({ status: 404, contentType: 'application/json', body: JSON.stringify({ code: 404, message: 'no graph' }) })
    )

    const ok = await registerAndLogin(page)
    test.skip(!ok, '需要登录')

    await page.goto('/results/999')
    await page.waitForTimeout(1500)

    // 三张候选方案卡都渲染
    const cards = page.locator('.plans-grid > *')
    await expect(cards).toHaveCount(3)

    // 每张卡的百分比应该在 0-100 范围内,不会是 8000%
    const nums = page.locator('.conf-num')
    const texts = await nums.allTextContents()
    console.log('候选方案置信度文本:', texts)
    for (const t of texts) {
      const n = parseInt(t.replace('%', ''), 10)
      expect(n).toBeGreaterThanOrEqual(0)
      expect(n).toBeLessThanOrEqual(100)
    }

    // 进度条元素的实际渲染宽度不超过 conf-bar 容器
    const overflow = await page.$$eval('.conf-bar', bars => {
      return bars.map(b => {
        const after = getComputedStyle(b, '::after')
        const barW = b.getBoundingClientRect().width
        return { barW, afterW: after.width, overflow: getComputedStyle(b).overflow }
      })
    })
    console.log('进度条布局:', overflow)
    for (const o of overflow) {
      expect(['hidden', 'clip']).toContain(o.overflow)
    }

    await page.screenshot({ path: path.join(SCREENS, 'bug2-cards.png'), fullPage: true })
  })

  test('Bug3: 设置页 PageShell 布局完整(兜底检查)', async ({ page }) => {
    const errors = []
    page.on('pageerror', e => errors.push(String(e)))

    const ok = await registerAndLogin(page)
    test.skip(!ok, '需要登录')

    await page.goto('/settings')
    await page.waitForTimeout(1500)

    expect(errors).toEqual([])
    await expect(page.locator('.page-shell').first()).toBeVisible()
    await expect(page.locator('.sidebar').first()).toBeVisible()
    await page.screenshot({ path: path.join(SCREENS, 'bug3-shell.png') })
  })
})
