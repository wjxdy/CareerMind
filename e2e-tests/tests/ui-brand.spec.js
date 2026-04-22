const { test, expect } = require('@playwright/test')

test.describe('UI brand redesign (P1)', () => {
  test('Home page renders logo, hero title, experts grid with 5 agent cards', async ({ page }) => {
    await page.goto('/')
    await expect(page.locator('.brand-logo').first()).toBeVisible()
    await expect(page.getByRole('heading', { level: 1 })).toContainText('五位 AI 专家')
    const cards = page.locator('#experts .agent-card')
    await expect(cards).toHaveCount(5)
  })

  test('Theme toggle switches data-theme attribute on html', async ({ page }) => {
    await page.goto('/')
    const html = page.locator('html')
    const before = await html.getAttribute('data-theme')
    await page.locator('.theme-toggle').first().click()
    const after = await html.getAttribute('data-theme')
    expect(before).not.toBe(after)
  })

  test('Login page renders split layout with orb art', async ({ page }) => {
    await page.goto('/login')
    await expect(page.locator('.login-layout')).toBeVisible()
    await expect(page.locator('.orb')).toHaveCount(6)
    await expect(page.getByText('欢迎回来')).toBeVisible()
  })

  test('Login → register toggle works', async ({ page }) => {
    await page.goto('/login')
    await page.getByText('去注册').click()
    await expect(page.getByText('创建账户')).toBeVisible()
    await page.getByText('去登录').click()
    await expect(page.getByText('欢迎回来')).toBeVisible()
  })
})
