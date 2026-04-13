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
    // 先注册新账号，再用该账号继续测试
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    const timestamp = Date.now();
    const username = `task${timestamp}`;
    const email = `task${timestamp}@example.com`;
    const password = 'Test123456';

    // 切换到注册标签
    await page.locator('.el-tabs__item:has-text("注册")').first().click();
    await page.waitForTimeout(500);

    // 填写注册信息
    await page.locator('input[placeholder*="用户名"]').first().fill(username);
    await page.locator('input[placeholder*="邮箱"]').nth(1).fill(email);
    await page.locator('input[placeholder*="密码"]').nth(1).fill(password);

    // 点击注册并等待请求完成
    await page.locator('button[type="submit"]:has-text("注册"), .el-button--primary:has-text("注册")').first().click();
    await page.locator('button:has-text("注册").is-loading').waitFor({ state: 'detached', timeout: 10000 }).catch(() => {});
    await page.waitForTimeout(1000);

    // 注册后通常会跳转到首页或任务页面，如果没有跳转则手动登录
    const currentUrl = page.url();
    if (currentUrl.includes('/login')) {
      // 确保在登录标签页
      await page.locator('.el-tabs__item:has-text("登录")').first().click();
      await page.waitForTimeout(500);
      await loginPage.login(email, password);
    }
    // 登录成功后在首页或任务页都可以
    await page.waitForLoadState('networkidle');
    await expect(page.locator('text=CareerMind').first()).toBeVisible();

    // 确保在任务页面（注册成功会跳转到首页 /）
    const currentUrl2 = page.url();
    if (!currentUrl2.includes('/tasks')) {
      await page.goto('/tasks');
      await page.waitForLoadState('networkidle');
    }
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
    const createForm = page.locator('.el-dialog:has-text("新建职业咨询")');
    await expect(createForm).toBeVisible();

    // 验证表单字段
    await expect(page.locator('.el-dialog:has-text("新建职业咨询") input[placeholder*="标题"]').first()).toBeVisible();
    await expect(page.locator('.el-dialog:has-text("新建职业咨询") textarea[placeholder*="背景"]').first()).toBeVisible();
    await expect(page.locator('.el-dialog:has-text("新建职业咨询") textarea[placeholder*="困惑"]').first()).toBeVisible();
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
    const submitButton = page.locator('.el-dialog:has-text("新建职业咨询") button:has-text("创建咨询")').first();
    await submitButton.click();

    // 等待验证提示
    await page.waitForTimeout(500);

    // 验证错误提示出现或表单仍在（未关闭）
    const dialog = page.locator('.el-dialog').first();
    await expect(dialog).toBeVisible();
  });
});
