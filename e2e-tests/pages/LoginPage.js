// @ts-check
const { expect } = require('@playwright/test');

/**
 * 登录页面对象模型
 *
 * 封装登录页面的所有操作:
 * 1. 访问登录页面
 * 2. 输入用户名密码
 * 3. 点击登录
 * 4. 验证登录成功/失败
 */
class LoginPage {
  constructor(page) {
    this.page = page;
    this.emailInput = page.locator('input[placeholder*="邮箱"], input[type="text"]').first();
    this.passwordInput = page.locator('input[type="password"]').first();
    this.loginButton = page.locator('button:has-text("登录"), button[type="submit"]').first();
    this.errorMessage = page.locator('.el-message--error, .error-message, [role="alert"]').first();
  }

  async goto() {
    await this.page.goto('/login');
    await this.page.waitForLoadState('networkidle');
  }

  async fillEmail(email) {
    await this.emailInput.fill(email);
  }

  async fillPassword(password) {
    await this.passwordInput.fill(password);
  }

  async clickLogin() {
    await this.loginButton.click();
  }

  async login(email, password) {
    await this.fillEmail(email);
    await this.fillPassword(password);
    await this.clickLogin();
    // 等待登录请求完成
    await this.page.waitForTimeout(1000);
  }

  async expectLoginSuccess() {
    // 登录成功后应该跳转到任务列表页面
    await expect(this.page).toHaveURL(/\/tasks/);
  }

  async expectLoginError() {
    // 登录失败应该显示错误消息
    await expect(this.errorMessage).toBeVisible({ timeout: 5000 });
  }

  async expectToBeOnLoginPage() {
    await expect(this.page).toHaveURL(/\/login/);
    await expect(this.loginButton).toBeVisible();
  }
}

module.exports = { LoginPage };
