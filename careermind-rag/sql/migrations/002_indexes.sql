-- 已有索引在 001_init.sql 中创建
-- 这里添加额外的优化索引

-- 知识库名称搜索
CREATE INDEX idx_kb_name ON knowledge_bases(name);

-- 文档文件名搜索
CREATE INDEX idx_doc_filename ON documents(filename);

-- 按状态和时间查询文档
CREATE INDEX idx_doc_status_created ON documents(status, created_at);
