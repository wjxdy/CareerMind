// @ts-check
const { expect } = require('@playwright/test');

/**
 * 讨论页面对象模型
 *
 * 封装讨论页面的所有操作:
 * 1. 验证讨论页面加载
 * 2. 查看AI消息
 * 3. 控制讨论（开始/暂停/继续/下一轮）
 * 4. 验证WebSocket消息接收
 */
class DiscussionPage {
  constructor(page) {
    this.page = page;
    this.startButton = page.locator('button:has-text("开始讨论"), .el-button--primary').filter({ hasText: /开始|启动/ }).first();
    this.nextRoundButton = page.locator('button:has-text("下一轮"), button:has-text("质疑与挑战")').first();
    this.pauseButton = page.locator('button:has-text("暂停"), [title*="暂停"]').first();
    this.stopButton = page.locator('button:has-text("停止"), [title*="停止"]').first();
    this.messageList = page.locator('.message-list, .chat-messages, .discussion-messages').first();
    this.aiMessages = page.locator('.message-ai, .ai-message, [class*="ai"]').all();
    this.loadingIndicator = page.locator('.loading, .el-loading-spinner, .typing-indicator').first();
  }

  async expectToBeOnDiscussionPage() {
    await expect(this.page).toHaveURL(/\/discussion/);
    // 等待页面完全加载
    await this.page.waitForTimeout(1500);
  }

  async expectStartButtonVisible() {
    await expect(this.startButton).toBeVisible({ timeout: 5000 });
  }

  async clickStartDiscussion() {
    await this.startButton.click();
    // 等待讨论启动
    await this.page.waitForTimeout(2000);
  }

  async clickNextRound() {
    await this.nextRoundButton.click();
    // 等待下一轮开始
    await this.page.waitForTimeout(2000);
  }

  async clickPause() {
    await this.pauseButton.click();
    await this.page.waitForTimeout(500);
  }

  async getMessageCount() {
    // 等待消息加载
    await this.page.waitForTimeout(3000);
    const messages = await this.page.locator('.message, .chat-message, .el-card').count();
    return messages;
  }

  async expectAIMessagesVisible() {
    // AI消息可能需要时间生成，设置较长的超时
    const aiMessage = this.page.locator('.message-ai, .ai-message, .agent-message').first();
    await expect(aiMessage).toBeVisible({ timeout: 30000 });
  }

  async expectRoundIndicator(roundNumber) {
    const roundText = this.page.locator('.round-indicator, .current-round').filter({ hasText: `第${roundNumber}轮` });
    await expect(roundText).toBeVisible({ timeout: 5000 });
  }

  async takeScreenshot(name) {
    await this.page.screenshot({ path: `playwright-report/screenshots/${name}.png`, fullPage: true });
  }

  async waitForWebSocketMessage(timeout = 30000) {
    // 等待WebSocket消息或新消息出现
    await this.page.waitForTimeout(5000);
    const initialCount = await this.getMessageCount();

    const startTime = Date.now();
    while (Date.now() - startTime < timeout) {
      await this.page.waitForTimeout(1000);
      const currentCount = await this.getMessageCount();
      if (currentCount > initialCount) {
        return true;
      }
    }
    return false;
  }
}

module.exports = { DiscussionPage };
