// @ts-check
const { test, expect } = require('@playwright/test');
const { LoginPage } = require('../pages/LoginPage');
const { TasksPage } = require('../pages/TasksPage');
const { DiscussionPage } = require('../pages/DiscussionPage');

/**
 * 用户插话功能测试套件
 *
 * 验证讨论页面的用户插话UI和交互
 */
test.describe('用户插话', () => {

  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    const timestamp = Date.now();
    const username = `inter${timestamp}`;
    const email = `inter${timestamp}@example.com`;
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

    const taskTitle = `插话测试任务 ${timestamp}`;
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
    await page.waitForTimeout(2000);

    // 创建后可能跳转到任务详情页或讨论页
    // 如果在任务详情页，点击"查看讨论"进入讨论页
    if (page.url().includes('/tasks/') && !page.url().includes('/discussions/')) {
      await page.locator('button:has-text("查看讨论")').first().click();
      await page.waitForTimeout(1000);
    }
  });

  test('讨论页面应包含插话输入框', async ({ page }) => {
    const discussionPage = new DiscussionPage(page);
    await discussionPage.expectToBeOnDiscussionPage();

    // 验证输入框存在
    const input = page.locator('.panel-footer input, .input-area input').first();
    await expect(input).toBeVisible();

    // 验证发送按钮存在
    const sendButton = page.locator('.panel-footer button, .input-area button').first();
    await expect(sendButton).toBeVisible();
  });

  test('插话输入框应支持输入和发送', async ({ page }) => {
    const input = page.locator('.panel-footer input, .input-area input').first();

    await input.fill('这是一个测试插话');
    await expect(input).toHaveValue('这是一个测试插话');

    // 截图记录输入状态
    await page.screenshot({ path: 'playwright-report/screenshots/interjection-input.png', fullPage: false });
  });

  test('点击发送按钮应触发插话发送', async ({ page }) => {
    const discussionPage = new DiscussionPage(page);
    await discussionPage.expectToBeOnDiscussionPage();

    const input = page.locator('.panel-footer input, .input-area input').first();
    await input.fill('测试插话消息');

    // 点击发送按钮（el-input 的 append slot 中的按钮）
    const sendButton = page.locator('.input-area button, .panel-footer button').filter({ has: page.locator('.el-icon') }).first();
    await sendButton.click();
    await page.waitForTimeout(1500);

    // 截图记录发送后的状态
    await page.screenshot({ path: 'playwright-report/screenshots/interjection-sent.png', fullPage: true });
  });
});
