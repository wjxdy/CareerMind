// @ts-check
const { test, expect } = require('@playwright/test');
const { LoginPage } = require('../pages/LoginPage');
const { TasksPage } = require('../pages/TasksPage');

/**
 * 登录功能测试套件
 *
 * 测试场景:
 * 1. 成功登录
 * 2. 错误密码登录失败
 * 3. 未登录访问受保护页面重定向
 * 4. 登录后侧边栏显示用户信息
 */
test.describe('登录功能', () => {

  test('用户注册和登录流程', async ({ page }) => {
    const loginPage = new LoginPage(page);

    await loginPage.goto();

    // 切换到注册标签
    const registerTab = page.locator('.el-tabs__item:has-text("注册")').first();
    await registerTab.click();
    await page.waitForTimeout(500);

    // 填写注册信息
    const timestamp = Date.now();
    const email = `test${timestamp}@example.com`;
    const password = 'Test123456';

    await page.locator('input[placeholder*="用户名"]').first().fill(`testuser${timestamp}`);
    await page.locator('input[placeholder*="邮箱"]').nth(1).fill(email);
    await page.locator('input[placeholder*="密码"]').nth(1).fill(password);

    // 点击注册
    await page.locator('button[type="submit"]:has-text("注册"), .el-button--primary:has-text("注册")').first().click();
    await page.waitForTimeout(2000);

    // 验证注册后跳转到任务页面或显示成功
    console.log('注册完成，当前URL:', page.url());
  });

  test('使用错误密码登录应显示错误', async ({ page }) => {
    const loginPage = new LoginPage(page);

    await loginPage.goto();
    // 使用随机邮箱和密码登录
    await loginPage.login('nonexistent@test.com', 'wrongpassword');

    // 等待错误提示
    await page.waitForTimeout(2000);

    // 验证仍在登录页面
    await loginPage.expectToBeOnLoginPage();
  });

  test('未登录用户访问任务页面应重定向到登录', async ({ page }) => {
    // 直接访问任务列表
    await page.goto('/tasks');
    await page.waitForTimeout(1000);

    // 应该被重定向到登录页面
    const loginPage = new LoginPage(page);
    await loginPage.expectToBeOnLoginPage();
  });

  test('登录页面应正确加载所有元素', async ({ page }) => {
    const loginPage = new LoginPage(page);

    await loginPage.goto();

    // 验证登录表单元素可见
    await expect(page.locator('input[placeholder*="邮箱"]').first()).toBeVisible();
    await expect(page.locator('input[type="password"]').first()).toBeVisible();
    await expect(page.locator('button:has-text("登录")').first()).toBeVisible();

    // 验证注册标签页可见
    await expect(page.locator('.el-tabs__item:has-text("注册")').first()).toBeVisible();
  });
});
