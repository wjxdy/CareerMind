# 🎯 CareerMind - 多Agent职业发展决策系统

一个基于多智能体协作的个性化职业规划顾问系统。让不同视角的AI Agent就用户的职业困惑进行深度讨论，结合知识库检索增强（RAG），最终输出可执行的规划方案。

## 📁 项目结构

```
CareerMind/
├── careermind-backend/       # Spring Boot 后端
│   ├── src/main/java/com/careermind/
│   │   ├── controller/       # REST API 控制器
│   │   ├── service/          # 业务逻辑层
│   │   ├── domain/           # 领域实体
│   │   ├── repository/       # 数据访问层
│   │   ├── config/           # 配置类
│   │   ├── client/           # 外部服务客户端（RAG）
│   │   └── websocket/        # WebSocket处理器
│   └── pom.xml
│
├── careermind-frontend/      # Vue3 前端
│   ├── src/
│   │   ├── views/            # 页面视图
│   │   ├── components/       # 组件
│   │   ├── stores/           # Pinia状态管理
│   │   ├── api/              # API接口
│   │   └── types/            # TypeScript类型
│   └── package.json
│
├── careermind-rag/           # Rust RAG 服务
│   ├── src/
│   │   ├── embedding/        # 向量模型客户端（Qwen/Gemini）
│   │   ├── services/         # 文档处理、检索服务
│   │   ├── qdrant/           # Qdrant 向量数据库客户端
│   │   ├── parser/           # 文档解析器
│   │   ├── chunking/         # 文本切分
│   │   └── routes/           # HTTP 路由
│   ├── Cargo.toml
│   └── docker-compose.yml
│
└── e2e-tests/                # Playwright 端到端测试
    ├── tests/
    └── playwright.config.js
```

## 🚀 快速开始

### 环境要求

- Java 17+
- MySQL 8.0+
- Redis 6.0+
- Node.js 18+
- Rust 1.75+
- Docker Desktop

### 1. 启动 Qdrant 向量数据库

```bash
cd careermind-rag
docker compose up -d qdrant
```

Qdrant 将在 http://localhost:6333 启动。

### 2. 启动 RAG 服务

```bash
cd careermind-rag

# 复制环境变量模板并编辑
cp .env.example .env
# 配置 DATABASE_URL、QWEN_API_KEY 等

cargo run
```

RAG 服务将在 http://localhost:3000 启动。

### 3. 启动 Spring Boot 后端

```bash
cd careermind-backend

# 创建数据库
mysql -u root -p -e "CREATE DATABASE careermind CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 安装依赖并启动
mvn clean install
mvn spring-boot:run
```

后端服务将在 http://localhost:8080 启动。

### 4. 启动前端

```bash
cd careermind-frontend

# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端服务将在 http://localhost:5173 启动。

### 5. 初始化数据

启动后端后，访问以下接口初始化预设Agent：

```bash
curl -X POST http://localhost:8080/api/agents/init
```

## 🎯 核心功能

### 1. 多Agent讨论系统
- 5个预设Agent角色：行业分析师、能力评估师、风险警示者、机会挖掘者、价值观拷问者
- 4轮深度讨论流程：独立诊断 → 质疑挑战 → 修正完善 → 最终陈述
- 支持自定义Agent创建

### 2. RAG 知识库
- 创建个人/公共知识库
- 支持上传 PDF 文档，后台自动解析、切分、向量化
- 咨询时可关联知识库，Agent 讨论中自动检索相关文档片段
- 向量模型支持 Qwen `text-embedding-v3`（默认）和 Gemini 可选切换

### 3. 实时讨论展示
- WebSocket实时推送Agent发言
- 类Kimi的简洁对话界面
- 轮次进度指示器
- 播放/暂停控制

### 4. Merge整合
- 自动提取各方观点
- 生成2-3个候选方案
- 标注置信度和认知盲区

## 🛠️ 技术栈

### Java 后端
- Java 17
- Spring Boot 3.x
- Spring Data JPA
- Spring WebSocket
- MySQL 8
- Redis
- JWT 认证

### RAG 服务
- Rust + Axum + tokio
- sqlx (MySQL)
- Qdrant 向量数据库
- Qwen / Gemini Embedding API
- pdf-extract（PDF 解析）

### 前端
- Vue 3 + TypeScript
- Vite
- Element Plus
- Pinia
- Tailwind CSS
- ECharts

### 测试
- Playwright（跨浏览器 E2E 测试）

## 📚 API 文档

### 认证接口
- `POST /api/auth/register` - 用户注册
- `POST /api/auth/login` - 用户登录

### Agent接口
- `GET /api/agents/preset` - 获取预设Agent
- `GET /api/agents` - 获取可用Agent
- `POST /api/agents` - 创建Agent
- `PUT /api/agents/{id}` - 更新Agent
- `DELETE /api/agents/{id}` - 删除Agent

### Task接口
- `GET /api/tasks` - 获取用户任务列表
- `GET /api/tasks/{id}` - 获取任务详情
- `POST /api/tasks` - 创建任务（可携带 `kbId` 关联知识库）
- `DELETE /api/tasks/{id}` - 删除任务

### 知识库接口（RAG 服务）
- `GET /api/kb` - 获取知识库列表
- `POST /api/kb` - 创建知识库
- `DELETE /api/kb/:id` - 删除知识库
- `GET /api/kb/:id/documents` - 获取文档列表
- `POST /api/kb/:id/documents` - 上传文档
- `DELETE /api/kb/:id/documents/:doc_id` - 删除文档
- `POST /api/kb/:id/query` - 向量检索查询

### Discussion接口
- `POST /api/discussions/tasks/{taskId}/start` - 开始讨论
- `GET /api/discussions/tasks/{taskId}` - 获取讨论状态
- `POST /api/discussions/tasks/{taskId}/pause` - 暂停讨论
- `POST /api/discussions/tasks/{taskId}/resume` - 继续讨论
- `POST /api/discussions/tasks/{taskId}/stop` - 停止讨论
- `POST /api/discussions/tasks/{taskId}/next-round` - 下一轮

### WebSocket
- `ws://localhost:8080/ws/discussion?taskId={taskId}` - 实时消息推送

## 🔮 未来扩展

1. **LLM集成** - 接入真实的OpenAI/Claude/文心等API
2. **群体讨论** - 多个用户带Agent参与同一场讨论
3. **方案追踪** - 用户选择方案后定期跟进执行进度

## 📄 License

MIT License
