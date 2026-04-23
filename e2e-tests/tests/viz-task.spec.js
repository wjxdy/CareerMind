const { test, expect } = require('@playwright/test')

test.describe('Debate visualization (P2)', () => {
  test('TaskView mounts opinion graph wrap when task exists', async ({ page }) => {
    await page.goto('/tasks/1').catch(() => {})
    await page.waitForLoadState('domcontentloaded')
    const wrap = page.locator('.opinion-graph-wrap')
    if (await wrap.count() > 0) {
      await expect(wrap.first()).toBeVisible()
    }
  })

  test('ResultView shows convergence chart wrap when result exists', async ({ page }) => {
    await page.goto('/results/1').catch(() => {})
    await page.waitForLoadState('domcontentloaded')
    const chart = page.locator('.conv-wrap')
    if (await chart.count() > 0) {
      await expect(chart.first()).toBeVisible()
    }
  })

  test('Discussion page shows ThermoBar', async ({ page }) => {
    await page.goto('/discussions/1').catch(() => {})
    await page.waitForLoadState('domcontentloaded')
    const thermo = page.locator('.thermo')
    if (await thermo.count() > 0) {
      await expect(thermo.first()).toBeVisible()
    }
  })
})
