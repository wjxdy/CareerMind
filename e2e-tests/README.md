# CareerMind 自动化测试套件

本项目提供 CareerMind 的完整自动化测试能力，包括单元测试、集成测试和端到端测试。

## 测试架构

```
CareerMind/
├── careermind-backend/
│   └── src/test/java/
│       ├── com/careermind/service/impl/    # 单元测试
│       └── com/careermind/controller/      # 集成测试
│
└── e2e-tests/                               # 端到端测试
    ├── tests/
    │   ├── auth.spec.js      # 登录功能测试
    │   ├── tasks.spec.js     # 任务管理测试
    │   └── discussion.spec.js # AI讨论功能测试
    └── pages/                # 页面对象模型
```

## 快速开始

### 1. 安装依赖

```bash
cd e2e-tests
npm install
npm run install:browsers  # 安装浏览器
```

### 2. 运行测试

```bash
# 运行所有测试
npm test

# 运行特定浏览器测试
npm run test:chromium
npm run test:firefox

# 带UI的测试模式
npm run test:ui

# 调试模式
npm run test:debug
```

### 3. 查看测试报告

```bash
npm run report
```

报告将包含：
- 测试执行结果
- 失败截图
- 视频录制（仅失败时）
- 性能追踪

## 后端单元测试

### 运行单元测试

```bash
cd careermind-backend
mvn test
```

### 运行特定测试类

```bash
mvn test -Dtest=TaskServiceImplTest
mvn test -Dtest=DiscussionEngineImplTest
mvn test -Dtest=AgentControllerTest
```

### 生成测试报告

```bash
mvn test jacoco:report
# 报告在 target/site/jacoco/index.html
```

## 关键测试用例

### 讨论引擎测试 (discussion.spec.js)

这是最重要的测试，验证AI讨论功能是否正常工作：

1. **第一轮讨论测试**: 验证开始讨论后AI能正常回复
2. **第二轮讨论测试**: 验证修复后的第二轮（质疑与挑战）功能
3. **WebSocket测试**: 验证实时消息接收
4. **状态控制测试**: 验证暂停/恢复功能

### 运行讨论功能专项测试

```bash
npx playwright test tests/discussion.spec.js --headed
```

## 页面对象模型 (POM)

使用POM模式封装页面操作，提高测试可维护性：

- `LoginPage`: 登录页面操作
- `TasksPage`: 任务列表页面操作
- `DiscussionPage`: 讨论页面操作

## 环境变量

```bash
# 指定后端API地址
export BASE_URL=http://localhost:5173

# 跳过启动本地服务器（如果已手动启动）
export SKIP_WEBSERVER=true

# CI模式（无头浏览器，更严格的超时）
export CI=true
```

## 截图和录制

测试失败时自动保存：
- 截图: `playwright-report/screenshots/`
- 视频: `playwright-report/videos/`
- 追踪: `playwright-report/trace/`

## 持续集成

可在CI/CD流程中集成：

```yaml
# GitHub Actions 示例
- name: Run E2E Tests
  run: |
    cd e2e-tests
    npm ci
    npx playwright install --with-deps
    npx playwright test
```

## 调试技巧

1. **查看测试日志**: `DEBUG=pw:api npx playwright test`
2. **慢动作回放**: 在配置中设置 `slowMo: 1000`
3. **可视化调试**: 使用 `npx playwright test --ui`

## 新增测试

1. 在 `tests/` 目录创建新的 `.spec.js` 文件
2. 如需新页面操作，在 `pages/` 创建页面对象
3. 遵循现有的命名规范和结构
