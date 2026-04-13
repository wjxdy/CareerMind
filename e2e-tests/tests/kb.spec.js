// @ts-check
const { test, expect } = require('@playwright/test');
const { LoginPage } = require('../pages/LoginPage');
const fs = require('fs');
const path = require('path');

/**
 * RAG 知识库功能端到端测试
 *
 * 测试场景:
 * 1. 创建知识库
 * 2. 上传 TXT 文档
 * 3. 等待文档处理完成
 * 4. 测试语义检索返回结果
 * 5. 新建咨询关联知识库
 */

test.describe('知识库管理', () => {
  test.beforeEach(async ({ page }) => {
    // 先注册新账号，再用该账号继续测试
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    const timestamp = Date.now();
    const username = `ragtest${timestamp}`;
    const email = `ragtest${timestamp}@example.com`;
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
  });

  test('完整知识库流程：创建、上传文档、检索', async ({ page }) => {
    // 1. 进入知识库页面
    await page.goto('/kb');
    await page.waitForLoadState('networkidle');

    // 验证知识库页面加载
    await expect(page.locator('text=知识库管理').first()).toBeVisible();

    // 2. 创建知识库
    await page.locator('button:has-text("新建知识库")').click();
    await expect(page.locator('.el-dialog:has-text("新建知识库")')).toBeVisible();

    const kbName = `测试知识库-${Date.now()}`;
    await page.locator('.el-dialog input[placeholder*="名称"]').fill(kbName);
    await page.locator('.el-dialog button:has-text("创建")').click();

    // 等待对话框关闭（API 请求完成）
    await expect(page.locator('.el-dialog:has-text("新建知识库")')).not.toBeVisible({ timeout: 10000 });
    await expect(page.locator(`text=${kbName}`)).toBeVisible({ timeout: 5000 });

    // 3. 上传测试文档
    // 创建临时测试文件
    const testContent = '职业规划建议：\n1. 技术路线适合喜欢深入钻研的人\n2. 管理路线需要培养沟通和协调能力\n3. 创业路线风险高但回报也大\n4. 每个路线都需要持续学习新技能';
    const tmpDir = path.join(__dirname, '../tmp');
    if (!fs.existsSync(tmpDir)) fs.mkdirSync(tmpDir, { recursive: true });
    const testFilePath = path.join(tmpDir, 'career-guide.txt');
    fs.writeFileSync(testFilePath, testContent);

    // 点击当前知识库卡片上的"上传文档"按钮
    const kbCard = page.locator('.kb-card', { hasText: kbName });
    await kbCard.locator('button:has-text("上传文档")').click();

    // 等待上传对话框
    await expect(page.locator('.el-dialog:has-text("上传文档")')).toBeVisible();

    // Element Plus upload 组件：找到 input[type=file] 并设置文件
    const fileInput = page.locator('.el-dialog input[type="file"]');
    await fileInput.setInputFiles(testFilePath);

    await page.locator('.el-dialog button:has-text("上传")').click();

    // 等待上传成功提示
    await page.waitForTimeout(2000);

    // 4. 查看文档列表，等待处理完成
    await kbCard.locator('button:has-text("查看文档")').click();
    await expect(page.locator('.el-drawer:has-text("文档列表")')).toBeVisible();

    // 轮询等待文档状态变为"完成"（最多60秒）
    let completed = false;
    for (let i = 0; i < 30; i++) {
      await page.waitForTimeout(2000);
      // 刷新文档列表（关闭再打开抽屉）
      await page.locator('.el-drawer__close-btn, .el-drawer__header .el-icon').first().click();
      await page.waitForTimeout(500);
      await kbCard.locator('button:has-text("查看文档")').click();
      await page.waitForTimeout(500);

      const statusCell = page.locator('.el-table__row .el-tag--success:has-text("完成")').first();
      if (await statusCell.isVisible().catch(() => false)) {
        completed = true;
        break;
      }
    }
    expect(completed).toBe(true);

    // 关闭抽屉
    await page.locator('.el-drawer__close-btn, .el-drawer__header .el-icon').first().click();
    await page.waitForTimeout(500);

    // 5. 测试语义检索
    await kbCard.locator('button:has-text("测试检索")').click();
    await expect(page.locator('.el-dialog:has-text("测试知识库检索")')).toBeVisible();

    await page.locator('.el-dialog:has-text("测试知识库检索") textarea').fill('什么职业路线适合我');
    await page.locator('.el-dialog:has-text("测试知识库检索") button:has-text("检索")').click();

    // 等待检索结果出现（最多30秒）
    const resultItem = page.locator('.result-item').first();
    await expect(resultItem).toBeVisible({ timeout: 30000 });

    // 验证结果包含来源和分数
    await expect(page.locator('.result-header:has-text("career-guide.txt")').first()).toBeVisible();

    // 关闭测试对话框
    await page.locator('.el-dialog:has-text("测试知识库检索") .el-dialog__headerbtn').click();

    // 6. 清理：删除知识库
    await kbCard.locator('.delete-icon').click();
    await page.locator('.el-message-box__btns button.el-button--primary, .el-message-box__btns button:has-text("OK"), .el-message-box__btns button:has-text("确定")').click();
    await page.waitForTimeout(1000);
    await expect(page.locator(`text=${kbName}`)).not.toBeVisible();

    // 清理临时文件
    fs.rmSync(tmpDir, { recursive: true, force: true });
  });

  test('新建咨询时可以关联知识库', async ({ page }) => {
    // 进入知识库页面先创建一个知识库
    await page.goto('/kb');
    await page.waitForLoadState('networkidle');

    await page.locator('button:has-text("新建知识库")').click();
    const kbName = `咨询测试知识库-${Date.now()}`;
    await page.locator('.el-dialog input[placeholder*="名称"]').fill(kbName);
    await page.locator('.el-dialog button:has-text("创建")').click();
    await page.waitForTimeout(1000);

    // 进入任务页面新建咨询
    await page.goto('/tasks');
    await page.waitForLoadState('networkidle');

    await page.locator('main.page-content button:has-text("新建咨询")').click();
    await expect(page.locator('.el-dialog:has-text("新建职业咨询")')).toBeVisible();

    // 验证知识库选择框存在
    await expect(page.locator('text=关联知识库（可选）').first()).toBeVisible();

    // 选择刚才创建的知识库
    await page.locator('.el-select:has-text("选择知识库为讨论提供背景资料")').click();
    await page.locator(`.el-select-dropdown__item:has-text("${kbName}")`).click();

    // 填写表单并创建
    await page.locator('.el-dialog input[placeholder*="标题"]').fill(`RAG测试咨询-${Date.now()}`);
    await page.locator('.el-dialog textarea[placeholder*="困惑"]').fill('测试RAG知识库关联');

    // 确保有选中的 Agent
    const selectedAgents = await page.locator('.agent-option.selected').count();
    if (selectedAgents === 0) {
      await page.locator('.agent-option').first().click();
    }

    await page.locator('.el-dialog button:has-text("创建咨询")').click();
    await page.waitForTimeout(2000);

    // 验证跳转到讨论页面
    await expect(page).toHaveURL(/\/discussions\/\d+/);

    // 回到知识库页面清理
    await page.goto('/kb');
    const kbCard = page.locator('.kb-card', { hasText: kbName });
    await kbCard.locator('.delete-icon').click();
    await page.locator('.el-message-box__btns button.el-button--primary, .el-message-box__btns button:has-text("OK"), .el-message-box__btns button:has-text("确定")').click();
  });
});
