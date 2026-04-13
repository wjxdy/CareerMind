// @ts-check
const { expect } = require('@playwright/test');

/**
 * 任务列表页面对象模型
 *
 * 封装任务列表页面的所有操作:
 * 1. 验证页面加载
 * 2. 创建新任务
 * 3. 查看任务详情
 * 4. 进入讨论页面
 */
class TasksPage {
  constructor(page) {
    this.page = page;
    this.createTaskButton = page.locator('main.page-content button:has-text("新建咨询")').first();
    this.taskCards = page.locator('.task-card, .el-card, [class*="task"]').all();
    this.sidebar = page.locator('.sidebar, aside').first();
    this.userInfo = page.locator('.user-info, .el-dropdown').first();
  }

  async expectToBeOnTasksPage() {
    await expect(this.page).toHaveURL(/\/tasks/);
    // 等待页面内容加载
    await this.page.waitForTimeout(1000);
  }

  async expectSidebarVisible() {
    await expect(this.sidebar).toBeVisible();
  }

  async expectUserInfoVisible() {
    await expect(this.userInfo).toBeVisible();
  }

  async clickCreateTask() {
    await this.createTaskButton.click();
    // 等待创建任务对话框或页面跳转
    await this.page.waitForTimeout(500);
  }

  async getTaskCount() {
    const cards = await this.page.locator('.task-card, .el-card').count();
    return cards;
  }

  async clickTaskByTitle(title) {
    const taskCard = this.page.locator('.task-card, .el-card').filter({ hasText: title }).first();
    await taskCard.click();
    await this.page.waitForTimeout(500);
  }

  async clickDiscussionButton() {
    const discussBtn = this.page.locator('button:has-text("开始讨论"), button:has-text("继续讨论"), .el-button:has-text("讨论")').first();
    await discussBtn.click();
    await this.page.waitForTimeout(1000);
  }

  async takeScreenshot(name) {
    await this.page.screenshot({ path: `playwright-report/screenshots/${name}.png`, fullPage: true });
  }
}

module.exports = { TasksPage };
