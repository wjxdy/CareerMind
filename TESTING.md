# CareerMind 自动化测试体系

本文档介绍 CareerMind 项目的完整自动化测试能力，让我能够自测前后端功能。

## 测试分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                    端到端测试 (E2E)                          │
│              Playwright + 浏览器自动化                       │
│          验证: 用户登录、任务管理、AI讨论全流程               │
├─────────────────────────────────────────────────────────────┤
│                   集成测试 (Integration)                     │
│                  Spring Boot Test + MockMvc                  │
│          验证: API接口、数据库交互、安全认证                  │
├─────────────────────────────────────────────────────────────┤
│                    单元测试 (Unit)                           │
│                     JUnit 5 + Mockito                        │
│          验证: 业务逻辑、服务层方法、边界条件                 │
└─────────────────────────────────────────────────────────────┘
```

## 已创建的测试内容

### 1. 后端单元测试

**文件位置**: `careermind-backend/src/test/java/com/careermind/`

| 测试类 | 覆盖功能 | 测试场景 |
|--------|---------|---------|
| `TaskServiceImplTest` | 任务服务 | 创建任务、查询任务、更新状态、删除任务 |
| `DiscussionEngineImplTest` | 讨论引擎 | 开始讨论、轮次切换、暂停恢复、边界条件 |
| `AgentControllerTest` | Agent API | 获取Agent列表、响应格式验证 |
| `TaskControllerTest` | 任务 API | 创建/查询/更新/删除任务的接口测试 |

**关键测试点**:
- ✅ 创建任务时验证Agent是否存在
- ✅ 讨论引擎的事务时序（修复了第二轮AI不回复的bug）
- ✅ 轮次切换的正确性（INDEPENDENT → CHALLENGE → REVISION → FINAL）

### 2. 端到端测试

**文件位置**: `e2e-tests/`

| 测试文件 | 测试场景 |
|---------|---------|
| `auth.spec.js` | 登录功能、未登录重定向、用户信息展示 |
| `tasks.spec.js` | 任务列表、创建任务、任务详情、表单验证 |
| `discussion.spec.js` | **AI讨论全流程**（最重要） |

**讨论功能专项测试**（修复验证）:
```javascript
// 第二轮讨论测试 - 验证之前的修复
test('进入第二轮（质疑与挑战）应继续接收AI消息', async ({ page }) => {
  // 1. 开始第一轮讨论
  // 2. 等待第一轮AI消息
  // 3. 点击"下一轮"进入第二轮
  // 4. 验证第二轮AI消息接收 ✅
});
```

## 快速开始

### 运行所有测试

```bash
cd /Users/xulei/Documents/CareerMind
./run-tests.sh
```

### 运行后端测试

```bash
cd careermind-backend
export JAVA_HOME=/opt/homebrew/Cellar/openjdk@17/17.0.16/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
mvn clean test
```

### 运行端到端测试

```bash
cd e2e-tests
npm install              # 首次运行
npx playwright install   # 安装浏览器

# 运行所有测试
npm test

# 运行特定浏览器
npm run test:chromium

# 可视化调试模式
npm run test:ui

# 有界面模式（可看浏览器操作）
npm run test:headed
```

## 测试验证流程

当我完成代码修改后，执行以下自测流程：

### 1. 单元测试验证
```bash
mvn test -Dtest=DiscussionEngineImplTest
```
验证业务逻辑正确性

### 2. 集成测试验证
```bash
mvn test -Dtest=AgentControllerTest,TaskControllerTest
```
验证API接口正常工作

### 3. 端到端测试验证
```bash
npx playwright test tests/discussion.spec.js --headed
```
**重点验证**: 第二轮讨论AI是否正常回复

### 4. 查看测试报告
```bash
# 后端测试覆盖率报告
open careermind-backend/target/site/jacoco/index.html

# 端到端测试报告（截图、视频）
npm run report
```

## 测试报告解读

### 后端测试报告
- **行覆盖率**: 业务代码被执行的比例
- **分支覆盖率**: 条件分支的覆盖情况
- **测试用例**: 通过的/失败的/跳过的

### 端到端测试报告
- **截图**: 失败时的页面状态
- **视频**: 失败时的操作录像
- **Trace**: 详细的操作日志和网络请求

## 页面对象模型 (POM)

测试代码使用POM模式，便于维护：

```javascript
// pages/DiscussionPage.js
class DiscussionPage {
  async clickNextRound() {
    await this.nextRoundButton.click();
  }
  
  async expectAIMessagesVisible() {
    // 等待AI消息出现（最长30秒）
  }
}
```

好处：
- 页面元素变化只需修改一处
- 测试用例更易读
- 复用页面操作逻辑

## 新增测试指南

### 添加后端单元测试

1. 在 `src/test/java/com/careermind/service/impl/` 创建测试类
2. 使用 `@ExtendWith(MockitoExtension.class)`
3. 使用 `@Mock` 和 `@InjectMocks` 注入依赖
4. 遵循 Given-When-Then 结构

### 添加端到端测试

1. 在 `e2e-tests/tests/` 创建 `.spec.js` 文件
2. 如需新页面操作，在 `pages/` 创建页面对象
3. 使用 `test.describe` 组织测试套件
4. 添加截图便于调试：`await page.screenshot({ path: 'debug.png' })`

## 自动化测试的价值

1. **快速验证修复**: 修复bug后运行测试，确保问题解决且没有引入新问题
2. **回归测试**: 新增功能后运行全量测试，确保现有功能正常
3. **文档作用**: 测试代码展示了功能预期行为
4. ** confidence**: 发布前全量测试通过增加信心

## 持续改进计划

- [ ] 增加更多边界条件测试
- [ ] 添加性能测试（API响应时间）
- [ ] 集成测试覆盖率检查到CI流程
- [ ] 添加移动端适配测试
- [ ] 添加视觉回归测试（截图对比）
