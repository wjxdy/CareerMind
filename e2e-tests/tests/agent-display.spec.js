const { test, expect } = require('@playwright/test');

test('任务详情页应显示参与专家', async ({ page }) => {
  await page.goto('/login');
  await page.waitForLoadState('networkidle');

  const ts = Date.now();
  const username = `agenttest${ts}`;
  const email = `agenttest${ts}@example.com`;
  const password = 'Test123456';

  await page.locator('.el-tabs__item:has-text("注册")').first().click();
  await page.waitForTimeout(500);
  await page.locator('input[placeholder*="用户名"]').first().fill(username);
  await page.locator('input[placeholder*="邮箱"]').nth(1).fill(email);
  await page.locator('input[placeholder*="密码"]').nth(1).fill(password);
  await page.locator('button[type="submit"]:has-text("注册"), .el-button--primary:has-text("注册")').first().click();
  await page.locator('button:has-text("注册").is-loading').waitFor({ state: 'detached', timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(1000);

  if (page.url().includes('/login')) {
    await page.locator('.el-tabs__item:has-text("登录")').first().click();
    await page.waitForTimeout(500);
    await page.locator('input[placeholder*="邮箱"], input[type="text"]').first().fill(email);
    await page.locator('input[type="password"]').first().fill(password);
    await page.locator('button:has-text("登录"), button[type="submit"]').first().click();
    await page.waitForTimeout(3000);
  }

  await page.waitForLoadState('networkidle');
  if (!page.url().includes('/tasks')) {
    await page.goto('/tasks');
    await page.waitForLoadState('networkidle');
  }

  await page.locator('button:has-text("新建咨询")').first().click();
  await page.waitForTimeout(500);
  await page.locator('.el-dialog:has-text("新建职业咨询") input[placeholder*="标题"]').first().fill(`Agent测试 ${ts}`);
  await page.locator('.el-dialog:has-text("新建职业咨询") textarea[placeholder*="背景"]').first().fill('背景');
  await page.locator('.el-dialog:has-text("新建职业咨询") textarea[placeholder*="困惑"]').first().fill('困惑');

  const agentCheckbox = page.locator('.el-dialog:has-text("新建职业咨询") .el-checkbox').first();
  if (await agentCheckbox.count() > 0) {
    await agentCheckbox.click();
  }

  await page.locator('.el-dialog:has-text("新建职业咨询") button:has-text("创建咨询")').first().click();
  await page.waitForTimeout(1500);

  if (!page.url().includes('/tasks/')) {
    await page.goto('/tasks');
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);
    await page.locator('.task-card, .el-card').first().click();
    await page.waitForTimeout(1000);
  }

  const url = page.url();
  const taskId = url.match(/\/(?:discussions|tasks)\/(\d+)/)?.[1];
  await page.goto(`/tasks/${taskId}`);
  await page.waitForLoadState('networkidle');
  await page.waitForTimeout(1000);

  await expect(page.locator('text=参与专家').first()).toBeVisible();
  const agentTags = page.locator('.agent-tags .agent-tag');
  await expect(agentTags.first()).toBeVisible();
  console.log('Agent tags count:', await agentTags.count());
});
