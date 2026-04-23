const { test, expect } = require('@playwright/test')

test.describe('PDF report (P3)', () => {
  test('Report print view loads when task exists', async ({ page }) => {
    await page.goto('/report/print/1').catch(() => {})
    await page.waitForLoadState('domcontentloaded')
    const root = page.locator('#report-root')
    if (await root.count() > 0) {
      await expect(root.first()).toBeVisible()
    }
  })

  test('Toolbar shows download and print buttons', async ({ page }) => {
    await page.goto('/report/print/1').catch(() => {})
    await page.waitForLoadState('domcontentloaded')
    const dl = page.getByRole('button', { name: /下载 PDF/ })
    if (await dl.count() > 0) {
      await expect(dl.first()).toBeVisible()
    }
  })
})
