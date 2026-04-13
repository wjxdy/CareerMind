# CareerMind RAG Service

Rust 实现的 RAG（检索增强生成）服务，支持 PDF、Word、Markdown 等文档的向量化存储和语义检索。

## 功能特性

- 支持 PDF、Word、Markdown、HTML、TXT 格式
- 基于 Gemini Embedding 的语义检索
- Qdrant 向量数据库存储
- 同步处理，快速响应
- 个人/公共知识库分离

## 快速开始

### 1. 环境要求

- Rust 1.75+
- MySQL 8.0
- Qdrant

### 2. 配置

复制 `.env.example` 为 `.env`，填写配置：

```bash
DATABASE_URL=mysql://user:pass@localhost/careermind
GEMINI_API_KEY=your-api-key
```

### 3. 运行

```bash
# 本地运行
cargo run

# Docker 运行
docker-compose up -d
```

## API 文档

### 知识库管理

- `POST /api/kb` - 创建知识库
- `GET /api/kb` - 列表查询
- `DELETE /api/kb/:id` - 删除知识库

### 文档管理

- `POST /api/kb/:id/documents` - 上传文档
- `GET /api/kb/:id/documents` - 文档列表
- `DELETE /api/kb/:id/documents/:doc_id` - 删除文档

### 检索

- `POST /api/kb/:id/query` - 语义检索

## 与 Java 后端集成

Java 服务通过 HTTP 调用 RAG 服务的检索接口，获取相关知识后注入 LLM Prompt。
