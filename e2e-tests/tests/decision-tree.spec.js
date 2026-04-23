// @ts-check
const { test, expect } = require('@playwright/test');
const { LoginPage } = require('../pages/LoginPage');
const { TasksPage } = require('../pages/TasksPage');

/**
 * 决策树可视化测试套件
 *
 * 验证任务详情页的决策树展示功能
 */
test.describe('决策树可视化', () => {

  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    const timestamp = Date.now();
    const username = `tree${timestamp}`;
    const email = `tree${timestamp}@example.com`;
    const password = 'Test123456';

    // 切换到注册标签
    await page.locator('.el-tabs__item:has-text("注册")').first().click();
    await page.waitForTimeout(500);

    // 填写注册信息
    await page.locator('input[placeholder*="用户名"]').first().fill(username);
    await page.locator('input[placeholder*="邮箱"]').nth(1).fill(email);
    await page.locator('input[placeholder*="密码"]').nth(1).fill(password);

    // 点击注册
    await page.locator('button[type="submit"]:has-text("注册"), .el-button--primary:has-text("注册")').first().click();
    await page.locator('button:has-text("注册").is-loading').waitFor({ state: 'detached', timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(1000);

    const currentUrl = page.url();
    if (currentUrl.includes('/login')) {
      await page.locator('.el-tabs__item:has-text("登录")').first().click();
      await page.waitForTimeout(500);
      await loginPage.login(email, password);
    }

    await page.waitForLoadState('networkidle');
    await expect(page.locator('text=CareerMind').first()).toBeVisible();

    if (!page.url().includes('/tasks')) {
      await page.goto('/tasks');
      await page.waitForLoadState('networkidle');
    }

    // 创建一个新任务用于测试
    const tasksPage = new TasksPage(page);
    await tasksPage.clickCreateTask();

    const taskTitle = `决策树测试任务 ${timestamp}`;
    await page.locator('.el-dialog:has-text("新建职业咨询") input[placeholder*="标题"]').first().fill(taskTitle);
    await page.locator('.el-dialog:has-text("新建职业咨询") textarea[placeholder*="背景"]').first().fill('测试背景信息');
    await page.locator('.el-dialog:has-text("新建职业咨询") textarea[placeholder*="困惑"]').first().fill('测试职业困惑');

    // 选择至少一个Agent
    const agentCheckbox = page.locator('.el-dialog:has-text("新建职业咨询") .el-checkbox').first();
    if (await agentCheckbox.count() > 0) {
      await agentCheckbox.click();
    }

    // 提交创建
    await page.locator('.el-dialog:has-text("新建职业咨询") button:has-text("创建咨询")').first().click();
    await page.waitForTimeout(1500);

    // 创建后跳转到讨论页或任务详情页，需要回到任务列表
    if (!page.url().includes('/tasks')) {
      await page.goto('/tasks');
      await page.waitForLoadState('networkidle');
      await page.waitForTimeout(1000);
    }
  });

  test('任务详情页应显示决策链路区块', async ({ page }) => {
    // 点击第一个任务卡片
    const firstCard = page.locator('.task-card, .el-card').first();
    await expect(firstCard).toBeVisible({ timeout: 5000 });
    await firstCard.click();
    await page.waitForTimeout(1000);

    // 从 URL 提取 taskId 并导航到任务详情页
    const url = page.url();
    const taskId = url.match(/\/(?:discussions|tasks)\/(\d+)/)?.[1];
    expect(taskId).toBeTruthy();

    await page.goto(`/tasks/${taskId}`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // 验证决策链路标题存在
    await expect(page.locator('text=决策链路').first()).toBeVisible();

    // 截图记录
    const decisionTreeSection = page.locator('.decision-tree-section').first();
    await expect(decisionTreeSection).toBeVisible();
    await decisionTreeSection.screenshot({ path: 'playwright-report/screenshots/decision-tree-section.png' });
  });

  test('决策树空状态应提示讨论启动后生成', async ({ page }) => {
    const firstCard = page.locator('.task-card, .el-card').first();
    await expect(firstCard).toBeVisible({ timeout: 5000 });
    await firstCard.click();
    await page.waitForTimeout(1000);

    const url = page.url();
    const taskId = url.match(/\/(?:discussions|tasks)\/(\d+)/)?.[1];
    expect(taskId).toBeTruthy();

    await page.goto(`/tasks/${taskId}`);
    await page.waitForLoadState('networkidle');
    await page.waitForTimeout(1000);

    // 新任务没有讨论，应该显示空状态提示
    await expect(page.locator('text=讨论启动后将生成决策树').first()).toBeVisible();

    // 验证包含查看讨论按钮
    await expect(page.locator('.decision-tree-section button:has-text("查看讨论")').first()).toBeVisible();
  });
});
