# CareerMind RAG 服务设计文档

## 1. 项目概述

### 1.1 目标
为 CareerMind 多 Agent 职业咨询系统增加 RAG（检索增强生成）能力，支持用户上传 PDF、Word、Markdown 等文档构建知识库，在咨询时自动检索相关知识并注入 AI 讨论流程。

### 1.2 架构决策
采用**独立 Rust 服务（方案 C）**：
- 保持现有 Java 后端稳定运行
- 新增 Rust RAG 服务处理文档解析、向量嵌入、语义检索
- Java 通过 HTTP API 调用 Rust 服务获取检索结果

### 1.3 技术选型

| 组件 | 选型 | 版本 |
|------|------|------|
| 语言 | Rust | 1.75+ |
| Web 框架 | Axum | 0.7+ |
| 数据库 | MySQL (与 Java 共用) | 8.0 |
| ORM | sqlx | 0.7+ |
| 向量库 | Qdrant | 1.7+ |
| 嵌入模型 | Gemini Embeddings API | gemini-embedding-001 |
| PDF 解析 | pdf-extract / lopdf | - |
| Word 解析 | docx-rs | 0.7+ |
| Markdown | pulldown-cmark | 0.9+ |
| HTTP 客户端 | reqwest | 0.11+ |

---

## 2. 数据模型

### 2.1 MySQL 表结构（与 Java 共用数据库）

```sql
-- 知识库表
CREATE TABLE knowledge_bases (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    kb_type ENUM('PERSONAL', 'PUBLIC') NOT NULL DEFAULT 'PERSONAL',
    owner_user_id BIGINT,  -- NULL 表示公共库
    embedding_model VARCHAR(100) DEFAULT 'gemini-embedding-001',
    embedding_dimension INT DEFAULT 768,
    chunk_size INT DEFAULT 512,
    chunk_overlap INT DEFAULT 50,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_kb_type (kb_type),
    INDEX idx_owner (owner_user_id)
);

-- 文档表
CREATE TABLE documents (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    kb_id BIGINT NOT NULL,
    filename VARCHAR(500) NOT NULL,
    file_type ENUM('PDF', 'WORD', 'EXCEL', 'PPT', 'TXT', 'MARKDOWN', 'HTML') NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    status ENUM('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PENDING',
    chunk_count INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (kb_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    INDEX idx_kb_id (kb_id),
    INDEX idx_status (status)
);

-- 文档分块表（元数据，实际向量存 Qdrant）
CREATE TABLE document_chunks (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    kb_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    chunk_index INT NOT NULL,
    vector_id VARCHAR(100) NOT NULL,  -- Qdrant 中的 point ID
    metadata JSON,  -- { "page": 1, "section": "第一章" }
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    FOREIGN KEY (kb_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE,
    INDEX idx_doc_id (document_id),
    INDEX idx_kb_id (kb_id),
    UNIQUE KEY uk_vector_id (vector_id)
);
```

### 2.2 Qdrant Collection 设计

```json
{
  "collection_name": "careermind_kb",
  "vectors": {
    "size": 768,
    "distance": "Cosine"
  },
  "payload_schema": {
    "kb_id": "integer",
    "document_id": "integer",
    "chunk_id": "integer",
    "content": "text",
    "metadata": "object"
  }
}
```

---

## 3. API 接口设计

### 3.1 知识库管理

#### 创建知识库
```http
POST /api/kb
Content-Type: application/json

{
  "name": "中国法律库",
  "description": "包含劳动法、合同法等相关法规",
  "kb_type": "PUBLIC",
  "chunk_size": 512,
  "chunk_overlap": 50
}

Response 201:
{
  "id": 1,
  "name": "中国法律库",
  "kb_type": "PUBLIC",
  "created_at": "2026-04-13T10:00:00Z"
}
```

#### 获取知识库列表
```http
GET /api/kb?type=PUBLIC&page=1&size=20

Response 200:
{
  "items": [
    {
      "id": 1,
      "name": "中国法律库",
      "description": "...",
      "kb_type": "PUBLIC",
      "document_count": 15,
      "chunk_count": 320,
      "created_at": "2026-04-13T10:00:00Z"
    }
  ],
  "total": 1,
  "page": 1,
  "size": 20
}
```

#### 删除知识库
```http
DELETE /api/kb/:id

Response 204
```

### 3.2 文档管理

#### 上传文档
```http
POST /api/kb/:id/documents
Content-Type: multipart/form-data

file: <binary>

Response 201:
{
  "id": 1,
  "filename": "劳动法.pdf",
  "file_type": "PDF",
  "file_size": 2048576,
  "status": "PROCESSING",
  "created_at": "2026-04-13T10:00:00Z"
}
```

#### 获取文档列表
```http
GET /api/kb/:id/documents?page=1&size=20

Response 200:
{
  "items": [
    {
      "id": 1,
      "filename": "劳动法.pdf",
      "file_type": "PDF",
      "file_size": 2048576,
      "status": "COMPLETED",
      "chunk_count": 45,
      "created_at": "2026-04-13T10:00:00Z"
    }
  ],
  "total": 15,
  "page": 1,
  "size": 20
}
```

#### 删除文档
```http
DELETE /api/kb/:id/documents/:doc_id

Response 204
```

### 3.3 检索接口（Java 后端调用）

```http
POST /api/kb/:id/query
Content-Type: application/json

{
  "query": "试用期最长多久",
  "top_k": 5,
  "score_threshold": 0.7
}

Response 200:
{
  "query": "试用期最长多久",
  "results": [
    {
      "content": "根据《劳动合同法》第十九条规定，劳动合同期限三个月以上不满一年的，试用期不得超过一个月；劳动合同期限一年以上不满三年的，试用期不得超过二个月；三年以上固定期限和无固定期限的劳动合同，试用期不得超过六个月。",
      "score": 0.92,
      "document": {
        "id": 1,
        "filename": "劳动合同法.pdf",
        "file_type": "PDF"
      },
      "metadata": {
        "page": 12,
        "section": "第三章 劳动合同的履行和变更"
      }
    }
  ]
}
```

### 3.4 健康检查

```http
GET /health

Response 200:
{
  "status": "healthy",
  "version": "0.1.0",
  "services": {
    "database": "connected",
    "qdrant": "connected"
  }
}
```

---

## 4. 核心流程

### 4.1 文档处理流程（同步）

```
用户上传文档
      ↓
[1] 保存文件到本地存储
      ↓
[2] 根据文件类型选择解析器
      ↓
[3] 提取纯文本内容
      ↓
[4] 文本分块（按 chunk_size + overlap）
      ↓
[5] 调用 Gemini API 获取嵌入向量
      ↓
[6] 存入 Qdrant（向量 + payload）
      ↓
[7] 保存 chunk 元数据到 MySQL
      ↓
返回处理结果
```

### 4.2 检索流程

```
Java 发起检索请求
      ↓
[1] 调用 Gemini API 嵌入查询文本（task_type=RETRIEVAL_QUERY）
      ↓
[2] Qdrant 向量相似度检索（filter by kb_id）
      ↓
[3] 按 score_threshold 过滤结果
      ↓
[4] 从 MySQL 获取文档元数据
      ↓
返回 Top-K 结果给 Java
```

---

## 5. Rust 项目结构

```
careermind-rag/
├── Cargo.toml
├── .env.example
├── Dockerfile
├── README.md
├── sql/
│   └── migrations/           # 数据库迁移文件
│       ├── 001_init.sql
│       └── 002_add_indexes.sql
└── src/
    ├── main.rs               # 入口：初始化配置、启动服务
    ├── config.rs             # 配置结构体，从环境变量读取
    ├── error.rs              # 全局错误类型定义
    ├── db/
    │   ├── mod.rs
    │   ├── pool.rs           # 数据库连接池
    │   └── models.rs         # sqlx 模型定义
    ├── qdrant/
    │   ├── mod.rs
    │   └── client.rs         # Qdrant 客户端封装
    ├── embedding/
    │   ├── mod.rs
    │   └── gemini.rs         # Gemini Embeddings 客户端
    ├── parser/
    │   ├── mod.rs            # 解析器 trait
    │   ├── pdf.rs            # PDF 解析
    │   ├── word.rs           # Word 解析
    │   ├── markdown.rs       # Markdown 解析
    │   ├── html.rs           # HTML 解析
    │   └── text.rs           # 纯文本解析
    ├── chunking/
    │   ├── mod.rs
    │   └── text.rs           # 文本分块算法
    ├── routes/
    │   ├── mod.rs            # 路由聚合
    │   ├── kb.rs             # 知识库路由
    │   ├── document.rs       # 文档路由
    │   └── health.rs         # 健康检查
    ├── services/
    │   ├── mod.rs
    │   ├── kb_service.rs     # 知识库业务逻辑
    │   ├── doc_service.rs    # 文档业务逻辑
    │   └── retrieve_service.rs # 检索业务逻辑
    └── middleware/
        └── auth.rs           # 可选：API Key 验证
```

---

## 6. 与 Java 后端集成

### 6.1 Java 侧新增代码

```java
// KnowledgeBaseClient.java - 调用 Rust RAG 服务
@Service
public class KnowledgeBaseClient {
    @Value("${rag.service.url:http://localhost:3000}")
    private String ragServiceUrl;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    public List<RetrievalResult> retrieve(Long kbId, String query, int topK) {
        String url = ragServiceUrl + "/api/kb/" + kbId + "/query";
        
        Map<String, Object> request = Map.of(
            "query", query,
            "top_k", topK,
            "score_threshold", 0.7
        );
        
        ResponseEntity<RetrieveResponse> response = restTemplate.postForEntity(
            url, request, RetrieveResponse.class
        );
        
        return response.getBody().getResults();
    }
}

// DiscussionEngineImpl.java - 修改 Prompt 构建
@Component
public class DiscussionEngineImpl implements DiscussionEngine {
    
    @Autowired
    private KnowledgeBaseClient kbClient;
    
    private String buildPromptWithRAG(Agent agent, Task task, String userMessage) {
        StringBuilder prompt = new StringBuilder();
        
        // 如果有绑定知识库，先检索
        if (task.getKnowledgeBaseId() != null) {
            List<RetrievalResult> contexts = kbClient.retrieve(
                task.getKnowledgeBaseId(), 
                userMessage, 
                5
            );
            
            if (!contexts.isEmpty()) {
                prompt.append("=== 参考资料 ===\n");
                for (RetrievalResult ctx : contexts) {
                    prompt.append(String.format(
                        "[来自: %s]\n%s\n\n",
                        ctx.getDocument().getFilename(),
                        ctx.getContent()
                    ));
                }
            }
        }
        
        // 原有 Prompt 构建逻辑...
        prompt.append("=== 用户信息 ===\n");
        prompt.append("背景：").append(task.getBackground()).append("\n");
        // ...
        
        return prompt.toString();
    }
}
```

### 6.2 Task 表扩展

```sql
-- 给 Task 表添加知识库关联字段
ALTER TABLE tasks ADD COLUMN knowledge_base_id BIGINT NULL;
ALTER TABLE tasks ADD FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id);
```

---

## 7. 部署配置

### 7.1 环境变量

```bash
# .env
# Server
RAG_PORT=3000
RAG_HOST=0.0.0.0

# Database (与 Java 共用)
DATABASE_URL=mysql://careermind:password@localhost:3306/careermind

# Qdrant
QDRANT_URL=http://localhost:6333
QDRANT_COLLECTION=careermind_kb

# Gemini API
GEMINI_API_KEY=your-gemini-api-key
GEMINI_MODEL=gemini-embedding-001
GEMINI_DIMENSION=768

# File Storage
UPLOAD_DIR=./uploads
MAX_FILE_SIZE=104857600  # 100MB
```

### 7.2 Docker Compose

```yaml
version: '3.8'

services:
  qdrant:
    image: qdrant/qdrant:v1.7.4
    ports:
      - "6333:6333"
      - "6334:6334"
    volumes:
      - qdrant_storage:/qdrant/storage

  rag-service:
    build: ./careermind-rag
    ports:
      - "3000:3000"
    environment:
      - DATABASE_URL=mysql://careermind:password@mysql:3306/careermind
      - QDRANT_URL=http://qdrant:6333
      - GEMINI_API_KEY=${GEMINI_API_KEY}
    volumes:
      - rag_uploads:/app/uploads
    depends_on:
      - qdrant
      - mysql

volumes:
  qdrant_storage:
  rag_uploads:
```

---

## 8. 错误处理

### 8.1 HTTP 状态码

| 状态码 | 场景 |
|--------|------|
| 200 | 成功 |
| 201 | 创建成功 |
| 204 | 删除成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 413 | 文件过大 |
| 415 | 不支持的文件类型 |
| 422 | 文档解析失败 |
| 500 | 服务器内部错误 |
| 503 | 依赖服务不可用（Qdrant/Gemini） |

### 8.2 错误响应格式

```json
{
  "error": {
    "code": "DOCUMENT_PARSE_FAILED",
    "message": "无法解析 PDF 文件，可能文件已损坏",
    "details": {
      "document_id": 123,
      "filename": "损坏的文件.pdf"
    }
  }
}
```

---

## 9. 性能指标

| 指标 | 目标 |
|------|------|
| 文档上传处理 | < 5 秒（< 10MB 文件）|
| 检索响应时间 | < 500ms（P95）|
| 向量检索延迟 | < 100ms |
| 并发处理 | 支持 50 个并发上传 |

---

## 10. 后续优化方向

1. **异步处理**：Redis 队列处理大文件
2. **OCR 支持**：扫描版 PDF 识别
3. **增量更新**：文档更新时只处理变更部分
4. **混合检索**：向量检索 + 关键词检索融合
5. **重排序**：使用 Cross-Encoder 优化检索结果排序
6. **缓存**：热门查询结果缓存

---

**设计确认日期**: 2026-04-13
**设计版本**: v1.0
