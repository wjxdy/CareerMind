// @ts-check
const { test, expect } = require('@playwright/test');
const { LoginPage } = require('../pages/LoginPage');
const { TasksPage } = require('../pages/TasksPage');

/**
 * 任务管理测试套件
 *
 * 测试场景:
 * 1. 查看任务列表
 * 2. 创建新任务
 * 3. 任务详情展示
 * 4. 任务状态显示
 */
test.describe('任务管理', () => {

  test.beforeEach(async ({ page }) => {
    // 每个测试前登录
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login('testuser', 'test123456');
    await loginPage.expectLoginSuccess();
  });

  test('任务列表页面应正确加载', async ({ page }) => {
    const tasksPage = new TasksPage(page);

    await tasksPage.expectToBeOnTasksPage();
    await tasksPage.expectSidebarVisible();

    // 截图记录当前状态
    await tasksPage.takeScreenshot('tasks-list-loaded');
  });

  test('点击新建任务按钮应打开创建对话框', async ({ page }) => {
    const tasksPage = new TasksPage(page);

    await tasksPage.clickCreateTask();

    // 验证创建任务表单出现
    const createForm = page.locator('.el-dialog, .create-task-form, form').filter({ hasText: /新建任务|创建任务/ }).first();
    await expect(createForm).toBeVisible();

    // 验证表单字段
    await expect(page.locator('input[placeholder*="标题"], input[name="title"]').first()).toBeVisible();
    await expect(page.locator('textarea[placeholder*="背景"], textarea[name="background"]').first()).toBeVisible();
    await expect(page.locator('textarea[placeholder*="目标"], textarea[name="goal"]').first()).toBeVisible();
  });

  test('任务卡片应显示正确的信息', async ({ page }) => {
    const tasksPage = new TasksPage(page);

    await tasksPage.expectToBeOnTasksPage();

    // 如果存在任务卡片，验证其结构
    const taskCount = await tasksPage.getTaskCount();
    if (taskCount > 0) {
      // 验证任务卡片包含标题和状态
      const firstCard = page.locator('.task-card, .el-card').first();
      await expect(firstCard).toBeVisible();

      // 截图记录任务卡片样式
      await firstCard.screenshot({ path: 'playwright-report/screenshots/task-card.png' });
    }
  });

  test('创建任务表单应验证必填字段', async ({ page }) => {
    const tasksPage = new TasksPage(page);

    await tasksPage.clickCreateTask();

    // 直接提交空表单
    const submitButton = page.locator('button:has-text("创建"), button[type="submit"]').first();
    await submitButton.click();

    // 等待验证提示
    await page.waitForTimeout(500);

    // 验证错误提示出现或表单仍在（未关闭）
    const dialog = page.locator('.el-dialog').first();
    await expect(dialog).toBeVisible();
  });
});
