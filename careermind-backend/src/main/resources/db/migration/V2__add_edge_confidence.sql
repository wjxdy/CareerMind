-- P2 辩论可视化所需字段
-- 注意：项目当前使用 spring.jpa.hibernate.ddl-auto=update，JPA 自动会添加这些列；
-- 此 SQL 仅作为生产部署/手动迁移的参考。

ALTER TABLE messages
  ADD COLUMN edge_type VARCHAR(16) NULL COMMENT 'SUPPORT/CHALLENGE/REVISE/NONE',
  ADD COLUMN confidence DECIMAL(3,2) NULL COMMENT '0.00-1.00';

ALTER TABLE rounds
  ADD COLUMN divergence DECIMAL(3,2) NULL COMMENT '整轮分歧度 0.00-1.00';

CREATE INDEX idx_messages_edge_type ON messages(edge_type);
