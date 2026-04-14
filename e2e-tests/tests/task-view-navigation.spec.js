// @ts-check
const { test, expect } = require('@playwright/test');
const { LoginPage } = require('../pages/LoginPage');

/**
 * TaskView 导航测试：验证查看讨论按钮
 */
test.describe('TaskView 导航', () => {

  test('已有任务的查看讨论按钮应正确跳转且无报错', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('testuser@example.com', 'test123456');
    await loginPage.expectLoginSuccess();

    // 直接访问 testuser 的已有任务详情页（任务 5 或 9）
    await page.goto('/tasks/9');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1500);

    // 收集控制台错误
    const consoleErrors = [];
    page.on('pageerror', err => consoleErrors.push(err.message));
    page.on('console', msg => {
      if (msg.type() === 'error') consoleErrors.push(msg.text());
    });

    // 点击 TaskView 中的查看讨论按钮
    await page.locator('.detail-actions button:has-text("查看讨论")').first().click();
    await page.waitForTimeout(2000);

    // 验证 URL
    await expect(page).toHaveURL(/\/discussions\/\d+/);

    // 检查控制台错误
    console.log('控制台错误:', consoleErrors);
    expect(consoleErrors.filter(e => !e.includes('WebSocket') && !e.includes('ws://'))).toEqual([]);
  });
});
